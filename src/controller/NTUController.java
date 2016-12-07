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
import entity.ntubus.NTUBusStopContainer;
import entity.ntubus.Node;
import main.BusTimeBot;

public class NTUController {
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
						} else { //Otherwise it is most likely a NUS-only bus stop
							BusStop stop = new BusStop();
							stop.BusStopCode = Integer.toString(node.id);
							stop.Description = fixName(isBlueRider, node);
							stop.Latitude = node.lat;
							stop.Longitude = node.lon;
							stop.type = Type.NTU_ONLY;
							BusTimeBot.bot.busStops.put(stop.BusStopCode, stop);
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
		String code = stop.BusStopCode;
		if (stop.type == Type.PUBLIC_NTU) {
			code = stop.NTUStopCode;
		}
		
		HashMap<String, ArrayList<Integer>> timings = new HashMap<String, ArrayList<Integer>>();
		NTUBusArrivalContainer results = WebController.retrieveData("https://baseride.com/routes/api/platformbusarrival/"+ code +"/?format=json", NTUBusArrivalContainer.class);
		for (NTUBusArrival arrival : results.forecast) {
			if (timings.containsKey(arrival.route.short_name)) {
				if (timings.get(arrival.route.short_name).size() < 2) {
					timings.get(arrival.route.short_name).add((int) Math.ceil(arrival.forecast_seconds/60.0));
				}
			} else {
				ArrayList<Integer> time = new ArrayList<Integer>();
				time.add((int) Math.floor(arrival.forecast_seconds/60.0));
				timings.put(arrival.route.short_name, time);
			}
		}
		
		Emoji emoji = EmojiManager.getForAlias("oncoming_bus");
		StringBuffer busTimings = new StringBuffer();
		//Now loop through the map and build the string
		for (Entry<String, ArrayList<Integer>> entry : timings.entrySet()) {
			busTimings.append(emoji.getUnicode() + "*" + entry.getKey() + "*: ");
			for (Integer time : entry.getValue()) {
				if (time == 0) {
					busTimings.append("Arr  |  ");
				} else {
					busTimings.append(time + "min  |  ");
				}
			}
			busTimings.delete(busTimings.length()-5, busTimings.length());
			busTimings.append("\n");
		}
		return busTimings.toString();
	}
}