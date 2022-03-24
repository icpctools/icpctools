package org.icpc.tools.contest.model.internal;

import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class Location {
	private static final String LATITUDE = "latitude";
	private static final String LONGITUDE = "longitude";

	public double latitude = Double.NaN;
	public double longitude = Double.NaN;

	public Location() {
		// do nothing
	}

	public Location(JsonObject obj) {
		latitude = obj.getDouble(LATITUDE);
		longitude = obj.getDouble(LONGITUDE);
	}

	public Location(Object value) {
		JsonObject obj = JSONParser.getOrReadObject(value);
		latitude = obj.getDouble(LATITUDE);
		longitude = obj.getDouble(LONGITUDE);
	}

	public static double getLatitude(Location l) {
		if (l == null)
			return Double.NaN;
		return l.latitude;
	}

	public static double getLongitude(Location l) {
		if (l == null)
			return Double.NaN;
		return l.longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	protected String getJSON() {
		return "{\"" + LATITUDE + "\":" + round(latitude) + ",\"" + LONGITUDE + "\":" + round(longitude) + "}";
	}

	private static double round(double d) {
		return Math.round(d * 10000.0) / 10000.0;
	}

	@Override
	public int hashCode() {
		return (int) Math.round(latitude * 3 + latitude * 17);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Location))
			return false;

		Location l = (Location) o;
		if (latitude != l.latitude)
			return false;
		if (longitude != l.longitude)
			return false;

		return true;
	}

	@Override
	public String toString() {
		return getJSON();
	}
}
