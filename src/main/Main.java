package main;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;

import logic.controller.WebController;

public class Main{
    //Development and Debugging attributes
    public static boolean isDev = true;

    //Gateways to run bot on
    public static TelegramGateway telegramGateway;

    public static void main(String[] args) {
        //Enable trust for SSL
        WebController.trustAll();
        ApiContextInitializer.init();

        //Start up bot
        BusTimeBot.getInstance();

        //Load telegram gateway
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramGateway = new TelegramGateway(isDev);
            telegramBotsApi.registerBot(telegramGateway);
        } catch (Exception e) {
            Logger.logError(e);
        }
    }
}
