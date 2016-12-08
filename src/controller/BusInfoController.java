package controller;

import java.util.ArrayList;

public class BusInfoController {
	/**
	 * Returns a nicely formatted string that contains bus information on a public bus service number
	 * @param serviceNo
	 * @return
	 */
	public static String getPublicBusTiming(String serviceNo) {
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
			formattedInformation.append(formatInformation(serviceNo, extractInformation(data)) + "\n\n");
			
			if (splitResponse.length == 5) {//Second set
				data = splitResponse[splitResponse.length-2];
				formattedInformation.append(formatInformation(serviceNo, extractInformation(data)));
			}
		} else {
			formattedInformation.append("No such bus service");
		}
		return formattedInformation.toString();
	}
	
	public static String formatInformation(String serviceNo, ArrayList<String> information) {
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
	}
	
	/**
	 * Extract the bus terminal, first/last bus for weekday/sat/sun & P.H
	 * @param data string containing the table taken from transitlink.com.sg
	 * @return List of String (Size: 7) in this order: Interchange, Weekday 1st bus, Weekday last bus, Sat 1st bus, Sat last bus, Sun & P.H 1st bus, Sun & P.H last bus
	 */
	public static ArrayList<String> extractInformation(String data) {
		ArrayList<String> information = new ArrayList<String>();
		String[] timings = data.split("<td width=\"10%\" align=\"center\">");
		String busHeaderStart = "<td width=\"20%\">";
		information.add(timings[0].substring(timings[0].indexOf(busHeaderStart) + busHeaderStart.length(), timings[0].lastIndexOf("</td>")));
		for (int i=1; i<timings.length; i++) {
			String time = timings[i];
			information.add(time.substring(0, time.lastIndexOf("</td>")));
		}
		return information;
	}
}
