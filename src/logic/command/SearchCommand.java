package logic.command;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import logic.controller.WebController;
import model.json.gothere.GeoCodeContainer;

/**
 * A command that gives the nearest bus stops when searching for a location
 */
public class SearchCommand implements Command {
    public static final String COMMAND = "/search";
    private static final String KEYWORD_SEARCH = COMMAND + " ";

    private String searchTerm;

    public SearchCommand(String searchTerm) {
        this.searchTerm = searchTerm.toLowerCase().replace(KEYWORD_SEARCH, "");
    }

    public String execute() {
        GeoCodeContainer results;
        String result;
        try {
            results = WebController.retrieveData("https://gothere.sg/a/search?q=" + URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.toString()), GeoCodeContainer.class);
            if (results != null && results.status == 1) {
                double lat = results.where.markers.get(0).getLatitude();
                double lon = results.where.markers.get(0).getLongitude();

                LocationCommand busTimeCommand = new LocationCommand(lat, lon);
                result = busTimeCommand.execute();
            } else {
                result = "Unable to find location";
            }
        } catch (UnsupportedEncodingException e) {
            result = "Unable to find location";
        }
        return result;
    }
}
