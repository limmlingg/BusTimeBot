package logic.controller;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import logic.Util;
import main.BusTimeBot;
import main.Logger;
import model.BusStop;
import model.BusStopMapping;
import model.json.nusbus.NusBusArrival;
import model.json.nusbus.NusBusArrivalContainer;
import model.json.nusbus.NusBusStop;
import model.json.nusbus.NusBusStopContainer;

public class NusController {
    /**
     * Retrieve bus stop data from NUS and put it into the bot's busStops list
     */
    public static void getNUSBusStopData() {
        NusBusStopContainer NUSdata = WebController.retrieveData("http://nextbus.comfortdelgro.com.sg/testMethod.asmx/GetBusStops?output=json", NusBusStopContainer.class);
        //Loop through and convert to SG bus stops style
        for (NusBusStop stop : NUSdata.BusStopsResult.busstops) {
            if (BusStopMapping.getValue(stop.name) != null) { //Add on to public bus stop if it is the same bus stop (will be considered both NUS & Public bus stop)
                BusStop existingStop = BusTimeBot.bot.busStops.get(BusStopMapping.getValue(stop.name));
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
