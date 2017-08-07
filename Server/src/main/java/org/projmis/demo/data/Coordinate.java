package org.projmis.demo.data;


public class Coordinate {
	public double lon;
	public double lat;
	
	public Coordinate(double lon, double lat) {
		super();
		this.lon = lon;
		this.lat = lat;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Coordinate) {
			Coordinate objCoordinate = (Coordinate) obj;
			if (lon==objCoordinate.lon && lat==objCoordinate.lat) {
				return true;
			}
		}
		return false;
	}
}
