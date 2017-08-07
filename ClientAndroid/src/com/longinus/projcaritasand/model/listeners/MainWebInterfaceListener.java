package com.longinus.projcaritasand.model.listeners;

import android.webkit.JavascriptInterface;

public interface MainWebInterfaceListener {
	@JavascriptInterface
	public void loadingFinished();
	
	@JavascriptInterface
	public float getListHeight();
	
	@JavascriptInterface
	public void onManualClick();

	@JavascriptInterface
	public void onScanClick();

	@JavascriptInterface
	public void finalizeBag();

	@JavascriptInterface
	public void onRowClick(String id);
}
