package logic.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import logic.Util;
import main.Logger;
import model.BusStop;
import model.BusStopMapping;
import model.businfo.BusInfo;
import model.businfo.BusInfoDirection;
import model.json.ntubus.Coordinate;
import model.json.ntubus.Node;
import model.json.ntubus.NtuBusArrival;
import model.json.ntubus.NtuBusArrivalContainer;
import model.json.ntubus.NtuBusList;
import model.json.ntubus.NtuBusStopContainer;
import model.json.ntubus.Route;

public class NtuController {
    public static final String[] NTU_BUSES = {"CL-BLUE", "CL-RED", "CR", "CWR"};

    public static final BusInfoDirection NTU_CLBLUE = new BusInfoDirection("", "0800", "2300", "0800", "2300", "0800", "2300");
    public static final BusInfoDirection NTU_CLRED = new BusInfoDirection("", "0800", "2300", "0800", "2300", "0800", "2300");
    public static final BusInfoDirection NTU_CR = new BusInfoDirection("", "0730", "2300", "0730", "2300", "0730", "2300");
    public static final BusInfoDirection NTU_CWR = new BusInfoDirection("", "0730", "2300", "0730", "2300", "0730", "2300");

    public static HashMap<String, String> busCode;
    public static HashMap<String, ArrayList<String>> busList;

    static {
        Arrays.sort(NTU_BUSES);
        busList = new HashMap<String, ArrayList<String>>(50);
        busCode = new HashMap<String, String>();
        busCode.put("Campus Loop - Blue (CL-B)", "CL-B");
        busCode.put("Campus Loop Red (CL-R)", "CL-R");
        busCode.put("Campus Rider Green", "CR");
        busCode.put("Campus WeekEnd Rider Brown", "CWR");
    }

    /**
     * Return information regarding NTU shuttle buses
     *
     * @param serviceNoUpper
     *            of NTU bus
     * @return a nicely formatted string with the first/last bus
     */
    public static BusInfo getNTUBusInfo(String serviceNo) {
        BusInfo busInfo = new BusInfo();

        //CL-Blue, CL-Red, CR, CWR
        String serviceNoUpper = serviceNo.toUpperCase();

        //In order: Service Number, Weekday-Sat-Sun/P.H First and Last bus
        if (serviceNoUpper.equalsIgnoreCase("CL-Blue")) {
            busInfo = new BusInfo(serviceNoUpper, true, NTU_CLBLUE);
        } else if (serviceNoUpper.equalsIgnoreCase("CL-Red")) {
            busInfo = new BusInfo(serviceNoUpper, true, NTU_CLRED);
        } else if (serviceNoUpper.equalsIgnoreCase("CR")) {
            busInfo = new BusInfo(serviceNoUpper, true, NTU_CR);
        } else if (serviceNoUpper.equalsIgnoreCase("CWR")) {
            busInfo = new BusInfo(serviceNoUpper, true, NTU_CWR);
        }

        return busInfo;
    }

    /**
     * Retrieve bus stop data from NTU and put it into the bot's busStops list
     */
    public static void getNTUBusStopData(HashMap<String, BusStop> busStops) {
        /*
         * Bus data from 44478 to 44481 inclusive
         * in order: red, blue, green, brown
         */
        for (int i = 44478; i <= 44481; i++) {
            NtuBusStopContainer result = WebController.retrieveData("https://baseride.com/routes/apigeo/routevariantgeo/" + i + "/?format=json", NtuBusStopContainer.class, true);
            //This boolean is explained below
            boolean isBlueRider = result.name.contains("CL-B");
            for (Coordinate coordinate : result.routevariant_geometry) {
                for (Node node : coordinate.nodes) {
                    if (node.id != 0) {
                        if (BusStopMapping.getValue(Integer.toString(node.id)) != null) { //Add on to public bus stop if it is the same bus stop (will be considered both NUS & Public bus stop)
                            BusStop existingStop = busStops.get(BusStopMapping.getValue(Integer.toString(node.id)));
                            existingStop.ntuStopCode = Integer.toString(node.id);
                            existingStop.ntuDescription = fixName(isBlueRider, node);
                            existingStop.isNtu = true;
                            retrieveBusList(existingStop.ntuStopCode);
                        } else { //Otherwise it is most likely a NTU-only bus stop
                            BusStop stop = new BusStop();
                            stop.BusStopCode = Integer.toString(node.id);
                            stop.Description = fixName(isBlueRider, node);
                            stop.Latitude = node.lat;
                            stop.Longitude = node.lon;
                            stop.isNtu = true;
                            busStops.put(stop.BusStopCode, stop);
                            retrieveBusList(stop.BusStopCode);
                        }
                    }
                } //End node for loop
            } //end coordinate for loop
        } //end i for loop
    }

