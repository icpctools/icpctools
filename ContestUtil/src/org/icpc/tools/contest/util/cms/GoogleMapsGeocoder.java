package org.icpc.tools.contest.util.cms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.internal.Organization;

/**
 * Class that uses the Google Maps geocode API to find the address of an organization
 */
public class GoogleMapsGeocoder {
	private final String apiKey;

	public GoogleMapsGeocoder(String apiKey) {
		this.apiKey = apiKey;
	}

	public void geocode(Organization org) {
		try {
			String address = URLEncoder.encode(org.getFormalName() + " " + org.getCountry(), "UTF-8");
			String fullUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=" + address + "&key=" + apiKey;

			HttpURLConnection conn = (HttpURLConnection) (new URL(fullUrl)).openConnection();
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestMethod("GET");

			if (conn.getResponseCode() != 200)
				return;

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String inputLine;
			StringBuilder content = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}
			in.close();
			conn.disconnect();

			JsonObject json = (new JSONParser(content.toString())).readObject();

			if (!json.containsKey("results")) {
				return;
			}

			Object[] results = json.getArray("results");
			if (results.length > 1) {
				System.err.println(
						"Geocode for " + org.getName() + " returned " + results.length + " results, using first one");
			} else if (results.length == 0) {
				System.err.println("Geocode for " + org.getName() + " returned no results");
				return;
			}

			JsonObject result = (JsonObject) results[0];
			JsonObject location = result.getJsonObject("geometry").getJsonObject("location");
			String lat = location.getString("lat");
			String lon = location.getString("lng");
			JsonObject obj = new JsonObject();
			obj.props.put("latitude", lat);
			obj.props.put("longitude", lon);
			org.add("location", obj);
		} catch (IOException | IllegalArgumentException e) {
			e.printStackTrace();
		}
	}
}
