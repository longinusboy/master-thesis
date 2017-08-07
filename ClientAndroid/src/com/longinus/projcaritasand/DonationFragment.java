package com.longinus.projcaritasand;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.NumberPicker;

import com.google.zxing.integration.android.IntentIntegrator;
import com.longinus.projcaritasand.model.SingletonModel;
import com.longinus.projcaritasand.model.database.Type;
import com.longinus.projcaritasand.model.listeners.CommonListener;
import com.longinus.projcaritasand.model.listeners.MainWebInterfaceListener;
import com.longinus.projcaritasand.model.products.IProduct;
import com.longinus.projcaritasand.model.products.ProductBarcode;
import com.longinus.projcaritasand.model.products.ProductManual;

@SuppressLint("SetJavaScriptEnabled")
public class DonationFragment extends Fragment implements IFragmentListener {
	private Context context;
	private WebView webView;
	
	private NumberPicker numberPicker;
	
	private int itemClickedIndex = -1;

	public DonationFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_webview, container, false);
		
		MainWebInterfaceListener mainWebInterfaceListener = new MainWebInterfaceListener() {
			@Override
			@JavascriptInterface
			public float getListHeight() {
				return /*getScreenHeightCm();//*/-0.5f;
			}
			
			@Override
			@JavascriptInterface
			public void onScanClick() {
				IntentIntegrator integrator = new IntentIntegrator(getActivity());
				integrator.addExtra("SCAN_FORMATS", "EAN_13,EAN_8");
				integrator.initiateScan();
			}

			@Override
			@JavascriptInterface
			public void onManualClick() {
				Intent intent = new Intent(getActivity(), ManualMainActivity.class);
				startActivity(intent);
			}

			@Override
			@JavascriptInterface
			public void finalizeBag() {
				((Activity)context).runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						AlertDialog.Builder builder = new AlertDialog.Builder(context);
						builder.setTitle("Lista pronta a submeter");
						builder.setMessage("A lista de doações será submetida e não será mais possivel a sua edição, deseja submeter?");
						builder.setPositiveButton("Submeter", new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								SingletonModel.getInstance().finilizeBag();
								dialog.dismiss();
							}
						});
						builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
							
						});
						builder.show();
					}
				});		
			}

			@Override
			@JavascriptInterface
			public void onRowClick(String id) {
				itemClickedIndex = Integer.parseInt(id) - 1;
				final Dialog dialog = new Dialog(getActivity());
				dialog.setContentView(R.layout.dialog_change_list_donations);
				dialog.setTitle("Alterar quantidade");				
				dialog.show();
				
				numberPicker = (NumberPicker) dialog.findViewById(R.id.numberPickerChange);
				numberPicker.setValue(SingletonModel.getInstance().getBagItemQuantity(itemClickedIndex));
				
				Button btnType = (Button) dialog.findViewById(R.id.btn_type);
				Button btnChange = (Button) dialog.findViewById(R.id.btn_change);
				Button btnDelete = (Button) dialog.findViewById(R.id.btn_delete);
				
				btnType.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(), DonationTypeSelectorActivity.class);
						intent.putExtra("itemIndex", itemClickedIndex);
						startActivity(intent);
						dialog.dismiss();
					}
				});

				btnChange.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						SingletonModel.getInstance().setBagItemQuantity(itemClickedIndex, numberPicker.getValue());
						dialog.dismiss();
					}
				});
				
				btnDelete.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						SingletonModel.getInstance().removeBagItem(itemClickedIndex);
						dialog.dismiss();
					}
				});
			}

			@Override
			@JavascriptInterface
			public void loadingFinished() {
				
			}			
		};
		
		webView = (WebView) view.findViewById(R.id.webView);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				updateListView();
			}
		});
		webView.addJavascriptInterface(mainWebInterfaceListener, "Android");
		
		CommonListener commonListener = new CommonListener() {

			@Override
			public void productUpdated() {
				updateListView();
			}

			@Override
			public void productAppend() {
				updateListView();
			}

			@Override
			public void productListChanged() {
				updateListView();
			}
		};

		webView.loadUrl("file:///android_asset/pages/main.html");
		
		SingletonModel.getInstance().setSimpleProductListener(commonListener);
		
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(1000);//Dirty hack, but it's just a backup
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				updateListView();
			}
		});		
	}

	@Override
	public void onAttach(Activity activity) {
		context = activity;
		super.onAttach(activity);
	}

	public void updateListView() {
		try {
			DonationFragment.this.getActivity().runOnUiThread(new Runnable() {
				public void run() {
					int size = SingletonModel.getInstance().getBagSize();
					if (size <= 0) {
						webView.loadUrl("javascript:setListProducts('<table class=\"tablecenter\"><tr><td><span class=\"gray\">Nenhum produto na lista</span>"
								+ "</td></tr></table>')");
						return;
					}
					List<Type> types = SingletonModel.getInstance().getTypes();
					StringBuilder sb = new StringBuilder();
					sb.append("<table border=\"0\" width=\"100%\">");
					sb.append("<tr><td><strong>Nome</strong></td><td><strong>Un.</strong></td><td><strong>Qt.</strong></td></tr>");
	
					for (int i = 0; i < size; i++) {
						sb.append("<tr><td colspan=\"3\"><hr /></td></tr>");
						IProduct itemData = SingletonModel.getInstance().getBagItem(i);
						if (itemData.getType() != null) {
							if (itemData instanceof ProductBarcode) {
								sb.append("<tr class=\"clickableRow\" data-url=\"" + (i + 1) + "\"><td>" + itemData.getName() + "</td><td>" + itemData.getWeight()
										+ itemData.getUnit() + "</td><td>" + itemData.getQuantity() + "</td></tr>");
							}
							if (itemData instanceof ProductManual) {
								sb.append("<tr class=\"clickableRow\" data-url=\"" + (i + 1) + "\"><td>");
								for (Type type : types) {
									if (type.getType().contentEquals(itemData.getType())) {
										sb.append("("+type.getName()+"-"+type.getSubname()+")");
										break;
									}
								}
								sb.append("</td><td>" + itemData.getWeight() + itemData.getUnit() + "</td><td>" + itemData.getQuantity() + "</td></tr>");
							}
						}else {
							if (itemData instanceof ProductBarcode) {
								sb.append("<tr class=\"clickableRow\" data-url=\"" + (i + 1) + "\"><td>" + ((ProductBarcode)itemData).getEan() + "</td><td>-</td><td>"
										+ itemData.getQuantity() + "</td></tr>");
							}						
						}
						
						sb.append("<tr><td colspan=\"3\"><hr /></td></tr>");
					}
					sb.append("</table>");
					webView.loadUrl("javascript:setListProducts('" + sb.toString() + "')");
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private float getScreenHeightCm() {
		DisplayMetrics metrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		
		int heightPixels = metrics.heightPixels;
		
		float heightDpi = metrics.ydpi;
		
		float heightInches = heightPixels / heightDpi;
		
		return heightInches * 2.54f;
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
