package logic.command;

import java.util.Arrays;

import logic.controller.BusInfoController;
import logic.gateway.TelegramGateway;
import model.BusInfo;
import model.CommandResponse;

/**
 * A command to handle the `/bus` command from users
 */
public class BusCommand extends Command {
    public static final String COMMAND = "/bus";
    private static final String KEYWORD_BUS = COMMAND + " ";

    private static final String BUS_HELP_TEXT = "Type /bus <Service Number> to look up first and last bus timings!\n" + "Example: /bus 969";

    private String searchTerm;

    public BusCommand(String searchTerm) {
        this.searchTerm = searchTerm.toLowerCase().replaceFirst(KEYWORD_BUS, "").toUpperCase();
    }

    @Override
    public CommandResponse execute() {
        BusInfo busInformation = null;
        String busInformationString = BUS_HELP_TEXT;

        if (searchTerm == null || searchTerm.isEmpty()) {
            busInformationString = BUS_HELP_TEXT;
        } else if (Arrays.binarySearch(BusInfoController.NTUBus, searchTerm) >= 0) { //NTU bus data
            busInformation = BusInfoController.getNTUBusInfo(searchTerm);
        } else if (Arrays.binarySearch(BusInfoController.NUSBus, searchTerm) >= 0) { //NUS bus data
            busInformation = BusInfoController.getNUSBusInfo(searchTerm);
        } else {
            busInformation = BusInfoController.getPublicBusInfo(searchTerm);
        }

        if (busInformation != null) {
            busInformationString = TelegramGateway.formatBusInfo(busInformation);
        }

        commandSuccess = true;
        return new CommandResponse(busInformationString);
    }

}
