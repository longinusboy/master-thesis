package com.longinus.projcaritasand;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.longinus.projcaritasand.model.SingletonModel;
import com.longinus.projcaritasand.model.listeners.ManualWebInterfaceListener;

@SuppressLint("SetJavaScriptEnabled")
public class SimpleListFragment extends Fragment implements IFragmentListener {
	private Context context;
	private WebView webView;

	private String selectedType;
	private boolean inSearch;

	public SimpleListFragment() {
		super();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_webview, container, false);
				
		webView = (WebView) view.findViewById(R.id.webView);

		ManualWebInterfaceListener manualWebInterfaceListener = new ManualWebInterfaceListener() {

			@Override
			@JavascriptInterface
			public void setTitle(String type, String measuredUnit) {
				if (context instanceof MainActivity) {
					((MainActivity)context).setLevelTitle(type, measuredUnit);
				}
			}

			@Override
			@JavascriptInterface
			public void addProduct(String type, int weight, int minWeight, int maxWeight, String unit, int realWeight, String displayedUnit) {
				throw new RuntimeException("Code not allowed to be reacheble");
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
			if (selectedType==null) {
				return false;
			}else {
				navigateMain();
				selectedType = null;
				if (context instanceof MainActivity) {
					((MainActivity)context).setLevelTitle(null, null);
				}
				return true;
			}
		}
	}

	@Override
	public void onSearchQuery(String query) {
	}

	@Override
	public void onSearchClose() {
	}

	@Override
	public void updateWebview() {
	}

	public boolean onSupportNavigateUp() {
		if (selectedType==null) {
			return false;
		}else {
			navigateMain();
			selectedType = null;
			if (context instanceof MainActivity) {
				((MainActivity)context).setLevelTitle(null, null);
			}
			return true;
		}
	}

	private String loadTemplate() {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(getActivity().getAssets().open("pages/manual_template.html"), "UTF-8"));

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
						sb.append("<input type=\"button\" class=\"typeButton\")\" value=\""
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
}
