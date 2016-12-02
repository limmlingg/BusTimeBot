package common;

import java.util.Comparator;

public class LocationComparator implements Comparator<BusStop> {

	public double lat;
	public double lon;
	
	public LocationComparator(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}
	
	@Override
	public int compare(BusStop o1, BusStop o2) {
		double distance1 = o1.getDistance(lat, lon);
		double distance2 = o2.getDistance(lat, lon);
		return (int) ((distance1 - distance2) * 100000.0);
	}

}
