package controller;

import java.util.Calendar;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import entity.BusStop;
import entity.BusStop.Type;
import entity.publicbus.PublicBusStopArrival;
import entity.publicbus.PublicBusStopArrivalContainer;
import entity.publicbus.PublicBusStopContainer;
import main.BusTimeBot;

public class PublicController {
	/**
	 * Retrieve bus stop data from LTA
	 */
	public static void getPublicBusStopData() {
		int skip=0;
		int stopCount=Integer.MAX_VALUE;
		while (stopCount>=50) { //Read until the number of stops read in is less than 50
			//Get 50 bus stops
			PublicBusStopContainer data = WebController.retrieveData("http://datamall2.mytransport.sg/ltaodataservice/BusStops?$skip="+skip, PublicBusStopContainer.class);
			//Update the stop count and number of stops to skip
			stopCount = data.value.size();
			skip += stopCount;
			//Copy to the total number of stops
			for (int i=0; i<data.value.size(); i++) {
				data.value.get(i).type = Type.PUBLIC_ONLY;
				BusTimeBot.bot.busStops.put(data.value.get(i).BusStopCode, data.value.get(i));
			}
		}
	}
	
	
	/**
	 * Get Public bus arrival timings from LTA datamall servers
	 * @param stop code for the Bus Stop
	 * @return A string of bus timings formatted properly
	 */
	public static String getPublicBusArrivalTimings(BusStop stop) {
		StringBuffer busArrivals = new StringBuffer();
		PublicBusStopArrivalContainer data = WebController.retrieveData("http://datamall2.mytransport.sg/ltaodataservice/BusArrival?BusStopID="+stop.BusStopCode+"&SST=True", PublicBusStopArrivalContainer.class);
		Emoji emoji = EmojiManager.getForAlias("oncoming_bus");
		for (PublicBusStopArrival services : data.Services) {
			busArrivals.append(emoji.getUnicode() + "*" + services.ServiceNo + "*: ");
			long firstEstimatedBus = Util.getTimeFromNow(services.NextBus.EstimatedArrival, Calendar.MINUTE);
			long secondEstimatedBus = Util.getTimeFromNow(services.SubsequentBus.EstimatedArrival, Calendar.MINUTE);
			
			//Construct string based on error and difference
			String firstEstimatedBusTime;
			if (firstEstimatedBus == Long.MAX_VALUE) {
				firstEstimatedBusTime = "N/A";
			} else if (firstEstimatedBus <= 0) {
				firstEstimatedBusTime = "Arr";
			} else {
				firstEstimatedBusTime = firstEstimatedBus + "min";
			}
			
			String secondEstimatedBusTime;
			if (secondEstimatedBus == Long.MAX_VALUE) {
				secondEstimatedBusTime = "";
			} else if (secondEstimatedBus <= 0) {
				secondEstimatedBusTime = "  |  Arr";
			} else {
				secondEstimatedBusTime = "  |  " + secondEstimatedBus + "min";
			}
			
			busArrivals.append(firstEstimatedBusTime + secondEstimatedBusTime);
			busArrivals.append("\n");
		}
		return busArrivals.toString();
	}
}
