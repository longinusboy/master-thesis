package com.longinus.projcaritasand.model;

import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


public class Utils {
	public static final String md5(final String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				String h = Integer.toHexString(0xFF & messageDigest[i]);
				while (h.length() < 2)
					h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public static String getID(Context context) {
		String androidId = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
		String serial = android.os.Build.SERIAL;
		
		return androidId+"-"+serial;
	}
	
	public static boolean isWifiConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		try {
			if (!activeNetwork.isConnectedOrConnecting()) {
				return false;
			}
			return activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
		} catch (Exception e) {
			return false;
		}
	}
	
	public static boolean isMobileConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		try {
			if (!activeNetwork.isConnectedOrConnecting()) {
				return false;
			}
			return activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
		} catch (Exception e) {
			return false;
		}
	}
	
	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager CManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		    NetworkInfo nInfo = CManager.getActiveNetworkInfo();
		    if (nInfo == null) {
				return false;
			}
		    return true;
	}
	
	public static String dateToFormattedText(Date date) {
		if (date == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		sb.append(String.format("%02d", cal.get(Calendar.DAY_OF_MONTH)));
		sb.append(Constants.monthNamesWithPrefix[cal.get(Calendar.MONTH)]);
		return sb.toString();
	}
}
