package main;

import logic.PropertiesLoader;
import model.BusStopData;
import model.busarrival.BusStopArrivalCache;

public class BusTimeBot {
    //Instance of BusTimeBot
    public static BusTimeBot instance = null;
    public static String LTA_TOKEN;

    public double maxDistanceFromPoint = 0.35;
    public BusStopData data;

    public static BusTimeBot getInstance() {
        if (instance == null) {
            instance = new BusTimeBot();
        }
        return instance;
    }

    private BusTimeBot() {
        data = new BusStopData();

        //Load LTA Token here
        PropertiesLoader propertiesLoader = new PropertiesLoader();
        LTA_TOKEN = propertiesLoader.getLtaToken();

        //Initialize cache
        BusStopArrivalCache.initialize();

        //Initialize bus stop data
        boolean useDatabase = propertiesLoader.getUseDatabase();
        data.getBusStopData(useDatabase);
    }

}
