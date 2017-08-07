package org.projmis.demo.data;

import org.vertx.java.core.json.JsonObject;

public class ManualProduct implements IProduct {
	private String name;
	private String type;
	private Integer weight;
	private String unit;
	private Integer realWeight;
	private Integer quantity;
	
	public ManualProduct(String type, int weight, String unit, int realWeight, int quantity) {
		this.type = type;
		this.weight = weight;
		this.unit = unit;
		this.realWeight = realWeight;
		this.quantity = quantity;
		this.name = null;
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
	public Integer getQuantity() {
		return quantity;
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
	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		return "{\"type\":\"" + type + "\", \"weight\":" + weight + ", \"unit\":\""+unit+"\", \"realWeight\":" + realWeight + ", \"quantity\":" + quantity + "}";
	}
	
	@Override
	public JsonObject toJsonObject() {
		return new JsonObject(toString());
	}	
}
