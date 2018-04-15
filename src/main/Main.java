package main;

import org.apache.logging.log4j.LogManager;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;

import logic.controller.WebController;
import logic.gateway.TelegramGateway;

public class Main{
    public static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(Main.class);

    //Gateways to run BusTimeBot on
    public enum Gateway {
        TELEGRAM
    };
    public static TelegramGateway telegramGateway;

    public static void main(String[] args) {
        logger.info("Enable trust for SSL");
        WebController.trustAll();
        logger.info("Initializing ApiContextInitializer (required for Telegram bot API)");
        ApiContextInitializer.init();

        logger.info("Starting up BusTimeBot instance");
        BusTimeBot.getInstance();

        logger.info("Loading up Telegram Gateway and registering bot");
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramGateway = new TelegramGateway();
            telegramBotsApi.registerBot(telegramGateway);
            logger.info("BusTimeBot for telegram ready to rock and roll!");
        } catch (Exception e) {
            logger.fatal("There is a problem registering the Telegram Gateway!", e);
        }
    }
}
