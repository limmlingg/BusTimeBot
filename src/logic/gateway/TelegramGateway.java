package logic.gateway;

import java.io.InputStream;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

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
import model.BusStop;
import model.CommandResponse;
import model.CommandResponseType;
import model.busarrival.BusArrival;
import model.busarrival.BusStopArrival;
import model.busarrival.BusStopArrivalContainer;
import model.businfo.BusInfo;
import model.businfo.BusInfoDirection;

/** Gateway for telegram communication */
public class TelegramGateway extends TelegramLongPollingBot {
    public static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(TelegramGateway.class);

    public static final String TELEGRAM_BOT_NAME = "bus_time_bot";
    private static final String KEYWORD_BOT_MENTION = "@" + TELEGRAM_BOT_NAME;

    private static final String EMOJI_BUSSTOP = "busstop";
    private static final String EMOJI_ONCOMING_BUS = "oncoming_bus";

    public static String TELEGRAM_TOKEN;

    public HashMap<Long, Date> lastQueried; //use to limit the rate of updating bus times

    //Message Texts
    private static final String LAST_UPDATED_TEXT = "\n_Last updated: {0}_";
    private static final String NO_BUS_STOPS_FOUND_TEXT = "I couldn't find any bus stops near you. Check your location accuracy or contact @SimpleLegend if you think this is a mistake.";

    public TelegramGateway() {
        try {
            loadProperties();
            lastQueried = new HashMap<Long, Date>();
        } catch (Exception e) {
            logger.fatal("Unable to start up Telegram Gateway!", e);
        }
    }

