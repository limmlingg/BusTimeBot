package model.json.gothere;

public class Marker {
    public boolean draggable;
    public double[] latlng; //lat followed by long
    //ignore others

    public double getLatitude() {
        return latlng[0];
    }

    public double getLongitude() {
        return latlng[1];
    }
}
