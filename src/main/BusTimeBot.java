package main;

import java.util.HashMap;

import datastructures.kdtree.KdTree;
import logic.controller.NtuController;
import logic.controller.NusController;
import logic.controller.PublicController;
import model.BusStop;

public class BusTimeBot {
    //Instance of BusTimeBot
    public static BusTimeBot instance = null;

    public HashMap<String, BusStop> busStops;
    public KdTree<BusStop> busStopsSortedByCoordinates;
    public double maxDistanceFromPoint = 0.35;

    public static BusTimeBot getInstance() {
        if (instance == null) {
            instance = new BusTimeBot();
        }
        return instance;
    }

    private BusTimeBot() {
        busStops = new HashMap<String, BusStop>(10000);
        busStopsSortedByCoordinates = new KdTree<BusStop>(2, 100);

        //Initialize bus stop data (we run it this way to prevent the api from registering the bot before it is fully initialized)
        getBusStopData();
    }

    /**
     * Retrieve Bus stop data from NUS & LTA
     */
    private void getBusStopData() {
        System.out.println("Retrieving Public Bus Stop Data");
        PublicController.getPublicBusStopData();
        System.out.println("Retrieving NUS Bus Stop Data");
        NusController.getNUSBusStopData();
        System.out.println("Retrieving NTU Bus Stop Data");
        NtuController.getNTUBusStopData();
        System.out.println("Populating KD-tree");
        //Populate the KD-tree after merging bus stops
        for (BusStop stop : busStops.values()) {
            double[] point = new double[2];
            point[0] = stop.Latitude;
            point[1] = stop.Longitude;
            busStopsSortedByCoordinates.addPoint(point, stop);
        }
        System.out.println("All bus stop data loaded!");
    }
}
