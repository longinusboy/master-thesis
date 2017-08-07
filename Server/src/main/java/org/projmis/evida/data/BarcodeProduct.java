package org.projmis.evida.data;

import org.vertx.java.core.json.JsonObject;

public class BarcodeProduct implements IProduct {
	private String ean;
	private String name;
	private String type;
	private Integer weight;
	private String unit;
	private Integer realWeight;
	private Integer quantity;
	private boolean inConflic;
	
	public BarcodeProduct(String ean, int quantity) {
		this.ean = ean;
		this.quantity = quantity;
		this.inConflic = false;
		this.name = null;
		this.type = null;
		this.weight = null;
		this.unit = null;
		this.realWeight = null;
	}

	public BarcodeProduct(BarcodeProduct barcodeProduct) {
		this.ean = new String(barcodeProduct.ean);
		this.quantity = barcodeProduct.quantity;
		this.type = new String(barcodeProduct.type);
		this.weight = barcodeProduct.weight;
		this.unit = new String(barcodeProduct.unit);
		this.inConflic = barcodeProduct.inConflic;
		this.realWeight = barcodeProduct.realWeight;
	}

	public String getEan() {
		return ean;
	}

	public boolean isInConflic() {
		return inConflic;
	}

	public void setInConflic(boolean inConflic) {
		this.inConflic = inConflic;
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
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"ean\":\"" + ean + "\", \"quantity\":" + quantity);
		if (type!=null) {
			sb.append(", \"type\":\""+type+"\"");
		}
		if (weight!=null) {
			sb.append(", \"weight\":"+weight);
		}
		if (unit!=null) {
			sb.append(", \"unit\":\""+unit+"\"");
		}
		if (realWeight!=null) {
			sb.append(", \"realWeight\":"+realWeight);
		}
		if (inConflic) {
			sb.append(", \"conflict\":\"true\"");
		}
		sb.append("}");
		return sb.toString();
	}
	
	@Override
	public JsonObject toJsonObject() {
		return new JsonObject(toString());
	}
}
