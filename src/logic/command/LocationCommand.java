package logic.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import datastructures.kdtree.NearestNeighborIterator;
import logic.LocationDistanceFunction;
import logic.Util;
import logic.controller.NtuController;
import logic.controller.NusController;
import logic.controller.PublicController;
import main.BusTimeBot;
import main.Logger;
import model.BusStop;
import model.CommandResponse;

/**
 * A command that returns bus times given a latitude and longitude
 */
public class LocationCommand implements Command {
    private double maxDistanceFromPoint = 0.35; //in km
    private double latitude;
    private double longitude;

    //Emoji alias
    private static final String EMOJI_BUSSTOP = "busstop";
    private static final String EMOJI_ONCOMING_BUS = "oncoming_bus";

    public LocationCommand(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public CommandResponse execute() {
        try {
            Iterator<BusStop> busstops = getNearbyBusStops(latitude, longitude);
            StringBuilder allStops = new StringBuilder();
            while (busstops.hasNext()) {
                BusStop stop = busstops.next();

                StringBuilder stops = new StringBuilder();

                //Build the header for the bus stop
                stops.append(buildBusStopHeader(stop));

                //Append the bus times accordingly
                stops.append("\n```\n"); //For fixed-width formatting
                if (stop.isPublic) {
                    stops.append(PublicController.getPublicBusArrivalTimings(stop));
                }
                if (stop.isNus) {
                    stops.append(NusController.getNUSArrivalTimings(stop));
                }
                if (stop.isNtu) {
                    stops.append(NtuController.getNTUBusArrivalTimings(stop));
                }
                stops.append("```"); //End fixed-width formatting

                //If there exist an oncoming_bus emoji, then we append, otherwise that bus stop has no buses (so we ignore)
                Emoji emoji = EmojiManager.getForAlias(EMOJI_ONCOMING_BUS);
                if (stops.toString().contains(emoji.getUnicode())) {
                    allStops.append(stops.toString());
                    allStops.append("\n");
                }
            }

            if (allStops.length() == 0) {
                allStops.append("No stops nearby");
            }

            //Build data to return
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("latitude", Double.toString(latitude));
            data.put("longitude", Double.toString(longitude));

            return new CommandResponse(allStops.toString().trim(), data);
        } catch (Exception e) {
            Logger.logError(e);
            return null;
        }
    }

    /**
     * Builds the bus stop header accordingly
     */
    private StringBuilder buildBusStopHeader(BusStop stop) {
        Emoji emoji = EmojiManager.getForAlias(EMOJI_BUSSTOP);
        StringBuilder stopHeader = new StringBuilder();
        stopHeader.append(emoji.getUnicode());
        stopHeader.append("*");
        stopHeader.append("" + stop.BusStopCode + " - ");
        stopHeader.append(stop.Description);
        if (stop.isPublic && stop.isNtu) {
            stopHeader.append("/");
            stopHeader.append(stop.ntuDescription);
        }
        if (stop.isPublic && stop.isNus) {
            stopHeader.append("/");
            stopHeader.append(stop.nusDescription);
        }
        stopHeader.append("*");
        return stopHeader;
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
    public Iterator<BusStop> getNearbyBusStops(double latitude, double longitude) {
        try {
            ArrayList<BusStop> busstops = new ArrayList<BusStop>();
            double[] searchPoint = {latitude, longitude};
            NearestNeighborIterator<BusStop> result = BusTimeBot.bot.busStopsSortedByCoordinates.getNearestNeighborIterator(searchPoint, 5, new LocationDistanceFunction());
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
