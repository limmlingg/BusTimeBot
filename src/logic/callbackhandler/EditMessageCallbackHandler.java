package logic.callbackhandler;

import java.io.Serializable;

import org.apache.logging.log4j.LogManager;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updateshandlers.SentCallback;

public class EditMessageCallbackHandler implements SentCallback<Serializable> {
    public static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(EditMessageCallbackHandler.class);

    @Override
    public void onResult(BotApiMethod<Serializable> method, Serializable response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onError(BotApiMethod<Serializable> method, TelegramApiRequestException apiException) {
        logger.error("Exception occurred at EditMessage.onError with method={}", method, apiException);
    }

    @Override
    public void onException(BotApiMethod<Serializable> method, Exception exception) {
        logger.error("Exception occurred at EditMessage.onException with method={}", method, exception);
    }

}
