package controller;

import java.util.ArrayList;

import main.Logger;

public class BusInfoController {
	
	/**
	 * Return information regarding NTU shuttle buses
	 * @param serviceNo of NTU bus
	 * @return a nicely formatted string with the first/last bus
	 */
	public static String getNTUBusInfo(String serviceNo) {
		//A1, A2, B, C, D1, D2, BTC1,
		serviceNo = serviceNo.toUpperCase();
		ArrayList<String> busTiming = new ArrayList<String>();
		if (serviceNo.equalsIgnoreCase("CL-Blue")) {
			busTiming.add(serviceNo);
			busTiming.add("0800");
			busTiming.add("2300");
			busTiming.add("0800");
			busTiming.add("2300");
			busTiming.add("0800");
			busTiming.add("2300");
		} else if (serviceNo.equalsIgnoreCase("CL-Red")) {
			busTiming.add(serviceNo);
			busTiming.add("0800");
			busTiming.add("2300");
			busTiming.add("0800");
			busTiming.add("2300");
			busTiming.add("0800");
			busTiming.add("2300");
		} else if (serviceNo.equalsIgnoreCase("CR")) {
			busTiming.add(serviceNo);
			busTiming.add("0730");
			busTiming.add("2300");
			busTiming.add("0730");
			busTiming.add("2300");
			busTiming.add("0730");
			busTiming.add("2300");
		} else if (serviceNo.equalsIgnoreCase("CWR")) {
			busTiming.add(serviceNo);
			busTiming.add("0730");
			busTiming.add("2300");
			busTiming.add("0730");
			busTiming.add("2300");
			busTiming.add("0730");
			busTiming.add("2300");
		}
		
		if (busTiming.size() <= 0) {
			return "No such bus service";
		} else {
			return formatInformation(busTiming);
		}
	}
	
	/**
	 * Return information regarding NUS shuttle buses
	 * @param serviceNo of NUS bus
	 * @return a nicely formatted string with the first/last bus
	 */
	public static String getNUSBusInfo(String serviceNo) {
		//A1, A2, B, C, D1, D2, BTC1,
		serviceNo = serviceNo.toUpperCase();
		ArrayList<String> busTiming = new ArrayList<String>();
		if (serviceNo.equalsIgnoreCase("A1") || serviceNo.equalsIgnoreCase("A2")) {
			busTiming.add(serviceNo);
			busTiming.add("0715");
			busTiming.add("2300");
			busTiming.add("0715");
			busTiming.add("2300");
			busTiming.add("0900");
			busTiming.add("2300");
		} else if (serviceNo.equalsIgnoreCase("B")) {
			busTiming.add(serviceNo);
			busTiming.add("0715");
			busTiming.add("2300");
			busTiming.add("0715");
			busTiming.add("1900");
			busTiming.add("No Service");
			busTiming.add("No Service");
		} else if (serviceNo.equalsIgnoreCase("BTC")) {
			busTiming.add(serviceNo);
			busTiming.add("0720");
			busTiming.add("2130");
			busTiming.add("0830");
			busTiming.add("1230");
			busTiming.add("No Service");
			busTiming.add("No Service");
		} else if (serviceNo.equalsIgnoreCase("C")) {
			busTiming.add(serviceNo);
			busTiming.add("0715");
			busTiming.add("2300");
			busTiming.add("0715");
			busTiming.add("1900");
			busTiming.add("No Service");
			busTiming.add("No Service");
		} else if (serviceNo.equalsIgnoreCase("D1") || serviceNo.equalsIgnoreCase("D1")) {
			busTiming.add(serviceNo);
			busTiming.add("0715");
			busTiming.add("2300");
			busTiming.add("0715");
			busTiming.add("2300");
			busTiming.add("0900");
			busTiming.add("2300");
		}
		
		if (busTiming.size() <= 0) {
			return "No such bus service";
		} else {
			return formatInformation(busTiming);
		}
	}
	
