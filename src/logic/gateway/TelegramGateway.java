package logic.gateway;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

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
import main.Logger;
import model.BusInfo;
import model.BusInfoDirection;
import model.CommandResponse;

/** Gateway for telegram communication */
public class TelegramGateway extends TelegramLongPollingBot {
    public static final String TELEGRAM_BOT_NAME = "bus_time_bot";
    private static final String KEYWORD_BOT_MENTION = "@" + TELEGRAM_BOT_NAME;
    public static String TELEGRAM_TOKEN;

    public HashMap<Long, Date> lastQueried; //use to limit the rate of updating bus times

    //Message Texts
    private static final String LAST_UPDATED_TEXT = "\n_Last updated: {0}_";

    public TelegramGateway() {
        try {
            loadProperties();
            lastQueried = new HashMap<Long, Date>();
        } catch (Exception e) {
            Logger.logError(e);
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
                }
                return busInfoString.toString();
            }
        } catch (Exception e) {
            Logger.logError(e);
            return null;
        }
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
