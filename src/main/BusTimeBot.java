package main;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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

import datastructures.kdtree.KdTree;
import factory.KeyboardFactory;
import logic.PropertiesLoader;
import logic.Util;
import logic.callbackhandler.AnswerCallbackQueryHandler;
import logic.callbackhandler.EditMessageCallbackHandler;
import logic.callbackhandler.SendMessageCallbackHandler;
import logic.command.BusCommand;
import logic.command.Command;
import logic.command.LocationCommand;
import logic.command.SearchCommand;
import logic.command.StartHelpCommand;
import logic.controller.NtuController;
import logic.controller.NusController;
import logic.controller.PublicController;
import logic.controller.WebController;
import model.BusStop;
import model.CommandResponse;

public class BusTimeBot extends TelegramLongPollingBot {

    public static final String TELEGRAM_BOT_NAME = "bus_time_bot";
    public static BusTimeBot bot;
    public static String TELEGRAM_TOKEN;
    public static String LTA_TOKEN;
    private static final String KEYWORD_BOT_MENTION = "@" + TELEGRAM_BOT_NAME;

    public HashMap<String, BusStop> busStops;
    public KdTree<BusStop> busStopsSortedByCoordinates;
    public HashMap<Long, Date> lastQueried; //use to prevent spamming of the update button
    public PropertiesLoader propertiesLoader;
    public double maxDistanceFromPoint = 0.35;

    //Development and Debugging attributes
    public boolean isDev = true;

    //Message Texts
    private static final String LAST_UPDATED_TEXT = "\n_Last updated: {0}_";

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
            boolean isGroupChat = true;
            Command command = null;
            if (update.hasCallbackQuery()) { //If the user presses update
                executeUpdateCommand(update);
            } else if (update.hasMessage()) { //Standard messages
                Message message = update.getMessage();
                isGroupChat = (message.getChatId() < 0) ? true : false;
                if (message.getText() != null) {
                    String text = removeMention(message.getText()); //Don't need the "@BusTimeBot" to handle commands
                    String commandText = getCommand(text);

                    switch (commandText) {
                    case StartHelpCommand.COMMAND_START: //Fall through
                    case StartHelpCommand.COMMAND_HELP:
                        command = new StartHelpCommand();
                        break;
                    case SearchCommand.COMMAND: //Search by postal code/popular names/bus stop
                        command = new SearchCommand(text);
                        break;
                    case BusCommand.COMMAND: //searching for bus info
                        command = new BusCommand(text);
                        break;
                    }
                } else if (message.getLocation() != null) {//By Location
                    Location location = update.getMessage().getLocation();
                    command = new LocationCommand(location.getLatitude(), location.getLongitude());
                }
            }

            //Process extra data, usually the keyboard stuff
            if (command != null) {
                CommandResponse reply = command.execute();
                ReplyKeyboard keyboard = null;

                if (!isGroupChat) { //To single user only
                    keyboard = KeyboardFactory.createSendLocationKeyboard();
                }

                if (reply.data != null) { //Append update button if responseCommand contains location
                    double latitude = Double.parseDouble(reply.data.get("latitude"));
                    double longitude = Double.parseDouble(reply.data.get("longitude"));
                    keyboard = KeyboardFactory.createUpdateInlineKeyboard(latitude, longitude);
                }
                sendMessage(reply.text, update.getMessage().getChatId(), keyboard);
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

        //Update the timings if the query has been longer than 1 second
        Date lastQuery = lastQueried.get(callbackQuery.getMessage().getChatId());
        long timeSince = lastQuery == null ? Long.MAX_VALUE : Util.getTimeFromNow(Util.format.format(lastQuery), Calendar.SECOND) * -1;
        if (timeSince > 1) { //Only update if more than 1 second has passed
            EditMessageText editMessageText = generateEditedMessage(callbackQuery);

            try {
                editMessageTextAsync(editMessageText, new EditMessageCallbackHandler());
            } catch (TelegramApiException e) {
            } //Ignore if MessageNotEdited error
            lastQueried.put(callbackQuery.getMessage().getChatId(), new Date());
        }

        //Send answer indicating a reply
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQuery.getId());

        try {
            answerCallbackQueryAsync(answer, new AnswerCallbackQueryHandler());
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

        Command busTimeCommand = new LocationCommand(latitude, longitude);
        CommandResponse answer = busTimeCommand.execute();

        editMessageText.setText(answer.text + lastUpdated);

        //Re-add the inline keyboard
        editMessageText.setReplyMarkup(KeyboardFactory.createUpdateInlineKeyboard(latitude, longitude));
        return editMessageText;
    }


    /**
     * Retrieve Bus stop data from NUS & LTA
     */
    public void getBusStopData() {
        System.out.println("Retrieving Public Bus Stop Data");
        PublicController.getPublicBusStopData();
        System.out.println("Retrieving NUS Bus Stop Data");
        NusController.getNUSBusStopData();
        System.out.println("Retrieving NTU Bus Stop Data");
        NtuController.getNTUBusStopData();
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
        sendMessageRequest.setParseMode(ParseMode.MARKDOWN); //Allow styling of text
        if (keyboard != null) {
            sendMessageRequest.setReplyMarkup(keyboard);
        }
        boolean success = false;
        try {
            sendMessageAsync(sendMessageRequest, new SendMessageCallbackHandler()); //at the end, so some magic and send the message ;)
            success = true;
        } catch (Exception e) {
            Logger.logError(e);
        }
        return success;
    }

    /**
     * @return the name of the telegram bot
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
