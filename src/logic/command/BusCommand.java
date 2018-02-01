package logic.command;

import java.util.Arrays;
import java.util.HashMap;

import logic.controller.NtuController;
import logic.controller.NusController;
import logic.controller.PublicController;
import logic.gateway.TelegramGateway;
import model.CommandResponse;
import model.CommandResponseType;
import model.businfo.BusInfo;

/**
 * A command to handle the `/bus` command from users
 */
public class BusCommand extends Command {
    public static final String COMMAND = "/bus";
    private static final String KEYWORD_BUS = COMMAND + " ";
    private static final String BUS_HELP_TEXT = "Type /bus <Service Number> to look up first and last bus timings!\n" + "Example: /bus 969";

    private static HashMap<String, BusInfo> cache = new HashMap<String, BusInfo>();
    private String searchTerm;

    public BusCommand(String searchTerm) {
        this.searchTerm = searchTerm.toLowerCase().replaceFirst(KEYWORD_BUS, "").toUpperCase();
    }

    @Override
    public CommandResponse execute() {
        BusInfo busInformation = cache.get(searchTerm); //Attempt to retrieve from cache
        String busInformationString = BUS_HELP_TEXT;
        HashMap<String, String> data = null;
        CommandResponseType type = CommandResponseType.NONE;
        String ntuRouteInformation = "";

        if (busInformation == null) {
            if (searchTerm == null || searchTerm.isEmpty()) {
                busInformationString = BUS_HELP_TEXT;
            } else if (Arrays.binarySearch(NtuController.NTU_BUSES, searchTerm) >= 0) { //NTU bus data
                busInformation = NtuController.getNTUBusInfo(searchTerm);
                ntuRouteInformation = "*Bus Route: *\n" + NtuController.busRoutes.get(searchTerm).toString();
            } else if (Arrays.binarySearch(NusController.NUS_BUSES, searchTerm) >= 0) { //NUS bus data
                busInformation = NusController.getNUSBusInfo(searchTerm);
                //Add images for the routes
                type = CommandResponseType.IMAGE;
                data = new HashMap<String, String>();
                String busService = searchTerm.startsWith("BTC")? "BTC" : searchTerm; //To truncate "btc1" and "btc2" to btc
                data.put("image", "/" + busService + ".png");
            } else {
                busInformation = PublicController.getPublicBusInfo(searchTerm);
                //Add links for the routes
                type = CommandResponseType.LINK;
                data = new HashMap<String, String>();
                data.put("link", "https://www.transitlink.com.sg/eservice/eguide/service_route.php?service=" + searchTerm);
            }

            //Add to cache
            cache.put(searchTerm, busInformation);
        }

        if (busInformation != null) {
            //ntuRouteInformation should be empty if not ntu bus
            busInformationString = TelegramGateway.formatBusInfo(busInformation) + ntuRouteInformation;
        }

        commandSuccess = true;
        return new CommandResponse(busInformationString, data, type);
    }

}
