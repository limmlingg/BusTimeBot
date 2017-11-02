package logic.command;

import java.util.Arrays;

import logic.controller.BusInfoController;
import model.CommandResponse;

/**
 * A command to handle the `/bus` command from users
 */
public class BusCommand implements Command {
    public static final String COMMAND = "/bus";
    private static final String KEYWORD_BUS = COMMAND + " ";

    private static final String BUS_HELP_TEXT = "Type /bus <Service Number> to look up first and last bus timings!\n" + "Example: /bus 969";

    private String searchTerm;

    public BusCommand(String searchTerm) {
        this.searchTerm = searchTerm.toLowerCase().replaceFirst(KEYWORD_BUS, "").toUpperCase();
    }

    @Override
    public CommandResponse execute() {
        String busInformation;
        if (searchTerm == null || searchTerm.isEmpty()) {
            busInformation = BUS_HELP_TEXT;
        } else if (Arrays.binarySearch(BusInfoController.NTUBus, searchTerm) >= 0) { //NTU bus data
            busInformation = BusInfoController.getNTUBusInfo(searchTerm);
        } else if (Arrays.binarySearch(BusInfoController.NUSBus, searchTerm) >= 0) { //NUS bus data
            busInformation = BusInfoController.getNUSBusInfo(searchTerm);
        } else {
            busInformation = BusInfoController.getPublicBusInfo(searchTerm);
        }
        return new CommandResponse(busInformation);
    }

}
