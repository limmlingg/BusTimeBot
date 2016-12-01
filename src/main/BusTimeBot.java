package main;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TimeZone;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.api.methods.ParseMode;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Location;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.google.gson.Gson;
import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import common.BusStop;
import common.BusStop.Type;
import common.BusStopMapping;
import nusbus.NUSBusArrival;
import nusbus.NUSBusArrivalContainer;
import nusbus.NUSBusStop;
import nusbus.NUSBusStopContainer;
import postalToCoordinates.GeoCodeContainer;
import publicbus.PublicBusStopArrival;
import publicbus.PublicBusStopArrivalContainer;
import publicbus.PublicBusStopContainer;

public class BusTimeBot extends TelegramLongPollingBot{

	public static void main(String[] args) {
		//Favourites command
		ApiContextInitializer.init();
		TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
	    try {
	        telegramBotsApi.registerBot(new BusTimeBot());
	    } catch (TelegramApiException e) {
	        e.printStackTrace();
	    }
	}
	
    public static String TELEGRAM_TOKEN;
    public static String LTA_TOKEN;
    public HashMap<String, BusStop> busStops;
    public HashMap<Long, Date> lastQueried; //use to prevent spamming of the update button
    public double distance = 0.3;
    public boolean dev = true; //Use a different bot if we use dev (rmb to change back)
	
