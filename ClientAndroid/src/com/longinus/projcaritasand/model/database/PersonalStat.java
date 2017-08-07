package com.longinus.projcaritasand.model.database;

public class PersonalStat {
	private String idCampaign;
	private Long sumWeights;
	private Integer sumRegistries;
	private Long mySumWeights;
	private Integer mySumRegistries;
	private Integer countDevices;
	private Integer myRank;

	public PersonalStat(String idCampaign, Long sumWeights, Integer sumRegistries) {
		super();
		this.idCampaign = idCampaign;
		this.sumWeights = sumWeights;
		this.sumRegistries = sumRegistries;
	}

	public PersonalStat(String idCampaign, Long sumWeights, Integer sumRegistries, Long mySumWeights, Integer mySumRegistries, Integer countDevices, Integer myRank) {
		super();
		this.idCampaign = idCampaign;
		this.sumWeights = sumWeights;
		this.sumRegistries = sumRegistries;
		this.mySumWeights = mySumWeights;
		this.mySumRegistries = mySumRegistries;
		this.countDevices = countDevices;
		this.myRank = myRank;
	}

	public String getIdCampaign() {
		return idCampaign;
	}

	public Long getSumWeights() {
		return sumWeights;
	}

	public Integer getSumRegistries() {
		return sumRegistries;
	}

	public Long getMySumWeights() {
		return mySumWeights;
	}

	public Integer getMySumRegistries() {
		return mySumRegistries;
	}

	public Integer getCountDevices() {
		return countDevices;
	}

	public Integer getMyRank() {
		return myRank;
	}
}
