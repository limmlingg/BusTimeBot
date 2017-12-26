package logic.command;

import model.CommandResponse;

/**
 * A command to show the welcome text when the user needs and introduction to the bot
 */
public class StartHelpCommand extends Command {

    public static final String COMMAND_START = "/start";
    public static final String COMMAND_HELP = "/help";
    private static final String WELCOME_TEXT = "Send me your location (Using the GPS) and get your bus timings(Public, NUS shuttle, NTU shuttle)!\n\n" +
            "Look up bus information by typing /bus <Service Number>, bus timings shown is the timing when the bus leaves the interchange\n" +
            "Example: /bus 969\n\n" + "You can type /search <Popular names/postal/address/bus stop number> (the \"search\" term is now optional)\n" +
            "Some examples:\n" +
            "/amk hub (defaults to search for NUS, also works for other examples below)\n" +
            "/search amk hub\n" +
            "/search 118426\n" +
            "/search Blk 1 Hougang Ave 1\n" +
            "/search 63151 (Bus Stop Code)\n\n" +
            "Contact @SimpleLegend for bugs/suggestions!";

    @Override
    public CommandResponse execute() {
        commandSuccess = true;
        return new CommandResponse(WELCOME_TEXT);
    }

}
