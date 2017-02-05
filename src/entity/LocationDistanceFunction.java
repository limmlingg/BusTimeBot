package entity;

import controller.Util;
import entity.kdtree.DistanceFunction;

public class LocationDistanceFunction implements DistanceFunction {

    @Override
    /**
     * Return distance in km given 2 coordinates (lat followed by long)
     *
     * @param p1
     *            coordinate {lat, long}
     * @param p2
     *            coordinate {lat, long}
     * @return distance in km between the 2 coordinates
     */
    public double distance(double[] p1, double[] p2) {
        return Util.getDistance(p1[0], p1[1], p2[0], p2[1]);
    }

    @Override
    /**
     * Return the shortest distance in km of a coordinate to an area
     *
     * @param point
     *            coordinate {lat, long} to check
     * @param min,
     *            btm left, coordinate {lat, long}
     * @param max,
     *            top right, coordinate {lat, long}
     * @return distance in km of point to the bound
     */
    public double distanceToRect(double[] point, double[] min, double[] max) {
        double distance = 0;
        /*
         * min[0] = btm
         * min[1] = left
         * max[0] = top
         * max[1] = right
         */
        //9 cases, think of it as a tic-tac-toe grid
        if (point[0] < min[0] && point[1] < min[1]) { //top left
            distance = Util.getDistance(point[0], point[1], max[0], min[1]);
        } else if (point[0] < min[0] && point[1] > min[1] && point[1] < max[1]) { //top
            distance = Util.getDistance(point[0], point[1], max[0], point[1]);
        } else if (point[0] < min[0] && point[1] > max[1]) { //top right
            distance = Util.getDistance(point[0], point[1], max[0], max[1]);
        } else if (point[0] > min[0] && point[0] < max[0] && point[1] < min[1]) { //middle left
            distance = Util.getDistance(point[0], point[1], point[0], min[1]);
        } else if (point[0] > min[0] && point[0] < max[0] && point[1] > min[1] && point[1] < max[1]) { //middle
            distance = 0;
        } else if (point[0] > min[0] && point[0] < max[0] && point[1] > max[1]) { //middle right
            distance = Util.getDistance(point[0], point[1], point[0], max[1]);
        } else if (point[0] < max[0] && point[1] < min[1]) { //bottom left
            distance = Util.getDistance(point[0], point[1], min[0], min[1]);
        } else if (point[0] < max[0] && point[1] > min[1] && point[1] < max[1]) { //bottom
            distance = Util.getDistance(point[0], point[1], min[0], point[1]);
        } else if (point[0] < max[0] && point[1] > max[1]) { //bottom right
            distance = Util.getDistance(point[0], point[1], min[0], max[1]);
        }
        return distance;
    }

}
