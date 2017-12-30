package main;

import java.util.HashMap;

import datastructures.kdtree.KdTree;
import logic.PropertiesLoader;
import logic.controller.NtuController;
import logic.controller.NusController;
import logic.controller.PublicController;
import model.json.BusStop;

public class BusTimeBot {
    //Instance of BusTimeBot
    public static BusTimeBot instance = null;

    public static String LTA_TOKEN;

    public KdTree<BusStop> busStopsSortedByCoordinates;
    public double maxDistanceFromPoint = 0.35;


    public static BusTimeBot getInstance() {
        if (instance == null) {
            instance = new BusTimeBot();
        }
        return instance;
    }

    private BusTimeBot() {
        busStopsSortedByCoordinates = new KdTree<BusStop>(2, 100);

        //Load LTA Token here
        PropertiesLoader propertiesLoader = new PropertiesLoader();
        LTA_TOKEN = propertiesLoader.getLtaToken();

        //Initialize bus stop data
        getBusStopData();
    }

    /**
     * Retrieve Bus stop data from NUS & LTA
     */
    private void getBusStopData() {
        HashMap<String, BusStop> busStops = new HashMap<String, BusStop>(10000);;
        System.out.println("Retrieving Public Bus Stop Data");
        PublicController.getPublicBusStopData(busStops);
        System.out.println("Retrieving NUS Bus Stop Data");
        NusController.getNUSBusStopData(busStops);
        System.out.println("Retrieving NTU Bus Stop Data");
        NtuController.getNTUBusStopData(busStops);
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
