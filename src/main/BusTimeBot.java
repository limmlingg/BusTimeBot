package main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import datastructures.kdtree.KdTree;
import logic.PropertiesLoader;
import logic.controller.DatabaseController;
import logic.controller.NtuController;
import logic.controller.NusController;
import logic.controller.PublicController;
import model.BusStop;

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
        boolean online = false;
        boolean saveToDatabase = false;
        getBusStopData(online, saveToDatabase);
    }

    /**
     * Retrieve Bus stop data from NUS & LTA
     */
    private void getBusStopData(boolean isOnline, boolean saveToDatabase) {
        if (isOnline) {
            HashMap<String, BusStop> busStops = new HashMap<String, BusStop>(10000);
            System.out.println("Retrieving Public Bus Stop Data");
            PublicController.getPublicBusStopData(busStops);

            System.out.println("Retrieving NUS Bus Stop Data");
            NusController.getNUSBusStopData(busStops);

            System.out.println("Retrieving NTU Bus Stop Data");
            NtuController.getNTUBusStopData(busStops);

            System.out.println("Populating KD-tree");
            for (BusStop stop : busStops.values()) {
                double[] point = new double[2];
                point[0] = stop.Latitude;
                point[1] = stop.Longitude;
                busStopsSortedByCoordinates.addPoint(point, stop);
            }

            if (saveToDatabase) {
                System.out.println("Saving to database");
                saveBusStops(busStops);
            }
        } else {
            System.out.println("Loading bus stops from database");
            loadBusStops();
        }

        System.out.println("All bus stop data loaded!");
    }

    private void loadBusStops() {
        try {
            Connection connection = DatabaseController.getConnection();
            connection.setAutoCommit(false);
            PreparedStatement statement = connection.prepareStatement("SELECT bus_stop_code, description, latitude, longitude, is_public, is_nus, is_ntu, nus_stop_code, nus_description, ntu_stop_code, ntu_description FROM bus_stop;");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                //Build and set the parameters
                BusStop stop = new BusStop();
                stop.BusStopCode = resultSet.getString("bus_stop_code");
                stop.Description = resultSet.getString("description");
                stop.Latitude = resultSet.getDouble("latitude");
                stop.Longitude = resultSet.getDouble("longitude");
                stop.isPublic = resultSet.getBoolean("is_public");
                stop.isNus = resultSet.getBoolean("is_nus");
                stop.isNtu = resultSet.getBoolean("is_ntu");
                stop.nusStopCode = resultSet.getString("nus_stop_code");
                stop.nusDescription = resultSet.getString("nus_description");
                stop.ntuStopCode = resultSet.getString("ntu_stop_code");
                stop.ntuDescription = resultSet.getString("ntu_description");

                //Add to KD-tree
                double[] point = new double[2];
                point[0] = stop.Latitude;
                point[1] = stop.Longitude;
                busStopsSortedByCoordinates.addPoint(point, stop);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveBusStops(HashMap<String, BusStop> busStops) {
        try {
            Connection connection = DatabaseController.getConnection();
            connection.setAutoCommit(false);

            //Delete old bus stops (since we want to save a new copy)
            PreparedStatement statement = connection.prepareStatement("DELETE FROM bus_stop;");
            statement.executeUpdate();
            statement.close();

            statement = connection.prepareStatement("INSERT INTO bus_stop VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
            for (BusStop stop : busStops.values()) {
                statement.setString(1, stop.BusStopCode);
                statement.setString(2, stop.Description);
                statement.setDouble(3, stop.Latitude);
                statement.setDouble(4, stop.Longitude);
                statement.setBoolean(5, stop.isPublic);
                statement.setBoolean(6, stop.isNus);
                statement.setBoolean(7, stop.isNtu);
                statement.setString(8, stop.nusStopCode);
                statement.setString(9, stop.nusDescription);
                statement.setString(10, stop.ntuStopCode);
                statement.setString(11, stop.ntuDescription);
                statement.executeUpdate();
            }
            statement.close();
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
