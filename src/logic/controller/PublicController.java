package logic.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import logic.Util;
import main.Logger;
import model.BusInfo;
import model.BusInfoDirection;
import model.BusStop;
import model.json.publicbus.PublicBusStopArrival;
import model.json.publicbus.PublicBusStopArrivalContainer;
import model.json.publicbus.PublicBusStopContainer;

public class PublicController {
    /**
     * Retrieve bus stop data from LTA and put it into the bot's busStops list
     */
    public static void getPublicBusStopData(HashMap<String, BusStop> busStops) {
        int skip = 0;
        int stopCount = Integer.MAX_VALUE;
        while (stopCount >= 500) { //Read until the number of stops read in is less than 500
            //Get 500 bus stops
            PublicBusStopContainer data = WebController.retrieveData("http://datamall2.mytransport.sg/ltaodataservice/BusStops?$skip=" + skip, PublicBusStopContainer.class);
            //Update the stop count and number of stops to skip
            stopCount = data.value.size();
            skip += stopCount;
            //Copy to the total number of stops
            for (int i = 0; i < data.value.size(); i++) {
                data.value.get(i).isPublic = true;
                busStops.put(data.value.get(i).BusStopCode, data.value.get(i));
            }
        }
    }

    /**
     * Get Public bus arrival timings from LTA datamall servers
     *
     * @param stop
     *            code for the Bus Stop
     * @return A string of bus timings formatted properly
     */
    public static String getPublicBusArrivalTimings(BusStop stop) {
        try {
            StringBuilder busArrivals = new StringBuilder();
            PublicBusStopArrivalContainer data = WebController.retrieveData("http://datamall2.mytransport.sg/ltaodataservice/BusArrivalv2?BusStopCode=" + stop.BusStopCode, PublicBusStopArrivalContainer.class);
            Emoji emoji = EmojiManager.getForAlias("oncoming_bus");
            for (PublicBusStopArrival services : data.Services) {
                busArrivals.append(emoji.getUnicode() + Util.padBusTitle(services.ServiceNo) + ": ");
                long firstEstimatedBus = Util.getTimeFromNow(services.NextBus.EstimatedArrival, Calendar.MINUTE);
                long secondEstimatedBus = Util.getTimeFromNow(services.NextBus2.EstimatedArrival, Calendar.MINUTE);

                //Construct string based on error and difference
                String firstEstimatedBusTime;
                if (firstEstimatedBus == Long.MAX_VALUE) {
                    firstEstimatedBusTime = Util.padBusTime("N/A ");
                } else if (firstEstimatedBus <= 0) {
                    firstEstimatedBusTime = Util.padBusTime("Arr ");
                } else {
                    firstEstimatedBusTime = Util.padBusTime(firstEstimatedBus + "min");
                }

                String secondEstimatedBusTime;
                if (secondEstimatedBus == Long.MAX_VALUE) {
                    secondEstimatedBusTime = "";
                } else if (secondEstimatedBus <= 0) {
                    secondEstimatedBusTime = " | " + "Arr";
                } else {
                    secondEstimatedBusTime = " | " + secondEstimatedBus + "min";
                }

                busArrivals.append(firstEstimatedBusTime + secondEstimatedBusTime);
                busArrivals.append("\n");
            }
            return busArrivals.toString();
        } catch (Exception e) {
            Logger.logError(e);
            return null;
        }
    }

    /**
     * Returns a nicely formatted string that contains bus information on a public bus service number
     *
     * @param serviceNo
     * @return Nicely formatted string that contains public bus info (timings and interchange)
     */
    public static BusInfo getPublicBusInfo(String serviceNo) {
        BusInfo busInfo = new BusInfo();
        try {
            String response = WebController.sendHttpsRequest("https://www.transitlink.com.sg/eservice/eguide/service_route.php?service=" + URLEncoder.encode(serviceNo, StandardCharsets.UTF_8.toString()));
            //Extract the table
            String starting = "<section class=\"eguide-table\">";
            String ending = "</table>";
            response = response.substring(response.indexOf(starting) + starting.length(), response.indexOf(ending, response.indexOf(starting)) + ending.length());
            //System.out.println(response);
            boolean valid = response.startsWith("<table border=\"0\" cellspacing=\"0\" width=\"100%\"><tr>");
            String[] splitResponse = response.split("<tr>"); //5 if 2 way, 4 if 1 way/Night Rider
            //System.out.println(splitResponse.length);

            //Extract first and last bus timings
            if (valid) {
                busInfo.serviceNo = serviceNo.toUpperCase();
                busInfo.isValidServiceNo = true;

                if (busInfo.serviceNo.startsWith("NR")) { //For NR buses
                    String data = splitResponse[2];
                    String start = "<td width=\"60%\">";
                    String end = "</td>";
                    String time[] = data.substring(data.indexOf(start) + start.length(), data.lastIndexOf(end)).split(" - ");

                    start = "<td width=\"20%\">";
                    end = "</td>";
                    String interchange = data.substring(data.indexOf(start) + start.length(), data.indexOf(end));

                    busInfo.addBusInfoDirection(new BusInfoDirection(interchange, time[0], time[1]));
                } else { //Other buses
                    //First set (Since there can be 2 buses coming from different terminals)
                    String data = splitResponse[splitResponse.length - 1];
                    busInfo.addBusInfoDirection(extractPublicInformation(data));

                    if (splitResponse.length == 5) {//Second direction from another terminal
                        data = splitResponse[splitResponse.length - 2];
                        busInfo.addBusInfoDirection(extractPublicInformation(data));
                    }
                }
            }
        } catch (Exception e) {
            Logger.logError(e);
        }

        return busInfo;
    }

    /**
     * Extract the bus terminal, first/last bus for weekday/sat/sun & P.H
     *
     * @param data
     *            string containing the table taken from transitlink.com.sg
     * @return List of String (Size: 7) in this order: Interchange, Weekday 1st bus, Weekday last bus, Sat 1st bus, Sat last bus, Sun & P.H 1st bus, Sun & P.H last bus
     */
    public static BusInfoDirection extractPublicInformation(String data) {
        try {
            ArrayList<String> information = new ArrayList<String>();
            String[] timings = data.split("<td width=\"10%\" align=\"center\">");
            String busHeaderStart = "<td width=\"20%\">";
            information.add(timings[0].substring(timings[0].indexOf(busHeaderStart) + busHeaderStart.length(), timings[0].lastIndexOf("</td>")));
            for (int i = 1; i < timings.length; i++) {
                String time = timings[i];
                information.add(time.substring(0, time.lastIndexOf("</td>")));
            }
            BusInfoDirection busInfoDirection = new BusInfoDirection(information.get(0),
                                                                     information.get(1),
                                                                     information.get(2),
                                                                     information.get(3),
                                                                     information.get(4),
                                                                     information.get(5),
                                                                     information.get(6));
            return busInfoDirection;
        } catch (Exception e) {
            Logger.logError(e);
            return null;
        }
    }
}
