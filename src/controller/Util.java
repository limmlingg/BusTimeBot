package controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Util {
	public static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
	/**
	 * Get time difference of the input date from the current time 
	 * @param date string that is in "yyyy-MM-dd'T'HH:mm:ssXXX" format
	 * @return (if Calendar.SECOND, return in seconds otherwise in minutes), Long.MAX_VALUE if date is invalid
	 */
	public static long getTimeFromNow(String date, int type) {
		long difference;
		try {
			long now = new Date().getTime();
			long designatedTime = format.parse(date).getTime();
			long divisor = type==Calendar.SECOND? 1000 : (60*1000); 
			difference = (designatedTime-now)/divisor;
		} catch (ParseException e) {
			difference = Long.MAX_VALUE;
		}
		return difference;
	}
}
