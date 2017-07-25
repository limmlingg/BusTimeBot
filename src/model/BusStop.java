package model;

import org.telegram.telegrambots.api.objects.Location;

import logic.Util;

public class BusStop {
    public enum Type {
        NUS_ONLY, NTU_ONLY, PUBLIC_ONLY, PUBLIC_NUS, PUBLIC_NTU;
    }

    public Type type;
    public String BusStopCode;
    public String RoadName;
    public String Description;
    public double Latitude;
    public double Longitude;
    //If combining both stops (use public's coordinates)
    public String NUSStopCode;
    public String NUSDescription;
    public String NTUStopCode;
    public String NTUDescription;

    public double getDistance(Location location) {
        return getDistance(location.getLatitude(), location.getLongitude());
    }

    public double getDistance(BusStop busStop) {
        return getDistance(busStop.Latitude, busStop.Longitude);
    }

    public double getDistance(double latitude, double longitude) {
        return Util.getDistance(Latitude, Longitude, latitude, longitude);
    }

    @Override
    public String toString() {
        return "BusStop [" + (type != null ? "type=" + type + ", " : "")
                + (BusStopCode != null ? "BusStopCode=" + BusStopCode + ", " : "")
                + (RoadName != null ? "RoadName=" + RoadName + ", " : "")
                + (Description != null ? "Description=" + Description + ", " : "")
                + "Latitude=" + Latitude
                + ", Longitude=" + Longitude + ", "
                + (NUSStopCode != null ? "NUSStopCode=" + NUSStopCode + ", " : "")
                + (NUSDescription != null ? "NUSDescription=" + NUSDescription + ", " : "")
                + (NTUStopCode != null ? "NTUStopCode=" + NTUStopCode + ", " : "")
                + (NTUDescription != null ? "NTUDescription=" + NTUDescription : "") + "]";
    }

}
