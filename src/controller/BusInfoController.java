package controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import main.Logger;

public class BusInfoController {
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
    public static String getNTUBusInfo(String serviceNo) {
        //CL-Blue, CL-Red, CR, CWR
        String serviceNoUpper = serviceNo.toUpperCase();
        ArrayList<String> busTiming = new ArrayList<String>();
        //In order: Service Number, Weekday-Sat-Sun/P.H First and Last bus
        if (serviceNoUpper.equalsIgnoreCase("CL-Blue")) {
            busTiming.add(serviceNoUpper);
            busTiming.addAll(Arrays.asList("0800", "2300", "0800", "2300", "0800", "2300"));
        } else if (serviceNoUpper.equalsIgnoreCase("CL-Red")) {
            busTiming.add(serviceNoUpper);
            busTiming.addAll(Arrays.asList("0800", "2300", "0800", "2300", "0800", "2300"));
        } else if (serviceNoUpper.equalsIgnoreCase("CR")) {
            busTiming.add(serviceNoUpper);
            busTiming.addAll(Arrays.asList("0730", "2300", "0730", "2300", "0730", "2300"));
        } else if (serviceNoUpper.equalsIgnoreCase("CWR")) {
            busTiming.add(serviceNoUpper);
            busTiming.addAll(Arrays.asList("0730", "2300", "0730", "2300", "0730", "2300"));
        }

        if (busTiming.size() <= 0) {
            return "No such bus service";
        } else {
            return formatInformation(busTiming);
        }
    }

    /**
     * Return information regarding NUS shuttle buses
     *
     * @param serviceNoUpper
     *            of NUS bus
     * @return a nicely formatted string with the first/last bus
     */
    public static String getNUSBusInfo(String serviceNo) {
        //A1, A2, B1, B2, C, D1, D2, BTC/BTC1
        String serviceNoUpper = serviceNo.toUpperCase();
        ArrayList<String> busTiming = new ArrayList<String>();
        //In order: Service Number, Weekday-Sat-Sun/P.H First and Last bus
        if (serviceNoUpper.equalsIgnoreCase("A1") || serviceNoUpper.equalsIgnoreCase("A2")) {
            busTiming.add(serviceNoUpper);
            busTiming.addAll(Arrays.asList("0715", "2300", "0715", "2300", "0900", "2300"));
        } else if (serviceNoUpper.equalsIgnoreCase("B1")) {
            busTiming.add(serviceNoUpper);
            busTiming.addAll(Arrays.asList("0715", "1900", "0715", "1900", "No Service", "No Service"));
        } else if (serviceNoUpper.equalsIgnoreCase("B2")) {
            busTiming.add(serviceNoUpper);
            busTiming.addAll(Arrays.asList("0715", "2300", "0715", "2300", "No Service", "No Service"));
        } else if (serviceNoUpper.equalsIgnoreCase("BTC") || serviceNoUpper.equalsIgnoreCase("BTC1")) {
            busTiming.add(serviceNoUpper);
            busTiming.addAll(Arrays.asList("0720", "2130", "0830", "1230", "No Service", "No Service"));
        } else if (serviceNoUpper.equalsIgnoreCase("C")) {
            busTiming.add(serviceNoUpper);
            busTiming.addAll(Arrays.asList("1000", "2300", "1000", "1900", "No Service", "No Service"));
        } else if (serviceNoUpper.equalsIgnoreCase("D1") || serviceNoUpper.equalsIgnoreCase("D2")) {
            busTiming.add(serviceNoUpper);
            busTiming.addAll(Arrays.asList("0715", "2300", "0715", "2300", "0900", "2300"));
        }

        if (busTiming.size() <= 0) {
            return "No such bus service";
        } else {
            return formatInformation(busTiming);
        }
    }

