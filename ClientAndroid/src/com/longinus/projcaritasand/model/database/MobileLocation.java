package com.longinus.projcaritasand.model.database;

public class MobileLocation implements ILocation {
	private long time;
	private Integer mcc;
	private Integer mnc;
	private Integer cid;
	private Integer lac;

	public MobileLocation(long lastUpdate, Integer mcc, Integer mnc, Integer cid, Integer lac) {
		super();
		this.time = lastUpdate;
		this.mcc = mcc;
		this.mnc = mnc;
		this.cid = cid;
		this.lac = lac;
	}

	@Override
	public long getTime() {
		return time;
	}

	public void setLastUpdate(long lastUpdate) {
		this.time = lastUpdate;
	}

	public Integer getMcc() {
		return mcc;
	}

	public void setMcc(Integer mcc) {
		this.mcc = mcc;
	}

	public Integer getMnc() {
		return mnc;
	}

	public void setMnc(Integer mnc) {
		this.mnc = mnc;
	}

	public Integer getCid() {
		return cid;
	}

	public void setCid(Integer cid) {
		this.cid = cid;
	}

	public Integer getLac() {
		return lac;
	}

	public void setLac(Integer lac) {
		this.lac = lac;
	}

	@Override
	public String toString() {
		return "{\"time\":"+time+", \"mcc\":"+mcc+", \"mnc\":"+mnc+", \"cid\":"+cid+", \"lac\":"+lac+"}";
	}

}
