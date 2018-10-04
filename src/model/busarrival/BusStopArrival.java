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

    /**
     * Returns the estimated time, in milliseconds, to the next bus that is going to arrive.
     */
    public boolean isOutdated() {
        boolean allNA = true;
        long timeDiff = (System.currentTimeMillis() - requestedTime) / 1000 / 60; // in minutes

        for (BusArrival busArrival : busArrivals) {
            allNA &= (busArrival.arrivalTime1 == BusArrival.TIME_NA);

            if (busArrival.arrivalTime1 - timeDiff < 0) {
                return true;
            } else {
                busArrival.arrivalTime1 -= timeDiff;
                if (busArrival.arrivalTime2 != BusArrival.TIME_NA) {
                    busArrival.arrivalTime2 -= timeDiff;
                }
            }
        }

        return allNA;
    }

    /**
     * Merges another set of busArrivals to the current busArrivals
     */
    public void merge(BusStopArrival other) {
        if (other != null){
            busArrivals.addAll(other.busArrivals);
        }
    }
}
