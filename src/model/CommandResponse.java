package model;

import java.util.HashMap;
/**
 * A class that contains the response of commands executed
 */
public class CommandResponse {

    public String text;
    public HashMap<String, String> data;
    public CommandResponseType type;

    public CommandResponse(String text) {
        this.text = text;
    }

    public CommandResponse(String text, HashMap<String, String> data, CommandResponseType type) {
        this(text, data);
        this.type = type;
    }

    public CommandResponse(String text, HashMap<String, String> data) {
        this.text = text;
        this.data = data;
    }
}
