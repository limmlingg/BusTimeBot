package main;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import datastructures.kdtree.KdTree;
import datastructures.kdtree.NearestNeighborIterator;
import factory.KeyboardFactory;
import logic.LocationDistanceFunction;
import logic.PropertiesLoader;
import logic.Util;
import logic.controller.BusInfoController;
import logic.controller.NTUController;
import logic.controller.NUSController;
import logic.controller.PublicController;
import logic.controller.WebController;
import model.BusStop;
import model.BusStop.Type;
import model.geocoding.GeoCodeContainer;

public class BusTimeBot extends TelegramLongPollingBot {

    public static final String TELEGRAM_BOT_NAME = "bus_time_bot";
    public static BusTimeBot bot;
    public static String TELEGRAM_TOKEN;
    public static String LTA_TOKEN;

    public HashMap<String, BusStop> busStops;
    public KdTree<BusStop> busStopsSortedByCoordinates;
    public HashMap<Long, Date> lastQueried; //use to prevent spamming of the update button
    public PropertiesLoader propertiesLoader;
    public double maxDistanceFromPoint = 0.35;

    //Development and Debugging attributes
    public boolean isDev = true;
    private static final String DEBUG_UPDATE_TEXT = "Got an update call by {0}";
    private static final String DEBUG_SEARCH_TEXT = "Got a search request by {0} for {1}\n";
    private static final String DEBUG_BUS_TEXT = "Got a bus info request by {0} for {1}\n";
    private static final String DEBUG_LOCATION_TEXT = "Got a location request by {0} for {1}\n";

    //Message Texts
    public static final String WELCOME_TEXT = "Send me your location (Using the GPS) and get your bus timings(Public, NUS shuttle, NTU shuttle)!\n\n" +
                                              "Look up bus information by typing /bus <Service Number>, bus timings shown is the timing when the bus leaves the interchange\n" +
                                              "Example: /bus 969\n\n" + "You can type /search <Popular names/postal/address/bus stop number>\n" +
                                              "Some examples:\n" +
                                              "/search amk hub\n" +
                                              "/search 118426\n" +
                                              "/search Blk 1 Hougang Ave 1\n" +
                                              "/search 63151\n\n" +
                                              "Contact @SimpleLegend for bugs/suggestions!";
    private static final String SEARCH_HELP_TEXT = "Search for an address or postal code (Example: /search 118426)";
    private static final String BUS_HELP_TEXT = "Type /bus <Service Number> to look up first and last bus timings!\n" + "Example: /bus 969";
    private static final String LAST_UPDATED_TEXT = "\n\n_Last updated: {0}_";

    //Emoji alias
    private static final String EMOJI_BUSSTOP = "busstop";
    private static final String EMOJI_ONCOMING_BUS = "oncoming_bus";

    //Commands
    public static final String COMMAND_START = "/start";
    public static final String COMMAND_HELP = "/help";
    public static final String COMMAND_SEARCH = "/search";
    public static final String COMMAND_BUS = "/bus";

    //Keywords for replacement
    public static final String KEYWORD_BOT_MENTION = "@" + TELEGRAM_BOT_NAME;
    public static final String KEYWORD_SEARCH = "/search ";
    public static final String KEYWORD_BUS = "/bus ";

