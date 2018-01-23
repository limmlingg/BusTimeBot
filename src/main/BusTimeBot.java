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
    public static int estimatedNumberOfBusStops = 5000;


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
        boolean useDatabase = propertiesLoader.getUseDatabase();
        getBusStopData(useDatabase);
    }

    /**
     * Retrieve Bus stop data from NUS & LTA
     */
    private void getBusStopData(boolean useDatabase) {
        if (!useDatabase) {
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

            System.out.println("Saving to database");
            saveBusStops(busStops);
        } else {
            System.out.println("Loading bus stops from database");
            boolean success = loadBusStops();
            //If unable to load database, use online method instead
            if (!success) {
                getBusStopData(false);
                return;
            }
        }

        System.out.println("All bus stop data loaded!");
    }

    private boolean loadBusStops() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        boolean isSuccess = false;
        try {
            connection = DatabaseController.getConnection();
            connection.setAutoCommit(false);
            statement = connection.prepareStatement("SELECT bus_stop_code, description, latitude, longitude, is_public, is_nus, is_ntu, nus_stop_code, nus_description, ntu_stop_code, ntu_description FROM bus_stop;");
            resultSet = statement.executeQuery();

            int rows = 0;

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

                rows++;
            }

            if (rows <= estimatedNumberOfBusStops) {
                //main.db file exists but no tables/rows exists
                busStopsSortedByCoordinates = new KdTree<BusStop>(2, 100);
                throw new Exception("Bus stop data incomplete");
            }

            isSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //Attempt to close resultSet, statement and connection
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return isSuccess;
    }

    private void saveBusStops(HashMap<String, BusStop> busStops) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DatabaseController.getConnection();
            connection.setAutoCommit(false);

            //Delete old bus stops (since we want to save a new copy)
            statement = connection.prepareStatement("DELETE FROM bus_stop;");
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
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }  finally {
            //Attempt to close statement and connection
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
