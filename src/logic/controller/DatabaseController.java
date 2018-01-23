package logic.controller;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

        //Check if file exists first
        File databaseFile = new File("main.db");
        boolean isFileExists = databaseFile.exists();

        if (!isFileExists) {
            try {
                databaseFile.createNewFile();
            } catch (IOException e1) {
                System.err.println("Unable to create file, default to online");
                return null;
            }
        }

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:main.db");
        } catch ( Exception e ) { //Database file does not exist, create the file
            e.printStackTrace();
        }

        if (!isBusStopTableExist(connection)) {
            initializeTables(connection);
        }

        return connection;
    }

    private static boolean isBusStopTableExist(Connection connection) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        boolean isTableExist = false;
        try {
            //Delete old bus stops (since we want to save a new copy)
            statement = connection.prepareStatement("SELECT * from `bus_stop`;");
            resultSet = statement.executeQuery();
            isTableExist = true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            //Attempt to close statement, don't close connection as we want the connection to persist for calling function
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return isTableExist;
    }

    private static void initializeTables(Connection connection) {
        PreparedStatement statement = null;
        try {
            //Create `bus_stops` table
            statement = connection.prepareStatement("CREATE TABLE `bus_stop` ("
                                                    + "`bus_stop_code` TEXT NOT NULL UNIQUE, "
                                                    + "`description`   TEXT NOT NULL, "
                                                    + "`latitude`  REAL NOT NULL, "
                                                    + "`longitude` REAL NOT NULL, "
                                                    + "`is_public` INTEGER NOT NULL DEFAULT 0, "
                                                    + "`is_nus`    INTEGER NOT NULL DEFAULT 0, "
                                                    + "`is_ntu`    INTEGER NOT NULL DEFAULT 0, "
                                                    + "`nus_stop_code` TEXT UNIQUE, "
                                                    + "`nus_description`   TEXT, "
                                                    + "`ntu_stop_code` TEXT UNIQUE, "
                                                    + "`ntu_description`   TEXT, "
                                                    + "PRIMARY KEY(`bus_stop_code`) "
                                                    + ");");
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }  finally {
            //Attempt to close statement, don't close connection as we want the connection to persist for calling function
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
