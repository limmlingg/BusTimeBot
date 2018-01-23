package model.json.gothere;

public class GeoCodeContainer {
    private static final String KEYWORD_TYPE_LOCATION = "LOCATION"; //Markers will be contained in 'Where'
    private static final String KEYWORD_TYPE_BUSINESS = "BUSINESS"; //Markers will be contained in 'What'

    public int status;
    public String color;
    public String type;
    public MarkerContainer what;
    public MarkerContainer where;

    public double getLatitude() {
        if (KEYWORD_TYPE_BUSINESS.equals(type)) {
            return what.markers.get(0).getLatitude();
        } else if (KEYWORD_TYPE_LOCATION.equals(type)) {
            return where.markers.get(0).getLatitude();
        } else {
            return -1; //Error, should not happen
        }
    }

    public double getLongitude() {
        if (KEYWORD_TYPE_BUSINESS.equals(type)) {
            return what.markers.get(0).getLongitude();
        } else if (KEYWORD_TYPE_LOCATION.equals(type)) {
            return where.markers.get(0).getLongitude();
        } else {
            return -1; //Error, should not happen
        }
    }
}
