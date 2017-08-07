package com.longinus.projcaritasand.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONObject;

public class SettingsManager {
	private File workingFolder = null;
	private String baseUrl = null;
	private String campaignSelected = null;
	
	public String getBaseUrl() {
		if(baseUrl==null)
			throw new RuntimeException("Settings not properly initialized");
		return baseUrl;
	}

	public String getCampaignSelected() {
		return campaignSelected;
	}
	
	public void setWorkingFolder(File workingFolder) {
		this.workingFolder = workingFolder;
	}

	public File getWorkingFolder() {
		if(workingFolder==null)
			throw new RuntimeException("Working Folder not defined");
		return workingFolder;
	}

	public boolean loadSettings() {
		if(workingFolder==null)
			throw new RuntimeException("Working Folder not defined");
		StringBuilder sb = new StringBuilder();
		char[] buffer = new char[1024];
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(workingFolder, "settings.json"))));
			int n;
			while ((n=bufferedReader.read(buffer))!=-1) {
				sb.append(buffer, 0, n);
			}
			JSONObject jsonObject = new JSONObject(sb.toString());
			try {
				baseUrl = jsonObject.getString("baseUrl");
				campaignSelected = jsonObject.getString("campaignSelected");
				return true;
			} catch (Exception e) {
				baseUrl = null;
				campaignSelected = null;
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}finally{
			try {
				bufferedReader.close();
			} catch (IOException e) {
			}
		}
	}
	
	
}
