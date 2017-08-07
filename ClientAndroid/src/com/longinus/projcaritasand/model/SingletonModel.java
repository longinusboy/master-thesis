package com.longinus.projcaritasand.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.longinus.projcaritasand.model.database.Campaign;
import com.longinus.projcaritasand.model.database.DatabaseHelper;
import com.longinus.projcaritasand.model.database.GeoLocation;
import com.longinus.projcaritasand.model.database.ILocation;
import com.longinus.projcaritasand.model.database.MobileLocation;
import com.longinus.projcaritasand.model.database.PersonalStat;
import com.longinus.projcaritasand.model.database.Type;
import com.longinus.projcaritasand.model.listeners.CommonListener;
import com.longinus.projcaritasand.model.listeners.DeviceListener;
import com.longinus.projcaritasand.model.network.ServerComm;
import com.longinus.projcaritasand.model.network.ServerResponse;
import com.longinus.projcaritasand.model.products.IProduct;
import com.longinus.projcaritasand.model.products.MassProductContainer;
import com.longinus.projcaritasand.model.products.ProductBarcode;
import com.longinus.projcaritasand.model.products.ProductManual;
import com.longinus.projcaritasand.model.products.ProductsBag;

public class SingletonModel {
	static SingletonModel instance = null;
	private ServerDataManager serverDataManager;
	private List<Campaign> campaigns;
	private DatabaseHelper databaseHelper;

	private DeviceListener deviceListener;
	private CommonListener simpleProductListener;
	private CommonListener massProductListener;

	private boolean alreadyInitialized;

	private Campaign campaignInUse;
	
	private boolean useLowDpi;

	private Context context;
	private Handler messageHandler;
	
	private MobileLocation mobileLocation;
	private GeoLocation geoLocation;

	protected SingletonModel() {
		alreadyInitialized = false;
	}

	public static SingletonModel getInstance() {
		if (instance == null) {
			instance = new SingletonModel();
		}
		return instance;
	}

