package controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.google.gson.Gson;

import main.BusTimeBot;
import main.Logger;

public class WebController {
    /**
     * Retrieve data from the URL and cast it to the class provided
     *
     * @param url
     *            of the server to retrieve data from
     * @param objectClass
     *            class to cast the data into
     * @return An object of the class provided
     */
    public static <T> T retrieveData(String url, Class<T> objectClass) {
        try {
            return jsonToObject(sendHTTPRequest(url), objectClass);
        } catch (Exception e) {
            Logger.log("Error when retriving url: " + url);
            Logger.logError(e);
            return null;
        }
    }

    /**
     * Retrieve data from the URL and cast it to the class provided
     *
     * @param url
     *            of the server to retrieve data from
     * @param objectClass
     *            class to cast the data into
     * @param https
     *            true or false
     * @return An object of the class provided
     */
    public static <T> T retrieveData(String url, Class<T> objectClass, boolean https) {
        try {
            if (https) {
                return jsonToObject(sendHTTPsRequest(url), objectClass);
            } else {
                return jsonToObject(sendHTTPRequest(url), objectClass);
            }
        } catch (Exception e) {
            Logger.log("Error when retriving url: " + url);
            Logger.logError(e);
            return null;
        }
    }

    /**
     * Converts JSON data to the appropiate class given (Case-Sensitive for variables)
     *
     * @param json
     *            data
     * @param objectClass
     *            class of the Object to cast to
     * @return An object of the class provided
     */
    public static <T> T jsonToObject(String json, Class<T> objectClass) throws Exception {
        Gson gson = new Gson();
        //To make sure json is json, we extract only from the first { to the last }
        String jsonTrimmed = json.substring(json.indexOf("{"), json.lastIndexOf("}") + 1);
        return gson.fromJson(jsonTrimmed, objectClass);
    }

    /**
     * Send a GET HTTP request to the url indicated and returns the response
     *
     * @param url
     *            of the server to retrieve data from
     * @return response returned from the Webserver
     */
    public static String sendHTTPRequest(String url) {
        return sendHTTPRequest(url, true);
    }

    /**
     * Send a GET HTTP request to the url indicated and returns the response
     *
     * @param url
     *            of the server to retrieve data from
     * @return response returned from the Webserver
     */
    public static String sendHTTPRequest(String url, boolean includeToken) {
        StringBuilder result = new StringBuilder();
        try {
            URL urlSite = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlSite.openConnection();
            connection.setRequestMethod("GET");
            if (includeToken) {
                connection.setRequestProperty("AccountKey", BusTimeBot.LTA_TOKEN);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
        } catch (Exception e) {
            Logger.logError(e);
        }
        //System.out.println(result);
        return result.toString();
    }

    /**
     * Send a GET HTTPS request to the url indicated and returns the response
     *
     * @param url
     *            of the server to retrieve data from
     * @return response returned from the Webserver
     */
    public static String sendHTTPsRequest(String url) {
        StringBuilder result = new StringBuilder();
        try {
            URL urlSite = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) urlSite.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
        } catch (Exception e) {
            Logger.log("Error!!!!\n" + e.toString() + "\n======================================================\n");
        }
        //System.out.println(result);
        return result.toString();
    }

    /**
     * Required to be able to retrieve searching using https protocol, will need to change next time
     */
    public static void trustAll() {
        TrustManager trm = new X509TrustManager() {
            /**
             * Ignore all accepted issuers
             */
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            /**
             * Override to do nothing when checking if client is trusted
             */
            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            /**
             * Override to do nothing when checking if server is trusted
             */
            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        };

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[] {trm}, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            Logger.log("Error!!!!\n" + e.toString() + "\n======================================================\n");
        }
    }
}
