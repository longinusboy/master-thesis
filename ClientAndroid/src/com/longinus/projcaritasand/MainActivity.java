package com.longinus.projcaritasand;

import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.longinus.projcaritasand.model.Constants;
import com.longinus.projcaritasand.model.SingletonModel;
import com.longinus.projcaritasand.model.database.GeoLocation;
import com.longinus.projcaritasand.model.database.MobileLocation;

public class MainActivity extends Activity implements IMessages {
	private ProgressDialog progressDialog = null;
	private AlertDialog alertDialog = null;
	private int selectedCampaign = -1;

	private LocationManager locationManager;
	private LocationListener slowLocationListener;
	private LocationListener fastLocationListener;
	private Location lastLocation = null;
	  
	private boolean showMenu = false;
	private boolean showSearchAction = false;
	private boolean showRefreshAction = false;
	private Menu mMenu;
	private boolean itemSelected = false;
	
	private boolean doubleBackToExitPressedOnce = false;
	private boolean updateOnWifiOnly = false;
	
	private String[] sideMenuEntries;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    
    private ActionBarDrawerToggle drawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    
    private String onMassType;
    private Integer onMassWeight;
    private Integer onMassMinWeight;
    private Integer onMassMaxWeight;
    private String onMassUnit;
    private Integer onMassRealWeight;
    private String onMassDisplayedUnit;
    private Integer onMassQuantity;
	protected DialogInterface dialog;
	private Handler messageHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		sideMenuEntries = getResources().getStringArray(R.array.sidemenu_array);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        drawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, sideMenuEntries));
        // Set the list's click listener
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        mTitle = mDrawerTitle = getTitle();
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        
        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		messageHandler = new Handler(new Handler.Callback() {
			
			@Override
			public boolean handleMessage(Message msg) {
				Toast.makeText(MainActivity.this, msg.getData().getString("msg", ""), Toast.LENGTH_SHORT).show();
				return true;
			}
		});
		
		if (SingletonModel.getInstance().isBlocked()) {
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.beginTransaction().replace(R.id.content_frame, new BlockedFragment()).commit();
			setActionBarIcons(false, false, false);
		}

		if (SingletonModel.getInstance().isAlreadyInitialized() && SingletonModel.getInstance().getCampaignInUse()!=null) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
	        getActionBar().setHomeButtonEnabled(true);
	        
	        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
	        
			showMenu = true;
			invalidateOptionsMenu();
			
			selectItem(1);
			return;
		}
		
		if (SingletonModel.getInstance().isAlreadyInitialized() && SingletonModel.getInstance().getCampaignInUse()==null) {
			selectItem(0);
			return;
		}
		
		getFragmentManager().beginTransaction().replace(R.id.content_frame, new LoadingFragment()).commit();		
		SingletonModel.getInstance().setupParameters(getBaseContext(), messageHandler);
		SingletonModel.getInstance().initialize(updateOnWifiOnly);
	}

	@Override
	protected void onDestroy() {		
		super.onDestroy();
	}

	@Override
	protected void onPause() {		
		super.onPause();
		tearOffLocationListeners();
	}

	@Override
	protected void onResume() {		
		super.onResume();
		setupLocationListeners();
		
		int titleId = getResources().getIdentifier("action_bar_title", "id", "android");

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
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		try {			
			IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
			if (scanResult != null) {
				String content = scanResult.getContents();
				if (content != null) {
					if (Constants.DEBUG_MODE)
						Log.d("code", content);
					Fragment fragment = getFragmentManager().findFragmentById(R.id.content_frame);
					if (fragment instanceof DonationFragment) {
						SingletonModel.getInstance().addSimpleDonationProduct(content);
					}
					if (fragment instanceof MassQuantityFragment) {
						SingletonModel.getInstance().addMassProduct(content, onMassType, onMassWeight, onMassMinWeight, onMassMaxWeight, onMassUnit, onMassRealWeight, onMassQuantity);
					}
					IntentIntegrator integrator = new IntentIntegrator((Activity)MainActivity.this);
					integrator.addExtra("SCAN_FORMATS", "EAN_13,EAN_8");
					integrator.initiateScan();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		
		MenuItem refreshItem = menu.findItem(R.id.item_refresh);
		MenuItem searchItem = menu.findItem(R.id.item_search);
		
	    SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
	    searchItem.setOnActionExpandListener(new OnActionExpandListener() {
			
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				return true;
			}
			
			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				if (itemSelected) {
					itemSelected = false;
					return true;
				}
				Fragment fragment = getFragmentManager().findFragmentById(R.id.content_frame);
				if (fragment instanceof IFragmentListener) {
					((IFragmentListener)fragment).onSearchClose();
				}
				return true;
			}
		});
	    searchView.setOnCloseListener(new OnCloseListener() {
			
			@Override
			public boolean onClose() {
				sendSearchClose();
				return true;
			}
		});
	    searchView.setOnQueryTextListener(new OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String query) {
				Log.d("main-query", query);
				sendSearchQuery(query);
				return true;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
				Log.d("main-newText", newText);
				//sendSearchQuery(newText);
				return true;
			}
		});
		
		refreshItem.setVisible(showRefreshAction);
		searchItem.setVisible(showSearchAction);
		
		mMenu = menu;
		return super.onCreateOptionsMenu(menu);
	}

	protected void sendSearchQuery(String query) {
		Fragment fragment = getFragmentManager().findFragmentById(R.id.content_frame);
		if (fragment instanceof IFragmentListener) {
			((IFragmentListener)fragment).onSearchQuery(query);
		}
	}

	protected void sendSearchClose() {
		Fragment fragment = getFragmentManager().findFragmentById(R.id.content_frame);
		if (fragment instanceof IFragmentListener) {
			((IFragmentListener)fragment).onSearchClose();
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
		return showMenu && !drawerOpen;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
		
		switch (item.getItemId()) {
			case R.id.item_refresh:
				Fragment fragment = getFragmentManager().findFragmentById(R.id.content_frame);
				if (fragment instanceof IFragmentListener) {
					((IFragmentListener)fragment).updateWebview();
					return true;
				}

			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
	    @Override
	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	    	setNavDrawerItemNormal();
	    	setSelectionColor(position);
	        TextView txtview = ((TextView) view.findViewById(R.id.tv_drawer));
	        txtview.setTypeface(Typeface.DEFAULT_BOLD);
	        selectItem(position);
	    }
	}
	
	public void setSelectionColor(int position) {
		for (int i = 0; i < drawerList.getChildCount(); i++) {
			View v = drawerList.getChildAt(i);
			v.setBackgroundColor(0xFFFF0000);
		}
		View v = drawerList.getChildAt(position);
		v.setBackgroundColor(0xFFCC0000);
	}
	
	public void setNavDrawerItemNormal() {
		for (int i = 0; i < drawerList.getChildCount(); i++) {
			View v = drawerList.getChildAt(i);
			TextView txtview = ((TextView) v.findViewById(R.id.tv_drawer));
			txtview.setTypeface(Typeface.DEFAULT);
			v.setBackgroundColor(getResources().getColor(android.R.color.transparent));
		}
	}

	/** Swaps fragments in the main content view */
	private void selectItem(int position) {
		try {
			if (lastLocation == null) {
				setupLocationListeners();
			}else if ((new Date()).getTime()-lastLocation.getTime()>1000*60*5) {
				setupLocationListeners();
			}

			FragmentManager fragmentManager = getFragmentManager();
			
		    switch (position) {
				case 0:
					fragmentManager.beginTransaction().replace(R.id.content_frame, new CampaignsFragment()).commit();
					setActionBarIcons(false, false, false);
					break;
				case 1:
					fragmentManager.beginTransaction().replace(R.id.content_frame, new DonationFragment()).commit();
					setActionBarIcons(true, false, true);
					break;
				case 2:
					fragmentManager.beginTransaction().replace(R.id.content_frame, new MassMainFragment()).commit();
					setActionBarIcons(true, true, false);
					break;
				case 3:
					fragmentManager.beginTransaction().replace(R.id.content_frame, new SimpleListFragment()).commit();
					setActionBarIcons(false, false, false);
					break;
				case 4:
					fragmentManager.beginTransaction().replace(R.id.content_frame, new InformationsFragment()).commit();
					setActionBarIcons(false, false, false);
					break;
				case 5:
					fragmentManager.beginTransaction().replace(R.id.content_frame, new StatisticsFragment()).commit();
					setActionBarIcons(false, false, false);
					break;
				case 6:
					fragmentManager.beginTransaction().replace(R.id.content_frame, new SelfStatisticsFragment()).commit();
					setActionBarIcons(false, false, false);
					break;
				case 7:
					fragmentManager.beginTransaction().replace(R.id.content_frame, new AboutFragment()).commit();
					setActionBarIcons(false, false, false);
					break;
				case 8:
					fragmentManager.beginTransaction().replace(R.id.content_frame, new HelpFragment()).commit();
					setActionBarIcons(false, false, false);
					break;
				case 9:
				default:
					SingletonModel.getInstance().disposeData();
					finish();
					break;
			}
		    // Highlight the selected item, update the title, and close the drawer
		    drawerList.setItemChecked(position, true);
		    setTitle(sideMenuEntries[position]);
		    setSelectionColor(position);
		    drawerLayout.closeDrawer(drawerList);
		} catch (Exception e) {
			e.printStackTrace();//Prevent crash if immediately minimized
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
	    getActionBar().setTitle(mTitle);
	}
	
	private void setActionBarIcons(boolean enableMenu, boolean enableSearch, boolean enableRefresh) {
		showMenu = enableMenu;
		showSearchAction = enableSearch;
		showRefreshAction = enableRefresh;
		invalidateOptionsMenu();
	}

	public void setLevelTitle(String type, String measuredUnit) {
		String title = SingletonModel.getInstance().getLevelTitle(type, measuredUnit);
		
		try {
			if (title != null) {
				title = Html.fromHtml(title).toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		final String decodedTitle = title;
		
		Fragment fragment = getFragmentManager().findFragmentById(R.id.content_frame);
		if (fragment instanceof MassMainFragment) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					setTitle(sideMenuEntries[2]+((decodedTitle!=null)?(" > " + decodedTitle):""));
				}
			});
		}
		if (fragment instanceof MassQuantityFragment) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					setTitle(sideMenuEntries[2]+((decodedTitle!=null)?(" > " + decodedTitle):""));
				}
			});
		}
		if (fragment instanceof SimpleListFragment) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					setTitle(sideMenuEntries[3]+((decodedTitle!=null)?(" > " + decodedTitle):""));
				}
			});
		}
	}

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();       
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig);
    }        

	@Override
	public void onBackPressed() {
		if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
			drawerLayout.closeDrawer(Gravity.LEFT);
			return;
		}
		Fragment fragment = getFragmentManager().findFragmentById(R.id.content_frame);
		if (fragment instanceof MassQuantityFragment) {
			getFragmentManager().beginTransaction().replace(R.id.content_frame, new MassMainFragment()).commit();
			setActionBarIcons(true, true, false);
			setLevelTitle(null, null);
			MenuItem searchItem = mMenu.findItem(R.id.item_search);
			if (searchItem != null) {
				try {
					SearchView searchView = (SearchView) searchItem.getActionView();
					searchView.setQuery("", false);
					searchView.clearFocus();
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}
			return;
		}
		if (fragment instanceof IFragmentListener) {
			if (((IFragmentListener)fragment).onBackPressed()) {
				return;
			}
		}
		
		if (doubleBackToExitPressedOnce) {
			if (SingletonModel.getInstance().isAlreadyInitialized() && SingletonModel.getInstance().getCampaignInUse()==null) {
				SingletonModel.getInstance().disposeData();
			}
	        super.onBackPressed();
	        return;
	    }

	    this.doubleBackToExitPressedOnce = true;
	    Toast.makeText(this, "Clique novamente em Retrocedor para sair", Toast.LENGTH_SHORT).show();

	    new Handler().postDelayed(new Runnable() {

	        @Override
	        public void run() {
	            doubleBackToExitPressedOnce=false;                       
	        }
	    }, 2000);
	}

	@Override
	public void onMessage(String message, final Object extra) {
		if(Constants.DEBUG_MODE)
			Log.d("message", message);
		if (message.contentEquals("LOADING_FINISHED")) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					runOnUiThread(new Runnable() {
						public void run() {
							selectItem(0);
						}
					});					
				}
			});			
			return;
		}
		if (message.contentEquals("BLOCKED")) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					runOnUiThread(new Runnable() {
						public void run() {
							FragmentManager fragmentManager = getFragmentManager();
							fragmentManager.beginTransaction().replace(R.id.content_frame, new BlockedFragment()).commit();
							setActionBarIcons(false, false, false);
						}
					});					
				}
			});			
			return;
		}
		if (message.contentEquals("CAMPAIGN_CLICKED")) {
			if (extra instanceof Integer) {
				selectedCampaign = ((Integer)extra).intValue();
				int result = Constants.REGISTER_CAMPAIGN_ERROR;
				if (SingletonModel.getInstance().isCampaignSubscribed(selectedCampaign)) {
					result = SingletonModel.getInstance().setCamapignInUse((Integer)extra, null);
					if (result == Constants.REGISTER_CAMPAIGN_OK) {
						getActionBar().setDisplayHomeAsUpEnabled(true);
				        getActionBar().setHomeButtonEnabled(true);
				        
				        runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
							}
						});
				        
				        setNavDrawerItemNormal();
				        
				        View v = drawerList.getChildAt(1);
				        TextView txtview = ((TextView) v.findViewById(R.id.tv_drawer));
				        txtview.setTypeface(Typeface.DEFAULT_BOLD);
				        setSelectionColor(1);
				        
				        selectItem(1);
						showMenu = true;
						invalidateOptionsMenu();
					}else {
						AlertDialog.Builder builder = new AlertDialog.Builder(this);
						builder.setMessage("Erro a definir a campanha")
						       .setCancelable(false)
						       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
						           public void onClick(DialogInterface dialog, int id) {
						                alertDialog.dismiss();
						           }
						       });
						alertDialog = builder.create();
						alertDialog.show();
						return;
					}
				}else {
					if (SingletonModel.getInstance().isCampaignSubscribable(selectedCampaign)) {
						progressDialog = new ProgressDialog(this);
						progressDialog.setMessage("A verificar ligação ao servidor");
						progressDialog.setCancelable(false);
						progressDialog.setIndeterminate(true);
						progressDialog.show();
						
						new RetrieveFastPing().execute(new Void[]{});
					}
				}				
			}	
			return;		
		}
		if (message.contentEquals("MASS_GROUP_CHOOSED")) {
			if (extra instanceof JSONObject) {
				JSONObject jsonData = (JSONObject) extra;
				try {
					onMassType = jsonData.getString("type");
					onMassWeight = jsonData.getInt("weight");
					onMassMinWeight = jsonData.getInt("minWeight");
					onMassMaxWeight = jsonData.getInt("maxWeight");
					onMassUnit = jsonData.getString("unit");
					onMassRealWeight = jsonData.getInt("realWeight");
					onMassDisplayedUnit = jsonData.getString("displayedUnit");
					
					if (onMassType != null && onMassWeight != null && onMassUnit != null && onMassRealWeight != null) {
						runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								Fragment fragment = new MassQuantityFragment();
								Bundle bundle = new Bundle();
								bundle.putString("type", onMassType);
								bundle.putInt("weight", onMassWeight);
								bundle.putInt("minWeight", onMassMinWeight);
								bundle.putInt("maxWeight", onMassMaxWeight);
								bundle.putString("unit", onMassUnit);
								bundle.putInt("realWeight", onMassRealWeight);
								bundle.putString("displayedUnit", onMassDisplayedUnit);
								fragment.setArguments(bundle);
								getFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
								setActionBarIcons(true, false, true);
							}
						});	
						
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}		
			return;
		}
		if (message.contentEquals("MASS_CAMERA")) {
			if (extra instanceof JSONObject) {
				JSONObject jsonData = (JSONObject) extra;
				try {
					onMassType = jsonData.getString("type");
					onMassWeight = jsonData.getInt("weight");
					onMassMinWeight = jsonData.getInt("minWeight");
					onMassMaxWeight = jsonData.getInt("maxWeight");
					onMassUnit = jsonData.getString("unit");
					onMassRealWeight = jsonData.getInt("realWeight");
					onMassQuantity = jsonData.getInt("quantity");
					
					IntentIntegrator integrator = new IntentIntegrator((Activity)MainActivity.this);
					integrator.addExtra("SCAN_FORMATS", "EAN_13,EAN_8");
					integrator.initiateScan();
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}		
			return;
		}
		if (message.contentEquals("MASS_GROUP_FINISHED")) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setTitle("Lista pronta a submeter");
					builder.setMessage("A lista de doações será submetida e não será mais possivel a sua edição, deseja submeter?");
					builder.setPositiveButton("Submeter", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							SingletonModel.getInstance().finilizeMassBag();
							selectItem(2);
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
			return;
		}
		if (message.contentEquals("FORCE_SEND")) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					Toast.makeText(MainActivity.this, "A forçar envio de dados", Toast.LENGTH_SHORT).show();;
					SingletonModel.getInstance().forceSend();					
				}
			});			
			return;
		}
		if (message.contentEquals("CLOSE_SEARCH")) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					if (mMenu == null) {
						return;
					}
					itemSelected = true;
					try {
						MenuItem searchItem = mMenu.findItem(R.id.item_search);
						if (searchItem != null) {
							if (searchItem.isActionViewExpanded()) {
								//SearchView searchView = (SearchView) searchItem.getActionView();
								searchItem.collapseActionView();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			return;
		}
		if (Constants.DEBUG_MODE)
			Log.e("Unrecognized message", message);
	}
	
	public void setMobileLocation() {
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		GsmCellLocation cellLocation = (GsmCellLocation) telephonyManager.getCellLocation();

		try {
			String networkOperator = telephonyManager.getNetworkOperator();
			int mcc;
			try {
				mcc = Integer.parseInt(networkOperator.substring(0, 3));
			} catch (Exception e) {
				mcc = 0;
			}
			int mnc;
			try {
				mnc = Integer.parseInt(networkOperator.substring(3));
			} catch (Exception e) {
				mnc = 0;
			}
			int cid = cellLocation.getCid();
			int lac = cellLocation.getLac();
			
			SingletonModel.getInstance().setLocation(new MobileLocation(new Date().getTime(), mcc, mnc, cid, lac));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setupLocationListeners() {
		
		setMobileLocation();
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setSpeedRequired(false);
		criteria.setCostAllowed(false);

		Location bestResult = null;
		float bestAccuracy = Float.MAX_VALUE;
		long bestTime = Long.MIN_VALUE;

		List<String> matchingProviders = locationManager.getAllProviders();
		for (String provider : matchingProviders) {
			Location location = locationManager.getLastKnownLocation(provider);
			if (location != null) {
				float accuracy = location.getAccuracy();
				long time = location.getTime();

				if ((time > bestTime && accuracy < bestAccuracy)) {
					bestResult = location;
					bestAccuracy = accuracy;
					bestTime = time;
				}
			}
		}
		if (bestResult != null) {
			lastLocation = bestResult;
			GeoLocation geoLocation = new GeoLocation(bestResult.getTime(), bestResult.getLongitude(), bestResult.getLatitude(), "last");
			Log.d("fastLocationListener", geoLocation.toString());
			SingletonModel.getInstance().setLocation(geoLocation);
		}

		slowLocationListener = new LocationListener() {

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}

			@Override
			public void onProviderEnabled(String provider) {
			}

			@Override
			public void onProviderDisabled(String provider) {
			}

			@Override
			public void onLocationChanged(Location location) {
				if (lastLocation != null) {
					if (lastLocation.getTime() < location.getTime() - Constants.MAX_TIME_LOCATION
							|| lastLocation.getAccuracy() == 0.0
							|| (lastLocation.getAccuracy() > location.getAccuracy() && location.getAccuracy() > 0.0)) {
						lastLocation = location;
						GeoLocation geoLocation = new GeoLocation(location.getTime(), location.getLongitude(), location.getLatitude(), "slow");
						Log.d("fastLocationListener", geoLocation.toString());
						SingletonModel.getInstance().setLocation(geoLocation);
					}
				}else {
					lastLocation = location;
					GeoLocation geoLocation = new GeoLocation(location.getTime(), location.getLongitude(), location.getLatitude(), "slow");
					Log.d("fastLocationListener", geoLocation.toString());
					SingletonModel.getInstance().setLocation(geoLocation);
				}
			}
		};

		fastLocationListener = new LocationListener() {

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}

			@Override
			public void onProviderEnabled(String provider) {
			}

			@Override
			public void onProviderDisabled(String provider) {
			}

			@Override
			public void onLocationChanged(Location location) {
				if (lastLocation != null) {
					if (lastLocation.getTime() < location.getTime() - Constants.MAX_TIME_LOCATION
							|| lastLocation.getAccuracy() == 0.0
							|| (lastLocation.getAccuracy() > location.getAccuracy() && location.getAccuracy() > 0.0)) {
						lastLocation = location;
						GeoLocation geoLocation = new GeoLocation(location.getTime(), location.getLongitude(), location.getLatitude(), "fast");
						Log.d("fastLocationListener", geoLocation.toString());
						SingletonModel.getInstance().setLocation(geoLocation);
					}
				}else {
					lastLocation = location;
					GeoLocation geoLocation = new GeoLocation(location.getTime(), location.getLongitude(), location.getLatitude(), "fast");
					Log.d("fastLocationListener", geoLocation.toString());
					SingletonModel.getInstance().setLocation(geoLocation);
				}
			}
		};

		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		String fastProvider = locationManager.getBestProvider(criteria, true);
		if (fastProvider != null) {
			locationManager.requestSingleUpdate(fastProvider, fastLocationListener, getMainLooper());
		}
		
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		String slowProvider = locationManager.getBestProvider(criteria, true);
		if (slowProvider != null) {
			locationManager.requestSingleUpdate(slowProvider, slowLocationListener, getMainLooper());
		}
	}
	
	private void tearOffLocationListeners() {
		if (locationManager != null) {
			if (slowLocationListener != null) {
				locationManager.removeUpdates(slowLocationListener);
			}
			if (fastLocationListener != null) {
				locationManager.removeUpdates(fastLocationListener);
			}
		}
	}
	
	private void serverPingable(Boolean serverAvailable) {
		progressDialog.dismiss();
		
		if (!serverAvailable) {
			runOnUiThread(new Runnable() {
				public void run() {
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setTitle("Servidor incontactavel").setMessage("Verifique se tem ligação à Internet antes de continuar")
					       .setCancelable(false)
					       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					                alertDialog.dismiss();
					           }
					       });
					alertDialog = builder.create();
					alertDialog.show();					
				}
			});
			return;
		}
		
		runOnUiThread(new Runnable() {
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle("Password da campanha");

				// Set up the input
				final EditText input = new EditText(MainActivity.this);
				// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
				input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
				builder.setView(input);

				// Set up the buttons
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				    	String password = input.getText().toString();
				    	MainActivity.this.dialog = dialog;
				        
				        progressDialog = new ProgressDialog(MainActivity.this);
						progressDialog.setMessage("A autenticar dispositivo");
						progressDialog.setCancelable(false);
						progressDialog.setIndeterminate(true);
						progressDialog.show();
						
				        new CampaignSetter().execute(new String[]{password});
				        
				    }
				});
				builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        dialog.cancel();
				    }
				});

				builder.show();
			}
		});
		
	}
	
	private void campaignSetterResult(Integer result) {
		if (result == null || result == Constants.REGISTER_CAMPAIGN_ERROR) {
			progressDialog.dismiss();
        	dialog.dismiss();
        	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setMessage("Ocorreu um erro a comunicar como servidor")
			       .setCancelable(false)
			       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                alertDialog.dismiss();
			           }
			       });
			alertDialog = builder.create();
			alertDialog.show();
			return;
		}
        if (result == Constants.REGISTER_CAMPAIGN_WRONG_PASSWORD) {
        	progressDialog.dismiss();
        	dialog.dismiss();
        	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setTitle("Password invalida")
					.setMessage("Verifique se introduziu a password corretamente e se tem ligação à Internet")
			       .setCancelable(false)
			       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                alertDialog.dismiss();
			           }
			       });
			alertDialog = builder.create();
			alertDialog.show();
			return;
		}
        //else OK
        try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		progressDialog.dismiss();
		dialog.dismiss();
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        
        runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
			}
		});			        
        
        selectItem(1);
		showMenu = true;
		invalidateOptionsMenu();
	}
	
	
	class RetrieveFastPing extends AsyncTask<Void, Void, Boolean> {

	    private Exception exception;

	    protected Boolean doInBackground(Void ...voids) {
	        try {
	        	return SingletonModel.getInstance().isServerAvailable();
	        } catch (Exception e) {
	            this.exception = e;
	            Log.d("RetrieveFastPing-ex", Log.getStackTraceString(exception));
	            return Boolean.valueOf(false);
	        }
	    }

	    protected void onPostExecute(Boolean result) {
	        serverPingable(result);
	    }
	}
	
	class CampaignSetter extends AsyncTask<String, Void, Integer> {

	    private Exception exception;

	    protected Integer doInBackground(String ...passwords) {
	        try {
	        	return SingletonModel.getInstance().setCamapignInUse(selectedCampaign, passwords[0]);
	        } catch (Exception e) {
	            this.exception = e;
	            Log.d("CampaignSetter-ex", Log.getStackTraceString(exception));
	            return null;
	        }
	    }

	    protected void onPostExecute(Integer result) {
	        campaignSetterResult(result);
	    }
	}

}
