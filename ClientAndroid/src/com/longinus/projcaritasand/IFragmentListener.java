package com.longinus.projcaritasand;

public interface IFragmentListener {
	boolean onBackPressed();
	void onSearchQuery(String query);
	void onSearchClose();
	void updateWebview();
}
