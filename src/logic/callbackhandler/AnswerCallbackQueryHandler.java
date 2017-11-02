package logic.callbackhandler;

import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updateshandlers.SentCallback;

public class AnswerCallbackQueryHandler implements SentCallback<Boolean> {

    @Override
    public void onResult(BotApiMethod<Boolean> method, Boolean response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onError(BotApiMethod<Boolean> method, TelegramApiRequestException apiException) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onException(BotApiMethod<Boolean> method, Exception exception) {
        // TODO Auto-generated method stub

    }

}
