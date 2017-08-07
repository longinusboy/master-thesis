package com.longinus.projcaritasand;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.Normalizer;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.longinus.projcaritasand.model.SingletonModel;
import com.longinus.projcaritasand.model.listeners.ManualWebInterfaceListener;

@SuppressLint("SetJavaScriptEnabled")
public class MassMainFragment extends Fragment implements IFragmentListener {
	private Context context;
	private WebView webView;
	
	private String selectedType;
	private String selectedSubtype;
	private boolean inSearch;

	public MassMainFragment() {
		super();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_webview, container, false);
		
		webView = (WebView) view.findViewById(R.id.webView);

		ManualWebInterfaceListener manualWebInterfaceListener = new ManualWebInterfaceListener() {

			@Override
			@JavascriptInterface
			public void addProduct(String type, int weight, int minWeight, int maxWeight, String unit, int realWeight, String displayedUnit) {
				try {
					JSONObject groupData = new JSONObject();
					groupData.put("type", type);
					groupData.put("weight", weight);
					groupData.put("minWeight", minWeight);
					groupData.put("maxWeight", maxWeight);
					groupData.put("unit", unit);
					groupData.put("realWeight", realWeight);
					groupData.put("displayedUnit", displayedUnit);
					Log.d("MassMainFragment", groupData.toString());
					if (context instanceof IMessages) {
						((IMessages)context).onMessage("MASS_GROUP_CHOOSED", groupData);
					}
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
				
			}

			@Override
			@JavascriptInterface
			public void setTitle(String type, String measuredUnit) {
				if (context instanceof MainActivity) {
					((MainActivity)context).setLevelTitle(type, measuredUnit);
				}
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
					if (context instanceof IMessages) {
						((IMessages)context).onMessage("CLOSE_SEARCH", null);
					}
					return true;
				}
				if (url.contains("manualquantity")) {
					String subtype = url.substring(url.indexOf('-') + 1, url.lastIndexOf('.'));
					selectedSubtype = subtype;

					navigateSubtype(subtype);
					if (context instanceof IMessages) {
						((IMessages)context).onMessage("CLOSE_SEARCH", null);
					}
					return true;
				}
				return false;
			}

		});
		webView.clearCache(true);
		webView.addJavascriptInterface(manualWebInterfaceListener, "Android");

		inSearch = false;
		
		navigateMain();
		
		return view;
	}
	
	@Override
	public void onAttach(Activity activity) {
		context = activity;
		super.onAttach(activity);
	}

	private void navigateMain() {
		String templateContent = loadTemplate();
		String manualScreenJsonString = loadScreen();

		String mainScreenString = loadMain(manualScreenJsonString);
		String pageContent = templateContent.replace("%DATACONTENT%", mainScreenString.toString());

		//Log.d("Manual content", pageContent);
		
		webView.loadDataWithBaseURL("file:///android_asset/pages/", pageContent, "text/html", "UTF-8", null);
	}
	
	private void navigateType(final String type) {
		String templateContent = loadTemplate();
		String manualScreenJsonString = loadScreen();

		String mainScreenString = loadSubType(manualScreenJsonString, type);
		String pageContent = templateContent.replace("%DATACONTENT%", mainScreenString.toString());
		
		//Log.d("Manual content", pageContent);

		webView.loadDataWithBaseURL("file:///android_asset/pages/", pageContent, "text/html", "UTF-8", null);
	}
	
	private void navigateSubtype(final String subtype) {
		String templateQuantitiesContent = loadQuantitiesTemplate();
		String manualScreenJsonString = loadScreen();

		String mainScreenString = loadQuantities(manualScreenJsonString, subtype);
		String pageContent = templateQuantitiesContent.replace("%DATACONTENT%", mainScreenString.toString());

		webView.loadDataWithBaseURL("file:///android_asset/pages/", pageContent, "text/html", "UTF-8", null);
	}
	
	private void navigateSearch(String search) {
		if (search.isEmpty()) {
			inSearch = false;
			navigateMain();
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
	public boolean onBackPressed() {
		if (inSearch) {
			inSearch = false;
			navigateMain();
			if (context instanceof MainActivity) {
				((MainActivity)context).setLevelTitle(null, null);
			}
			return true;
		}else {
			if (selectedType==null && selectedSubtype==null) {
				return false;
			}else {
				if (selectedSubtype!=null) {
					navigateType(selectedType);				
					selectedSubtype = null;
					if (context instanceof MainActivity) {
						((MainActivity)context).setLevelTitle(selectedType, null);
					}
				}else if (selectedType!=null) {
					navigateMain();
					selectedType = null;
					if (context instanceof MainActivity) {
						((MainActivity)context).setLevelTitle(null, null);
					}
				}
				return true;
			}
		}
	}

	@Override
	public void onSearchQuery(String query) {
		navigateSearch(query);
	}

	@Override
	public void onSearchClose() {
		inSearch = false;//true;
		navigateMain();
	}

	@Override
	public void updateWebview() {
	}

	private String loadTemplate() {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(context.getAssets().open("pages/manual_template.html"), "UTF-8"));

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
			reader = new BufferedReader(new InputStreamReader(context.getAssets().open("pages/manualquantities_template.html"), "UTF-8"));

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
			reader = new BufferedReader(new InputStreamReader(context.getAssets().open("pages/default_screen.json"), "UTF-8"));

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
			
			//Log.d("names", jsonArraySubtypesNames.toString());
			for (int i = 0; i < jsonArraySubtypesNames.length(); i++) {
				String subtypeName = jsonArraySubtypesNames.getString(i);
				JSONArray jsonTypeArray = subtypesObject.getJSONArray(subtypeName);
				for (int j = 0; j < jsonTypeArray.length(); j++) {
					JSONObject jsonSubType = jsonTypeArray.getJSONObject(j);
					try {
						if (replaceUrlEncodes(jsonSubType.getString("name")).contains(Normalizer.normalize(query, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""))) {
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
}
