package logic.command;

import main.Main.Gateway;
import model.CommandResponse;

/**
 * A command interface to execute commands given
 */
public abstract class Command {

    public boolean commandSuccess;
    public Gateway gateway = Gateway.TELEGRAM; //Default this to Telegram for now

    /**
     * Executes the command given and returns the answer given
     */
    public abstract CommandResponse execute();
}
