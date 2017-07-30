package logic.command;

/**
 * A command interface to execute commands given
 */
public interface Command {

    /**
     * Executes the command given and returns the answer given
     */
    public String execute();
}
