package com.longinus.projcaritasand.model.database;

public class Type {
	private String idPrimary;
	private Integer idSecondary;
	private String name;
	private String subname;

	public Type(String idPrimary, Integer idSecondary, String name, String subname) {
		super();
		this.idPrimary = idPrimary;
		this.idSecondary = idSecondary;
		this.name = name;
		this.subname = subname;
	}

	public String getType() {
		if (idSecondary!=null) {
			return idPrimary+"-"+idSecondary;
		}
		return idPrimary;
	}

	public String getName() {
		return name;
	}

	public String getSubname() {
		return subname;
	}

	@Override
	public String toString() {
		return "Type [idPrimary=" + idPrimary + ", idSecondary=" + idSecondary + ", name=" + name + ", subname=" + subname + "]";
	}

}
