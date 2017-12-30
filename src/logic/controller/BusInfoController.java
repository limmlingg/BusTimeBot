package logic.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import main.Logger;
import model.BusInfo;
import model.BusInfoDirection;

public class BusInfoController {
    //Bus information
    public static final BusInfoDirection NTU_CLBLUE = new BusInfoDirection("", "0800", "2300", "0800", "2300", "0800", "2300");
    public static final BusInfoDirection NTU_CLRED = new BusInfoDirection("", "0800", "2300", "0800", "2300", "0800", "2300");
    public static final BusInfoDirection NTU_CR = new BusInfoDirection("", "0730", "2300", "0730", "2300", "0730", "2300");
    public static final BusInfoDirection NTU_CWR = new BusInfoDirection("", "0730", "2300", "0730", "2300", "0730", "2300");

    public static final BusInfoDirection NUS_A1_A2 = new BusInfoDirection("", "0715", "2300", "0715", "2300", "0900", "2300");
    public static final BusInfoDirection NUS_B1 = new BusInfoDirection("", "0715", "1900", "0715", "1900", "No Service", "No Service");
    public static final BusInfoDirection NUS_B2 = new BusInfoDirection("", "0715", "2300", "0715", "2300", "No Service", "No Service");
    public static final BusInfoDirection NUS_BTC = new BusInfoDirection("", "0720", "2130", "0830", "1230", "No Service", "No Service");
    public static final BusInfoDirection NUS_C = new BusInfoDirection("", "1000", "2300", "1000", "1900", "No Service", "No Service");
    public static final BusInfoDirection NUS_D1_D2 = new BusInfoDirection("", "0715", "2300", "0715", "2300", "0900", "2300");

    //Bus Types
    public static String[] NUSBus = {"A1", "A2", "B1", "B2", "C", "D1", "D2", "BTC", "BTC1"};
    public static String[] NTUBus = {"CL-BLUE", "CL-RED", "CR", "CWR"};
    static {
        Arrays.sort(NUSBus);
        Arrays.sort(NTUBus);
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
