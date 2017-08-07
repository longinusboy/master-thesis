package com.longinus.projcaritasand;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLDecoder;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.longinus.projcaritasand.model.SingletonModel;
import com.longinus.projcaritasand.model.listeners.ManualWebInterfaceListener;

@SuppressLint("SetJavaScriptEnabled")
public class DonationTypeSelectorActivity extends Activity {
	private WebView webView;
	
	private String selectedType;
	private String selectedSubtype;
	private boolean inSearch;
	private int itemIndex;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.manual_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		itemIndex = getIntent().getIntExtra("itemIndex", -1);
		if (itemIndex < 0) {
			finish();
		}

		webView = new WebView(this);
		setContentView(webView);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);

		ManualWebInterfaceListener manualWebInterfaceListener = new ManualWebInterfaceListener() {

			@Override
			@JavascriptInterface
			public void addProduct(String type, int weight, int minWeight, int maxWeight, String unit, int realWeight, String displayedUnit) {
				Intent intent = new Intent(DonationTypeSelectorActivity.this, ManualQuantityActivity.class);
				intent.putExtra("type", type);
				intent.putExtra("weight", weight);//min and max weight doesn't appear to be necessary
				intent.putExtra("unit", unit);
				intent.putExtra("realWeight", realWeight);
				intent.putExtra("displayedUnit", displayedUnit);
				Log.d("DonationTypeSelectorActivity-interface-addProduct", "type "+type+" weight "+weight+" unit "+unit);
				SingletonModel.getInstance().setBagItemData(itemIndex, type, weight, minWeight, maxWeight, unit, realWeight, displayedUnit);
				DonationTypeSelectorActivity.this.finish();
			}

			@Override
			@JavascriptInterface
			public void setTitle(String type, String measuredUnit) {
				setLevelTitle(type, measuredUnit);
			}
		};

		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url.contains("manualsubtype")) {
					String type = url.substring(url.indexOf('-') + 1, url.indexOf('-') + 1 + 1);
					selectedType = type;

					navigateType(type);
					return true;
				}
				if (url.contains("manualquantity")) {
					String subtype = url.substring(url.indexOf('-') + 1, url.lastIndexOf('.'));
					selectedSubtype = subtype;

					navigateSubtype(subtype);
					return true;
				}
				return false;
			}

		});
		webView.clearCache(true);
		webView.addJavascriptInterface(manualWebInterfaceListener, "Android");

		inSearch = false;
		
		navigateMain();
	}
	
	public void setLevelTitle(String type, String measuredUnit) {
		String title = SingletonModel.getInstance().getLevelTitle(type, measuredUnit);
		
		Log.d("setLevelTitle", "type "+type+" measuredUnit "+measuredUnit+" title "+title);
		
		try {
			title = URLDecoder.decode(title, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		final String decodedTitle = title;
		
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				getActionBar().setTitle("Intro. Manual"+((decodedTitle!=null)?(" > " + decodedTitle):""));
			}
		});
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void navigateMain() {
		Log.d("webview", "navigateMain");
		/*runOnUiThread(new Runnable() {
			
			@Override
			public void run() {*/
				String templateContent = loadTemplate();
				String manualScreenJsonString = loadScreen();

				String mainScreenString = loadMain(manualScreenJsonString);
				String pageContent = templateContent.replace("%DATACONTENT%", mainScreenString.toString());

				//Log.d("Manual content", pageContent);
				
				webView.loadDataWithBaseURL("file:///android_asset/pages/", pageContent, "text/html", "UTF-8", null);
			/*}
		});	*/	
	}
	
	private void navigateType(final String type) {
		Log.d("webview", "navigateType "+type);
		/*runOnUiThread(new Runnable() {
			
			@Override
			public void run() {*/
				String templateContent = loadTemplate();
				String manualScreenJsonString = loadScreen();

				String mainScreenString = loadSubType(manualScreenJsonString, type);
				String pageContent = templateContent.replace("%DATACONTENT%", mainScreenString.toString());

				webView.loadDataWithBaseURL("file:///android_asset/pages/", pageContent, "text/html", "UTF-8", null);
			/*}
		});	*/	
	}
	
	private void navigateSubtype(final String subtype) {
		Log.d("webview", "navigateSubtype "+subtype);
		/*runOnUiThread(new Runnable() {
			
			@Override
			public void run() {*/
				String templateQuantitiesContent = loadQuantitiesTemplate();
				String manualScreenJsonString = loadScreen();

				String mainScreenString = loadQuantities(manualScreenJsonString, subtype);
				String pageContent = templateQuantitiesContent.replace("%DATACONTENT%", mainScreenString.toString());

				webView.loadDataWithBaseURL("file:///android_asset/pages/", pageContent, "text/html", "UTF-8", null);
			/*}
		});		*/
	}
	
	private void navigateSearch(final String search) {
		if (search.isEmpty()) {
			inSearch = false;
			navigateMain();
			setLevelTitle(null, null);
			return;
		}
		inSearch = true;
		String templateContent = loadTemplate();
		String manualScreenJsonString = loadScreen();
	
		String mainScreenString = loadSearch(manualScreenJsonString, search);
		String pageContent = templateContent.replace("%DATACONTENT%", mainScreenString.toString());
	
		webView.loadDataWithBaseURL("file:///android_asset/pages/", pageContent, "text/html", "UTF-8", null);
	}

	@Override
	public void onBackPressed() {
		if (inSearch) {
			inSearch = false;
			navigateMain();
			setLevelTitle(null, null);
		}else {
			if (selectedType==null && selectedSubtype==null) {
				super.onBackPressed();
			}else {
				if (selectedSubtype!=null) {
					navigateType(selectedType);				
					selectedSubtype = null;
				}else if (selectedType!=null) {
					navigateMain();
					selectedType = null;
				}
				setLevelTitle(selectedType, selectedSubtype);
			}
		}
	}

	@Override
	public boolean onNavigateUp() {
		if (selectedType==null && selectedSubtype==null) {
			finish();
			return true;
		}else {
			if (selectedSubtype!=null) {
				navigateType(selectedType);				
				selectedSubtype = null;
			}else if (selectedType!=null) {
				navigateMain();
				selectedType = null;
			}
		}
		setLevelTitle(selectedType, selectedSubtype);
		return false;
	}

	private String loadTemplate() {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(getAssets().open("pages/manual_template.html"), "UTF-8"));

			String mLine;
			while ((mLine = reader.readLine()) != null) {
				sb.append(mLine + "\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}
	
	private String loadQuantitiesTemplate() {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(getAssets().open("pages/manualquantities_template.html"), "UTF-8"));

			String mLine;
			while ((mLine = reader.readLine()) != null) {
				sb.append(mLine + "\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	private String loadScreen() {
		
		String screen = SingletonModel.getInstance().getScreen();		
		if (screen != null) {
			return screen;
		}
		
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(getAssets().open("pages/default_screen.json"), "UTF-8"));

			String mLine;
			while ((mLine = reader.readLine()) != null) {
				sb.append(mLine + "\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	private String loadMain(String screen) {
		StringBuilder sb = new StringBuilder();
		try {
			JSONObject screenObject = new JSONObject(screen);

			JSONArray jsonArray = screenObject.getJSONArray("main");
			sb.append("<table border=\"0\" width=\"100%\">\n");
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					JSONObject object = jsonArray.getJSONObject(i);
					if (SingletonModel.getInstance().mustUseLowDpi() && object.getString("name").contentEquals("null")) {
						continue;
					}

					if (!SingletonModel.getInstance().mustUseLowDpi()) {
						if (i % 2 == 0) {
							sb.append("<tr>\n");
						}
						sb.append("<td width=\"50%\">");
					}else {
						sb.append("<tr>\n<td width=\"100%\">");
					}
					
					if (!object.getString("name").contentEquals("null")) {
						sb.append("<input type=\"button\" class=\"typeButton\" onClick=\"openPage('manualsubtype-" + object.getString("symbol") + ".html', '"
								+ object.getString("symbol") + "', null, null)\" value=\"" + object.getString("name") + "\"/>");
					} else {
						sb.append("&nbsp;");
					}
					sb.append("</td>\n");
					
					if (i % 2 != 0 || SingletonModel.getInstance().mustUseLowDpi()) {
						sb.append("</tr>\n");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			sb.append("</table>\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	private String loadSubType(String screen, String type) {
		StringBuilder sb = new StringBuilder();
		try {
			JSONObject screenObject = new JSONObject(screen);

			JSONObject subtypesObject = screenObject.getJSONObject("subtypes");
			JSONArray jsonArray = subtypesObject.getJSONArray(type);
			sb.append("<table border=\"0\" width=\"100%\">\n");
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					JSONObject object = jsonArray.getJSONObject(i);
					if (SingletonModel.getInstance().mustUseLowDpi() && object.getString("name").contentEquals("null")) {
						continue;
					}

					if (!SingletonModel.getInstance().mustUseLowDpi()) {
						if (i % 2 == 0) {
							sb.append("<tr>\n");
						}
						sb.append("<td width=\"50%\">");
					}else {
						sb.append("<tr>\n<td width=\"100%\">");
					}
					if (!object.getString("name").contentEquals("null")) {
						sb.append("<input type=\"button\" class=\"typeButton\" onClick=\"openPage('manualquantity-" + object.getString("symbol") + ".html', '"
								+ object.getString("symbol") + "', null)\" value=\"" + object.getString("name") + "\"/>");
					} else {
						sb.append("&nbsp;");
					}
					sb.append("</td>\n");
					
					if (i % 2 != 0 || SingletonModel.getInstance().mustUseLowDpi()) {
						sb.append("</tr>\n");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			sb.append("</table>\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	private String loadQuantities(String screen, String subtype) {
		StringBuilder sb = new StringBuilder();
		try {
			JSONObject screenObject = new JSONObject(screen);

			JSONObject subtypesObject = screenObject.getJSONObject("quantities");
			JSONArray jsonArray = subtypesObject.getJSONArray(subtype);
			sb.append("<table border=\"0\" width=\"100%\">\n");
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					JSONObject object = jsonArray.getJSONObject(i);
					if (SingletonModel.getInstance().mustUseLowDpi() && object.getString("name").contentEquals("null")) {
						continue;
					}

					if (!SingletonModel.getInstance().mustUseLowDpi()) {
						if (i % 2 == 0) {
							sb.append("<tr>\n");
						}
						sb.append("<td width=\"50%\">");
					}else {
						sb.append("<tr>\n<td width=\"100%\">");
					}
					if (!object.getString("name").contentEquals("null")) {
						sb.append("<input type=\"button\" class=\"typeButton\" onClick=\"addProduct('" + subtype + "', '" + object.getString("value")
								+ "', '" + object.getString("minWeight") + "', '" + object.getString("maxWeight") + "', '" + object.getString("unit")
								+ "', '" + object.getString("realWeight")+ "', '" + object.getString("name") + "')\" value=\"" + object.getString("name") + "\"/>");
					} else {
						sb.append("&nbsp;");
					}
					sb.append("</td>\n");
					
					if (i % 2 != 0 || SingletonModel.getInstance().mustUseLowDpi()) {
						sb.append("</tr>\n");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			sb.append("</table>\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	private String loadSearch(String screen, String query) {
		StringBuilder sb = new StringBuilder();
		try {
			JSONObject screenObject = new JSONObject(screen);
			JSONObject subtypesObject = screenObject.getJSONObject("subtypes");
			
			JSONArray jsonArraySubtypesNames = subtypesObject.names();
			JSONArray foundArray = new JSONArray();
			
			Log.d("names", jsonArraySubtypesNames.toString());
			for (int i = 0; i < jsonArraySubtypesNames.length(); i++) {
				String subtypeName = jsonArraySubtypesNames.getString(i);
				JSONArray jsonTypeArray = subtypesObject.getJSONArray(subtypeName);
				for (int j = 0; j < jsonTypeArray.length(); j++) {
					JSONObject jsonSubType = jsonTypeArray.getJSONObject(j);
					try {
						if (replaceUrlEncodes(jsonSubType.getString("name")).contains(replaceUrlEncodes(query))) {
							foundArray.put(jsonSubType);
						}
					} catch (Exception e) {
					}
				}
			}
			
			sb.append("<table border=\"0\" width=\"100%\">\n");
			for (int i = 0; i < foundArray.length(); i++) {
				try {
					JSONObject object = foundArray.getJSONObject(i);
					if (SingletonModel.getInstance().mustUseLowDpi() && object.getString("name").contentEquals("null")) {
						continue;
					}

					if (!SingletonModel.getInstance().mustUseLowDpi()) {
						if (i % 2 == 0) {
							sb.append("<tr>\n");
						}
						sb.append("<td width=\"50%\">");
					}else {
						sb.append("<tr>\n<td width=\"100%\">");
					}
					if (!object.getString("name").contentEquals("null")) {
						sb.append("<input type=\"button\" class=\"typeButton\" onClick=\"openPage('manualquantity-" + object.getString("symbol") + ".html')\" value=\""
								+ object.getString("name") + "\"/>");
					} else {
						sb.append("&nbsp;");
					}
					sb.append("</td>\n");
					
					if (i % 2 != 0 || SingletonModel.getInstance().mustUseLowDpi()) {
						sb.append("</tr>\n");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			sb.append("</table>\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	private String replaceUrlEncodes(String string) {
		String localString = new String(string.toLowerCase());
		
		String[][] replacements = {
				{"&aacute;", "a"}, 
                {"&agrave;", "a"},
                {"&acirc;", "a"},
                {"&atilde;", "a"},
                {"&eacute;", "e"}, 
                {"&egrave;", "e"},
                {"&iacute;", "i"},
                {"&oacute;", "o"},
                {"&otilde;", "o"},
                {"&ccedil;", "c"},
                {"&#13;&#10;", " "},
		};

		for(String[] replacement: replacements) {
			localString = localString.replace(replacement[0], replacement[1]);
		}
		
		return localString;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		int titleId =  getResources().getIdentifier("action_bar_title", "id", "android");

		// titleTxv : member reference to the action bar title textview
		// Setting Marquee here
		TextView titleTxv = (TextView) findViewById(titleId);
		titleTxv.setEllipsize(TextUtils.TruncateAt.START);
		titleTxv.setMarqueeRepeatLimit(-1);
		titleTxv.setFocusable(true);
		titleTxv.setFocusableInTouchMode(true);
		titleTxv.setHorizontallyScrolling(true);
		titleTxv.setFreezesText(true);
		titleTxv.setSingleLine(true);
		
		super.onResume();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.manual, menu);
		MenuItem searchItem = menu.findItem(R.id.item_search);
	    SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
	    searchView.setOnCloseListener(new OnCloseListener() {
			
			@Override
			public boolean onClose() {
				inSearch = false;
				navigateMain();
				return true;
			}
		});
	    searchView.setOnQueryTextListener(new OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String query) {
				Log.d("main-query", query);
				navigateSearch(query);
				return true;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
				Log.d("main-newText", newText);
				navigateSearch(newText);
				return true;
			}
		});
		return super.onCreateOptionsMenu(menu);
	}
}
