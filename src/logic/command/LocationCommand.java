package logic.command;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;

import datastructures.kdtree.NearestNeighborIterator;
import logic.LocationDistanceFunction;
import logic.Util;
import logic.controller.NtuController;
import logic.controller.NusController;
import logic.controller.PublicController;
import logic.gateway.TelegramGateway;
import main.BusTimeBot;
import model.BusStop;
import model.CommandResponse;
import model.CommandResponseType;
import model.busarrival.BusStopArrival;
import model.busarrival.BusStopArrivalContainer;

/**
 * A command that returns bus times given a latitude and longitude
 */
public class LocationCommand extends Command {
    public static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(LocationCommand.class);

    public static final int DEFAULT_NUMBER_OF_STOPS = 5;

    //Caching stuff
    private static final long CACHE_CLEAR_INTERVAL = 1000 * 60 * 60 * 1; //in milliseconds
    private static final int CACHE_REFRESH_INTERVAL = 1000 * 30; //Milliseconds before refreshing cache
    private static final String WAB_TOOLTIP = "\\* _Wheelchair Accessible_";

    private static long CACHE_LAST_CLEARED = 0;
    private static HashMap<String, BusStopArrivalContainer> cache = new HashMap<String, BusStopArrivalContainer>();

    private double maxDistanceFromPoint = 0.35; //in km
    private int numberOfStopsWanted = DEFAULT_NUMBER_OF_STOPS;
    private double latitude;
    private double longitude;
    private String specificBusStopCode;

    public LocationCommand(double latitude, double longitude, int numberOfStopsWanted) {
        this(latitude, longitude);
        this.numberOfStopsWanted = numberOfStopsWanted;
    }

    public LocationCommand(String busStopCode) {
        specificBusStopCode = busStopCode;
    }

    public LocationCommand(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public CommandResponse execute() {
        clearCache();

        try {
            BusStopArrivalContainer allStops = new BusStopArrivalContainer();
            ArrayList<BusStop> busstops;
            if (specificBusStopCode != null) {
                BusStop specificBusStop = BusTimeBot.getInstance().data.getSpecificBusStop(specificBusStopCode);
                busstops = new ArrayList<BusStop>();
                busstops.add(specificBusStop);
            } else {
                busstops = getNearbyBusStops(latitude, longitude, numberOfStopsWanted);
            }

            //Build cache key
            String key = "";
            for (BusStop busStop : busstops) {
                key += busStop.BusStopCode + " : ";
            }

            //Check cache if within 1 minute
            BusStopArrivalContainer cachedContainer = cache.get(key);
            long differenceInMilliseconds = Long.MAX_VALUE;
            if (cachedContainer != null) {
                differenceInMilliseconds = (System.currentTimeMillis() - cachedContainer.requestedTime);
            }

            if (differenceInMilliseconds < CACHE_REFRESH_INTERVAL) {
                allStops = cachedContainer;
            } else {
                for (BusStop stop : busstops) {
                    BusStopArrival busStopArrival = new BusStopArrival();
                    busStopArrival.busStop = stop;
                    //Append the bus times accordingly
                    if (stop.isPublic) {
                        busStopArrival.merge(PublicController.getPublicBusArrivalTimings(stop));
                    }
                    if (stop.isNus) {
                        busStopArrival.merge(NusController.getNUSArrivalTimings(stop));
                    }
                    if (stop.isNtu) {
                        busStopArrival.merge(NtuController.getNTUBusArrivalTimings(stop));
                    }

                    //If there exist a bus for the bus stop, then we append, otherwise that bus stop has no buses (so we ignore)
                    if (busStopArrival != null && busStopArrival.busArrivals.size() != 0) {
                        allStops.busStopArrivals.add(busStopArrival);
                    }
                }

                if (allStops.busStopArrivals != null && allStops.busStopArrivals.size() > 0) {
                    //Save the requested time (for caching)
                    allStops.requestedTime = System.currentTimeMillis();
                    //Save object to cache
                    cache.put(key, allStops);
                }
            }

            String busArrivalString = "";
            if (allStops != null) {
                busArrivalString = TelegramGateway.formatBusArrival(allStops, numberOfStopsWanted).trim() + "\n" + WAB_TOOLTIP;
            }

            //Build data to return
            HashMap<String, String> data = null;
            if (allStops.busStopArrivals.size() != 0) {
                data = new HashMap<String, String>();
                data.put("latitude", Double.toString(latitude));
                data.put("longitude", Double.toString(longitude));
                data.put("numberOfStopsWanted", Integer.toString(numberOfStopsWanted));
                data.put("searchTerm", specificBusStopCode);
            }

            commandSuccess = true;
            return new CommandResponse(busArrivalString, data, CommandResponseType.LOCATION);
        } catch (Exception e) {
            logger.warn("Exception occurred at execute()", e);
            return null;
        }
    }

    /** Returns true if the current time has exceeded the cache clear interval */
    private boolean isClearCacheTime() {
        return (System.currentTimeMillis() - CACHE_LAST_CLEARED) >= CACHE_CLEAR_INTERVAL;
    }

    /** Clears the cache to save memory space */
    private void clearCache() {
        if (isClearCacheTime()) {
            logger.info("Time to clear cache at " + new Date());
            cache.clear();
            CACHE_LAST_CLEARED = System.currentTimeMillis();
        }
    }

    /**
     * Get near by bus stops of a given location
     *
     * @param latitude of the user
     * @param longitude of the user
     * @return a list of bus stops near that location sorted by distance
     */
    public ArrayList<BusStop> getNearbyBusStops(double latitude, double longitude, int numberOfStops) {
        try {
            ArrayList<BusStop> busstops = new ArrayList<BusStop>();
            NearestNeighborIterator<BusStop> result = BusTimeBot.getInstance().data.getNearestNeighbours(latitude, longitude, DEFAULT_NUMBER_OF_STOPS, new LocationDistanceFunction());
            Iterator<BusStop> iterator = result.iterator();
            while (iterator.hasNext()) {
                BusStop stop = iterator.next();
                double distance = Util.getDistance(stop.Latitude, stop.Longitude, latitude, longitude);
                if (distance < this.maxDistanceFromPoint) {
                    busstops.add(stop);
                }
            }
            return busstops;
        } catch (Exception e) {
            logger.warn("Exception occurred at getNearbyBusStops with latitude={}, longitude={}, numberOfStops={}", latitude, longitude, numberOfStops, e);
            return null;
        }
    }
}
