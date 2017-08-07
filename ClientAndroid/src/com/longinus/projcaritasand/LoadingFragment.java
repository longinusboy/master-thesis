package com.longinus.projcaritasand;

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

import com.longinus.projcaritasand.model.Constants;
import com.longinus.projcaritasand.model.SingletonModel;
import com.longinus.projcaritasand.model.listeners.DeviceListener;
import com.longinus.projcaritasand.model.listeners.MainWebInterfaceListener;

@SuppressLint("SetJavaScriptEnabled")
public class LoadingFragment extends Fragment {
	private Context context;
	private WebView webView;
	
	public LoadingFragment() {
		super();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_webview, container, false);
		
		MainWebInterfaceListener mainWebInterfaceListener = new MainWebInterfaceListener() {
			@Override
			@JavascriptInterface
			public float getListHeight() {
				return 0;
			}
			
			@Override
			@JavascriptInterface
			public void onScanClick() {
				
			}

			@Override
			@JavascriptInterface
			public void onManualClick() {
				
			}

			@Override
			@JavascriptInterface
			public void finalizeBag() {
				
			}

			@Override
			@JavascriptInterface
			public void onRowClick(String id) {
				
			}

			@Override
			@JavascriptInterface
			public void loadingFinished() {
				if (context instanceof IMessages) {
					((IMessages)context).onMessage("LOADING_FINISHED", null);
				}
			}			
		};
		
		webView = (WebView) view.findViewById(R.id.webView);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient());
		webView.addJavascriptInterface(mainWebInterfaceListener, "Android");
		

		DeviceListener deviceListener = new DeviceListener() {

			@Override
			public void deviceChanged(final int status) {
				LoadingFragment.this.getActivity().runOnUiThread(new Runnable() {
					public void run() {
						try {
							switch (status) {
								case Constants.DEVICE_STATUS_INITIALIZING:
									webView.loadUrl("javascript:showMessage('A inicializar')");
									break;
								case Constants.DEVICE_STATUS_SEARCHING_SERVERS:
									webView.loadUrl("javascript:showMessage('A procurar servidor')");
									break;
								case Constants.DEVICE_STATUS_FOUND_SERVER:
									webView.loadUrl("javascript:showMessage('Servidor encontrado')");
									break;
								case Constants.DEVICE_STATUS_SERVER_NOT_FOUND:
									webView.loadUrl("javascript:showMessage('Nenhum servidor encontrado')");
									break;
								case Constants.DEVICE_STATUS_REGISTERING_DEVICE:
									webView.loadUrl("javascript:showMessage('A registar dispositivo')");
									break;
								case Constants.DEVICE_STATUS_BLOCKED:
									webView.loadUrl("javascript:showMessage('Coneção bloqueada pelo servidor')");
									if (context instanceof IMessages) {
										((IMessages)context).onMessage("BLOCKED", null);
									}
									break;
								case Constants.DEVICE_STATUS_REGISTERED:
									webView.loadUrl("javascript:showMessage('Dispositivo registado no servidor')");
									break;
								case Constants.DEVICE_STATUS_OK:
									webView.loadUrl("javascript:showMessage('Dispositivo já registado no servidor')");
									break;
								case Constants.DEVICE_STATUS_UPDATING_LOCAL_DATABASE:
									webView.loadUrl("javascript:showMessage('A atualizar dados')");
									break;
								case Constants.DEVICE_STATUS_NO_NETWORK:
									webView.loadUrl("javascript:showMessage('Dispositivo sem rede, os dados serão guardados localmente')");
									break;
								case Constants.DEVICE_STATUS_READY:
									webView.loadUrl("javascript:finishSequence('Dispositivo pronto')");									
									break;

								default:
									webView.loadUrl("javascript:showMessage('Falha a inicializar')");
									break;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		};
		webView.loadUrl("file:///android_asset/pages/intro.html");
		
		SingletonModel.getInstance().setDeviceListener(deviceListener);
		
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		context = activity;
		super.onAttach(activity);
	}
}
