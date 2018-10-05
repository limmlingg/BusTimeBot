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
     * Returns true if
     *      There is bus timing since the requestedTime which becomes -1, meaning we need to request for new timings
     *      All timings for that bus stop are N/A
     */
    public boolean isOutdated() {
        boolean allNA = true;
        long timeDiff = (System.currentTimeMillis() - requestedTime) / 1000 / 60; // in minutes

        for (BusArrival busArrival : busArrivals) {
            allNA &= (busArrival.arrivalTime1 == BusArrival.TIME_NA);

            if (busArrival.arrivalTime1 - timeDiff < 0) {
                return true;
            } else {
                //Only update the timings when it is not N/A
                if (busArrival.arrivalTime1 != BusArrival.TIME_NA) {
                    busArrival.arrivalTime1 -= timeDiff;
                }

                if (busArrival.arrivalTime2 != BusArrival.TIME_NA) {
                    busArrival.arrivalTime2 -= timeDiff;
                }
            }
        }

        //Update the requested time if timeDiff > 0 since we have deducted the time based on the last check
        if (timeDiff > 0) {
            requestedTime = System.currentTimeMillis();
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
