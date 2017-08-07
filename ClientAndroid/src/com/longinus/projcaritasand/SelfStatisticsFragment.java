package com.longinus.projcaritasand;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.longinus.projcaritasand.model.SingletonModel;
import com.longinus.projcaritasand.model.database.PersonalStat;

@SuppressLint("SetJavaScriptEnabled")
public class SelfStatisticsFragment extends Fragment {
	private Context context;
	private WebView webView;

	public SelfStatisticsFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view;
		
		PersonalStat stat = SingletonModel.getInstance().getCampaignPersonalStats();
		
		if (stat != null) {
			view = inflater.inflate(R.layout.fragment_self_statistics, container, false);		
			
			TextView productsRegisteredText = (TextView) view.findViewById(R.id.tv_products_registered);
			Integer mySumRegistries = stat.getMySumRegistries();
			productsRegisteredText.setText((mySumRegistries!=null)?String.valueOf(mySumRegistries):"-");
			
			TextView weightRegisteredText = (TextView) view.findViewById(R.id.tv_weight_registered);
			Long mySumWeights = stat.getMySumWeights();
			weightRegisteredText.setText((mySumWeights!=null)?(String.valueOf((int)(mySumWeights/1000.0))+" Kg"):"-");
			
			TextView contributeCampaignText = (TextView) view.findViewById(R.id.tv_contribute_campaign);
			Long sumWeights = stat.getSumWeights();
			if (mySumWeights != null && sumWeights != null) {
				if (sumWeights!=0) {
					contributeCampaignText.setText(String.format("%.2f", (mySumWeights/(float)sumWeights)*100f)+"%");
				}else {
					contributeCampaignText.setText("-");
				}
				
			}else {
				contributeCampaignText.setText("-");
			}		
			
			TextView contributePositionText = (TextView) view.findViewById(R.id.tv_contribute_position);
			Integer myRank = stat.getMyRank();
			Integer countDevices = stat.getCountDevices();
			contributePositionText.setText(((myRank!=null)?String.valueOf(myRank):"-")+"/"+((countDevices!=null)?String.valueOf(countDevices):"-"));
			
			if (sumWeights != null && mySumWeights != null) {
				WebView webView = (WebView) view.findViewById(R.id.webViewSelfStats);
				webView.getSettings().setJavaScriptEnabled(true);
				webView.setWebViewClient(new WebViewClient());
				
				String templateContent = loadTemplate();
				String myContribute = String.format("%.1f", (mySumWeights/(float)sumWeights)*100f);
				String othersContribute = String.format("%.1f", (1.0-(mySumWeights/(float)sumWeights))*100f);
				String pageContent = templateContent.replace("%%MY_SCORE%%", myContribute).replace("%%OTHER_SCORE%%", othersContribute);
				//saveContent(pageContent);
				webView.loadDataWithBaseURL("file:///android_asset/pages/", pageContent, "text/html", "UTF-8", null);
			}
		}else {
			view = inflater.inflate(R.layout.fragment_webview, container, false);
			
			webView = (WebView) view.findViewById(R.id.webView);
			
			webView.getSettings().setJavaScriptEnabled(true);
			webView.setWebViewClient(new WebViewClient() {
				@Override
				public void onPageFinished(WebView view, String url) {
					webView.loadUrl("javascript:setMessage('Estat&iacute;sticas ainda n&atilde; dispon&iacute;veis.')");
				}
			});
			
			webView.loadUrl("file:///android_asset/pages/empty_content.html");
		}
		
		return view;
	}
	
	private void saveContent(String pageContent) {
		String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	    	File file = new File(Environment.getExternalStorageDirectory(), "persstats.html");
	    	if (file.exists()) {
				return;
			}
	    	
            try {
            	FileOutputStream fOut = new FileOutputStream(file, true);
                OutputStreamWriter myOutWriter =new OutputStreamWriter(fOut);
				myOutWriter.append(pageContent);
				myOutWriter.close();
	            fOut.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	}

	@Override
	public void onAttach(Activity activity) {
		context = activity;
		super.onAttach(activity);
	}

	private String loadTemplate() {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(context.getAssets().open("pages/pie_template.html"), "UTF-8"));

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
}
