package com.longinus.projcaritasand;

import java.util.List;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.NumberPicker;

import com.longinus.projcaritasand.model.SingletonModel;
import com.longinus.projcaritasand.model.listeners.CommonListener;
import com.longinus.projcaritasand.model.listeners.MainWebInterfaceListener;
import com.longinus.projcaritasand.model.products.ProductBarcode;

@SuppressLint("SetJavaScriptEnabled")
public class MassQuantityFragment extends Fragment implements IFragmentListener {
	private Context context;
	private WebView webView;
	private String type = "";
	private int weight = 0;
	private int minWeight = 0;
	private int maxWeight = 0;
	private String unit = "";
	private int realWeight = 0;
	private String displayedUnit = "";
	private int itemClickedIndex;
	private NumberPicker numberPicker;
	
	public MassQuantityFragment() {
		super();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_mass_quantity, container, false);
		Bundle args = getArguments();
		type = args.getString("type");
		weight = args.getInt("weight", 0);
		minWeight = args.getInt("minWeight", 0);
		maxWeight = args.getInt("maxWeight", 0);
		unit = args.getString("unit");
		realWeight = args.getInt("realWeight", 0);
		String localDisplayedUnit = args.getString("displayedUnit");
		if (localDisplayedUnit != null)
			displayedUnit = localDisplayedUnit;
		
		if (context instanceof MainActivity) {
			((MainActivity)context).setLevelTitle(type, displayedUnit);
		}
		
		numberPicker = (NumberPicker) view.findViewById(R.id.numberPickerMass);
		
		ImageButton buttonBarcode = (ImageButton) view.findViewById(R.id.imgBtnBarcode);
		buttonBarcode.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					
					JSONObject groupData = new JSONObject();
					groupData.put("type", type);
					groupData.put("weight", weight);
					groupData.put("minWeight", minWeight);
					groupData.put("maxWeight", maxWeight);
					groupData.put("unit", unit);
					groupData.put("realWeight", realWeight);
					groupData.put("quantity", numberPicker.getValue());
					if (context instanceof IMessages) {
						((IMessages)context).onMessage("MASS_CAMERA", groupData);
					}
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
		});
		
		Button finalizeButton = (Button) view.findViewById(R.id.btn_finish);
		finalizeButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (context instanceof IMessages) {
					((IMessages)context).onMessage("MASS_GROUP_FINISHED", null);
				}
			}
		});
		
		webView = (WebView) view.findViewById(R.id.webViewMassQuantity);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				updateListView();
			}
		});
		
		MainWebInterfaceListener mainWebInterfaceListener = new MainWebInterfaceListener() {
			@Override
			@JavascriptInterface
			public float getListHeight() {
				return 0f;
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
				itemClickedIndex = Integer.parseInt(id) - 1;
				final Dialog dialog = new Dialog(getActivity());
				dialog.setContentView(R.layout.dialog_change_list);
				dialog.setTitle("Alterar quantidade");				
				dialog.show();
				
				final NumberPicker numberPicker = (NumberPicker) dialog.findViewById(R.id.numberPickerChange);
				Button btnChange = (Button) dialog.findViewById(R.id.btn_change);
				Button btnDelete = (Button) dialog.findViewById(R.id.btn_delete);
				
				int current = SingletonModel.getInstance().getMassBagItemQuantity(type, weight, unit, itemClickedIndex);
				Log.d("MassQuantity-dialog", "current: "+current);
				
				numberPicker.setValue(current);
				
				btnChange.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						SingletonModel.getInstance().setMassBagItemQuantity(type, weight, unit, itemClickedIndex, numberPicker.getValue());
						dialog.dismiss();
					}
				});
				
				btnDelete.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						SingletonModel.getInstance().removeMassBagItem(type, weight, unit, itemClickedIndex);
						dialog.dismiss();
					}
				});
			}

			@Override
			@JavascriptInterface
			public void loadingFinished() {
				
			}			
		};
		webView.addJavascriptInterface(mainWebInterfaceListener, "Android");
		
		SingletonModel.getInstance().setMassProductListener(new CommonListener() {
			
			@Override
			public void productUpdated() {
				Log.d("setMassProductListener", "productUpdated");
				updateListView();
			}
			
			@Override
			public void productListChanged() {
				Log.d("setMassProductListener", "productListChanged");
				updateListView();
			}
			
			@Override
			public void productAppend() {
				Log.d("setMassProductListener", "productAppend");
				updateListView();
			}
		});
		
		webView.loadUrl("file:///android_asset/pages/mass_quantity.html");
		
		return view;
	}
	
	@Override
	public void onAttach(Activity activity) {
		context = activity;
		super.onAttach(activity);
	}
	
	public void updateListView() {
		((Activity)context).runOnUiThread(new Runnable() {
			public void run() {
				Log.d("MassQuantityFragment", "updateListView");
				List<ProductBarcode> itensData = SingletonModel.getInstance().getMassBagItem(type, weight, unit);
				int size = (itensData!=null)?itensData.size():0;
				if (size <= 0) {
					webView.loadUrl("javascript:setListProducts('<table class=\"tablecenter\"><tr><td><span class=\"gray\">Nenhum produto na lista</span>"
							+ "</td></tr></table>')");
					return;
				}
				StringBuilder sb = new StringBuilder();
				sb.append("<table border=\"0\" width=\"100%\">");
				sb.append("<tr><td><strong>Nome</strong></td><td><strong>Qt.</strong></td></tr>");

				for (int i = 0; i < size; i++) {
					sb.append("<tr><td colspan=\"3\"><hr /></td></tr>");
					ProductBarcode itemData = itensData.get(i);
					sb.append("<tr class=\"clickableRow\" data-url=\"" + (i + 1) + "\"><td>" + itemData.getEan() + "</td><td>"
							+ itemData.getQuantity() + "</td></tr>");
					sb.append("<tr><td colspan=\"3\"><hr /></td></tr>");
				}
				sb.append("</table>");
				//Log.d("updateListView-data", "javascript:setListProducts('" + sb.toString() + "')");
				webView.loadUrl("javascript:setListProducts('" + sb.toString() + "')");
			}
		});
	}

	@Override
	public boolean onBackPressed() {
		return false;
	}

	@Override
	public void onSearchQuery(String query) {		
	}

	@Override
	public void onSearchClose() {		
	}

	@Override
	public void updateWebview() {
		updateListView();
	}
}
