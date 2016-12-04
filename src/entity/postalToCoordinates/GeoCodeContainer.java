package entity.postalToCoordinates;

import java.util.ArrayList;

public class GeoCodeContainer {
	public ArrayList<GeoCode> results;
	public String status; //OK, ZERO_RESULTS, OVER_QUERY_LIMIT, REQUEST_DENIED, INVALID_REQUEST, UNKNOWN_ERROR
}
