package model;

import java.util.ArrayList;
import java.util.Arrays;

/** Responsible for holding information on first & last bus timings of a certain bus */
public class BusInfo {
    public String serviceNo;
    public boolean isValidServiceNo;
    public ArrayList<BusInfoDirection> busInfoDirections;

    public BusInfo() {
        isValidServiceNo = false;
        this.busInfoDirections = new ArrayList<BusInfoDirection>();
    }

    public BusInfo(String serviceNo, boolean isValidServiceNo, BusInfoDirection... busInfoDirections) {
        super();
        this.serviceNo = serviceNo;
        this.isValidServiceNo = isValidServiceNo;
        addBusInfoDirection(busInfoDirections);
    }

    public void addBusInfoDirection(BusInfoDirection... busInfoDirections) {
        this.busInfoDirections.addAll(Arrays.asList(busInfoDirections));
    }
}
