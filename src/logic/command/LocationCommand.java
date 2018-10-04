package logic.command;

import java.util.ArrayList;
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
import model.busarrival.BusStopArrivalCache;
import model.busarrival.BusStopArrivalContainer;

/**
 * A command that returns bus times given a latitude and longitude
 */
public class LocationCommand extends Command {
    public static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(LocationCommand.class);

    public static final int DEFAULT_NUMBER_OF_STOPS = 5;
    private static final String WAB_TOOLTIP = "\\* _Wheelchair Accessible_";

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


            for (BusStop stop : busstops) {
                BusStopArrival busStopArrival;

                //Check if current bus stop is in cache
                busStopArrival = BusStopArrivalCache.cache.getOrDefault(stop.BusStopCode, new BusStopArrival());
                if (busStopArrival.isOutdated()) { //cache miss or outdated
                    busStopArrival = new BusStopArrival();
                    busStopArrival.busStop = stop;
                    busStopArrival.requestedTime = System.currentTimeMillis();
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
                }
                //If there exist a bus for the bus stop, then we append, otherwise that bus stop has no buses (so we ignore)
                if (busStopArrival != null && busStopArrival.busArrivals.size() != 0) {
                    allStops.busStopArrivals.add(busStopArrival);
                    //Populate cache for this bus stop
                    BusStopArrivalCache.cache.put(busStopArrival.busStop.BusStopCode, busStopArrival);
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
