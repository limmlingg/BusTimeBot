package test;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.telegram.telegrambots.ApiContextInitializer;

import logic.command.BusCommand;
import logic.controller.WebController;
import main.BusTimeBot;

public class SystemTesting {
    public static BusTimeBot bot;

    @BeforeClass
    public static void setUp() {
      //Enable trust for SSL
        WebController.trustAll();
        ApiContextInitializer.init();

        //Start up bot
        bot = BusTimeBot.getInstance();
    }

    @Test
    public void testBusCommand() {
        //Test "/bus 112"
        String result = new BusCommand("112").execute().text;
        Assert.assertTrue(result.contains("Bus 112"));
        Assert.assertTrue(result.contains("Hougang Ctrl Int"));
        Assert.assertTrue(result.contains("Weekdays"));
        Assert.assertTrue(result.contains("Saturdays"));
        Assert.assertTrue(result.contains("Suns & P.H"));
        Assert.assertTrue(result.contains("1st Bus"));
        Assert.assertTrue(result.contains("Last Bus"));
    }

}
