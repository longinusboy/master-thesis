package com.longinus.projcaritasand.model.products;

import java.util.List;

public class MassProductContainer {
	private String type;
	private Integer weight;
	private Integer minWeight;
	private Integer maxWeight;
	private String unit;
	private Integer realWeight;
	private List<ProductBarcode> products;

	public MassProductContainer(String type, Integer weight, Integer minWeight, Integer maxWeight, String unit, Integer realWeight, List<ProductBarcode> products) {
		super();
		this.type = type;
		this.weight = weight;
		this.minWeight = minWeight;
		this.maxWeight = maxWeight;
		this.unit = unit;
		this.realWeight = realWeight;
		this.products = products;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	public Integer getMinWeight() {
		return minWeight;
	}

	public void setMinWeight(Integer minWeight) {
		this.minWeight = minWeight;
	}

	public Integer getMaxWeight() {
		return maxWeight;
	}

	public void setMaxWeight(Integer maxWeight) {
		this.maxWeight = maxWeight;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public Integer getRealWeight() {
		return realWeight;
	}

	public void setRealWeight(Integer realWeight) {
		this.realWeight = realWeight;
	}

	public List<ProductBarcode> getProducts() {
		return products;
	}

	public void setProducts(List<ProductBarcode> products) {
		this.products = products;
	}

}
