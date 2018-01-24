package logic.command;

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
    private double maxDistanceFromPoint = 0.35; //in km
    private int numberOfStops = 5;
    private double latitude;
    private double longitude;

    public LocationCommand(double latitude, double longitude, int numberOfStopsWanted) {
        this(latitude, longitude);
        numberOfStops = numberOfStopsWanted;
    }

    public LocationCommand(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public CommandResponse execute() {
        try {
            Iterator<BusStop> busstops = getNearbyBusStops(latitude, longitude, numberOfStops);
            BusStopArrivalContainer allStops = new BusStopArrivalContainer();

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

            String busArrivalString = "";
            if (allStops != null) {
                busArrivalString = TelegramGateway.formatBusArrival(allStops).trim();
            }

            //Build data to return
            HashMap<String, String> data = null;
            if (allStops.busStopArrivals.size() != 0) {
                data = new HashMap<String, String>();
                data.put("latitude", Double.toString(latitude));
                data.put("longitude", Double.toString(longitude));
            }

            //Save the requested time (for caching)
            allStops.requestedTime = System.currentTimeMillis();

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
            NearestNeighborIterator<BusStop> result = BusTimeBot.getInstance().busStopsSortedByCoordinates.getNearestNeighborIterator(searchPoint, numberOfStops, new LocationDistanceFunction());
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
