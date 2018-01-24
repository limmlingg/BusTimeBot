package model.busarrival;

import java.util.ArrayList;

public class BusStopArrivalContainer {
    public ArrayList<BusStopArrival> busStopArrivals;
    public long requestedTime;

    public BusStopArrivalContainer() {
        busStopArrivals = new ArrayList<BusStopArrival>();
    }
}
