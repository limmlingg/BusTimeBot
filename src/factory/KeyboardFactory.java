package factory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;

public class KeyboardFactory {

    /**
     * Creates an inline keyboard with the update button for users to update the bus timings
     */
    public static InlineKeyboardMarkup createUpdateInlineKeyboard(double latitude, double longitude, int numberOfStopsWanted, String searchTerm) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> btns = new LinkedList<List<InlineKeyboardButton>>();
        List<InlineKeyboardButton> firstRow = new LinkedList<InlineKeyboardButton>();
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText("Update");
        btn.setCallbackData(latitude + ":" + longitude + ":" + numberOfStopsWanted + ":" + searchTerm);
        firstRow.add(btn);
        btns.add(firstRow);
        inlineKeyboard.setKeyboard(btns);
        return inlineKeyboard;
    }

    /**
     * Creates an inline keyboard with the "More information" button for users view bus stop routes
     */
    public static InlineKeyboardMarkup createMoreInformationInlineKeyboard(String link) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> btns = new LinkedList<List<InlineKeyboardButton>>();
        List<InlineKeyboardButton> firstRow = new LinkedList<InlineKeyboardButton>();
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText("More information");
        btn.setUrl(link);
        firstRow.add(btn);
        btns.add(firstRow);
        inlineKeyboard.setKeyboard(btns);
        return inlineKeyboard;
    }

    /**
     * Create a keyboard for the user to send their location
     *
     * @return keyboardMarkup that allows user to send location
     */
    public static ReplyKeyboardMarkup createSendLocationKeyboard() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
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
}
