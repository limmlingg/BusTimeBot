package model;

import java.util.HashMap;

/**
 * A class that contains the response of commands executed
 */
public class CommandResponse {
    public String text;
    public HashMap<String, String> data;

    public CommandResponse(String text) {
        this.text = text;
    }

    public CommandResponse(String text, HashMap<String, String> data) {
        this(text);
        this.data = data;
    }
}
