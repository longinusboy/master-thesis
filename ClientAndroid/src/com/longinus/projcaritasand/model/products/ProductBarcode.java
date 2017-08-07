package com.longinus.projcaritasand.model.products;

import org.json.JSONException;
import org.json.JSONObject;

public class ProductBarcode implements IProduct {
	private String ean;
	private String name;
	private String type;
	private Integer weight;
	private String unit;
	private Integer realWeight;
	private Integer quantity;
	
	public ProductBarcode(String ean, String name, String type, Integer weight, String unit, Integer realWeight, Integer quantity) {
		super();
		this.ean = ean;
		this.name = name;
		this.type = type;
		this.weight = weight;
		this.unit = unit;
		this.realWeight = realWeight;
		this.quantity = quantity;
	}
	
	public ProductBarcode(String ean, String name, String type, Integer weight, String unit, Integer realWeight) {
		super();
		this.ean = ean;
		this.name = name;
		this.type = type;
		this.weight = weight;
		this.unit = unit;
		this.realWeight = realWeight;
		this.quantity = 0;
	}

	public ProductBarcode(String ean, String type, Integer weight, String unit, Integer realWeight, Integer quantity) {
		super();
		this.ean = ean;
		this.type = type;
		this.weight = weight;
		this.unit = unit;
		this.realWeight = realWeight;
		this.quantity = quantity;
	}

	public ProductBarcode(String ean, int quantity) {
		this.ean = ean;
		this.quantity = quantity;
	}

	public String getEan() {
		return ean;
	}

	public void setEan(String ean) {
		this.ean = ean;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public Integer getWeight() {
		return weight;
	}

	@Override
	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	@Override
	public String getUnit() {
		return unit;
	}

	@Override
	public void setUnit(String unit) {
		this.unit = unit;
	}

	@Override
	public Integer getRealWeight() {
		return realWeight;
	}

	@Override
	public void setRealWeight(Integer weight) {
		this.realWeight = weight;
	}

	@Override
	public Integer getQuantity() {
		return quantity;
	}

	@Override
	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	@Override
	public String[] getStringArray() {
		if (name!=null) {
			return new String []{name, String.valueOf(weight)+unit, String.valueOf(quantity)};
		}
		return new String []{ean, "", String.valueOf(quantity)};
	}

	@Override
	public String toString() {		
		return toJsonObject().toString();
	}
	
	@Override
	public JSONObject toJsonObject() {		
		try {
			JSONObject object = new JSONObject();
			object.put("ean", ean).put("quantity", quantity);
			if (name != null) {
				object.put("name", name);
			}
			if (type != null) {
				object.put("type", type);
			}
			if (weight != null) {
				object.put("weight", weight);
			}
			if (unit != null) {
				object.put("unit", unit);
			}
			if (realWeight != null) {
				object.put("realWeight", realWeight);
			}
			return object;
		} catch (JSONException e) {
			e.printStackTrace();
			return new JSONObject();
		}		
	}
}
