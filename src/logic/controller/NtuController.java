package logic.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;

import model.BusStop;
import model.BusStopMapping;
import model.BusType.Type;
import model.busarrival.BusArrival;
import model.busarrival.BusStopArrival;
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
    public static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(NtuController.class);
    public static final String[] NTU_BUSES = {"CL-BLUE", "CL-RED", "CR", "CWR"};

    public static final BusInfoDirection NTU_CLBLUE = new BusInfoDirection("", "0800", "2300", "0800", "2300", "0800", "2300");
    public static final BusInfoDirection NTU_CLRED = new BusInfoDirection("", "0800", "2300", "0800", "2300", "0800", "2300");
    public static final BusInfoDirection NTU_CR = new BusInfoDirection("", "0730", "2300", "0730", "2300", "0730", "2300");
    public static final BusInfoDirection NTU_CWR = new BusInfoDirection("", "0730", "2300", "0730", "2300", "0730", "2300");

    public static HashMap<String, String> busCode;
    public static HashMap<String, ArrayList<String>> busList;
    public static HashMap<String, StringBuilder> busRoutes;

    static {
        Arrays.sort(NTU_BUSES);
        busList = new HashMap<String, ArrayList<String>>(50);
        busCode = new HashMap<String, String>();
        busRoutes = new HashMap<String, StringBuilder>();
        busCode.put("Campus Loop - Blue (CL-B)", "CL-B");
        busCode.put("Campus Loop Red (CL-R)", "CL-R");
        busCode.put("Campus Rider Green", "CR");
        busCode.put("Campus WeekEnd Rider Brown", "CWR");
        for (String bus : NTU_BUSES) {
            busRoutes.put(bus, new StringBuilder());
        }
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
                            existingStop.busType.setTrue(Type.NTU);
                            retrieveBusList(existingStop.ntuStopCode);
                        } else { //Otherwise it is most likely a NTU-only bus stop
                            BusStop stop = new BusStop();
                            stop.BusStopCode = Integer.toString(node.id);
                            stop.Description = fixName(isBlueRider, node);
                            stop.Latitude = node.lat;
                            stop.Longitude = node.lon;
                            stop.busType.setTrue(Type.NTU);
                            busStops.put(stop.BusStopCode, stop);
                            retrieveBusList(stop.BusStopCode);
                        }

                        //Add name to bus route
                        addBusRoute(i, fixName(isBlueRider, node));
                    }
                } //End node for loop
            } //end coordinate for loop
        } //end i for loop
    }

    private static void addBusRoute(int index, String stopName) {
        if (index == 44478) { //red
            busRoutes.get(NTU_BUSES[1]).append(stopName + "\n");
        } else if (index == 44479) { //blue
            busRoutes.get(NTU_BUSES[0]).append(stopName + "\n");
        } else if (index == 44480) { //green
            busRoutes.get(NTU_BUSES[2]).append(stopName + "\n");
        } else if (index == 44481) { //brown
            busRoutes.get(NTU_BUSES[3]).append(stopName + "\n");
        }
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
    public static BusStopArrival getNTUBusArrivalTimings(BusStop stop) {
        BusStopArrival busStopArrival = new BusStopArrival();
        busStopArrival.busStop = stop;

        try {
            String code = stop.BusStopCode;
            if (stop.busType.isType(Type.NTU) && stop.busType.isType(Type.PUBLIC)) {
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

            //Now loop through the map and build the string
            for (Entry<String, ArrayList<Integer>> entry : timings.entrySet()) {
                BusArrival busArrival = new BusArrival();
                busArrival.serviceNo = busCode.get(entry.getKey());

                //Append the timings into the model
                if (entry.getValue().size() == 1) {
                    busArrival.arrivalTime1 = entry.getValue().get(0);
                    busArrival.arrivalTime2 = BusArrival.TIME_NA;
                } else if (entry.getValue().size() >= 2) {
                    busArrival.arrivalTime1 = entry.getValue().get(0);
                    busArrival.arrivalTime2 = entry.getValue().get(1);
                } else { //Add N/A if no timing is available
                    busArrival.arrivalTime1 = BusArrival.TIME_NA;
                    busArrival.arrivalTime2 = BusArrival.TIME_NA;
                }

                busStopArrival.busArrivals.add(busArrival);
            }
            return busStopArrival;
        } catch (Exception e) {
            logger.warn("Exception occurred at getNTUBusArrivalTimings with BusStop={}", stop, e);
            return null;
        }
    }
}
