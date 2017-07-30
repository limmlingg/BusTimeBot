package logic.controller;

import java.util.Calendar;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import logic.Util;
import main.BusTimeBot;
import main.Logger;
import model.BusStop;
import model.json.publicbus.PublicBusStopArrival;
import model.json.publicbus.PublicBusStopArrivalContainer;
import model.json.publicbus.PublicBusStopContainer;

public class PublicController {
    /**
     * Retrieve bus stop data from LTA and put it into the bot's busStops list
     */
    public static void getPublicBusStopData() {
        int skip = 0;
        int stopCount = Integer.MAX_VALUE;
        while (stopCount >= 50) { //Read until the number of stops read in is less than 50
            //Get 50 bus stops
            PublicBusStopContainer data = WebController.retrieveData("http://datamall2.mytransport.sg/ltaodataservice/BusStops?$skip=" + skip, PublicBusStopContainer.class);
            //Update the stop count and number of stops to skip
            stopCount = data.value.size();
            skip += stopCount;
            //Copy to the total number of stops
            for (int i = 0; i < data.value.size(); i++) {
                data.value.get(i).isPublic = true;
                BusTimeBot.bot.busStops.put(data.value.get(i).BusStopCode, data.value.get(i));
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
            StringBuffer busArrivals = new StringBuffer();
            PublicBusStopArrivalContainer data = WebController.retrieveData("http://datamall2.mytransport.sg/ltaodataservice/BusArrival?BusStopID=" + stop.BusStopCode + "&SST=True", PublicBusStopArrivalContainer.class);
            Emoji emoji = EmojiManager.getForAlias("oncoming_bus");
            for (PublicBusStopArrival services : data.Services) {
                busArrivals.append(emoji.getUnicode() + Util.pad(services.ServiceNo, 13) + ": ");
                long firstEstimatedBus = Util.getTimeFromNow(services.NextBus.EstimatedArrival, Calendar.MINUTE);
                long secondEstimatedBus = Util.getTimeFromNow(services.SubsequentBus.EstimatedArrival, Calendar.MINUTE);

                //Construct string based on error and difference
                String firstEstimatedBusTime;
                if (firstEstimatedBus == Long.MAX_VALUE) {
                    firstEstimatedBusTime = Util.pad("N/A ", 5);
                } else if (firstEstimatedBus <= 0) {
                    firstEstimatedBusTime = Util.pad("Arr ", 5);
                } else {
                    firstEstimatedBusTime = Util.pad(firstEstimatedBus + "min", 5);
                }

                String secondEstimatedBusTime;
                if (secondEstimatedBus == Long.MAX_VALUE) {
                    secondEstimatedBusTime = "";
                } else if (secondEstimatedBus <= 0) {
                    secondEstimatedBusTime = " | Arr";
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
}
