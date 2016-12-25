package main;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import controller.BusInfoController;
import controller.NTUController;
import controller.NUSController;
import controller.PublicController;
import controller.Util;
import controller.WebController;
import entity.BusStop;
import entity.BusStop.Type;
import entity.LocationDistanceFunction;
import entity.geocoding.GeoCodeContainer;
import entity.kdtree.KdTree;
import entity.kdtree.NearestNeighborIterator;

public class BusTimeBot extends TelegramLongPollingBot{

	public static void main(String[] args) {
		WebController.trustAll();
		ApiContextInitializer.init();
		TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
	    try {
	    	bot = new BusTimeBot();
	    	//Initialize bus stop data
	    	bot.getBusStopData();   	
	        telegramBotsApi.registerBot(bot);
	    } catch (Exception e) {
			Logger.logError(e);
	    }
	}
	
	public static BusTimeBot bot;
    public static String TELEGRAM_TOKEN;
    public static String LTA_TOKEN;
    public HashMap<String, BusStop> busStops;
    public KdTree<BusStop> busStopsSortedByCoordinates;
    public HashMap<Long, Date> lastQueried; //use to prevent spamming of the update button
    public double distance = 0.35;
    public boolean dev = true; //Use a different bot if we use dev (rmb to change back)
    
	public BusTimeBot() {
		super();
		try {
			lastQueried = new HashMap<Long, Date>();
			busStops = new HashMap<String, BusStop>(10000); 
			busStopsSortedByCoordinates = new KdTree<BusStop>(2, 100);
			TELEGRAM_TOKEN = Files.readAllLines(Paths.get("src/keys/telegram_key").toAbsolutePath()).get(dev? 1 : 0);
			LTA_TOKEN = Files.readAllLines(Paths.get("src/keys/lta_key").toAbsolutePath()).get(0);
		} catch (Exception e) {
			Logger.logError(e);
		}
	}
	