    /**
     * Returns a nicely formatted string that contains bus information on a public bus service number
     *
     * @param serviceNo
     * @return Nicely formatted string that contains public bus info (timings and interchange)
     */
    public static String getPublicBusInfo(String serviceNo) {
        try {
            String response = WebController.sendHTTPRequest("http://www.transitlink.com.sg/eservice/eguide/service_route.php?service=" + URLEncoder.encode(serviceNo, StandardCharsets.UTF_8.toString()), false);
            //Extract the table
            String starting = "<section class=\"eguide-table\">";
            String ending = "</table>";
            response = response.substring(response.indexOf(starting) + starting.length(), response.indexOf(ending, response.indexOf(starting)) + ending.length());
            //System.out.println(response);
            boolean valid = response.startsWith("<table border=\"0\" cellspacing=\"0\" width=\"100%\"><tr>");
            String[] splitResponse = response.split("<tr>"); //5 if 2 way, 4 if 1 way/Night Rider
            //System.out.println(splitResponse.length);
            StringBuffer formattedInformation = new StringBuffer();
            //Extract first and last bus timings
            if (valid) {
                formattedInformation.append("*Service " + serviceNo.toUpperCase() + "*\n");
                if (serviceNo.startsWith("NR")) { //For NR buses
                    String data = splitResponse[2];
                    String start = "<td width=\"60%\">";
                    String end = "</td>";
                    String time[] = data.substring(data.indexOf(start) + start.length(), data.lastIndexOf(end)).split(" - ");

                    start = "<td width=\"20%\">";
                    end = "</td>";
                    String interchange = data.substring(data.indexOf(start) + start.length(), data.indexOf(end));

                    //System.out.println(interchange + ": " + time[0] + " - " + time[1]);
                    ArrayList<String> timeInfo = new ArrayList<String>();
                    timeInfo.add(interchange);
                    timeInfo.add(time[0]);
                    timeInfo.add(time[1]);
                    formattedInformation.append(formatNRInformation(timeInfo));
                } else { //Other buses
                    //First set
                    String data = splitResponse[splitResponse.length - 1];
                    formattedInformation.append(formatInformation(extractPublicInformation(data)) + "\n\n");

                    if (splitResponse.length == 5) {//Second set
                        data = splitResponse[splitResponse.length - 2];
                        formattedInformation.append(formatInformation(extractPublicInformation(data)));
                    }
                }
            } else {
                formattedInformation.append("No such bus service");
            }
            return formattedInformation.toString();
        } catch (Exception e) {
            Logger.logError(e);
            return "No such bus service";
        }
    }

    /**
     * Format information in a present-able manner
     *
     * @param information
     *            List of String (Size: 7) in this order: Header, Weekday 1st bus, Weekday last bus, Sat 1st bus, Sat last bus, Sun & P.H 1st bus, Sun & P.H last bus
     * @return Formatted string of bus information
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
            Logger.logError(e);
            return null;
        }
    }

    /**
     * Format Night Rider information from SBST website
     *
     * @param information
     *            List in order {Service number, first bus, last bus}
     * @return Formatted string of bus information
     */
    public static String formatNRInformation(ArrayList<String> information) {
        try {
            if (information.size() != 3) {
                return null;
            }

            StringBuffer busInfo = new StringBuffer("*" + information.get(0) + "*\n");
            busInfo.append("*Fri, Sat, Sun & eve of P.H*\n");
            busInfo.append("1st Bus: " + information.get(1) + " | Last Bus: " + information.get(2));
            return busInfo.toString();
        } catch (Exception e) {
            Logger.logError(e);
            return null;
        }
    }

    /**
     * Extract the bus terminal, first/last bus for weekday/sat/sun & P.H
     *
     * @param data
     *            string containing the table taken from transitlink.com.sg
     * @return List of String (Size: 7) in this order: Interchange, Weekday 1st bus, Weekday last bus, Sat 1st bus, Sat last bus, Sun & P.H 1st bus, Sun & P.H last bus
     */
    public static ArrayList<String> extractPublicInformation(String data) {
        try {
            ArrayList<String> information = new ArrayList<String>();
            String[] timings = data.split("<td width=\"10%\" align=\"center\">");
            String busHeaderStart = "<td width=\"20%\">";
            information.add(timings[0].substring(timings[0].indexOf(busHeaderStart) + busHeaderStart.length(), timings[0].lastIndexOf("</td>")));
            for (int i = 1; i < timings.length; i++) {
                String time = timings[i];
                information.add(time.substring(0, time.lastIndexOf("</td>")));
            }
            return information;
        } catch (Exception e) {
            Logger.logError(e);
            return null;
        }
    }
}
