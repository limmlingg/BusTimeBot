package entity;

import org.telegram.telegrambots.api.objects.Location;

public class BusStop {
	public enum Type {
		NUS_ONLY,
		PUBLIC_ONLY,
		BOTH;
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
	
	public double getDistance(Location location) {
		return getDistance(Latitude, Longitude, location.getLatitude(), location.getLongitude());
	}
	
	public double getDistance(double latitude, double longitude) {
		return getDistance(Latitude, Longitude, latitude, longitude);
	}
	
	public double getDistance(BusStop busStop) {
		return getDistance(Latitude, Longitude, busStop.Latitude, busStop.Longitude);
	}
	
	private double getDistance(double lat1, double lon1, double lat2, double lon2) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		//Convert to Km
		dist = dist * 1.609344;
		return dist;
	}
	
	private static double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	private static double rad2deg(double rad) {
		return (rad * 180 / Math.PI);
	}
}
