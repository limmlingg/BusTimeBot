package model.geocoding;

public class Marker {
    public boolean draggable;
    public double[] latlng; //late followed by long
    //ignore others

    public double getLatitude() {
        return latlng[0];
    }

    public double getLongitude() {
        return latlng[1];
    }
}