	protected void alertDeviceListeners(int newStatus) {
		try {
			if (deviceListener != null) {
				deviceListener.deviceChanged(newStatus);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected void alertSimpleProductAppend() {
		try {
			if (simpleProductListener != null) {
				simpleProductListener.productAppend();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void alertSimpleProductUpdated() {
		try {
			if (simpleProductListener != null) {
				simpleProductListener.productUpdated();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void alertSimpleProductListChanged() {
		try {
			if (simpleProductListener != null) {
				simpleProductListener.productListChanged();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void alertMassProductAppend() {
		try {
			if (massProductListener != null) {
				massProductListener.productAppend();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void alertMassProductUpdated() {
		try {
			if (massProductListener != null) {
				massProductListener.productUpdated();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void alertMassProductListChanged() {
		try {
			if (massProductListener != null) {
				massProductListener.productListChanged();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Set Android Context to initialize model
	 * 
	 * @param context
	 */
	public void setupParameters(Context context, Handler messageHandler) {
		this.context = context;
		this.messageHandler = messageHandler;
		
		campaigns = new ArrayList<Campaign>();

		deviceListener = null;
		simpleProductListener = null;
		massProductListener = null;
		
		useLowDpi = false;

		campaignInUse = null;		

		int screenSize = context.getResources().getConfiguration().screenLayout &
		        Configuration.SCREENLAYOUT_SIZE_MASK;

		switch(screenSize) {
		    /*case Configuration.SCREENLAYOUT_SIZE_NORMAL:
		        break;*/
		    case Configuration.SCREENLAYOUT_SIZE_SMALL:
		        useLowDpi = true;
		        if (Constants.DEBUG_MODE)
		        	Log.d("SingletonModel", "Using lowdpi (auto)");
		        break;
		}
		
		serverDataManager = new ServerDataManager(context, messageHandler);
		serverDataManager.setDeviceListener(new DeviceListener() {
			@Override
			public void deviceChanged(int status) {
				alertDeviceListeners(status);
			}
		});
		if (databaseHelper == null) {
			databaseHelper = new DatabaseHelper(context);
		}
		//TODO completar
	}

	public void initialize(final boolean updateOnWifiOnly) {
		Runnable initializerRunnable = new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
				if (Utils.isWifiConnected(context) || (Utils.isMobileConnected(context) && !updateOnWifiOnly)) {
					if (Constants.DEBUG_MODE)
						Log.d("SingletonModel", "Starting server search");
					if (serverDataManager.findUsableServer()) {
						if (Constants.DEBUG_MODE)
							Log.d("SingletonModel", "Server defined");
						alertDeviceListeners(Constants.DEVICE_STATUS_UPDATING_LOCAL_DATABASE);
						serverDataManager.loadDataStartup();
						serverDataManager.startUploaderThread();
						campaigns = getDatabaseHelper().getCampaigns();
						alreadyInitialized = true;
						return;
					}
					if (Constants.DEBUG_MODE)
						Log.d("SingletonModel", "No server");
				}
				serverDataManager.loadWithoutServer();
				serverDataManager.startUploaderThread();
				campaigns = getDatabaseHelper().getCampaigns();
				alreadyInitialized = true;
			}
		};
		Thread initializerThread = new Thread(initializerRunnable, "Initializer");
		initializerThread.start();

	}

	public void setDeviceListener(DeviceListener deviceListener) {
		this.deviceListener = deviceListener;
	}

	public void setSimpleProductListener(CommonListener productListener) {
		this.simpleProductListener = productListener;
	}
	
	public void setMassProductListener(CommonListener productListener) {
		this.massProductListener = productListener;
	}
	
	public void forceLowDpi() {
		useLowDpi = true;
	}

	public boolean mustUseLowDpi() {
		return useLowDpi;
	}

	/**
	 * Get device status (ie. blocked, registered, ok)
	 * 
	 * @return
	 */
	public int getDeviceStatus() {
		return serverDataManager.getDeviceStatus();
	}

	//---Campaign
	public List<Campaign> getCampaigns() {
		try {
			return campaigns;
		} catch (Exception e) {
			return null;
		}
	}

	public List<Campaign> getSubscribedCampaigns() {
		try {
			List<Campaign> campaignsSubscribed = new ArrayList<Campaign>();
			for (int i = 0; i < campaigns.size(); i++) {
				if (campaigns.get(i).isSubscribed()) {
					campaignsSubscribed.add(campaigns.get(i));
				}
			}
			return campaignsSubscribed;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Campaign getCampaignInUse() {
		return campaignInUse;
	}

	public int setCamapignInUse(int index, String password) {
		
		Campaign campaign = null;
		try {
			campaign = campaigns.get(index);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (campaign == null) {
			if (Constants.DEBUG_MODE)
				Log.d("setCamapignInUse", "Empty campaign");
			return Constants.REGISTER_CAMPAIGN_ERROR;
		}
		
		if (campaign.isSubscribed()) {
			campaignInUse = campaigns.get(index);
			return Constants.REGISTER_CAMPAIGN_OK;
		}
		
		if (password == null) {
			return Constants.REGISTER_CAMPAIGN_ERROR;
		}
		
		ServerResponse response = ServerComm.registerCampaignSynchronous(serverDataManager.generateServerUrl(Constants.SERVER_URL_CAMPAIGNS), Utils.getID(context), campaign.getId(), password);
		if (response.hasException()) {
			if (Constants.DEBUG_MODE)
				Log.d("registerDeviceCampaign", response.getException().getLocalizedMessage());
			return Constants.REGISTER_CAMPAIGN_ERROR;
		}
		if (response.getCode()==Constants.REGISTER_CAMPAIGN_CODE_OK || response.getCode()==Constants.REGISTER_CAMPAIGN_CODE_REGISTERED) {
			campaignInUse = campaigns.get(index);
			serverDataManager.updateNewCampaignData();
			campaigns = getDatabaseHelper().getCampaigns();			
			return Constants.REGISTER_CAMPAIGN_OK;
		}
		if (response.getCode()==Constants.REGISTER_CAMPAIGN_CODE_PASSWORD_MISMATCH) {
			return Constants.REGISTER_CAMPAIGN_WRONG_PASSWORD;
		}
		if (Constants.DEBUG_MODE)
			Log.d("registerDeviceCampaign", "return code: "+response.getCode());
		return Constants.REGISTER_CAMPAIGN_ERROR;
	}
	
	public boolean isCampaignSubscribable(int index) {
		Date now = new Date();
		try {
			Campaign campaign  = campaigns.get(index);
			if (campaign.getStartDate().getTime()>now.getTime() || campaign.getEndDate().getTime()<now.getTime()) {
				return false;
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean isCampaignSubscribed(int index) {
		try {
			return campaigns.get(index).isSubscribed();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	//---Receiving products

	public void finilizeBag() {
		List<IProduct> products = getDatabaseHelper().getProductsOnScreen();
		if (products == null) {
			return;
		}
		if (products.size()==0) {
			return;
		}
		ProductsBag productsBag = new ProductsBag(products);
		productsBag.setMobileLocation(mobileLocation);
		productsBag.setGeoLocation(geoLocation);
		getDatabaseHelper().saveBag(campaignInUse.getId(), productsBag);
		getDatabaseHelper().removeProductsOnScreen();
		alertSimpleProductListChanged();
	}

	public void addSimpleDonationProduct(String ean) {
		getDatabaseHelper().addProductOnScreen(new ProductBarcode(ean, 1));
		alertSimpleProductAppend();
	}

	public void addSimpleDonationProduct(String type, int weight, String unit, int realWeight, int quantity) {
		getDatabaseHelper().addProductOnScreen(new ProductManual(type, weight, unit, realWeight, quantity));
		alertSimpleProductAppend();
	}
	
	public void finilizeMassBag() {
		Log.d(this.getClass().getName(), "finishing mass bag");
		List<MassProductContainer> productsOnMassContainers = getDatabaseHelper().getProductsOnMass();
		if (productsOnMassContainers == null) {
			Log.d(this.getClass().getName(), "productsOnMassContainers == null");
			return;
		}
		
		Log.d(this.getClass().getName(), "productsOnMassContainers len: "+productsOnMassContainers.size());
		for (MassProductContainer entry : productsOnMassContainers) {
			ProductsBag productsBag = new ProductsBag(entry);
			productsBag.setMobileLocation(mobileLocation);
			productsBag.setGeoLocation(geoLocation);
			getDatabaseHelper().saveBag(campaignInUse.getId(), productsBag);
			List<ProductBarcode> products = entry.getProducts();
			for (ProductBarcode product : products) {
				getDatabaseHelper().removeProductOnMass(product);
			}
		}
		alertMassProductListChanged();
	}
	
	public void addMassProduct(String ean, String type, int weight, int minWeight, int maxWeight, String unit, int realWeight, int quantity) {
		getDatabaseHelper().addProductOnMass(type, weight, minWeight, maxWeight, unit, realWeight, new ProductBarcode(ean, quantity));
		alertMassProductAppend();
	}

	//---View product list
	public IProduct getBagItem(int index) {
		try {
			return getDatabaseHelper().getProductOnScreen(index);
		} catch (Exception e) {
			return null;
		}
	}

	public int getBagSize() {
		try {
			return getDatabaseHelper().getProductsOnScreen().size();
		} catch (Exception e) {
			return 0;
		}
	}
	
	public boolean isBagItemRecognized(int index) {
		try {
			return getDatabaseHelper().isBagItemRecognized(index);
		} catch (Exception e) {
			e.printStackTrace();
			return true;
		}
	}

	public int getBagItemQuantity(int index) {
		try {
			return getDatabaseHelper().getProductOnScreen(index).getQuantity();
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}		
	}
	
	public void setBagItemData(int index, String type, Integer weight, Integer minWeight, Integer maxWeight, String unit, Integer realWeight, String displayedUnit) {
		if (index < 0) {
			return;
		}
		try {
			IProduct product = getDatabaseHelper().getProductOnScreen(index);
			product.setType(type);
			product.setWeight(weight);
			product.setUnit(unit);
			product.setRealWeight(realWeight);
			getDatabaseHelper().setCustomProduct((ProductBarcode)product);
		} catch (Exception e) {
			e.printStackTrace();
		}
		alertSimpleProductUpdated();
	}
	
	public void setBagItemQuantity(int index, int quantity) {
		if (quantity<=0 || index < 0) {
			return;
		}
		try {
			IProduct product = getDatabaseHelper().getProductOnScreen(index);
			product.setQuantity(quantity);
			getDatabaseHelper().setQuantityProductOnScreen(product);
		} catch (Exception e) {
			e.printStackTrace();
		}
		alertSimpleProductUpdated();
	}

	public void removeBagItem(int index) {
		if (index < 0) {
			return;
		}
		try {
			IProduct product = getDatabaseHelper().getProductOnScreen(index);
			getDatabaseHelper().removeProductOnScreen(product);
		} catch (Exception e) {
			e.printStackTrace();
		}
		alertSimpleProductListChanged();
	}
	
	//---View mass product list
		public List<ProductBarcode> getMassBagItem(String type, Integer weight, String unit) {
			try {
				return getDatabaseHelper().getProductsOnMass(type, weight, unit);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		public int getMassBagSize(String type, Integer weight, String unit) {
			try {
				return getDatabaseHelper().getProductsOnMassSize(type, weight, unit);
			} catch (Exception e) {
				e.printStackTrace();
				return 0;
			}
		}

		public int getMassBagItemQuantity(String type, Integer weight, String unit, int index) {
			try {
				return getDatabaseHelper().getProductOnMass(type, weight, unit, index).getQuantity();
			} catch (Exception e) {
				e.printStackTrace();
				return 1;//XXX
			}		
		}
		
		public void setMassBagItemQuantity(String type, Integer weight, String unit, int index, int quantity) {
			if (quantity<=0 || index < 0) {
				return;
			}
			try {
				ProductBarcode product = getDatabaseHelper().getProductOnMass(type, weight, unit, index);
				Log.d("setMassBagItemQuantity", product.toString());
				product.setQuantity(quantity);
				Log.d("setMassBagItemQuantity", product.toString());
				getDatabaseHelper().setQuantityProductOnMass(type, weight, unit, product);
			} catch (Exception e) {
				e.printStackTrace();
			}
			alertMassProductUpdated();
		}

		public void removeMassBagItem(String type, Integer weight, String unit, int index) {
			if (index < 0) {
				return;
			}
			try {
				ProductBarcode product = getDatabaseHelper().getProductOnMass(type, weight, unit, index);
				Log.d("removeMassBagItem", product.toString());
				getDatabaseHelper().removeProductOnMass(product);
			} catch (Exception e) {
				e.printStackTrace();
			}
			alertMassProductListChanged();
		}

	//---View stats
	public Map<String, Integer> getStatistics() {
		Map<String, Integer> statsMap = new HashMap<String, Integer>();
		List<TypeNumber> stats = databaseHelper.getPrimaryStats(getCampaignInUse());
		for (TypeNumber typeNumber : stats) {
			//Log.d("getStatistics", typeNumber.getType()+"->"+typeNumber.getValue());
			statsMap.put(typeNumber.getType(), typeNumber.getValue());
		}
		return statsMap;
	}

	public Map<String, Integer> getObjectivs() {
		Map<String, Integer> objectivesMap = new HashMap<String, Integer>();
		List<TypeNumber> objectives = databaseHelper.getPrimaryObjectives(getCampaignInUse());
		for (TypeNumber typeNumber : objectives) {
			//Log.d("getObjectivs", typeNumber.getType()+"->"+typeNumber.getValue());
			objectivesMap.put(typeNumber.getType(), typeNumber.getValue());
		}
		return objectivesMap;
	}

	public Map<String, Float> getStatsRacio() {
		Map<String, Integer> stats = getStatistics();
		Map<String, Integer> objectives = getObjectivs();
		Map<String, Float> statsRacio = new HashMap<String, Float>();
		List<Type> types = databaseHelper.getPrimaryTypes();

		Map<String, String> typeNames = new HashMap<String, String>();
		for (Type type : types) {
			typeNames.put(type.getType(), type.getName());
			//Log.d("typeNames", type.getType()+"->"+type.getName());
		}

		for (String objectiveKey : objectives.keySet()) {
			if (stats.containsKey(objectiveKey)) {
				String name = typeNames.get(objectiveKey);
				float stat = (float) stats.get(objectiveKey);
				float objective = (float) objectives.get(objectiveKey);
				statsRacio.put((name != null) ? name : objectiveKey, (((int)objective)!=0)?stat/objective:-1f);
			}
		}
		return statsRacio;
	}
	
	public int getNumberRegistriesSent() {
		try {
			return serverDataManager.getNumberRegistriesSent();
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}		
	}
	
	public int getNumberRegistriesAwaiting() {
		try {
			return databaseHelper.getNumberRegistriesAwaiting();
		} catch (Exception e) {
			return 0;
		}
	}

	public DatabaseHelper getDatabaseHelper() {
		return databaseHelper;
	}

	public boolean isAlreadyInitialized() {
		return alreadyInitialized;
	}
	
	public void disposeData() {
		serverDataManager.stopThreads();
		
		serverDataManager = null;
		campaignInUse = null;
		mobileLocation = null;
		geoLocation = null;
		alreadyInitialized = false;
	}
	
	public String getDeviceID() {
		return Utils.getID(context);
	}
	
	public void setLocation(ILocation location) {
		if (location instanceof MobileLocation) {
			this.mobileLocation = (MobileLocation) location;
		}else if (location instanceof GeoLocation) {
			this.geoLocation = (GeoLocation) location;
		}
	}

	public String getLevelTitle(String symbol, String measuredUnit) {
		if (symbol == null) {
			return null;
		}
		Type type = getDatabaseHelper().getType(symbol);
		if (type == null) {
			return null;
		}
		if (type.getName() != null ) {
			if (type.getSubname() == null) {
				return type.getName();
			}else {
				if (measuredUnit == null) {
					return type.getName() + " > " + type.getSubname();
				}else {
					return type.getName() + " > " + type.getSubname() + " > " + measuredUnit;
				}				
			}
		}
		
		return null;
	}

	public String getScreen() {
		if (campaignInUse == null) {
			return null;
		}
		
		if (campaignInUse.getScreen() == null) {
			return null;
		}
		
		if (campaignInUse.getScreen().trim().isEmpty()) {
			return null;
		}
		
		return campaignInUse.getScreen();
	}
	
	public boolean isServerAvailable() {
		if (serverDataManager == null) {
			return false;
		}
		if (!Utils.isNetworkAvailable(context)) {
			Log.d("isNetworkAvailable", "false");
			return false;
		}
		return serverDataManager.pingServerSuccessfull();
	}
	
	public long getLastServerAccess() {
		return ServerComm.getLastServerAccess();
	}
	
	public long getLastServerUpdate() {
		return serverDataManager.getLastServerUpdate();
	}

	public void forceSend() {
		serverDataManager.forceSend();
	}
	
	public void exportDB() {
		File sd = Environment.getExternalStorageDirectory();
		File data = Environment.getDataDirectory();
		FileChannel source = null;
		FileChannel destination = null;
		String currentDBPath = "/data/com.longinus.projcaritasand/databases/proj_database.sqlite";
		String backupDBPath = "proj_database.sqlite";
		File currentDB = new File(data, currentDBPath);
		File backupDB = new File(sd, backupDBPath);
		try {
			source = new FileInputStream(currentDB).getChannel();
			destination = new FileOutputStream(backupDB).getChannel();
			destination.transferFrom(source, 0, source.size());
			source.close();
			destination.close();
			Toast.makeText(context, "DB Exported!", Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<Type> getTypes() {
		return databaseHelper.getTypes();
	}

	public PersonalStat getCampaignPersonalStats() {
		return databaseHelper.getPersonalStat(campaignInUse.getId());
	}

	public boolean isBlocked() {
		try {
			return serverDataManager.isBlocked();
		} catch (Exception e) {
			return false;
		}		
	}
	
	/*
	TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
	GsmCellLocation cellLocation = (GsmCellLocation) telephonyManager.getCellLocation();

	Log.d("start", ""+new Date().getTime());
	String networkOperator = telephonyManager.getNetworkOperator();
	String mcc = networkOperator.substring(0, 3);
	String mnc = networkOperator.substring(3);
	int cid = cellLocation.getCid();
	int lac = cellLocation.getLac();
	
	Log.d("basic", "mcc: "+mcc+" mnc: "+mnc+" lac: "+lac+" cid: "+cid);
	
	List<NeighboringCellInfo> NeighboringList = telephonyManager.getNeighboringCellInfo();

	for (int i = 0; i < NeighboringList.size(); i++) {

		String dBm;
		int rssi = NeighboringList.get(i).getRssi();
		if (rssi == NeighboringCellInfo.UNKNOWN_RSSI) {
			dBm = "Unknown RSSI";
		} else {
			dBm = String.valueOf(-113 + 2 * rssi) + " dBm";
		}
		
		Log.d("Neighboring1", String.valueOf(NeighboringList.get(i).getLac()) +" : " + String.valueOf(NeighboringList.get(i).getCid()) +" : "
		 + String.valueOf(NeighboringList.get(i).getPsc()) +" : " + String.valueOf(NeighboringList.get(i).getNetworkType()) +" : " + dBm);
	}
	Log.d("end", ""+new Date().getTime());
	 */
}