    /**
     * Loads properties into token variables
     */
    private void loadProperties() {
        PropertiesLoader propertiesLoader = new PropertiesLoader();
        TELEGRAM_TOKEN = propertiesLoader.getTelegramToken();
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
                    command = parseCommand(text, commandText, isGroupChat);
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

                if (reply.type == CommandResponseType.LOCATION && reply.data != null) { //Append update button if responseCommand contains location
                    double latitude = Double.parseDouble(reply.data.get("latitude"));
                    double longitude = Double.parseDouble(reply.data.get("longitude"));
                    int numberOfStopsWanted = Integer.parseInt(reply.data.get("numberOfStopsWanted"));
                    String searchTerm = reply.data.get("searchTerm");
                    keyboard = KeyboardFactory.createUpdateInlineKeyboard(latitude, longitude, numberOfStopsWanted, searchTerm);
                }

                if (reply.type == CommandResponseType.IMAGE && reply.data != null) { //if the data contains an image path for an image to be sent
                    InputStream imageStream = getClass().getResourceAsStream(reply.data.get("image"));
                    sendPhotoMessage("", update.getMessage().getChatId(), imageStream, null);
                }

                if (reply.type == CommandResponseType.LINK && reply.data != null) {
                    keyboard = KeyboardFactory.createMoreInformationInlineKeyboard(reply.data.get("link"));
                }

                sendMessage(reply.text, update.getMessage().getChatId(), keyboard);
            }
        } catch (Exception e) {
            logger.warn("Exception occurred at onUpdateReceived with update={}", update, e);
        }
    }

    /*
     * Parses command from text
     */
    private Command parseCommand(String text, String commandText, boolean isGroupChat) {
        Command command = null;
        switch (commandText) {
        case StartHelpCommand.COMMAND_START: //Fall through
        case StartHelpCommand.COMMAND_HELP:
            command = new StartHelpCommand();
            break;
        case BusCommand.COMMAND: //searching for bus info
            command = new BusCommand(text);
            break;
        default: //Default to search if there is no command
            //Append the "search" term at the start only if the term starts with a '/' and its not a group chat
            if (text.charAt(0) == '/' && !isGroupChat) {
                text = SearchCommand.COMMAND + " " + text.substring(1);
            } else {
                break;
            }
        case SearchCommand.COMMAND: //Search by postal code/popular names/bus stop
            command = new SearchCommand(text);
            break;
        }
        return command;
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
                executeAsync(editMessageText, new EditMessageCallbackHandler());
            } catch (TelegramApiException e) {
            } //Ignore if MessageNotEdited error
            lastQueried.put(callbackQuery.getMessage().getChatId(), new Date());
        }

        //Send answer indicating a reply
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQuery.getId());

        try {
            executeAsync(answer, new AnswerCallbackQueryHandler());
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
        int numberOfStopsWanted = LocationCommand.DEFAULT_NUMBER_OF_STOPS;
        String searchTerm = null;

        //For backwards compatible by checking length
        int dataSize = data.length;
        switch (dataSize) {
            case 4:
                searchTerm = data[3];
            case 3:
                try {
                    numberOfStopsWanted = Integer.parseInt(data[2]);
                } catch (Exception e) { } //Ignore parsing error and use default number of stops
        }

        Command busTimeCommand;
        if (searchTerm != null) {
            busTimeCommand = new LocationCommand(searchTerm);
        } else {
            busTimeCommand = new LocationCommand(latitude, longitude, numberOfStopsWanted);
        }
        CommandResponse answer = busTimeCommand.execute();

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        String lastUpdated = MessageFormat.format(LAST_UPDATED_TEXT, timeFormat.format(new Date()));

        editMessageText.setText(answer.text + lastUpdated);

        //Re-add the inline keyboard
        editMessageText.setReplyMarkup(KeyboardFactory.createUpdateInlineKeyboard(latitude, longitude, numberOfStopsWanted, searchTerm));
        return editMessageText;
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
        sendMessageRequest.disableWebPagePreview(); //Hide previews if it exists
        sendMessageRequest.setParseMode(ParseMode.MARKDOWN); //Allow styling of text
        if (keyboard != null) {
            sendMessageRequest.setReplyMarkup(keyboard);
        }
        boolean success = false;
        try {
            executeAsync(sendMessageRequest, new SendMessageCallbackHandler()); //at the end, so some magic and send the message ;)
            success = true;
        } catch (Exception e) {
            logger.warn("Exception occurred at sendMessage with message={}, id={}, keyboard={}", message, id, keyboard, e);
        }
        return success;
    }

    /**
     * Sends an image to the id given
     */
    public boolean sendPhotoMessage(String message, long id, InputStream image, ReplyKeyboard keyboard) {
        SendPhoto photoMessage = new SendPhoto();
        photoMessage.setCaption(message);
        photoMessage.setPhoto("Nil", image);
        photoMessage.setChatId(id);
        if (keyboard != null) {
            photoMessage.setReplyMarkup(keyboard);
        }

        try {
            execute(photoMessage);
        } catch (TelegramApiException e) {
            logger.warn("Exception occurred at sendPhotoMessage with message={}, id={}, image={}, keyboard={}", message, id, image, keyboard, e);
            return false;
        }
        return true;
    }

    /**
     * Format information in a present-able manner
     *
     * @param information
     *            List of String (Size: 7) in this order: Header, Weekday 1st bus, Weekday last bus, Sat 1st bus, Sat last bus, Sun & P.H 1st bus, Sun & P.H last bus
     * @return Formatted string of bus information
     */
    public static String formatBusInfo(BusInfo busInfo) {
        try {
            if (!busInfo.isValidServiceNo) {
                return "No such bus service";
            } else {
                StringBuilder busInfoString = new StringBuilder("*Bus " + busInfo.serviceNo + "*\n");

                for (BusInfoDirection busInfoDirection : busInfo.busInfoDirections) {
                    if (busInfoDirection.fromTerminal != null && !busInfoDirection.fromTerminal.isEmpty()) {
                        busInfoString.append("*From " + busInfoDirection.fromTerminal + "*\n");
                    }

                    if (busInfo.serviceNo.startsWith("NR")) {
                        busInfoString.append("*Fri, Sat, Sun & eve of P.H*\n");
                        busInfoString.append("1st Bus: " + busInfoDirection.friSatEvePhFirstBus + " | Last Bus: " + busInfoDirection.friSatEvePhLastBus);
                    } else {
                        busInfoString.append("*Weekdays*\n");
                        busInfoString.append("1st Bus: " + busInfoDirection.weekdayFirstBus + " | Last Bus: " + busInfoDirection.weekdayFirstBus + "\n");
                        busInfoString.append("*Saturdays*\n");
                        busInfoString.append("1st Bus: " + busInfoDirection.satFirstBus + " | Last Bus: " + busInfoDirection.satLastBus + "\n");
                        busInfoString.append("*Suns & P.H*\n");
                        busInfoString.append("1st Bus: " + busInfoDirection.sunAndPhFirstBus + " | Last Bus: " + busInfoDirection.sunAndPhLastBus);
                    }
                    busInfoString.append("\n\n");
                }

                return busInfoString.toString();
            }
        } catch (Exception e) {
            logger.warn("Exception occurred at formatBusInfo with BusInfo={}", busInfo, e);
            return null;
        }
    }

    /**
     * Formats a BusStopArrivalContainer into the telegram format with markdown
     * @return user-friendly string to display on telegram
     */
    public static String formatBusArrival(BusStopArrivalContainer busStopArrivalContainer, int numberOfStopsWanted) {
        int count = 0;
        StringBuilder formattedString = new StringBuilder();
        for (BusStopArrival busStopArrival : busStopArrivalContainer.busStopArrivals) {
            count++;
            formattedString.append(buildBusStopHeader(busStopArrival.busStop));

            formattedString.append("\n```\n"); //For fixed-width formatting
            for (BusArrival busArrival : busStopArrival.busArrivals) {
                Emoji emoji = EmojiManager.getForAlias(EMOJI_ONCOMING_BUS);
                formattedString.append(emoji.getUnicode() + Util.padBusTitle(busArrival.serviceNo) + ": ");

                //Build string for each of the arrival time
                String firstEstimatedBusTime;
                if (busArrival.arrivalTime1 == BusArrival.TIME_NA) {
                    firstEstimatedBusTime = Util.padBusTime(BusArrival.LABEL_NA) + appendWab(false) + " | "; //N/A does not have WAB
                } else if (busArrival.arrivalTime1 <= BusArrival.TIME_ARRIVING) {
                    String busInfo = BusArrival.LABEL_ARRIVING + appendWab(busArrival.isWab1);
                    firstEstimatedBusTime = Util.padBusTime(busInfo) + " | ";
                } else {
                    String busInfo = Util.padFront(Long.toString(busArrival.arrivalTime1), 2) + "m" + appendWab(busArrival.isWab1);
                    firstEstimatedBusTime = Util.padBusTime(busInfo) + " | ";
                }

                String secondEstimatedBusTime;
                if (busArrival.arrivalTime2 == BusArrival.TIME_NA) {
                    secondEstimatedBusTime = BusArrival.LABEL_NA_BLANK;
                } else if (busArrival.arrivalTime2 <= BusArrival.TIME_ARRIVING) {
                    secondEstimatedBusTime = BusArrival.LABEL_ARRIVING + appendWab(busArrival.isWab2);
                } else {
                    String busInfo = Util.padFront(Long.toString(busArrival.arrivalTime2), 2) + "m" + appendWab(busArrival.isWab2);
                    secondEstimatedBusTime = busInfo;
                }

                //Append the string to the formatted string
                formattedString.append(firstEstimatedBusTime + secondEstimatedBusTime);
                formattedString.append("\n");
            }
            formattedString.append("```"); //End fixed-width formatting
            formattedString.append("\n");

            //Exit if hit the number of stops
            if (count >= numberOfStopsWanted) {
                break;
            }
        }

        //Report if no bus stops are found
        if (count == 0) {
            formattedString.append(NO_BUS_STOPS_FOUND_TEXT);
        }

        return formattedString.toString();
    }

    /**
     * Appends the sign for wheelchair accessible buses
     */
    private static String appendWab(boolean isWab) {
        return isWab? "*" : " ";
    }

    /**
     * Builds the bus stop header accordingly
     */
    private static StringBuilder buildBusStopHeader(BusStop stop) {
        Emoji emoji = EmojiManager.getForAlias(EMOJI_BUSSTOP);
        StringBuilder stopHeader = new StringBuilder();
        stopHeader.append(emoji.getUnicode());
        stopHeader.append("*");
        stopHeader.append("" + stop.BusStopCode + " - ");
        stopHeader.append(stop.Description);
        if (stop.isPublic && stop.isNtu) {
            stopHeader.append("/");
            stopHeader.append(stop.ntuDescription);
        }
        if (stop.isPublic && stop.isNus) {
            stopHeader.append("/");
            stopHeader.append(stop.nusDescription);
        }
        stopHeader.append("*");
        return stopHeader;
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
