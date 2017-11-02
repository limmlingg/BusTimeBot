package logic.callbackhandler;

import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updateshandlers.SentCallback;

public class SendMessageCallbackHandler implements SentCallback<Message> {

    @Override
    public void onResult(BotApiMethod<Message> method, Message response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onError(BotApiMethod<Message> method, TelegramApiRequestException apiException) {
        // TODO Auto-generated method stub
        apiException.printStackTrace();
    }

    @Override
    public void onException(BotApiMethod<Message> method, Exception exception) {
        // TODO Auto-generated method stub
        exception.printStackTrace();
    }

}
