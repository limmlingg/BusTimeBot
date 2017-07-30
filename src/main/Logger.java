package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class Logger {
    public static File log = new File("BusTimeBot.log");
    public static final String DEBUG_SEPARATOR = "\n\n======================================================\n";

    /**
     * Append information to the log file
     *
     * @param text
     *            to append to log file
     */
    public static void log(String text) {
        try {
            if (!log.exists()) {
                log.createNewFile();
            }
            Files.write(log.toPath(), text.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Log errors if there are any to fix in the future
     *
     * @param e
     *            Exception caused
     */
    public static void logError(Exception e) {
        try {
            PrintStream ps;
            ps = new PrintStream(Logger.log);
            e.printStackTrace(ps);
            ps.close();
        } catch (FileNotFoundException e1) {
        }
    }
}
