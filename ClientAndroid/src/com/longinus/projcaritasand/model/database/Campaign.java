package com.longinus.projcaritasand.model.database;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class Campaign {
	private String id;
	private String name;
	private Date startDate;
	private Date endDate;
	private boolean subscribed;
	private String screen;
	private Long sumWeights;
	private Integer sumRegistries;
	
	public Campaign(String id, String name, Date startDate, Date endDate, boolean subscribed, String screen, Long sumWeights, Integer sumRegistries) {
		super();
		this.id = id;
		this.name = name;
		this.startDate = startDate;
		this.endDate = endDate;
		this.subscribed = subscribed;
		this.screen = screen;
		this.sumWeights = sumWeights;
		this.sumRegistries = sumRegistries;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public boolean isSubscribed() {
		return subscribed;
	}

	public void setSubscribed(boolean subscribed) {
		this.subscribed = subscribed;
	}

	public String getScreen() {
		return screen;
	}

	public Long getSumWeights() {
		return sumWeights;
	}

	public Integer getSumRegistries() {
		return sumRegistries;
	}

	@Override
	public String toString() {
		try {
			return new JSONObject()
					.put("id", id)
					.put("name", name)
					.put("startDate", startDate.getTime())
					.put("endDate", endDate.getTime())
					.put("subscribed", subscribed)
					.put("screen", screen).toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return "{\"error\":\""+e.getLocalizedMessage()+"\"}";
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Campaign) {
			if (((Campaign)obj).getId().contentEquals(this.id)) {
				return true;
			}
		}
		return false;
	}

}