    public static void main(String[] args) {
        WebController.trustAll();
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            bot = new BusTimeBot();
            //Initialize bus stop data (we run it this way to prevent the api from registering the bot before it is fully initialized)
            bot.getBusStopData();
            telegramBotsApi.registerBot(bot);
        } catch (Exception e) {
            Logger.logError(e);
        }
    }

    public BusTimeBot() {
        super();
        try {
            propertiesLoader = new PropertiesLoader();
            loadProperties(isDev);
            lastQueried = new HashMap<Long, Date>();
            busStops = new HashMap<String, BusStop>(10000);
            busStopsSortedByCoordinates = new KdTree<BusStop>(2, 100);
        } catch (Exception e) {
            Logger.logError(e);
        }
    }

    /**
     * Loads properties into token variables
     */
    private void loadProperties(boolean isDev) {
        LTA_TOKEN = propertiesLoader.getLtaToken();
        if (isDev) {
            TELEGRAM_TOKEN = propertiesLoader.getTelegramDevToken();
        } else {
            TELEGRAM_TOKEN = propertiesLoader.getTelegramToken();
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) { //If the user presses update
                executeUpdateCommand(update);
            } else if (update.hasMessage()) { //Standard messages
                Message message = update.getMessage();
                if (message.getText() != null) {
                    String text = removeMention(message.getText()); //Don't need the "@BusTimeBot" to handle commands
                    String command = getCommand(text);
                    switch (command) {
                    case COMMAND_START: //Fall through
                    case COMMAND_HELP:
                        executeHelpCommand(message.getChatId());
                        break;
                    case COMMAND_SEARCH: //Search by postal code/popular names/bus stop
                        executeSearchCommand(message);
                        break;
                    case COMMAND_BUS: //searching for bus info
                        executeBusCommand(message);
                        break;
                    default : //Do nothing
                        break;
                    }
                } else if (message.getLocation() != null) {//By Location
                    executeLocationRequestCommand(message);
                }
            }
        } catch (Exception e) {
            Logger.logError(e);
        }
    }

    /**
     * Process Update Request
     */
    public void executeUpdateCommand(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Logger.log(MessageFormat.format(DEBUG_UPDATE_TEXT, callbackQuery.getFrom()));

        //Update the timings if the query has been longer than 1 second
        Date lastQuery = lastQueried.get(callbackQuery.getMessage().getChatId());
        long timeSince = lastQuery == null ? Long.MAX_VALUE : Util.getTimeFromNow(Util.format.format(lastQuery), Calendar.SECOND) * -1;
        if (timeSince > 1) { //Only update if more than 1 second has passed
            EditMessageText editMessageText = generateEditedMessage(callbackQuery);
            Logger.log("\nReturned:\n" + editMessageText.getText());

            try {
                editMessageText(editMessageText);
            } catch (TelegramApiException e) {
            } //Ignore if MessageNotEdited error
            lastQueried.put(callbackQuery.getMessage().getChatId(), new Date());
        }
        Logger.log(Logger.DEBUG_SEPARATOR);

        //Send answer indicating a reply
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQuery.getId());

        try {
            answerCallbackQuery(answer);
        } catch (TelegramApiException e) {
        } //Ignore query errors, probably expired queries
    }

    /**
     * Remove any instance of {@value #KEYWORD_BOT_MENTION}
     */
    public String removeMention(String text) {
        return text.replaceAll(KEYWORD_BOT_MENTION, "");
    }

    /**
     * Process request with location sent
     */
    public void executeLocationRequestCommand(Message message) {
        long chatId = message.getChatId();
        Location location = message.getLocation();

        Logger.log(MessageFormat.format(DEBUG_LOCATION_TEXT, message.getFrom(), location));
        String nearbyBusStops = getNearbyBusStopsAndTimings(location);
        sendMessage(nearbyBusStops, chatId, KeyboardFactory.createUpdateInlineKeyboard(location));
        Logger.log("Returned:\n" + nearbyBusStops + Logger.DEBUG_SEPARATOR);
    }

    /**
     * Process /bus commands
     */
    public void executeBusCommand(Message message) {
        long chatId = message.getChatId();
        String text = message.getText();

        Logger.log(MessageFormat.format(DEBUG_BUS_TEXT, message.getFrom(), text));
        text = text.toLowerCase().replaceAll(KEYWORD_BUS, "").toUpperCase();
        String info;
        if (text.equalsIgnoreCase(COMMAND_BUS) || text.isEmpty()) {
            info = BUS_HELP_TEXT;
        } else if (Arrays.binarySearch(BusInfoController.NTUBus, text) >= 0) { //NTU bus data
            info = BusInfoController.getNTUBusInfo(text);
        } else if (Arrays.binarySearch(BusInfoController.NUSBus, text) >= 0) { //NUS bus data
            info = BusInfoController.getNUSBusInfo(text);
        } else {
            info = BusInfoController.getPublicBusInfo(text);
        }
        sendMessage(info, chatId);
        Logger.log("Returned:\n" + info + Logger.DEBUG_SEPARATOR);
    }

    /**
     * Process /search <location> commands
     */
    public void executeSearchCommand(Message message) throws UnsupportedEncodingException {
        long chatId = message.getChatId();
        String text = message.getText();

        Logger.log(MessageFormat.format(DEBUG_SEARCH_TEXT, message.getFrom(), text));
        text = text.toLowerCase().replace(KEYWORD_SEARCH, "");
        if (text.equalsIgnoreCase(COMMAND_SEARCH) || text.isEmpty()) { //Give help text if only /search was given
            sendMessage(SEARCH_HELP_TEXT, chatId);
        } else {
            GeoCodeContainer results = WebController.retrieveData("https://gothere.sg/a/search?q=" + URLEncoder.encode(text, StandardCharsets.UTF_8.toString()), GeoCodeContainer.class);
            Logger.log("Returned:\n");
            if (results != null && results.status == 1) {
                double lat = results.where.markers.get(0).getLatitude();
                double lon = results.where.markers.get(0).getLongitude();

                String nearbyBusStops = getNearbyBusStopsAndTimings(lat, lon);
                sendMessage(nearbyBusStops, chatId, KeyboardFactory.createUpdateInlineKeyboard(lat, lon));
                Logger.log(nearbyBusStops);
            } else {
                sendMessage("Unable to find location", chatId);
                Logger.log("Unable to find location");
            }
        }
        Logger.log(Logger.DEBUG_SEPARATOR);
    }

    /**
     * Process /help and /start commands
     * @param chatId
     */
    public void executeHelpCommand(long chatId) {
        if (chatId > 0) { //is a 1-1 chat (+ve)
            sendMessage(WELCOME_TEXT, chatId, KeyboardFactory.createSendLocationKeyboard());
        } else { //No location keyboard for group chats
            sendMessage(WELCOME_TEXT, chatId);
        }
    }

    /**
     * Extract command from the user's message
     *
     * @param text
     *            which has the command replaced '@bus_time_bot'
     * @return command
     */
    public String getCommand(String text) {
        return text.split(" ")[0].toLowerCase();
    }

    /**
     * @param callbackQuery
     *            details of the user's callback
     * @return EditMessageText object to be sent to the user
     */
    public EditMessageText generateEditedMessage(CallbackQuery callbackQuery) {
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
        String lastUpdated = MessageFormat.format(LAST_UPDATED_TEXT, timeFormat.format(new Date()));
        editMessageText.setText(getNearbyBusStopsAndTimings(latitude, longitude) + lastUpdated);

        //Re-add the inline keyboard
        editMessageText.setReplyMarkup(KeyboardFactory.createUpdateInlineKeyboard(latitude, longitude));
        return editMessageText;
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
            StringBuilder allStops = new StringBuilder();
            while (busstops.hasNext()) {
                BusStop stop = busstops.next();
                Emoji emoji = EmojiManager.getForAlias(EMOJI_BUSSTOP);

                StringBuffer stops = new StringBuffer();
                if (stop.type == Type.NUS_ONLY) {
                    stops.append(emoji.getUnicode() + "*" + stop.Description + "*");
                    stops.append("\n```\n");
                    stops.append(NUSController.getNUSArrivalTimings(stop));
                    stops.append("```");
                } else if (stop.type == Type.PUBLIC_ONLY) {
                    stops.append(emoji.getUnicode() + stop.BusStopCode + " " + stop.Description);
                    stops.append("\n```\n");
                    stops.append(PublicController.getPublicBusArrivalTimings(stop));
                    stops.append("```");
                } else if (stop.type == Type.NTU_ONLY) {
                    stops.append(emoji.getUnicode() + stop.BusStopCode + " " + "*" + stop.Description + "*");
                    stops.append("\n```\n");
                    stops.append(NTUController.getNTUBusArrivalTimings(stop));
                    stops.append("```");
                } else if (stop.type == Type.PUBLIC_NUS) {
                    stops.append(emoji.getUnicode() + (stop.BusStopCode + " ") + "*" + stop.Description + "*/" + "*" + stop.NUSDescription + "*");
                    stops.append("\n```\n");
                    stops.append(NUSController.getNUSArrivalTimings(stop));
                    stops.append(PublicController.getPublicBusArrivalTimings(stop));
                    stops.append("```");
                } else if (stop.type == Type.PUBLIC_NTU) {
                    stops.append(emoji.getUnicode() + (stop.BusStopCode + " ") + "*" + stop.Description + "*/" + "*" + stop.NTUDescription + "*");
                    stops.append("\n```\n");
                    stops.append(NTUController.getNTUBusArrivalTimings(stop));
                    stops.append(PublicController.getPublicBusArrivalTimings(stop));
                    stops.append("```");
                }

                //If there exist an oncoming_bus emoji, then we append, otherwise that bus stop has no buses
                emoji = EmojiManager.getForAlias(EMOJI_ONCOMING_BUS);
                if (stops.toString().contains(emoji.getUnicode())) {
                    allStops.append(stops.toString());
                    allStops.append("\n");
                }
            }

            if (allStops.length() == 0) {
                allStops.append("No stops nearby");
            }

            return allStops.toString().trim();
        } catch (Exception e) {
            Logger.logError(e);
            return null;
        }
    }

    /**
     * Get near by bus stops of a given location
     *
     * @param latitude
     *            of the user
     * @param longitude
     *            of the user
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
                if (distance < this.maxDistanceFromPoint) {
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
     *
     * @param message
     * @param id
     *            of the chat group
     * @return success of sending the message
     */
    public boolean sendMessage(String message, long id) {
        return sendMessage(message, id, null);
    }

    /**
     * Sends a message to the given chat id
     *
     * @param message
     *            to send to the chat id
     * @param id
     *            of the chat group
     * @param keyboard
     *            to attach to message if there is any
     * @return success of sending the message
     */
    public boolean sendMessage(String message, long id, ReplyKeyboard keyboard) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(Long.toString(id)); //who should get from the message the sender that sent it.
        sendMessageRequest.setText(message);
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setParseMode(ParseMode.MARKDOWN); //Allow bold & italics
        if (keyboard != null) {
            sendMessageRequest.setReplyMarkup(keyboard);
        }
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
        return TELEGRAM_BOT_NAME;
    }

    /**
     * @return the telegram bot's token
     */
    @Override
    public String getBotToken() {
        return TELEGRAM_TOKEN;
    }

}
