package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.Gson;

import main.BusTimeBot;

public class WebController {
	/**
	 * Retrieve data from the URL and cast it to the class provided
	 * @param url of the server to retrieve data from
	 * @param objectClass class to cast the data into
	 * @return An object of the class provided
	 */
	public static <T> T retrieveData(String url, Class<T> objectClass) {
		return jsonToObject(sendHTTPRequest(url), objectClass);
	}
	
	/**
	 * Retrieve data from the URL and cast it to the class provided
	 * @param url of the server to retrieve data from
	 * @param objectClass class to cast the data into
	 * @return An object of the class provided
	 */
	public static <T> T retrieveData(String url, Class<T> objectClass, boolean https) {
		if (https) {
			return jsonToObject(sendHTTPsRequest(url), objectClass);
		} else {
			return jsonToObject(sendHTTPRequest(url), objectClass);
		}
	}
	
	/**
	 * Converts JSON data to the appropiate class given (Case-Sensitive for variables)
	 * @param json data
	 * @param objectClass class of the Object to cast to
	 * @return An object of the class provided
	 */
	public static <T> T jsonToObject (String json, Class<T> objectClass) {
		Gson gson = new Gson();
		//To make sure json is json, we extract only from the first { to the last }
		//System.out.println(json);
		json = json.substring(json.indexOf("{"), json.lastIndexOf("}")+1);
		return gson.fromJson(json, objectClass);
	}
	
	
	/**
	 * Send a GET HTTP request to the url indicated and returns the response
	 * @param url of the server to retrieve data from
	 * @return response returned from the Webserver
	 */
	public static String sendHTTPRequest(String url) {
		StringBuilder result = new StringBuilder();
		try {
			URL urlSite = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) urlSite.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("AccountKey", BusTimeBot.LTA_TOKEN);
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println(result);
		return result.toString();
	}
	
	/**
	 * Send a GET HTTPS request to the url indicated and returns the response
	 * @param url of the server to retrieve data from
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
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(result);
		return result.toString();
	}
}
