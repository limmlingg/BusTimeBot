package main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class Logger {
    public static File log = new File("BusTimeBot.log");

	/**
	 * Append information to the log file
	 * @param text to append to log file
	 */
	public static void log(String text) {
		try {
			if (!log.exists()) {
				log.createNewFile();
			}
			Files.write(log.toPath(), text.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
