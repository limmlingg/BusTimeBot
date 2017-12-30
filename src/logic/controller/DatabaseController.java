package logic.controller;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Handles connections with the database
 */
public class DatabaseController {

    /**
     * Gets a connection to main.db file
     * Remember to close the connection as it will not be handled automatically.
     */
    public static Connection getConnection() {
        Connection connection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:main.db");
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return connection;
    }
}
