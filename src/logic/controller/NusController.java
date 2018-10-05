package logic.controller;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;

import model.BusStop;
import model.BusStopMapping;
import model.BusType.Type;
import model.busarrival.BusArrival;
import model.busarrival.BusStopArrival;
import model.businfo.BusInfo;
import model.businfo.BusInfoDirection;
import model.json.nusbus.NusBusArrival;
import model.json.nusbus.NusBusArrivalContainer;
import model.json.nusbus.NusBusStop;
import model.json.nusbus.NusBusStopContainer;

/** Responsible for retrieving information regarding NUS */
public class NusController {
    public static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(NusController.class);
    public static final String[] NUS_BUSES = {"A1", "A2", "B1", "B2", "C", "D1", "D2", "BTC", "BTC1", "BTC2"};

    public static final BusInfoDirection NUS_A1_A2 = new BusInfoDirection("", "0715", "2300", "0715", "2300", "0900", "2300");
    public static final BusInfoDirection NUS_B1 = new BusInfoDirection("", "0715", "1900", "0715", "1900", "No Service", "No Service");
    public static final BusInfoDirection NUS_B2 = new BusInfoDirection("", "0715", "2300", "0715", "2300", "No Service", "No Service");
    public static final BusInfoDirection NUS_BTC = new BusInfoDirection("", "0720", "2130", "0830", "1230", "No Service", "No Service");
    public static final BusInfoDirection NUS_C = new BusInfoDirection("", "1000", "2300", "1000", "1900", "No Service", "No Service");
    public static final BusInfoDirection NUS_D1_D2 = new BusInfoDirection("", "0715", "2300", "0715", "2300", "0900", "2300");

    static {
        Arrays.sort(NUS_BUSES);
    }

    /**
     * Return information regarding NUS shuttle buses
     *
     * @param serviceNoUpper
     *            of NUS bus
     * @return a nicely formatted string with the first/last bus
     */
    public static BusInfo getNUSBusInfo(String serviceNo) {
        BusInfo busInfo = new BusInfo();

        //A1, A2, B1, B2, C, D1, D2, BTC/BTC1
        String serviceNoUpper = serviceNo.toUpperCase();

        //In order: Service Number, Weekday-Sat-Sun/P.H First and Last bus
        if (serviceNoUpper.equalsIgnoreCase("A1") || serviceNoUpper.equalsIgnoreCase("A2")) {
            busInfo = new BusInfo(serviceNoUpper, true, NUS_A1_A2);
        } else if (serviceNoUpper.equalsIgnoreCase("B1")) {
            busInfo = new BusInfo(serviceNoUpper, true, NUS_B1);
        } else if (serviceNoUpper.equalsIgnoreCase("B2")) {
            busInfo = new BusInfo(serviceNoUpper, true, NUS_B2);
        } else if (serviceNoUpper.equalsIgnoreCase("BTC") || serviceNoUpper.equalsIgnoreCase("BTC1") || serviceNoUpper.equalsIgnoreCase("BTC2")) {
            busInfo = new BusInfo(serviceNoUpper, true, NUS_BTC);
        } else if (serviceNoUpper.equalsIgnoreCase("C")) {
            busInfo = new BusInfo(serviceNoUpper, true, NUS_C);
        } else if (serviceNoUpper.equalsIgnoreCase("D1") || serviceNoUpper.equalsIgnoreCase("D2")) {
            busInfo = new BusInfo(serviceNoUpper, true, NUS_D1_D2);
        }

        return busInfo;
    }

    /**
     * Retrieve bus stop data from NUS and put it into the bot's busStops list
     */
    public static void getNUSBusStopData(HashMap<String, BusStop> busStops) {
        NusBusStopContainer NUSdata = WebController.retrieveData("http://nextbus.comfortdelgro.com.sg/testMethod.asmx/GetBusStops?output=json", NusBusStopContainer.class);
        //Loop through and convert to SG bus stops style
        for (NusBusStop stop : NUSdata.BusStopsResult.busstops) {
            if (BusStopMapping.getValue(stop.name) != null) { //Add on to public bus stop if it is the same bus stop (will be considered both NUS & Public bus stop)
                BusStop existingStop = busStops.get(BusStopMapping.getValue(stop.name));
                existingStop.nusStopCode = stop.name;
                existingStop.nusDescription = stop.caption;
                existingStop.busType.setTrue(Type.NUS);
            } else { //Otherwise it is most likely a NUS-only bus stop
                BusStop newStop = new BusStop();
                newStop.busType.setTrue(Type.NUS);
                newStop.BusStopCode = stop.name;
                newStop.Description = stop.caption;
                newStop.Latitude = stop.latitude;
                newStop.Longitude = stop.longitude;
                busStops.put(newStop.BusStopCode, newStop);
            }
        }
    }

    /**
     * Get NUS Shuttle Service bus arrival timings from comfort delgro servers
     *
     * @param stop
     *            code for the Bus Stop
     * @return A string of bus timings formatted properly
     */
    public static BusStopArrival getNUSArrivalTimings(BusStop stop) {
        BusStopArrival busStopArrival = new BusStopArrival();
        busStopArrival.busStop = stop;
        try {
            //Use the appropriate code
            String code = stop.BusStopCode;
            if (stop.busType.isType(Type.NUS) && stop.busType.isType(Type.PUBLIC)) {
                code = stop.nusStopCode;
            }

            NusBusArrivalContainer data = WebController.retrieveData("http://nextbus.comfortdelgro.com.sg/testMethod.asmx/GetShuttleService?busstopname=" + code, NusBusArrivalContainer.class);
            for (NusBusArrival s : data.ShuttleServiceResult.shuttles) {
                BusArrival busArrival = new BusArrival(true);
                busArrival.serviceNo = s.name;

                //We either get "Arr", "-" or a time in minutes
                if ("-".equals(s.arrivalTime)) { //No more bus service
                    busArrival.arrivalTime1 = BusArrival.TIME_NA;
                } else if ("Arr".equalsIgnoreCase(s.arrivalTime)) { //First bus arriving
                    busArrival.arrivalTime1 = BusArrival.TIME_ARRIVING;
                } else {
                    busArrival.arrivalTime1 = Long.parseLong(s.arrivalTime);
                }

                if ("-".equals(s.nextArrivalTime)) { //No more bus service, no need to append anything
                    busArrival.arrivalTime2 = BusArrival.TIME_NA;
                } else if ("Arr".equalsIgnoreCase(s.nextArrivalTime)) { //First bus arriving
                    busArrival.arrivalTime2 = BusArrival.TIME_ARRIVING;
                } else {
                    busArrival.arrivalTime2 = Long.parseLong(s.nextArrivalTime);
                }

                busStopArrival.busArrivals.add(busArrival);
            }
            return busStopArrival;
        } catch (Exception e) {
            logger.warn("Exception occurred at getNUSArrivalTimings with BusStop={}", stop, e);
            return null;
        }
    }
}
