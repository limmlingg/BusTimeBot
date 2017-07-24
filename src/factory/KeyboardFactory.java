package factory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.telegram.telegrambots.api.objects.Location;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;

public class KeyboardFactory {

    /**
     * Creates an inline keyboard with the update button for users to update the bus timings
     *
     * @param location
     *            of the user
     * @return an InlineKeyboardMarkup with the button update
     */
    public static InlineKeyboardMarkup createUpdateInlineKeyboard(Location location) {
        return createUpdateInlineKeyboard(location.getLatitude(), location.getLongitude());
    }

    /**
     * Creates an inline keyboard with the update button for users to update the bus timings
     *
     * @param latitude
     *            of the user
     * @param longitude
     *            of the user
     * @return an InlineKeyboardMarkup with the button update
     */
    public static InlineKeyboardMarkup createUpdateInlineKeyboard(double latitude, double longitude) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> btns = new LinkedList<List<InlineKeyboardButton>>();
        List<InlineKeyboardButton> firstRow = new LinkedList<InlineKeyboardButton>();
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText("Update");
        btn.setCallbackData(latitude + ":" + longitude);
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
