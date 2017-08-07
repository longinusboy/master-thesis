package com.longinus.projcaritasand.model.database;

public class GeoLocation implements ILocation {
	private long time;
	private double lon;
	private double lat;
	private String source;
	
	/*public GeoLocation(long lastUpdate, double lon, double lat) {
		super();
		this.time = lastUpdate;
		this.lon = lon;
		this.lat = lat;
	}*/

	public GeoLocation(long time, double lon, double lat, String source) {
		super();
		this.time = time;
		this.lon = lon;
		this.lat = lat;
		this.source = source;
	}

	@Override
	public long getTime() {
		return time;
	}

	public void setLastUpdate(long lastUpdate) {
		this.time = lastUpdate;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	@Override
	public String toString() {
		return "{\"time\":"+time+", \"lon\":"+lon+", \"lat\":"+lat+", \"src\":\""+source+"\"}";
	}
	
}
