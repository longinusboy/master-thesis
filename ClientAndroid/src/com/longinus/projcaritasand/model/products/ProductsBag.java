package com.longinus.projcaritasand.model.products;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.longinus.projcaritasand.model.Constants;
import com.longinus.projcaritasand.model.database.GeoLocation;
import com.longinus.projcaritasand.model.database.MobileLocation;

public class ProductsBag {
	private Date registeredTime;
	private GeoLocation geoLocation;
	private MobileLocation mobileLocation;
	private List<IProduct> products;
	private boolean massInput;
	private MassProductContainer massContainer;
	
	public ProductsBag(List<IProduct> products) {
		registeredTime = new Date();
		this.products = new ArrayList<IProduct>();
		this.products.addAll(products);
		massInput = false;
	}
	
	public ProductsBag(Date registeredTime) {
		this.registeredTime = registeredTime;
		this.products = new ArrayList<IProduct>();
		massInput = false;
	}
	
	public ProductsBag(MassProductContainer container) {
		registeredTime = new Date();		
		this.products = new ArrayList<IProduct>();
		this.products.addAll(container.getProducts());
		massInput = true;
		massContainer = container;
	}
	
	public ProductsBag(Date registeredTime, MassProductContainer container) {
		this.registeredTime = registeredTime;
		this.products = new ArrayList<IProduct>();
		if (container.getProducts() != null) {
			this.products.addAll(container.getProducts());
		}		
		massInput = true;
		massContainer = container;
	}

	public Date getRegisteredTime() {
		return registeredTime;
	}

	public List<IProduct> getProducts() {
		return products;
	}

	public GeoLocation getGeoLocation() {
		return geoLocation;
	}

	public void setGeoLocation(GeoLocation geoLocation) {
		this.geoLocation = geoLocation;
	}

	public MobileLocation getMobileLocation() {
		return mobileLocation;
	}

	public void setMobileLocation(MobileLocation mobileLocation) {
		this.mobileLocation = mobileLocation;
	}

	public boolean isMassInput() {
		return massInput;
	}
	
	public String getType() {
		if (!massInput) {
			throw new RuntimeException("Not mass input bag");
		}
		try {
			return massContainer.getType();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String getUnit() {
		if (!massInput) {
			throw new RuntimeException("Not mass input bag");
		}
		try {
			return massContainer.getUnit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Integer getWeight() {
		if (!massInput) {
			throw new RuntimeException("Not mass input bag");
		}
		try {
			return massContainer.getWeight();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Integer getMinWeight() {
		if (!massInput) {
			throw new RuntimeException("Not mass input bag");
		}
		try {
			return massContainer.getMinWeight();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Integer getMaxWeight() {
		if (!massInput) {
			throw new RuntimeException("Not mass input bag");
		}
		try {
			return massContainer.getMaxWeight();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Integer getRealWeight() {
		if (!massInput) {
			throw new RuntimeException("Not mass input bag");
		}
		try {
			return massContainer.getRealWeight();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String toString() {
		JSONObject object = new JSONObject();
		try {
			object.put("date", registeredTime.getTime());
			
			JSONObject location = new JSONObject();
					
			if (geoLocation != null) {
				location.put("date", (long)geoLocation.getTime());
				location.put("lon", (double)geoLocation.getLon());
				location.put("lat", (double)geoLocation.getLat());
				location.put("src", geoLocation.getSource());
			}
			if (mobileLocation != null) {
				location.put("mcc", mobileLocation.getMcc());
				location.put("mnc", mobileLocation.getMnc());
				location.put("cid", mobileLocation.getCid());
				location.put("lac", mobileLocation.getLac());
			}
			if(location.names()!=null) {
				object.put("location", location);
			}
			if (massInput && massContainer!=null) {
				object.put("massInput", true);
				object.put("type", massContainer.getType());
				object.put("weight", massContainer.getWeight());
				object.put("minWeight", massContainer.getMinWeight());
				object.put("maxWeight", massContainer.getMaxWeight());
				object.put("unit", massContainer.getUnit());
				object.put("realWeight", massContainer.getRealWeight());
				
			}
			JSONArray productsJsonArray = new JSONArray();
			for (int i = 0; i < products.size(); i++) {
				productsJsonArray.put(new JSONObject(products.get(i).toString()));
			}
			object.put("products", productsJsonArray);
			
			if (Constants.DEBUG_MODE) 
				Log.d("ProductsBag-toString", object.toString());
			
			return object.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return "{}";
		}
	}
	
	public int size() {
		return products.size();
	}
}