	/**
	 * Returns a nicely formatted string that contains bus information on a public bus service number
	 * @param serviceNo
	 * @return
	 */
	public static String getPublicBusInfo(String serviceNo) {
		try {
			String response = WebController.sendHTTPRequest("http://www.transitlink.com.sg/eservice/eguide/service_route.php?service="+serviceNo, false);
			//Extract the table
			String starting = "<section class=\"eguide-table\">";
			String ending = "</table>";
			response = response.substring(response.indexOf(starting) + starting.length(), response.indexOf(ending, response.indexOf(starting))+ending.length());
			boolean valid = response.startsWith("<table border=\"0\" cellspacing=\"0\" width=\"100%\"><tr>");
			String[] splitResponse = response.split("<tr>"); //5 if 2 way, 4 if 1 way
			
			StringBuffer formattedInformation = new StringBuffer();
			//Extract first and last bus timings
			if (valid) {
				formattedInformation.append("*Service " + serviceNo.toUpperCase() + "*\n");
				//First set
				String data = splitResponse[splitResponse.length-1];
				formattedInformation.append(formatInformation(extractPublicInformation(data)) + "\n\n");
				
				if (splitResponse.length == 5) {//Second set
					data = splitResponse[splitResponse.length-2];
					formattedInformation.append(formatInformation(extractPublicInformation(data)));
				}
			} else {
				formattedInformation.append("No such bus service");
			}
			return formattedInformation.toString();
		} catch (Exception e) {
			Logger.log("Error!!!!\n" + e.toString()  + "\n======================================================\n");
			return null;
		}
	}
	
	/**
	 * Format information in a present-able manner
	 * @param information List of String (Size: 7) in this order: Header, Weekday 1st bus, Weekday last bus, Sat 1st bus, Sat last bus, Sun & P.H 1st bus, Sun & P.H last bus
	 * @return a nicely formatted string with all the information presented nicely
	 */
	public static String formatInformation(ArrayList<String> information) {
		try {
			if (information.size() != 7) {
				return null;
			}
			
			StringBuffer busInfo = new StringBuffer("*" + information.get(0) + "*\n");
			busInfo.append("*Weekdays*\n");
			busInfo.append("1st Bus: " + information.get(1) + " | Last Bus: " + information.get(2) + "\n");
			busInfo.append("*Saturdays*\n");
			busInfo.append("1st Bus: " + information.get(3) + " | Last Bus: " + information.get(4) + "\n");
			busInfo.append("*Suns & P.H*\n");
			busInfo.append("1st Bus: " + information.get(5) + " | Last Bus: " + information.get(6));
			return busInfo.toString();
		} catch (Exception e) {
			Logger.log("Error!!!!\n" + e.toString()  + "\n======================================================\n");
			return null;
		}
	}
	
	/**
	 * Extract the bus terminal, first/last bus for weekday/sat/sun & P.H
	 * @param data string containing the table taken from transitlink.com.sg
	 * @return List of String (Size: 7) in this order: Interchange, Weekday 1st bus, Weekday last bus, Sat 1st bus, Sat last bus, Sun & P.H 1st bus, Sun & P.H last bus
	 */
	public static ArrayList<String> extractPublicInformation(String data) {
		try {
			ArrayList<String> information = new ArrayList<String>();
			String[] timings = data.split("<td width=\"10%\" align=\"center\">");
			String busHeaderStart = "<td width=\"20%\">";
			information.add(timings[0].substring(timings[0].indexOf(busHeaderStart) + busHeaderStart.length(), timings[0].lastIndexOf("</td>")));
			for (int i=1; i<timings.length; i++) {
				String time = timings[i];
				information.add(time.substring(0, time.lastIndexOf("</td>")));
			}
			return information;
		} catch (Exception e) {
			Logger.log("Error!!!!\n" + e.toString()  + "\n======================================================\n");
			return null;
		}
	}
}
