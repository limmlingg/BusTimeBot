package logic;

import java.io.FileInputStream;
import java.util.Properties;

import main.Logger;

/**
 * Controls the keys for various sources utilizes for the bot
 */
public class PropertiesLoader {
    private static final String PROPERTIES_FILE = "key.properties";

    private static final String PROPERTIES_KEY_LTA = "lta";
    private static final String PROPERTIES_KEY_TELEGRAM = "telegram";
    private static final String PROPERTIES_KEY_TELEGRAM_DEV = "telegram_dev";

    private String ltaToken;
    private String telegramToken;
    private String telegramDevToken;

    public PropertiesLoader() {
        try {
            FileInputStream propertiesStream = new FileInputStream(PROPERTIES_FILE);
            Properties properties = new Properties();
            properties.load(propertiesStream);

            telegramDevToken = properties.getProperty(PROPERTIES_KEY_TELEGRAM_DEV, "");
            telegramToken = properties.getProperty(PROPERTIES_KEY_TELEGRAM, "");
            ltaToken = properties.getProperty(PROPERTIES_KEY_LTA, "");

            //Close the stream and file as we don't need it anymore
            properties.clear();
            propertiesStream.close();
        } catch (Exception e) {
            Logger.logError(e);
        }
    }

    public String getLtaToken() {
        return ltaToken;
    }

    public String getTelegramToken() {
        return telegramToken;
    }

    public String getTelegramDevToken() {
        return telegramDevToken;
    }
}
