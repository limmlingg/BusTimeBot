package controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import entity.BusStop;
import entity.BusStop.Type;
import entity.BusStopMapping;
import entity.ntubus.Coordinate;
import entity.ntubus.NTUBusArrival;
import entity.ntubus.NTUBusArrivalContainer;
import entity.ntubus.NTUBusList;
import entity.ntubus.NTUBusStopContainer;
import entity.ntubus.Node;
import entity.ntubus.Route;
import main.BusTimeBot;
import main.Logger;

public class NTUController {
	public static HashMap<String, String> busCode;
	public static HashMap<String, ArrayList<String>> busList;
	
	static {
		busList = new HashMap<String, ArrayList<String>>(50);
		busCode = new HashMap<String, String>();
		busCode.put("Campus Loop - Blue (CL-B)", "CL-Blue");
		busCode.put("Campus Loop Red (CL-R)", "CL-Red");
		busCode.put("Campus Rider Green", "CR");
		busCode.put("Campus WeekEnd Rider Brown", "CWR");
	}
	
	/**
	 * Retrieve bus stop data from NTU and put it into the bot's busStops list
	 */
	public static void getNTUBusStopData() {
		/*Bus data from 44478 to 44481 inclusive
		 * in order: red, blue, green, brown
		*/
		for (int i=44478; i<= 44481; i++) {
			NTUBusStopContainer result = WebController.retrieveData("https://baseride.com/routes/apigeo/routevariantgeo/" + i + "/?format=json", NTUBusStopContainer.class, true);
			//This boolean is explained below
			boolean isBlueRider = result.name.contains("CL-B");
			for (Coordinate coordinate : result.routevariant_geometry) {
				for (Node node : coordinate.nodes) {
					if (node.id != 0) {
						if (BusStopMapping.getValue(Integer.toString(node.id)) != null) { //Add on to public bus stop if it is the same bus stop (will be considered both NUS & Public bus stop)
							BusStop existingStop = BusTimeBot.bot.busStops.get(BusStopMapping.getValue(Integer.toString(node.id)));
							existingStop.NTUStopCode = Integer.toString(node.id);
							existingStop.NTUDescription = fixName(isBlueRider, node);
							existingStop.type = Type.PUBLIC_NTU;
							retrieveBusList(existingStop.NTUStopCode);
						} else { //Otherwise it is most likely a NTU-only bus stop
							BusStop stop = new BusStop();
							stop.BusStopCode = Integer.toString(node.id);
							stop.Description = fixName(isBlueRider, node);
							stop.Latitude = node.lat;
							stop.Longitude = node.lon;
							stop.type = Type.NTU_ONLY;
							BusTimeBot.bot.busStops.put(stop.BusStopCode, stop);
							retrieveBusList(stop.BusStopCode);
						}
						//printStop(stop);
						//getNTUArrivalTimings(stop);
					}
				} //End node for loop
				//System.out.println("===================");
			} //end coordinate for loop
		}//end i for loop
	}
	
	/**
	 * Retrieve the types of NTU shuttle bus that will arrive at this particular stop (Since getting arrival timings will just not give the timings)
	 * @param stopCode of the NTU Bus Stop
	 */
	public static void retrieveBusList(String stopCode) {
		NTUBusList retrievedbusList = WebController.retrieveData("https://baseride.com/routes/api/platformroutelist/" + stopCode + "/?format=json", NTUBusList.class);
		busList.put(stopCode, new ArrayList<String>());
		for (Route route : retrievedbusList.routes) {
			busList.get(stopCode).add(route.short_name);
		}
	}
	
	/**
	 * We need to do this since the blue rider's API does not append opposite to certain bus stop names
	 * @param isBlueRider Only the blue rider's naming is wrong
	 * @param node node of the data returned
	 * @return the appropriate string for that bus stop name
	 */
	public static String fixName(boolean isBlueRider, Node node) {
		if (isBlueRider && node.name.startsWith("LWN")) {
			return "NIE, Opp. LWN Library";
		} else if (isBlueRider && (!node.name.startsWith("Nanyang") && !node.name.startsWith("Hall 6"))) {
			return "Opp. " + node.name;
		} else {
			return node.name;
		}
	}
	
	/**
	 * Bus the details of the bus stop
	 * @param stop
	 */
	public static void printStop(BusStop stop) {
		//System.out.println("NTUToPublic.put(\"" + stop.NTUStopCode + "\", \"\");");
		System.out.println("(" + stop.NTUStopCode + ") " + stop.Description + " -> (" + stop.Latitude + "," + stop.Longitude +")");
	}
	
	/**
	 * Get NTU Shuttle Service bus arrival timings from overdrive servers
	 * @param stop code for the Bus Stop
	 * @return A string of bus timings formatted properly
	 */
	public static String getNTUBusArrivalTimings(BusStop stop) {
		try {
			String code = stop.BusStopCode;
			if (stop.type == Type.PUBLIC_NTU) {
				code = stop.NTUStopCode;
			}
			
			//We save the timings to a hash map first since the bus arrival timings are all given together in a list
			HashMap<String, ArrayList<Integer>> timings = new HashMap<String, ArrayList<Integer>>();
			
			//We retrieve all the possible buses for that particular bus stop
			for (String buses : busList.get(code)) {
				timings.put(buses, new ArrayList<Integer>());
			}
			
			//Append the bus timings for each bus
			NTUBusArrivalContainer results = WebController.retrieveData("https://baseride.com/routes/api/platformbusarrival/"+ code +"/?format=json", NTUBusArrivalContainer.class);
			for (NTUBusArrival arrival : results.forecast) {
				if (timings.containsKey(arrival.route.short_name)) {
					if (timings.get(arrival.route.short_name).size() < 2) {
						timings.get(arrival.route.short_name).add((int) Math.ceil(arrival.forecast_seconds/60.0));
					}
				}
			}
			
			Emoji emoji = EmojiManager.getForAlias("oncoming_bus");
			StringBuffer busTimings = new StringBuffer();
			//Now loop through the map and build the string
			for (Entry<String, ArrayList<Integer>> entry : timings.entrySet()) {
				busTimings.append(emoji.getUnicode() + "*" + busCode.get(entry.getKey()) + "*: ");
				boolean hasBus = false;
				for (Integer time : entry.getValue()) {
					hasBus = true;
					if (time <= 0) {
						busTimings.append("Arr  |  ");
					} else {
						busTimings.append(time + "min  |  ");
					}
				}
				//Add N/A if no timing is available
				if (!hasBus) {
					busTimings.append("N/A  |  ");
				}
				busTimings.delete(busTimings.length()-5, busTimings.length());
				busTimings.append("\n");
			}
			return busTimings.toString();
		} catch (Exception e) {
			Logger.log("Error!!!!\n" + e.toString()  + "\n======================================================\n");
			return null;
		}
	}
}