	public BusTimeBot() {
		super();
		try {
			lastQueried = new HashMap<Long, Date>();
			busStops = new HashMap<String, BusStop>(10000);
			TELEGRAM_TOKEN = Files.readAllLines(Paths.get("keys/telegram_key").toAbsolutePath()).get(dev? 1 : 0);
			LTA_TOKEN = Files.readAllLines(Paths.get("keys/lta_key").toAbsolutePath()).get(0);
			//Get Busstop Locations
			getBusStopData();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onUpdateReceived(Update update) {
		if (update.hasCallbackQuery()) { //If the user presses update
			CallbackQuery callbackQuery = update.getCallbackQuery();
			
			//Update the timings if the query has been longer than 1 second
			Date lastQuery = lastQueried.get(callbackQuery.getMessage().getChatId());
			long timeSince = lastQuery==null? Long.MAX_VALUE : getTimeFromNow(format.format(lastQuery), Calendar.SECOND)*-1;
			if (timeSince>1) { //Only update if more than 1 second has passed
				EditMessageText editMessageText = new EditMessageText();
				editMessageText.setInlineMessageId(callbackQuery.getInlineMessageId());
				editMessageText.setChatId(callbackQuery.getMessage().getChatId());
				editMessageText.setMessageId(callbackQuery.getMessage().getMessageId());
				editMessageText.setParseMode(ParseMode.MARKDOWN);
				
				//New timings to update
				String[] data = callbackQuery.getData().split(":");
				double latitude = Double.parseDouble(data[0]);
				double longitude = Double.parseDouble(data[1]);
				SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
				timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
				String lastUpdated = "_Last updated at " + timeFormat.format(new Date()) + "_";
				editMessageText.setText(getNearbyBusStopsAndTimings(latitude, longitude) + lastUpdated);
				
				//Re-add the inline keyboard
	            editMessageText.setReplyMarkup(createUpdateInlineKeyboard(latitude, longitude));
	            try {
					editMessageText(editMessageText);
				} catch (TelegramApiException e) {}	//Ignore if MessageNotEdited error
	            lastQueried.put(callbackQuery.getMessage().getChatId(), new Date());
			}
			
			//Send answer indicating a reply
			AnswerCallbackQuery answer = new AnswerCallbackQuery();
			answer.setCallbackQueryId(callbackQuery.getId());
			
			try {
				answerCallbackQuery(answer);
			} catch (TelegramApiException e) {} //Ignore query errors, probably expired queries

		} if (update.hasMessage()) { //Standard messages
			Message message = update.getMessage();
			long chatId = message.getChatId();
			String text = message.getText();
			Location location = message.getLocation();
			if (text != null) {
				if (text.equalsIgnoreCase("/start")) {
			        SendMessage sendMessage = new SendMessage();
			        sendMessage.setChatId(chatId);
			        sendMessage.enableMarkdown(true);
			        sendMessage.setReplyMarkup(createSendLocationKeyboard());
			        sendMessage.setText("Send me your location and get your bus timings!\n"
			        		+ "Alternatively, you can type /search <<Address or postal code>> to find by terms");
			        try {
			        	if (chatId > 0) {
			        		sendMessage(sendMessage);
			        	}
					} catch (TelegramApiException e) {
						e.printStackTrace();
					}
				} else if (text.startsWith("/search ")) { //Search by postal code
					GeoCodeContainer results = retrieveData("http://maps.googleapis.com/maps/api/geocode/json?address="+text.replace("/search ", "").replace(" ", "%20"), GeoCodeContainer.class);
					if (results.status.equals("OK")) {
						double lat = results.results.get(0).geometry.location.lat;
						double lon = results.results.get(0).geometry.location.lng;
						//Send the message to user
						SendMessage sendMessage = new SendMessage();
						sendMessage.setChatId(chatId);
						sendMessage.setText(getNearbyBusStopsAndTimings(lat, lon));
						sendMessage.setParseMode(ParseMode.MARKDOWN);
			            sendMessage.setReplyMarkup(createUpdateInlineKeyboard(lat, lon));
			            try {
							sendMessage(sendMessage);
						} catch (TelegramApiException e) {
							e.printStackTrace();
						}
					} else {
						sendMessage("Unable to find location", chatId);
					}
				} else if (text.startsWith("/add")) {
					
				}
			} else if (location != null) {//By Location
				//Send the message to user
				SendMessage sendMessage = new SendMessage();
				sendMessage.setChatId(chatId);
				sendMessage.setText(getNearbyBusStopsAndTimings(location));
				sendMessage.setParseMode(ParseMode.MARKDOWN);
	            sendMessage.setReplyMarkup(createUpdateInlineKeyboard(location));
	            try {
					sendMessage(sendMessage);
				} catch (TelegramApiException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Creates an inline keyboard with the update button for users to update the bus timings
	 * @param location of the user
	 * @return an InlineKeyboardMarkup with the button update
	 */
	public InlineKeyboardMarkup createUpdateInlineKeyboard(Location location) {
		return createUpdateInlineKeyboard(location.getLatitude(), location.getLongitude());
	}
	
	/**
	 * Creates an inline keyboard with the update button for users to update the bus timings
	 * @param latitude of the user
	 * @param longitude of the user
	 * @return an InlineKeyboardMarkup with the button update
	 */
	public InlineKeyboardMarkup createUpdateInlineKeyboard(double latitude, double longitude) {
		InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> btns = new LinkedList<List<InlineKeyboardButton>>();
        List<InlineKeyboardButton> firstRow = new LinkedList<InlineKeyboardButton>();
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText("Update");
        btn.setCallbackData(latitude +":" + longitude);
        firstRow.add(btn);
        btns.add(firstRow);
        inlineKeyboard.setKeyboard(btns);
        return inlineKeyboard;
	}
	
	/**
	 * Create a keyboard for the user to send their location
	 * @return keyboardMarkup that allows user to send location
	 */
	public ReplyKeyboardMarkup createSendLocationKeyboard() {
		ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(false);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        KeyboardButton button = new KeyboardButton();
        button.setText("Send Location");
        button.setRequestLocation(true);
        keyboardFirstRow.add(button);
        keyboard.add(keyboardFirstRow);
        replyKeyboardMarkup.setKeyboard(keyboard);
        return replyKeyboardMarkup;
	}
	
	/**
	 * Gets a nicely formatted String of text of the 5 nearest bus stops and their bus timings
	 */ 
	public String getNearbyBusStopsAndTimings(Location location) {
		return getNearbyBusStopsAndTimings(location.getLatitude(), location.getLongitude());
	}
	
	/**
	 * Gets a nicely formatted String of text of the 5 nearest bus stops and their bus timings
	 */ 
	public String getNearbyBusStopsAndTimings(double latitude, double longitude) {
		PriorityQueue<BusStop> busstops = getNearbyBusStops(latitude, longitude);
		StringBuffer stops = new StringBuffer();
		int count = 0;
		while (!busstops.isEmpty() && count < 5) {
			BusStop stop = busstops.poll();
			Emoji emoji = EmojiManager.getForAlias("busstop");
			
			if (stop.type == Type.NUS_ONLY) {
				stops.append(emoji.getUnicode() + "*"+stop.Description+"*");
				stops.append("\n");
				stops.append(getNUSArrivalTimings(stop));
			} else if (stop.type == Type.PUBLIC_ONLY) {
				stops.append(emoji.getUnicode() + stop.BusStopCode + " " + "*"+stop.Description+"*");
				stops.append("\n");
				stops.append(getPublicBusArrivalTimings(stop));
			} else if (stop.type == Type.BOTH) {
				stops.append(emoji.getUnicode() + (stop.BusStopCode + " ") + "*" + stop.Description + "*/" + "*"+stop.NUSDescription+"*");
				stops.append("\n");
				stops.append(getNUSArrivalTimings(stop));
				stops.append(getPublicBusArrivalTimings(stop));
			}
			stops.append("\n");
			count++;
		}
		
		if (stops.toString().equals("")) {
			stops.append("No stops nearby\n");
		}
		
		return stops.toString();
	}
	
	/**
	 * Get near by bus stops of a given location
	 * @param latitude of the user
	 * @param longitude of the user
	 * @return a list of bus stops near that location sorted by distance
	 */
	public PriorityQueue<BusStop> getNearbyBusStops(double latitude, double longitude) {
		PriorityQueue<BusStop> nearbyBusStops = new PriorityQueue<BusStop>(30);
		for (BusStop stop : busStops.values()) {	
			if (stop.getDistance(latitude, longitude) <= distance) {
				nearbyBusStops.add(stop);
			}
		}
		return nearbyBusStops;
	}
	
	/**
	 * Get NUS Shuttle Service bus arrival timings from comfort delgro servers
	 * @param stop code for the Bus Stop
	 * @return A string of bus timings formatted properly
	 */
	public String getNUSArrivalTimings(BusStop stop) {
		StringBuffer busArrivals = new StringBuffer();
		//Use the appropiate code
		String code = stop.BusStopCode;
		if (stop.type == Type.BOTH) {
			code = stop.NUSStopCode;
		}
		
		NUSBusArrivalContainer data = retrieveData("http://nextbus.comfortdelgro.com.sg/testMethod.asmx/GetShuttleService?busstopname="+code, NUSBusArrivalContainer.class);
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
	}
	
	/**
	 * Get Public bus arrival timings from LTA datamall servers
	 * @param stop code for the Bus Stop
	 * @return A string of bus timings formatted properly
	 */
	public String getPublicBusArrivalTimings(BusStop stop) {
		StringBuffer busArrivals = new StringBuffer();
		PublicBusStopArrivalContainer data = retrieveData("http://datamall2.mytransport.sg/ltaodataservice/BusArrival?BusStopID="+stop.BusStopCode+"&SST=True", PublicBusStopArrivalContainer.class);
		Emoji emoji = EmojiManager.getForAlias("oncoming_bus");
		for (PublicBusStopArrival services : data.Services) {
			busArrivals.append(emoji.getUnicode() + "*" + services.ServiceNo + "*: ");
			long firstEstimatedBus = getTimeFromNow(services.NextBus.EstimatedArrival, Calendar.MINUTE);
			long secondEstimatedBus = getTimeFromNow(services.SubsequentBus.EstimatedArrival, Calendar.MINUTE);
			
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
	
	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
	/**
	 * Get time difference of the input date from the current time 
	 * @param date string that is in "yyyy-MM-dd'T'HH:mm:ssXXX" format
	 * @return (if Calendar.SECOND, return in seconds otherwise in minutes), Long.MAX_VALUE if date is invalid
	 */
	public long getTimeFromNow(String date, int type) {
		long difference;
		try {
			long now = new Date().getTime();
			long designatedTime = format.parse(date).getTime();
			long divisor = type==Calendar.SECOND? 1000 : (60*1000); 
			difference = (designatedTime-now)/divisor;
		} catch (ParseException e) {
			difference = Long.MAX_VALUE;
		}
		return difference;
	}
	
	/**
	 * Retrieve Bus stop data from NUS & LTA
	 */
	public void getBusStopData() {
		getPublicBusStopData();
		getNUSBusStopData();
		System.out.println("Bus stop data loaded!");
	}
	
	/**
	 * Retrieve bus stop data from LTA
	 */
	public void getPublicBusStopData() {
		int skip=0;
		int stopCount=Integer.MAX_VALUE;
		while (stopCount>=50) { //Read until the number of stops read in is less than 50
			//Get 50 bus stops
			PublicBusStopContainer data = retrieveData("http://datamall2.mytransport.sg/ltaodataservice/BusStops?$skip="+skip, PublicBusStopContainer.class);
			//Update the stop count and number of stops to skip
			stopCount = data.value.size();
			skip += stopCount;
			//Copy to the total number of stops
			for (int i=0; i<data.value.size(); i++) {
				data.value.get(i).type = Type.PUBLIC_ONLY;
				busStops.put(data.value.get(i).BusStopCode, data.value.get(i));
			}
		}
	}
	
	/**
	 * Retrieve bus stop data from NUS
	 */
	public void getNUSBusStopData() {
		NUSBusStopContainer NUSdata = retrieveData("http://nextbus.comfortdelgro.com.sg/testMethod.asmx/GetBusStops?output=json", NUSBusStopContainer.class);
		//Loop through and convert to SG bus stops style
		for (NUSBusStop stop : NUSdata.BusStopsResult.busstops) {
			if (BusStopMapping.getValue(stop.name) != null) { //Add on to public bus stop if it is the same bus stop (will be considered both NUS & Public bus stop)
				BusStop existingStop = busStops.get(BusStopMapping.getValue(stop.name));
				existingStop.NUSStopCode = stop.name;
				existingStop.NUSDescription = stop.caption;
				existingStop.type = Type.BOTH;
			} else { //Otherwise it is most likely a NUS-only bus stop
				BusStop newStop = new BusStop();
				newStop.type = Type.NUS_ONLY;
				newStop.BusStopCode = stop.name;
				newStop.Description = stop.caption;
				newStop.Latitude = stop.latitude;
				newStop.Longitude = stop.longitude;
				busStops.put(newStop.BusStopCode, newStop);
			}
		}
	}
	
	/**
	 * Sends a message to the given chat id
	 * @param message to send to the chat id
	 * @param id of the chat group
	 * @return if the message is sent successfully
	 */
	public boolean sendMessage(String message, long id) {
		SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(Long.toString(id)); //who should get from the message the sender that sent it.
        sendMessageRequest.setText(message);
        sendMessageRequest.setParseMode(ParseMode.MARKDOWN); //Allow bold & italics
        boolean success = false;
        try {
            sendMessage(sendMessageRequest); //at the end, so some magic and send the message ;)
            success = true;
        } catch (TelegramApiException e) {
        	e.printStackTrace();
        }
        return success;
	}
	
	/**
	 * Retrieve data from the URL and cast it to the class provided
	 * @param url of the server to retrieve data from
	 * @param objectClass class to cast the data into
	 * @return An object of the class provided
	 */
	public <T> T retrieveData(String url, Class<T> objectClass) {
		return jsonToObject(sendHTTPRequest(url), objectClass);
	}
	
	/**
	 * Send a GET HTTP request to the url indicated and returns the response
	 * @param url of the server to retrieve data from
	 * @return response returned from the Webserver
	 */
	public String sendHTTPRequest(String url) {
		StringBuilder result = new StringBuilder();
		try {
			URL urlSite = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) urlSite.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("AccountKey", LTA_TOKEN);
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result.toString();
	}
	
	/**
	 * Converts JSON data to the appropiate class given (Case-Sensitive for variables)
	 * @param json data
	 * @param objectClass class of the Object to cast to
	 * @return An object of the class provided
	 */
	public <T> T jsonToObject (String json, Class<T> objectClass) {
		Gson gson = new Gson();
		//To make sure json is json, we extract only from the first { to the last }
		//System.out.println(json);
		json = json.substring(json.indexOf("{"), json.lastIndexOf("}")+1);
		return gson.fromJson(json, objectClass);
	}

	/**
	 * @return the username of the telegram bot
	 */
	@Override
	public String getBotUsername() {
		return "bus_time_bot";
	}

	/**
	 * @return the telegram bot's token
	 */
	@Override
	public String getBotToken() {
		return TELEGRAM_TOKEN;
	}

}
