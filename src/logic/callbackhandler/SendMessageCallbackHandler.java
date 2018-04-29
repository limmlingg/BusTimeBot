package logic.callbackhandler;

import org.apache.logging.log4j.LogManager;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updateshandlers.SentCallback;

public class SendMessageCallbackHandler implements SentCallback<Message> {
    public static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(SendMessageCallbackHandler.class);

    @Override
    public void onResult(BotApiMethod<Message> method, Message response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onError(BotApiMethod<Message> method, TelegramApiRequestException apiException) {
        logger.error("Exception occurred at SendMessage.onError with method={}", method, apiException);

    }

    @Override
    public void onException(BotApiMethod<Message> method, Exception exception) {
        logger.error("Exception occurred at SendMessage.Exception with method={}", method, exception);
    }

}
