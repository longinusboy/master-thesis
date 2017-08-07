package com.longinus.projcaritasand;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.longinus.projcaritasand.model.SingletonModel;

@SuppressLint("SetJavaScriptEnabled")
public class StatisticsFragment extends Fragment {
	private Context context;
	private WebView webView;
	
	public StatisticsFragment() {
		super();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_webview, container, false);
		
		webView = (WebView) view.findViewById(R.id.webView);
		
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient());
		
		Map<String, Float> statsRacio = SingletonModel.getInstance().getStatsRacio();
		
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
		    reader = new BufferedReader(new InputStreamReader(context.getAssets().open("pages/chart_stats.html"), "UTF-8")); 
		    
		    String mLine;
		    while ((mLine = reader.readLine()) != null) {
		       sb.append(mLine+"\n");
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
		
		String assetContent = sb.toString();

		sb = new StringBuilder();
		List<Entry<String, Float>> listStatsRacio = new ArrayList<Map.Entry<String,Float>>();
		Set<Entry<String, Float>> setStatsRacio = statsRacio.entrySet();
		for (Entry<String, Float> entry : setStatsRacio) {
			listStatsRacio.add(entry);
		}
		if (listStatsRacio.size()>0) {
			sb.append("\t<table>\n");
			
			for (int i = 0; i < listStatsRacio.size(); i++) {
				Entry<String, Float> stat = listStatsRacio.get(i);
				sb.append("\t\t<tr>\n\t\t\t<td>\n\t\t\t\t<div class=\"image\">\n");
				
				int value = (int) (stat.getValue()*100.0);
				sb.append("\t\t\t\t\t<img width=\""+((value<95)?((value>=0)?value:0):95)+"%\" height=\"24\" src=\"bar_full.png\" />\n");
				/*if (value<95) {
					sb.append("\t\t\t\t\t<img width=\"*\" height=\"24\" src=\"bar_blank.png\" style=\"float: right\" />\n");
				}*/
				sb.append("\t\t\t\t\t<span class=\"percvalue\">"+stat.getKey()+" ("+((stat.getValue()>=0f)?String.format("%.1f", stat.getValue()*100.0)+"%":"ND")+")</span>\n");
				sb.append("\t\t\t\t</div>\n\t\t\t</td>\n\t\t</tr>\n");
			}
			
			sb.append("\t</table>\n");
		}else {
			sb.append("<table class=\"tablecenter\"><tr><td><span class=\"gray\">Estat&iacute;sticas ainda n&atilde;o obtidas.</span></td></tr></table>");
		}
		
		
		String pageContent = assetContent.replace("%DATACONTENT%", sb.toString());
		
		//Log.d("Chart content", pageContent);
		
		
		webView.loadDataWithBaseURL("file:///android_asset/pages/", pageContent, "text/html", "UTF-8", null);
		
		return view;
	}
	
	@Override
	public void onAttach(Activity activity) {
		context = activity;
		super.onAttach(activity);
	}

}
