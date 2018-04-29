package logic.callbackhandler;

import org.apache.logging.log4j.LogManager;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updateshandlers.SentCallback;

public class AnswerCallbackQueryHandler implements SentCallback<Boolean> {
    public static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(AnswerCallbackQueryHandler.class);


    @Override
    public void onResult(BotApiMethod<Boolean> method, Boolean response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onError(BotApiMethod<Boolean> method, TelegramApiRequestException apiException) {
        logger.error("Exception occurred at AnswerCallbackQuery.onError with method={}", method, apiException);

    }

    @Override
    public void onException(BotApiMethod<Boolean> method, Exception exception) {
        logger.error("Exception occurred at AnswerCallbackQuery.onException with method={}", method, exception);
    }

}
