package logic.command;

import model.CommandResponse;

/**
 * A command interface to execute commands given
 */
public abstract class Command {

    public boolean commandSuccess;

    /**
     * Executes the command given and returns the answer given
     */
    public abstract CommandResponse execute();
}
