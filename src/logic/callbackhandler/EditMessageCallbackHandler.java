package logic.callbackhandler;

import java.io.Serializable;

import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updateshandlers.SentCallback;

public class EditMessageCallbackHandler implements SentCallback<Serializable> {

    @Override
    public void onResult(BotApiMethod<Serializable> method, Serializable response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onError(BotApiMethod<Serializable> method, TelegramApiRequestException apiException) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onException(BotApiMethod<Serializable> method, Exception exception) {
        // TODO Auto-generated method stub

    }

}
