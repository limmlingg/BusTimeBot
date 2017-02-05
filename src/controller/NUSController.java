package controller;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import entity.BusStop;
import entity.BusStop.Type;
import entity.BusStopMapping;
import entity.nusbus.NUSBusArrival;
import entity.nusbus.NUSBusArrivalContainer;
import entity.nusbus.NUSBusStop;
import entity.nusbus.NUSBusStopContainer;
import main.BusTimeBot;
import main.Logger;

public class NUSController {
    /**
     * Retrieve bus stop data from NUS and put it into the bot's busStops list
     */
    public static void getNUSBusStopData() {
        NUSBusStopContainer NUSdata = WebController.retrieveData("http://nextbus.comfortdelgro.com.sg/testMethod.asmx/GetBusStops?output=json", NUSBusStopContainer.class);
        //Loop through and convert to SG bus stops style
        for (NUSBusStop stop : NUSdata.BusStopsResult.busstops) {
            if (BusStopMapping.getValue(stop.name) != null) { //Add on to public bus stop if it is the same bus stop (will be considered both NUS & Public bus stop)
                BusStop existingStop = BusTimeBot.bot.busStops.get(BusStopMapping.getValue(stop.name));
                existingStop.NUSStopCode = stop.name;
                existingStop.NUSDescription = stop.caption;
                existingStop.type = Type.PUBLIC_NUS;
            } else { //Otherwise it is most likely a NUS-only bus stop
                BusStop newStop = new BusStop();
                newStop.type = Type.NUS_ONLY;
                newStop.BusStopCode = stop.name;
                newStop.Description = stop.caption;
                newStop.Latitude = stop.latitude;
                newStop.Longitude = stop.longitude;
                BusTimeBot.bot.busStops.put(newStop.BusStopCode, newStop);
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
            StringBuffer busArrivals = new StringBuffer();
            //Use the appropriate code
            String code = stop.BusStopCode;
            if (stop.type == Type.PUBLIC_NUS) {
                code = stop.NUSStopCode;
            }

            NUSBusArrivalContainer data = WebController.retrieveData("http://nextbus.comfortdelgro.com.sg/testMethod.asmx/GetShuttleService?busstopname=" + code, NUSBusArrivalContainer.class);
            Emoji emoji = EmojiManager.getForAlias("oncoming_bus");
            for (NUSBusArrival s : data.ShuttleServiceResult.shuttles) {
                //Append the bus and the service name
                busArrivals.append(emoji.getUnicode() + "*" + s.name + "*: ");
                //We either get "Arr", "-" or a time in minutes
                String firstEstimatedBusTiming;
                if (s.arrivalTime.equals("-")) { //No more bus service
                    firstEstimatedBusTiming = "N/A";
                } else if (s.arrivalTime.equalsIgnoreCase("Arr")) { //First bus arriving
                    firstEstimatedBusTiming = s.arrivalTime;
                } else {
                    firstEstimatedBusTiming = s.arrivalTime + "min";
                }

                String secondEstimatedBusTiming;
                if (s.nextArrivalTime.equals("-")) { //No more bus service, no need to append anything
                    secondEstimatedBusTiming = "";
                } else if (s.nextArrivalTime.equalsIgnoreCase("Arr")) { //First bus arriving
                    secondEstimatedBusTiming = "  |  " + s.nextArrivalTime;
                } else {
                    secondEstimatedBusTiming = "  |  " + s.nextArrivalTime + "min";
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
