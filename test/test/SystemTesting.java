package test;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.telegram.telegrambots.ApiContextInitializer;

import logic.command.BusCommand;
import logic.command.StartHelpCommand;
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
    public void testStartCommand() {
        //"/help"
        String result = new StartHelpCommand().execute().text;
        Assert.assertTrue(result.contains("Send me your location"));
        Assert.assertTrue(result.contains("Contact @SimpleLegend for bugs/suggestions!"));
    }
    
    @Test
    public void testHelpCommand() {
        //"/help"
        String result = new StartHelpCommand().execute().text;
        Assert.assertTrue(result.contains("Send me your location"));
        Assert.assertTrue(result.contains("Contact @SimpleLegend for bugs/suggestions!"));
    }

    @Test
    public void test1WayBusCommand() {
        //"/bus 112"
        String result = new BusCommand("112").execute().text;
        Assert.assertTrue(result.contains("Bus 112"));
        Assert.assertTrue(result.contains("Hougang Ctrl Int"));
        Assert.assertTrue(result.contains("Weekdays"));
        Assert.assertTrue(result.contains("Saturdays"));
        Assert.assertTrue(result.contains("Suns & P.H"));
        Assert.assertTrue(result.contains("1st Bus"));
        Assert.assertTrue(result.contains("Last Bus"));
    }

    @Test
    public void testNUSBusCommand() {
        //"/bus A1"
        String result = new BusCommand("A1").execute().text;
        Assert.assertTrue(result.contains("Bus A1"));
        Assert.assertTrue(result.contains("Weekdays"));
        Assert.assertTrue(result.contains("Saturdays"));
        Assert.assertTrue(result.contains("Suns & P.H"));
        Assert.assertTrue(result.contains("1st Bus"));
        Assert.assertTrue(result.contains("Last Bus"));
    }

    @Test
    public void testNTUBusCommand() {
        //"/bus CWR"
        String result = new BusCommand("CWR").execute().text;
        Assert.assertTrue(result.contains("Bus CWR"));
        Assert.assertTrue(result.contains("Weekdays"));
        Assert.assertTrue(result.contains("Saturdays"));
        Assert.assertTrue(result.contains("Suns & P.H"));
        Assert.assertTrue(result.contains("1st Bus"));
        Assert.assertTrue(result.contains("Last Bus"));
    }

    @Test
    public void test2WayBusCommand() {
        //"/bus 854"
        String result = new BusCommand("854").execute().text;
        Assert.assertTrue(result.contains("Bus 854"));
        Assert.assertTrue(result.contains("Bedok Int"));
        Assert.assertTrue(result.contains("Yishun Temp Int"));
        Assert.assertTrue(result.contains("Weekdays"));
        Assert.assertTrue(result.contains("Saturdays"));
        Assert.assertTrue(result.contains("Suns & P.H"));
        Assert.assertTrue(result.contains("1st Bus"));
        Assert.assertTrue(result.contains("Last Bus"));
    }

    @Test
    public void testNRBusCommand() {
        //"/bus NR7"
        String result = new BusCommand("NR7").execute().text;
        Assert.assertTrue(result.contains("Bus NR7"));
        Assert.assertTrue(result.contains("Marina Ctr Ter"));
        Assert.assertTrue(result.contains("Fri, Sat, Sun & eve of P.H"));
        Assert.assertTrue(result.contains("1st Bus"));
        Assert.assertTrue(result.contains("Last Bus"));
    }

}
