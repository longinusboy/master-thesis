package org.projmis.demo.data;

import org.vertx.java.core.json.JsonObject;

public class DeviceLocation {
	private long date;
	private Integer mcc, mnc, lac, cid;
	private Double lon, lat;
	private String source;
	private boolean locationResolved;
	private boolean locationInferred;

	public DeviceLocation(long date) {
		this.date = date;
	}

	public DeviceLocation(long date, Integer mcc, Integer mnc, Integer lac, Integer cid) {
		this.date = date;
		this.mcc = mcc;
		this.mnc = mnc;
		this.lac = lac;
		this.cid = cid;
	}

	public DeviceLocation(long date, Double lon, Double lat, String source) {
		this.date = date;
		this.lon = lon;
		this.lat = lat;
		this.source = source;
	}

	public DeviceLocation(long date, Integer mcc, Integer mnc, Integer lac, Integer cid, Double lon, Double lat, String source) {
		this.date = date;
		this.mcc = mcc;
		this.mnc = mnc;
		this.lac = lac;
		this.cid = cid;
		this.lon = lon;
		this.lat = lat;
		this.source = source;
	}

	public void setMobileLocation(Integer mcc, Integer mnc, Integer lac, Integer cid) {
		this.mcc = mcc;
		this.mnc = mnc;
		this.lac = lac;
		this.cid = cid;
	}

	public boolean hasTowerLocation() {
		return mcc != null && mnc != null && lac != null && cid != null;
	}

	public boolean hasGpsLocation() {
		return lon != null && lat != null;
	}

	public void setGpsLocationResolved(Coordinate coordinate) {
		if (coordinate == null) {
			return;
		}
		lon = coordinate.lon;
		lat = coordinate.lat;
	}

	public Coordinate getGpsCoordinate() {
		if (lon != null && lat != null) {
			return new Coordinate(lon, lat);
		}
		return null;
	}

	public void setGpsLocationInferred(Coordinate coordinate) {
		if (coordinate == null) {
			return;
		}
		lon = coordinate.lon;
		lat = coordinate.lat;
		this.locationInferred = true;
	}

	public JsonObject getLocation() {
		if (hasGpsLocation()) {
			JsonObject location = new JsonObject().putNumber("date", date).putNumber("lon", lon).putNumber("lat", lat);
			if (locationResolved) {
				location.putBoolean("resolved", true);
			}
			if (locationInferred) {
				location.putBoolean("inferred", true);
			}
			return location;
		}
		if (hasTowerLocation()) {
			return new JsonObject().putNumber("date", date).putNumber("mcc", mcc).putNumber("mnc", mnc).putNumber("lac", lac).putNumber("cid", cid);
		}
		return null;
	}

	public JsonObject toJsonObject() {
		JsonObject location = null;
		if (hasGpsLocation()) {
			location = new JsonObject().putNumber("date", date).putNumber("lon", lon).putNumber("lat", lat);
			if (locationResolved) {
				location.putBoolean("resolved", true);
			}
			if (locationInferred) {
				location.putBoolean("inferred", true);
			}
			location.putString("src", source);
		}
		if (hasTowerLocation()) {
			if (location == null) {
				location = new JsonObject().putNumber("date", date);
			}
			location.putNumber("mcc", mcc).putNumber("mnc", mnc).putNumber("lac", lac).putNumber("cid", cid);
		}
		return location;
	}
}
