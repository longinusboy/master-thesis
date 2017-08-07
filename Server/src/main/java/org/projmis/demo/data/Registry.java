package org.projmis.demo.data;

import java.util.Date;
import java.util.List;

import org.projmis.caritas.Utils;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class Registry {
	private String idCampaign;
	private String idDevice;
	private DeviceLocation location;
	private long dateReceived;

	private long date;
	private String type;
	private Integer weight;
	private String unit;
	private Integer minWeight;
	private Integer maxWeight;
	private Integer realWeight;
	private List<IProduct> products;

	public Registry(String idCampaign, String idDevice, JsonObject geo, long date, List<IProduct> products) {
		if (geo != null) {
			
			Long geoDate = geo.getLong("date", date);
			Integer mcc = geo.getInteger("mcc");
			Integer mnc = geo.getInteger("mnc");
			Integer lac = geo.getInteger("lac");
			Integer cid = geo.getInteger("cid");
			
			Number lon = geo.getNumber("lon");
			Number lat = geo.getNumber("lat");
			
			if ((mcc!=null && mnc!=null && lac!=null && cid!=null && lon!=null && lat!=null)) {
				location = new DeviceLocation(geoDate, mcc, mnc, lac, cid, lon.doubleValue(), lat.doubleValue(), geo.getString("src"));
			}else {
				if (mcc!=null && mnc!=null && lac!=null && cid!=null) {
					location = new DeviceLocation(geoDate, mcc, mnc, lac, cid);
				}
				if (lon!=null && lat!=null) {
					location = new DeviceLocation(geoDate, lon.doubleValue(), lat.doubleValue(), geo.getString("src"));
				}
			}
		}
		this.idCampaign = idCampaign;
		this.idDevice = idDevice;
		this.date = date;
		this.products = products;
		this.dateReceived = new Date().getTime();
	}

	public Registry(String idCampaign, String idDevice, JsonObject geo, String type, int weight, String unit, int minWeight, int maxWeight, int realWeight, long date,
			List<IProduct> products) {
		if (geo != null) {
			
			Long geoDate = geo.getLong("date", date);
			Integer mcc = geo.getInteger("mcc");
			Integer mnc = geo.getInteger("mnc");
			Integer lac = geo.getInteger("lac");
			Integer cid = geo.getInteger("cid");
			
			Number lon = geo.getNumber("lon");
			Number lat = geo.getNumber("lat");
			
			if ((mcc!=null && mnc!=null && lac!=null && cid!=null && lon!=null && lat!=null)) {
				location = new DeviceLocation(geoDate, mcc, mnc, lac, cid, lon.doubleValue(), lat.doubleValue(), geo.getString("src"));
			}else {
				if (mcc!=null && mnc!=null && lac!=null && cid!=null) {
					location = new DeviceLocation(geoDate, mcc, mnc, lac, cid);
				}
				if (lon!=null && lat!=null) {
					location = new DeviceLocation(geoDate, lon.doubleValue(), lat.doubleValue(), geo.getString("src"));
				}
			}
		}
		this.type = type;
		this.weight = weight;
		this.unit = unit;
		this.minWeight = minWeight;
		this.maxWeight = maxWeight;
		this.realWeight = realWeight;
		this.idCampaign = idCampaign;
		this.idDevice = idDevice;
		this.date = date;
		this.products = products;
		this.dateReceived = new Date().getTime();
	}

	@Override
	public String toString() {
		return toJsonObject().toString();
	}

	public JsonObject toJsonObject() {
		JsonArray productsArray = new JsonArray();
		for (int i = 0; i < products.size(); i++) {
			productsArray.add(products.get(i).toJsonObject());
		}

		JsonObject object = new JsonObject();
		object.putString("idCampaign", idCampaign);
		object.putString("idDevice", idDevice);
		object.putNumber("date", date);
		object.putNumber("dateReceived", dateReceived);
		if (location != null) {
			object.putObject("location", location.toJsonObject());
		}
		if (type != null && weight != null && unit != null && minWeight != null && maxWeight != null && realWeight != null) {
			object.putBoolean("massInput", true);
			object.putString("type", type);
			object.putNumber("weight", weight);
			object.putString("unit", unit);
			object.putNumber("minWeight", minWeight);
			object.putNumber("maxWeight", maxWeight);
			object.putNumber("realWeight", realWeight);
		}else {
			Utils.log(this.getClass().getName(), "type", type, "weight", String.valueOf(weight), "unit", unit, "minWeight", String.valueOf(minWeight), "maxWeight", 
					String.valueOf(maxWeight), "realWeight", String.valueOf(realWeight));
		}
		object.putArray("products", productsArray);
		return object;
	}

	public boolean hasTowerLocation() {
		if (location == null) {
			return false;
		}
		return location.hasTowerLocation();
	}

	public boolean hasGpsLocation() {
		return location.hasGpsLocation();
	}

	public void setGpsLocationInferred(Coordinate coordinate) {
		location.setGpsLocationInferred(coordinate);
	}

	public void setGpsLocationResolved(Coordinate coordinate) {
		location.setGpsLocationResolved(coordinate);
	}

	public Coordinate getGpsCoordinate() {
		return location.getGpsCoordinate();
	}

	public JsonObject getLocation() {
		return location.getLocation();
	}

	public JsonObject getCompleteLocation() {
		return location.toJsonObject();
	}

	public boolean isPreSet() {
		return type != null;
	}

	public void setMinMaxWeight(int minWeight, int maxWeight) {
		this.minWeight = minWeight;
		this.maxWeight = maxWeight;
	}

	public boolean hasMinMaxWeight() {
		return minWeight != null && maxWeight != null;
	}

	public String getIdCampaign() {
		return idCampaign;
	}

	public String getIdDevice() {
		return idDevice;
	}

	public long getDate() {
		return date;
	}

	public String getType() {
		return type;
	}

	public Integer getWeight() {
		return weight;
	}

	public String getUnit() {
		return unit;
	}

	public Integer getMaxWeight() {
		return maxWeight;
	}

	public Integer getMinWeight() {
		return minWeight;
	}

	public Integer getRealWeight() {
		return realWeight;
	}

	public List<IProduct> getProducts() {
		return products;
	}
}
