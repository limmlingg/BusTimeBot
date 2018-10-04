package model.busarrival;

import java.util.ArrayList;

import model.BusStop;

public class BusStopArrival {
    public BusStop busStop;
    public ArrayList<BusArrival> busArrivals;
    public long requestedTime;

    public BusStopArrival() {
        busArrivals = new ArrayList<BusArrival>();
    }

    public void merge(BusStopArrival other) {
        if (other != null){
            busArrivals.addAll(other.busArrivals);
        }
    }
}
