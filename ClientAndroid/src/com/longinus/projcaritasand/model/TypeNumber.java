package com.longinus.projcaritasand.model;

public class TypeNumber {
	private String idCampaign;
	private String type;
	private int value;

	public TypeNumber(String idCampaign, String type, int value) {
		this.idCampaign = idCampaign;
		this.type = type;
		this.value = value;
	}

	public String getIdCampaign() {
		return idCampaign;
	}

	public String getType() {
		return type;
	}

	public int getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "{\"idCampaign\":\"" + idCampaign + "\", \"type\":\"" + type + "\", \"value\":" + value + "}";
	}

}
