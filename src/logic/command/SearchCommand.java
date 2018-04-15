package logic.command;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import logic.controller.WebController;
import model.CommandResponse;
import model.json.gothere.GeoCodeContainer;

/**
 * A command that gives the nearest bus stops when searching for a location
 */
public class SearchCommand extends Command {
    public static final String COMMAND = "/search";
    public static final String SEARCH_HELP_TEXT = "You can type /search <Popular names/postal/address/bus stop number> (the \"search\" term is now optional, NOT FOR GROUP CHATS)\n" +
                                                  "Some examples:\n" +
                                                  "/amk hub (defaults to search for AMK HUB, also works for other examples below)\n" +
                                                  "/search amk hub\n" +
                                                  "/search 118426\n" +
                                                  "/search Blk 1 Hougang Ave 1\n" +
                                                  "/search 63151 (Bus Stop Code)\n\n";

    private String searchTerm;

    public SearchCommand(String searchTerm) {
        this.searchTerm = searchTerm.toLowerCase().replaceFirst(COMMAND, "").trim();
    }

    public CommandResponse execute() {
        GeoCodeContainer results;
        CommandResponse result;

        if (searchTerm == null || searchTerm.isEmpty()) {
            return new CommandResponse(SEARCH_HELP_TEXT);
        }

        try {
            String encodedSearchTerm = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.toString());
            results = WebController.retrieveData("https://gothere.sg/a/search?q=" + encodedSearchTerm, GeoCodeContainer.class, true);
            if (results != null && results.status == 1) {
                double lat = results.getLatitude();
                double lon = results.getLongitude();

                int numberOfStops = getNumberOfStopsWanted(searchTerm);
                LocationCommand busTimeCommand;
                if (numberOfStops == 1) {
                    busTimeCommand = new LocationCommand(searchTerm);
                } else {
                    busTimeCommand = new LocationCommand(lat, lon, numberOfStops);
                }
                result = busTimeCommand.execute();
            } else {
                result = new CommandResponse("Unable to find location");
            }
        } catch (UnsupportedEncodingException e) {
            result = new CommandResponse("Unable to find location");
        }
        commandSuccess = true;
        return result;
    }

    private int getNumberOfStopsWanted(String input) {
        int numberOfStops = 5;
        if (Pattern.matches("^[0-9]{5}$", input)) { //length is 5 & all digits
            numberOfStops = 1;
        }
        return numberOfStops;
    }
}
