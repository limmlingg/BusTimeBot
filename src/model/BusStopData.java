package model;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;

import datastructures.kdtree.DistanceFunction;
import datastructures.kdtree.KdTree;
import datastructures.kdtree.NearestNeighborIterator;
import logic.controller.NtuController;
import logic.controller.NusController;
import logic.controller.PublicController;

public class BusStopData {
    public static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(BusStopData.class);

    public static final int estimatedNumberOfBusStops = 5000;

    public KdTree<BusStop> busStopsSortedByCoordinates;
    public HashMap<String, BusStop> busStops;

    public BusStopData() {
        busStopsSortedByCoordinates = new KdTree<BusStop>(2, estimatedNumberOfBusStops * 2);
        busStops = new HashMap<String, BusStop>(estimatedNumberOfBusStops * 2);
    }

    public NearestNeighborIterator<BusStop> getNearestNeighbours(double latitude, double longitude, int numberOfStops, DistanceFunction distanceFunction) {
        double[] searchPoint = {latitude, longitude};
        return busStopsSortedByCoordinates.getNearestNeighborIterator(searchPoint, numberOfStops, distanceFunction);
    }

    public BusStop getSpecificBusStop(String stopCode) {
        return busStops.get(stopCode);
    }

    /**
     * Retrieve Bus stop data from NUS & LTA
     */
    public void getBusStopData(boolean useDatabase) {
        logger.info("Retrieving Public Bus Stop Data");
        PublicController.getPublicBusStopData(busStops);

        logger.info("Retrieving NUS Bus Stop Data");
        NusController.getNUSBusStopData(busStops);

        logger.info("Retrieving NTU Bus Stop Data");
        NtuController.getNTUBusStopData(busStops);

        logger.info("Populating KD-tree");
        for (BusStop stop : busStops.values()) {
            double[] point = new double[2];
            point[0] = stop.Latitude;
            point[1] = stop.Longitude;
            busStopsSortedByCoordinates.addPoint(point, stop);
        }
        logger.info("All bus stop data loaded!");
    }

}