	@Override
	public void onUpdateReceived(Update update) {
		try {
			if (update.hasCallbackQuery()) { //If the user presses update
				CallbackQuery callbackQuery = update.getCallbackQuery();
				Logger.log(("Got an update call by " + callbackQuery.getFrom()));
				
				//Update the timings if the query has been longer than 1 second
				Date lastQuery = lastQueried.get(callbackQuery.getMessage().getChatId());
				long timeSince = lastQuery==null? Long.MAX_VALUE : Util.getTimeFromNow(Util.format.format(lastQuery), Calendar.SECOND)*-1;
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
					Logger.log("\nReturned:\n" + editMessageText.getText());
					
					//Re-add the inline keyboard
		            editMessageText.setReplyMarkup(createUpdateInlineKeyboard(latitude, longitude));
		            try {
						editMessageText(editMessageText);
					} catch (TelegramApiException e) {}	//Ignore if MessageNotEdited error
		            lastQueried.put(callbackQuery.getMessage().getChatId(), new Date());
				}
				Logger.log("\n\n======================================================\n");
				
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
					text = message.getText().replace("@bus_time_bot", ""); //Don't need the "@BusTimeBot" to handle commands
					if (text.equalsIgnoreCase("/start") || text.equalsIgnoreCase("/help")) {
						String welcomeText = "\nSend me your location (Using the GPS) and get your bus timings(Public, NUS shuttle, NTU shuttle)!\n\n"
								+ "Look up bus information by typing /bus <Service Number>, bus timings shown is the timing when the bus leaves the interchange\n"
								+ "Example: /bus 969\n\n"
				        		+ "You can type /search <Popular names/postal/address/bus stop number>\n"
				        		+ "Some examples:\n"
				        		+ "/search amk hub\n"
				        		+ "/search 118426\n"
				        		+ "/search Blk 1 Hougang Ave 1\n"
				        		+ "/search 63151\n\n"
				        		+ "Contact @SimpleLegend for bugs/suggestions!";
						if (chatId > 0) {
			        		sendMessage(welcomeText, chatId, createSendLocationKeyboard());
			        	} else {
			        		sendMessage(welcomeText, chatId);
			        	}
					} else if (text.startsWith("/search")) { //Search by postal code
						Logger.log("Got a search request by " + message.getFrom() + " for " + text.replace("/search ", "") + "\n");
						if (text.equalsIgnoreCase("/search")) {
							sendMessage("Search for an address or postal code (Example: /search 118426)", chatId);
						} else { 
							GeoCodeContainer results = WebController.retrieveData("https://gothere.sg/a/search?q="+URLEncoder.encode(text.replace("/search ", ""), StandardCharsets.UTF_8.toString()), GeoCodeContainer.class);
							if (results.status == 1) {
								double lat = results.where.markers.get(0).getLatitude();
								double lon = results.where.markers.get(0).getLongitude();
		
								String nearbyBusStops = getNearbyBusStopsAndTimings(lat, lon);
					            sendMessage(nearbyBusStops, chatId, createUpdateInlineKeyboard(lat, lon));
					            Logger.log("Returned:\n" + nearbyBusStops);
							} else {
								sendMessage("Unable to find location", chatId);
								Logger.log("Returned:\nUnable to find location");
							}
						}
						Logger.log("======================================================\n");
					} else if (text.startsWith("/bus")) {
						Logger.log("Got a bus info request by " + message.getFrom() + " for " + text.replace("/bus ", "") + "\n");
						text = text.replaceAll("/bus ", "").toUpperCase();
						String info;
						if (text.equalsIgnoreCase("/bus")) {
							info = "Type /bus <Service Number> to look up first and last bus timings!\n"
									+ "Example: /bus 969";
						} else if (Arrays.binarySearch(BusInfoController.NTUBus, text) >= 0) { //NTU bus data
							info = BusInfoController.getNTUBusInfo(text);
						} else if (Arrays.binarySearch(BusInfoController.NUSBus, text) >= 0) { //NUS bus data 
							info = BusInfoController.getNUSBusInfo(text);
						} else {
							info = BusInfoController.getPublicBusInfo(text);
						}
						sendMessage(info, chatId);
			            Logger.log("Returned:\n"+info+"\n======================================================\n");
					}
				} else if (location != null) {//By Location
					Logger.log("Got a location request by " + message.getFrom() + " for " + location +"\n");
					//Send the message to user
					String nearbyBusStops = getNearbyBusStopsAndTimings(location);
					sendMessage(nearbyBusStops, chatId, createUpdateInlineKeyboard(location));
		            Logger.log("Returned:\n"+nearbyBusStops+"\n======================================================\n");
				}
			}
		} catch (Exception e) {
			Logger.logError(e);
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
		try {
			Iterator<BusStop> busstops = getNearbyBusStops(latitude, longitude);
			StringBuffer allStops = new StringBuffer();
			while (busstops.hasNext()) {
				BusStop stop = busstops.next();
				Emoji emoji = EmojiManager.getForAlias("busstop");
				
				StringBuffer stops = new StringBuffer();
				if (stop.type == Type.NUS_ONLY) {
					stops.append(emoji.getUnicode() + "*"+stop.Description+"*");
					stops.append("\n");
					stops.append(NUSController.getNUSArrivalTimings(stop));
				} else if (stop.type == Type.PUBLIC_ONLY) {
					stops.append(emoji.getUnicode() + stop.BusStopCode + " " + "*"+stop.Description+"*");
					stops.append("\n");
					stops.append(PublicController.getPublicBusArrivalTimings(stop));
				} else if (stop.type == Type.NTU_ONLY) {
					stops.append(emoji.getUnicode() + stop.BusStopCode + " " + "*"+stop.Description+"*");
					stops.append("\n");
					stops.append(NTUController.getNTUBusArrivalTimings(stop));
				} else if (stop.type == Type.PUBLIC_NUS) {
					stops.append(emoji.getUnicode() + (stop.BusStopCode + " ") + "*" + stop.Description + "*/" + "*"+stop.NUSDescription+"*");
					stops.append("\n");
					stops.append(NUSController.getNUSArrivalTimings(stop));
					stops.append(PublicController.getPublicBusArrivalTimings(stop));
				} else if (stop.type == Type.PUBLIC_NTU) {
					stops.append(emoji.getUnicode() + (stop.BusStopCode + " ") + "*" + stop.Description + "*/" + "*"+stop.NTUDescription+"*");
					stops.append("\n");
					stops.append(NTUController.getNTUBusArrivalTimings(stop));
					stops.append(PublicController.getPublicBusArrivalTimings(stop));
				}
				
				//If there exist an oncoming_bus emoji, then we append, otherwise that bus stop has no buses
				emoji = EmojiManager.getForAlias("oncoming_bus");
				if (stops.toString().contains(emoji.getUnicode())) {
					allStops.append(stops.toString());
					allStops.append("\n");
				}
			}
			
			if (allStops.toString().equals("")) {
				allStops.append("No stops nearby\n\n");
			}
			
			return allStops.toString();
		} catch (Exception e) {
			Logger.logError(e);
			return null;
		}
	}
	
	/**
	 * Get near by bus stops of a given location
	 * @param latitude of the user
	 * @param longitude of the user
	 * @return a list of bus stops near that location sorted by distance
	 */
	public Iterator<BusStop> getNearbyBusStops(double latitude, double longitude) {
		try {
			ArrayList<BusStop> busstops = new ArrayList<BusStop>();
			double[] searchPoint = {latitude, longitude};
	    	NearestNeighborIterator<BusStop> result = bot.busStopsSortedByCoordinates.getNearestNeighborIterator(searchPoint, 5, new LocationDistanceFunction());
	    	Iterator<BusStop> iterator = result.iterator();
	    	while (iterator.hasNext()) {
	    		BusStop stop = iterator.next();
	    		double distance = Util.getDistance(stop.Latitude, stop.Longitude, latitude, longitude);
	    		if (distance < this.distance) {
	    			busstops.add(stop);
	    		}
	    	}
			return busstops.iterator();
		} catch (Exception e) {
			Logger.logError(e);
			return null;
		}
	}
	
	/**
	 * Retrieve Bus stop data from NUS & LTA
	 */
	public void getBusStopData() {
		System.out.println("Retrieving Public Bus Stop Data");
		PublicController.getPublicBusStopData();
		System.out.println("Retrieving NUS Bus Stop Data");
		NUSController.getNUSBusStopData();
		System.out.println("Retrieving NTU Bus Stop Data");
		NTUController.getNTUBusStopData();
		System.out.println("Populating KD-tree");
    	//Populate the KD-tree after merging bus stops
    	for (BusStop stop : bot.busStops.values()) {
    		double[] point = new double[2];
    		point[0] = stop.Latitude;
    		point[1] = stop.Longitude;
    		bot.busStopsSortedByCoordinates.addPoint(point, stop);
    	}
		System.out.println("All bus stop data loaded!");
	}
	
	/**
	 * Sends a message to the given chat id
	 * @param message
	 * @param id of the chat group
	 * @return
	 */
	public boolean sendMessage(String message, long id) {
		return sendMessage(message, id, null);
	}
	
	/**
	 * Sends a message to the given chat id
	 * @param message to send to the chat id
	 * @param id of the chat group
	 * @param keyboard to attach to message if there is any
	 * @return if the message is sent successfully
	 */
	public boolean sendMessage(String message, long id, ReplyKeyboard keyboard) {
		SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(Long.toString(id)); //who should get from the message the sender that sent it.
        sendMessageRequest.setText(message);
        if (keyboard!=null) {
        	sendMessageRequest.setReplyMarkup(keyboard);
        }
        sendMessageRequest.setParseMode(ParseMode.MARKDOWN); //Allow bold & italics
        boolean success = false;
        try {
            sendMessage(sendMessageRequest); //at the end, so some magic and send the message ;)
            success = true;
        } catch (Exception e) {
        	Logger.logError(e);
        }
        return success;
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