    /**
     * Retrieve the types of NTU shuttle bus that will arrive at this particular stop (Since getting arrival timings will just not give the timings)
     *
     * @param stopCode
     *            of the NTU Bus Stop
     */
    public static void retrieveBusList(String stopCode) {
        NtuBusList retrievedbusList = WebController.retrieveData("https://baseride.com/routes/api/platformroutelist/" + stopCode + "/?format=json", NtuBusList.class);
        busList.put(stopCode, new ArrayList<String>());
        for (Route route : retrievedbusList.routes) {
            busList.get(stopCode).add(route.short_name);
        }
    }

    /**
     * We need to do this since the blue rider's API does not append opposite to certain bus stop names
     *
     * @param isBlueRider
     *            Only the blue rider's naming is wrong
     * @param node
     *            node of the data returned
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
     *
     * @param stop
     */
    public static void printStop(BusStop stop) {
        System.out.println("(" + stop.ntuStopCode + ") " + stop.Description + " -> (" + stop.Latitude + "," + stop.Longitude + ")");
    }

    /**
     * Get NTU Shuttle Service bus arrival timings from overdrive servers
     *
     * @param stop
     *            code for the Bus Stop
     * @return A string of bus timings formatted properly
     */
    public static String getNTUBusArrivalTimings(BusStop stop) {
        try {
            String code = stop.BusStopCode;
            if (stop.isNtu && stop.isPublic) {
                code = stop.ntuStopCode;
            }

            //We save the timings to a hash map first since the bus arrival timings are all given together in a list
            HashMap<String, ArrayList<Integer>> timings = new HashMap<String, ArrayList<Integer>>();

            //We retrieve all the possible buses for that particular bus stop
            for (String buses : busList.get(code)) {
                timings.put(buses, new ArrayList<Integer>());
            }

            //Append the bus timings for each bus
            NtuBusArrivalContainer results = WebController.retrieveData("https://baseride.com/routes/api/platformbusarrival/" + code + "/?format=json", NtuBusArrivalContainer.class);
            for (NtuBusArrival arrival : results.forecast) {
                if (timings.containsKey(arrival.route.short_name) && timings.get(arrival.route.short_name).size() < 2) {
                    timings.get(arrival.route.short_name).add((int) Math.ceil(arrival.forecast_seconds / 60.0));
                }
            }

            Emoji emoji = EmojiManager.getForAlias("oncoming_bus");
            StringBuilder busArrivals = new StringBuilder();
            //Now loop through the map and build the string
            for (Entry<String, ArrayList<Integer>> entry : timings.entrySet()) {
                busArrivals.append(emoji.getUnicode() + Util.padBusTitle(busCode.get(entry.getKey())) + ": ");
                boolean hasBus = false;
                for (Integer time : entry.getValue()) {
                    hasBus = true;
                    if (time <= 0) {
                        busArrivals.append(Util.padBusTime("Arr") + " | ");
                    } else {
                        busArrivals.append(Util.padBusTime(time + "min") + " | ");
                    }
                }
                //Add N/A if no timing is available
                if (!hasBus) {
                    busArrivals.append("N/A  " + " | ");
                }
                busArrivals.delete(busArrivals.length() - 3, busArrivals.length());
                busArrivals.append("\n");
            }
            return busArrivals.toString();
        } catch (Exception e) {
            Logger.logError(e);
            return null;
        }
    }
}
