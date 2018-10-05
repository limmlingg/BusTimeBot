package model;

import org.telegram.telegrambots.meta.api.objects.Location;

import logic.Util;

public class BusStop {
    public String BusStopCode;
    //public String RoadName;
    public String Description;
    public double Latitude;
    public double Longitude;

    //Flags to indicate type of bus stop
    public BusType busType;

    //If combining both stops (use public's coordinates)
    public String nusStopCode;
    public String nusDescription;
    public String ntuStopCode;
    public String ntuDescription;

    public BusStop() {
        busType = new BusType();
    }

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
        return "BusStop [BusStopCode=" + BusStopCode
                //+ ", RoadName=" + RoadName
                + ", Description=" + Description
                + ", Latitude=" + Latitude
                + ", Longitude=" + Longitude
                + ", busType=" + busType.toString()
                + ", NUSStopCode=" + nusStopCode
                + ", NUSDescription=" + nusDescription
                + ", NTUStopCode=" + ntuStopCode
                + ", NTUDescription=" + ntuDescription + "]";
    }
}
