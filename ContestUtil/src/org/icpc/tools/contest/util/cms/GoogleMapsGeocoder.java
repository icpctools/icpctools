package org.icpc.tools.contest.util.cms;

import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.internal.Organization;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Class that uses the Google Maps geocode API to find the address of an organization
 */
public class GoogleMapsGeocoder {
	private final String apiKey;

	private static final HttpClient httpClient = HttpClient.newBuilder().build();

	public GoogleMapsGeocoder(String apiKey) {
		this.apiKey = apiKey;
	}

	public void geocode(Organization org) {
		String address = URLEncoder.encode(org.getFormalName() + " " + org.getCountry(), StandardCharsets.UTF_8);
		String fullUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=" + address + "&key=" + apiKey;

		try {
			HttpRequest request = HttpRequest.newBuilder(new URI(fullUrl))
					.header("Accept", "application/json")
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200)
				return;

			JsonObject json = (new JSONParser(response.body())).readObject();

			if (!json.containsKey("results")) {
				return;
			}

			Object[] results = json.getArray("results");
			if (results.length > 1) {
				System.err.println("Geocode for " + org.getName() + " returned " + results.length + " results, using first one");
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
		} catch (URISyntaxException | IOException | InterruptedException | IllegalArgumentException e) {
			e.printStackTrace();
		}
	}
}
