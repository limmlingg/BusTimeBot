package logic.command;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import datastructures.kdtree.NearestNeighborIterator;
import logic.LocationDistanceFunction;
import logic.Util;
import logic.controller.NtuController;
import logic.controller.NusController;
import logic.controller.PublicController;
import logic.gateway.TelegramGateway;
import main.BusTimeBot;
import main.Logger;
import model.BusStop;
import model.CommandResponse;
import model.busarrival.BusStopArrival;
import model.busarrival.BusStopArrivalContainer;

/**
 * A command that returns bus times given a latitude and longitude
 */
public class LocationCommand extends Command {
    private static final int defaultNumberOfStops = 5;
    private static final int refreshCacheSeconds = 30; //Time before refreshing cache

    private static HashMap<Point2D.Double, BusStopArrivalContainer> cache = new HashMap<Point2D.Double, BusStopArrivalContainer>();

    private double maxDistanceFromPoint = 0.35; //in km
    private int numberOfStopsWanted = defaultNumberOfStops;
    private double latitude;
    private double longitude;

    public LocationCommand(double latitude, double longitude, int numberOfStopsWanted) {
        this(latitude, longitude);
        this.numberOfStopsWanted = numberOfStopsWanted;
    }

    public LocationCommand(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public CommandResponse execute() {
        try {
            BusStopArrivalContainer allStops = new BusStopArrivalContainer();

            //Build cache key
            Point2D.Double point = new Point2D.Double(latitude, longitude);
            //Check cache if within 1 minute
            BusStopArrivalContainer cachedContainer = cache.get(point);
            long differenceInSeconds = Long.MAX_VALUE;
            if (cachedContainer != null) {
                differenceInSeconds = (System.currentTimeMillis() - cachedContainer.requestedTime) / 1000;
            }

            if (differenceInSeconds < refreshCacheSeconds) {
                allStops = cachedContainer;
            } else {
                Iterator<BusStop> busstops = getNearbyBusStops(latitude, longitude, numberOfStopsWanted);

                while (busstops.hasNext()) {
                    BusStop stop = busstops.next();
                    BusStopArrival busStopArrival = null;
                    //Append the bus times accordingly
                    if (stop.isPublic) {
                        busStopArrival = PublicController.getPublicBusArrivalTimings(stop);
                    }
                    if (stop.isNus) {
                        busStopArrival = NusController.getNUSArrivalTimings(stop);
                    }
                    if (stop.isNtu) {
                        busStopArrival = NtuController.getNTUBusArrivalTimings(stop);
                    }

                    //If there exist a bus for the bus stop, then we append, otherwise that bus stop has no buses (so we ignore)
                    if (busStopArrival != null && busStopArrival.busArrivals.size() != 0) {
                        allStops.busStopArrivals.add(busStopArrival);
                    }
                }

                //Save the requested time (for caching)
                allStops.requestedTime = System.currentTimeMillis();
                //Save object to cache
                cache.put(point, allStops);
            }

            String busArrivalString = "";
            if (allStops != null) {
                busArrivalString = TelegramGateway.formatBusArrival(allStops, numberOfStopsWanted).trim();
            }

            //Build data to return
            HashMap<String, String> data = null;
            if (allStops.busStopArrivals.size() != 0) {
                data = new HashMap<String, String>();
                data.put("latitude", Double.toString(latitude));
                data.put("longitude", Double.toString(longitude));
                data.put("numberOfStopsWanted", Integer.toString(numberOfStopsWanted));
            }

            commandSuccess = true;
            return new CommandResponse(busArrivalString, data);
        } catch (Exception e) {
            Logger.logError(e);
            return null;
        }
    }



    /**
     * Get near by bus stops of a given location
     *
     * @param latitude
     *            of the user
     * @param longitude
     *            of the user
     * @return a list of bus stops near that location sorted by distance
     */
    public Iterator<BusStop> getNearbyBusStops(double latitude, double longitude, int numberOfStops) {
        try {
            ArrayList<BusStop> busstops = new ArrayList<BusStop>();
            double[] searchPoint = {latitude, longitude};
            NearestNeighborIterator<BusStop> result = BusTimeBot.getInstance().busStopsSortedByCoordinates.getNearestNeighborIterator(searchPoint, defaultNumberOfStops, new LocationDistanceFunction());
            Iterator<BusStop> iterator = result.iterator();
            while (iterator.hasNext()) {
                BusStop stop = iterator.next();
                double distance = Util.getDistance(stop.Latitude, stop.Longitude, latitude, longitude);
                if (distance < this.maxDistanceFromPoint) {
                    busstops.add(stop);
                }
            }
            return busstops.iterator();
        } catch (Exception e) {
            Logger.logError(e);
            return null;
        }
    }
}
