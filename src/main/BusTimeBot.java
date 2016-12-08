package main;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import entity.LocationComparator;
import entity.geocoding.GeoCodeContainer;

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
	    } catch (TelegramApiException e) {
	        e.printStackTrace();
	    }
	}
	
	public static BusTimeBot bot;
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
			TELEGRAM_TOKEN = Files.readAllLines(Paths.get("src/keys/telegram_key").toAbsolutePath()).get(dev? 1 : 0);
			LTA_TOKEN = Files.readAllLines(Paths.get("src/keys/lta_key").toAbsolutePath()).get(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onUpdateReceived(Update update) {
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
				text = message.getText().replace("@BusTimeBot", ""); //Don't need the "@BusTimeBot" to handle commands
				if (text.equalsIgnoreCase("/start") || text.equalsIgnoreCase("/help")) {
					String welcomeText = "\nSend me your location (Using the GPS) and get your bus timings(Public, NUS shuttle, NTU shuttle)!\n\n"
							+ "Look up bus information by typing /bus <Service Number>, bus timings shown is the timing when the bus leaves the interchange\n"
							+ "Example: /bus 969\n\n"
			        		+ "You can type /search <Popular names/postal/address/bus stop number>\n"
			        		+ "Some examples:\n"
			        		+ "/search amk hub\n"
			        		+ "/search 118426\n"
			        		+ "/search Blk 1 Hougang Ave 1\n"
			        		+ "/search 63151";
					if (chatId > 0) {
		        		sendMessage(welcomeText, chatId, createSendLocationKeyboard());
		        	} else {
		        		sendMessage(welcomeText, chatId, null);
		        	}
				} else if (text.startsWith("/search")) { //Search by postal code
					Logger.log("Got a search request by " + message.getFrom() + " for " + text.replace("/search ", "") + "\n");
					if (text.equalsIgnoreCase("/search")) {
						sendMessage("Search for an address or postal code (Example: /search 118426)", chatId, null);
					} else { 
						GeoCodeContainer results = WebController.retrieveData("https://gothere.sg/a/search?q="+text.replace("/search ", "").replace(" ", "%20"), GeoCodeContainer.class);
						if (results.status == 1) {
							double lat = results.where.markers.get(0).getLatitude();
							double lon = results.where.markers.get(0).getLongitude();
	
							String nearbyBusStops = getNearbyBusStopsAndTimings(lat, lon);
				            sendMessage(nearbyBusStops, chatId, createUpdateInlineKeyboard(lat, lon));
				            Logger.log("Returned:\n" + nearbyBusStops);
						} else {
							sendMessage("Unable to find location", chatId, null);
							Logger.log("Returned:\nUnable to find location");
						}
					}
					Logger.log("======================================================\n");
				} else if (text.startsWith("/bus")) {
					text = text.replaceAll("/bus ", "");
					if (text.equalsIgnoreCase("/bus")) {
						sendMessage("Type /bus <Service Number> to look up first and last bus timings!\n"
								+ "Example: /bus 969", chatId, null);
					} else if (Character.isDigit(text.charAt(0)) || text.startsWith("BPS") || text.startsWith("NR") || text.startsWith("CT")) {//searching for public bus information
						sendMessage(BusInfoController.getPublicBusInfo(text), chatId, null);
					} else if (text.equalsIgnoreCase("CL-Blue") ||
							text.equalsIgnoreCase("CL-Red") ||
							text.equalsIgnoreCase("CR") ||
							text.equalsIgnoreCase("CWR")) { //NTU bus data
						sendMessage(BusInfoController.getNTUBusInfo(text), chatId, null);
					} else if (text.startsWith("A") || text.startsWith("B") || text.startsWith("C") || text.startsWith("D") || 
							text.startsWith("a") || text.startsWith("b") || text.startsWith("c") || text.startsWith("d")) { //NUS bus data 
						sendMessage(BusInfoController.getNUSBusInfo(text), chatId, null);
					} else {
						sendMessage("No such bus service", chatId, null);
					}
				}
			} else if (location != null) {//By Location
				Logger.log("Got a location request by " + message.getFrom() + " for " + location +"\n");
				//Send the message to user
				String nearbyBusStops = getNearbyBusStopsAndTimings(location);
				sendMessage(nearbyBusStops, chatId, createUpdateInlineKeyboard(location));
	            Logger.log("Returned:\n"+nearbyBusStops+"\n======================================================\n");
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
		StringBuffer allStops = new StringBuffer();
		int count = 0;
		while (!busstops.isEmpty() && count < 5) {
			BusStop stop = busstops.poll();
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
				count++;	
			}
		}
		
		if (allStops.toString().equals("")) {
			allStops.append("No stops nearby\n\n");
		}
		
		return allStops.toString();
	}
	
	/**
	 * Get near by bus stops of a given location
	 * @param latitude of the user
	 * @param longitude of the user
	 * @return a list of bus stops near that location sorted by distance
	 */
	public PriorityQueue<BusStop> getNearbyBusStops(double latitude, double longitude) {
		PriorityQueue<BusStop> nearbyBusStops = new PriorityQueue<BusStop>(30, new LocationComparator(latitude, longitude));
		for (BusStop stop : busStops.values()) {	
			if (stop.getDistance(latitude, longitude) <= distance) {
				nearbyBusStops.add(stop);
			}
		}
		return nearbyBusStops;
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
		System.out.println("All bus stop data loaded!");
	}
	
	/**
	 * Sends a message to the given chat id
	 * @param message to send to the chat id
	 * @param id of the chat group
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
        } catch (TelegramApiException e) {
        	e.printStackTrace();
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
