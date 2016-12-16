package entity;

import controller.Util;
import entity.kdtree.DistanceFunction;

public class LocationDistanceFunction implements DistanceFunction{
	public double lat;
	public double lon;
	
	public LocationDistanceFunction(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	@Override
	public double distance(double[] p1, double[] p2) {
		double distance1 = Util.getDistance(lat, lon, p1[0], p1[1]);
		double distance2 = Util.getDistance(lat, lon, p2[0], p2[1]);
		return distance1 - distance2;
	}

	@Override
	public double distanceToRect(double[] point, double[] min, double[] max) {
		double d = 0;

        for (int i = 0; i < point.length; i++) {
            double diff = 0;
            if (point[i] > max[i]) {
                diff = (point[i] - max[i]);
            }
            else if (point[i] < min[i]) {
                diff = (point[i] - min[i]);
            }
            d += diff * diff;
        }

        return d;
	}

}
