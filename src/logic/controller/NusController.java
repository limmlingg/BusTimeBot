package logic.controller;

import java.util.Arrays;
import java.util.HashMap;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import logic.Util;
import main.Logger;
import model.BusStop;
import model.BusStopMapping;
import model.businfo.BusInfo;
import model.businfo.BusInfoDirection;
import model.json.nusbus.NusBusArrival;
import model.json.nusbus.NusBusArrivalContainer;
import model.json.nusbus.NusBusStop;
import model.json.nusbus.NusBusStopContainer;

/** Responsible for retrieving information regarding NUS */
public class NusController {
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
                existingStop.isNus = true;
            } else { //Otherwise it is most likely a NUS-only bus stop
                BusStop newStop = new BusStop();
                newStop.isNus = true;
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
    public static String getNUSArrivalTimings(BusStop stop) {
        try {
            StringBuilder busArrivals = new StringBuilder();
            //Use the appropriate code
            String code = stop.BusStopCode;
            if (stop.isNus && stop.isPublic) {
                code = stop.nusStopCode;
            }

            NusBusArrivalContainer data = WebController.retrieveData("http://nextbus.comfortdelgro.com.sg/testMethod.asmx/GetShuttleService?busstopname=" + code, NusBusArrivalContainer.class);
            Emoji emoji = EmojiManager.getForAlias("oncoming_bus");
            for (NusBusArrival s : data.ShuttleServiceResult.shuttles) {
                //Append the bus and the service name
                busArrivals.append(emoji.getUnicode() + Util.padBusTitle(s.name) + ": ");
                //We either get "Arr", "-" or a time in minutes
                String firstEstimatedBusTiming;
                if ("-".equals(s.arrivalTime)) { //No more bus service
                    firstEstimatedBusTiming = "N/A ";
                } else if ("Arr".equalsIgnoreCase(s.arrivalTime)) { //First bus arriving
                    firstEstimatedBusTiming = Util.padBusTime(s.arrivalTime);
                } else {
                    firstEstimatedBusTiming = Util.padBusTime(s.arrivalTime + "min");
                }

                String secondEstimatedBusTiming;
                if ("-".equals(s.nextArrivalTime)) { //No more bus service, no need to append anything
                    secondEstimatedBusTiming = "";
                } else if ("Arr".equalsIgnoreCase(s.nextArrivalTime)) { //First bus arriving
                    secondEstimatedBusTiming = " | " + s.nextArrivalTime;
                } else {
                    secondEstimatedBusTiming = " | " + s.nextArrivalTime + "min";
                }

                busArrivals.append(firstEstimatedBusTiming + secondEstimatedBusTiming);
                busArrivals.append("\n");
            }
            return busArrivals.toString();
        } catch (Exception e) {
            Logger.logError(e);
            return null;
        }
    }
}
