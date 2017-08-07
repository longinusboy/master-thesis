package org.projmis.demo.data;

import org.vertx.java.core.json.JsonObject;

public interface IProduct {
	public String getName();
	public void setName(String name);
	
	public String getType();
	public void setType(String type);
	
	public Integer getWeight();
	public void setWeight(Integer weight);
	
	public String getUnit();
	public void setUnit(String unit);
	
	public Integer getRealWeight();
	public void setRealWeight(Integer weight);
	
	public Integer getQuantity();
	public void setQuantity(Integer quantity);
	
	public String toString();
	public JsonObject toJsonObject();
}
