package logic.command;

import model.CommandResponse;

/**
 * A command to show the welcome text when the user needs and introduction to the bot
 */
public class StartHelpCommand extends Command {

    public static final String COMMAND_START = "/start";
    public static final String COMMAND_HELP = "/help";
    private static final String WELCOME_TEXT = "Send me your location (Using the GPS) and get your bus timings(Public, NUS shuttle, NTU shuttle)!\n\n" +
            "Here are some commands that can be used:\n" +
            "/bus <Bus service number>\n" +
            "/search <Popular names/postal/address/bus stop number>\n" +
            "\nContact @SimpleLegend for bugs/suggestions!";

    @Override
    public CommandResponse execute() {
        commandSuccess = true;
        return new CommandResponse(WELCOME_TEXT);
    }

}
