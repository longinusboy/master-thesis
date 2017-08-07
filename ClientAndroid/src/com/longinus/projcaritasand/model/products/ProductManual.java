package com.longinus.projcaritasand.model.products;

import org.json.JSONException;
import org.json.JSONObject;

public class ProductManual implements IProduct {
	private String name;
	private String type;
	private Integer weight;
	private String unit;
	private Integer realWeight;
	private Integer quantity;
	
	public ProductManual(String type, int weight, String unit, Integer realWeight, int quantity) {
		this.type = type;
		this.weight = weight;
		this.unit = unit;
		this.realWeight = realWeight;
		this.quantity = quantity;
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
		return new String []{type, String.valueOf(weight)+unit, String.valueOf(quantity)};
	}

	@Override
	public String toString() {
		return "{\"type\":" + type + ", \"weight\":" + weight +  ", \"unit\":\"" + unit + "\", \"realWeight\":" + realWeight + ", \"quantity\":" + quantity + "}";
	}

	@Override
	public JSONObject toJsonObject() {
		try {
			return new JSONObject("{\"type\":" + type + ", \"weight\":" + weight +  ", \"unit\":\"" + unit + "\", \"realWeight\":" + realWeight + ", \"quantity\":" + quantity + "}");
		} catch (JSONException e) {
			e.printStackTrace();
			return new JSONObject();
		}
	}
}
