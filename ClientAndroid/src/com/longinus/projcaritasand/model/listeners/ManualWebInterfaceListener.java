package com.longinus.projcaritasand.model.listeners;

import android.webkit.JavascriptInterface;

public interface ManualWebInterfaceListener {
	@JavascriptInterface
	void addProduct(String type, int weight, int minWeight, int maxWeight, String unit, int realWeight, String displayedUnit);
	
	@JavascriptInterface
	void setTitle(String type, String measuredUnit);
}
