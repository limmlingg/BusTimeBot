package controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Util {
    public static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

    /**
     * Get time difference of the input date from the current time
     *
     * @param date
     *            string that is in "yyyy-MM-dd'T'HH:mm:ssXXX" format
     * @return (if Calendar.SECOND, return in seconds otherwise in minutes), Long.MAX_VALUE if date is invalid
     */
    public static long getTimeFromNow(String date, int type) {
        long difference;
        try {
            long now = new Date().getTime();
            long designatedTime = format.parse(date).getTime();
            long divisor = type == Calendar.SECOND ? 1000 : (60 * 1000);
            difference = (designatedTime - now) / divisor;
        } catch (ParseException e) {
            difference = Long.MAX_VALUE;
        }
        return difference;
    }

    /**
     * Calculate the distance between 2 coordinates (lat+lon = 1 coordinate)
     *
     * @param lat1
     *            coordinate
     * @param lon1
     *            coordinate
     * @param lat2
     *            coordinate
     * @param lon2
     *            coordinate
     * @return distance in km
     */
    public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double distance = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        distance = Math.acos(distance);
        distance = rad2deg(distance);
        distance = distance * 60 * 1.1515;
        //Convert to Km
        distance = distance * 1.609344;
        return distance;
    }

    /**
     * Convert degree to radian
     *
     * @param degree
     * @return radian
     */
    private static double deg2rad(double degree) {
        return (degree * Math.PI / 180.0);
    }

    /**
     * Convert radian to degree
     *
     * @param radian
     * @return degree
     */
    private static double rad2deg(double radian) {
        return (radian * 180 / Math.PI);
    }

    /**
     * Pads the string with spaces until a certain length
     */
    public static String pad(String input, int length) {
        String paddedInput = input;
        while (paddedInput.length() < length) {
            paddedInput = paddedInput + " ";
        }
        return paddedInput;
    }
}
