package com.longinus.projcaritasand.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.longinus.projcaritasand.model.database.Campaign;
import com.longinus.projcaritasand.model.database.DatabaseHelper;
import com.longinus.projcaritasand.model.database.PersonalStat;
import com.longinus.projcaritasand.model.database.Type;
import com.longinus.projcaritasand.model.listeners.DeviceListener;
import com.longinus.projcaritasand.model.network.ServerComm;
import com.longinus.projcaritasand.model.network.ServerResponse;
import com.longinus.projcaritasand.model.products.ProductBarcode;
import com.longinus.projcaritasand.model.products.ProductsBag;

public class ServerDataManager {
	
	private Object awaiterObj = new Object();

	private Context context;
	private Handler messageHandler;

	private DeviceListener deviceListener;

	private String usedServer;

	private Thread uploadThread;

	private int deviceStatus;
	protected boolean mustRunUploader;
	
	private int numberRegistriesSent;	
	private long lastServerUpdate = -1;
	
	private boolean deviceBlocked = false;
	
	private boolean forceSend = false;

	public ServerDataManager(Context context, Handler messageHandler) {
		this.context = context;
		this.messageHandler = messageHandler;
		usedServer = null;
		uploadThread = null;

		deviceStatus = Constants.DEVICE_STATUS_INITIALIZING;
		numberRegistriesSent = 0;
	}

	public void setDeviceListener(DeviceListener deviceListener) {
		this.deviceListener = deviceListener;
		this.deviceListener.deviceChanged(deviceStatus);
	}

	private void setDeviceStatus(int status) {
		deviceStatus = status;
		if (deviceListener != null)
			deviceListener.deviceChanged(status);
	}
	
	public boolean isBlocked() {
		return deviceBlocked;
	}

	public boolean findUsableServer() {
		setDeviceStatus(Constants.DEVICE_STATUS_SEARCHING_SERVERS);
		for (int i = 0; i < Constants.serversList.length && usedServer == null; i++) {
			if (Constants.DEBUG_MODE)
				Log.d("ServerDataManager", "trying server " + Constants.serversList[i]);
			ServerResponse response = ServerComm.fastPingSynchronous(Constants.serversList[i] + "/ping");
			if (response.hasException()) {
				continue;
			}
			if (response.getCode() == Constants.PING_CODE_OK) {
				if (Constants.DEBUG_MODE)
					Log.d("ServerDataManager", "received ping from " + Constants.serversList[i]);
				setDeviceStatus(Constants.DEVICE_STATUS_FOUND_SERVER);
				usedServer = Constants.serversList[i];
				return true;
			}
		}
		if (Constants.DEBUG_MODE)
			Log.d("ServerDataManager", "No server defined");
		setDeviceStatus(Constants.DEVICE_STATUS_SERVER_NOT_FOUND);
		return false;
	}
	
	public boolean pingServerSuccessfull() {
		if (usedServer == null) {
			if (Constants.DEBUG_MODE)
				Log.d("pingServerSuccessfull", "usedServer not defined");
			return false;
		}
		ServerResponse response = ServerComm.fastPingSynchronous(usedServer + "/ping");
		if (response.hasException()) {
			if (Constants.DEBUG_MODE)
				Log.d("pingServerSuccessfull", "response exception: "+Log.getStackTraceString(response.getException()));
			return false;
		}
		if (response.getCode() == Constants.PING_CODE_OK) {
			if (Constants.DEBUG_MODE)
				Log.d("pingServerSuccessfull", "received ping from " + usedServer);
			return true;
		}
		if (Constants.DEBUG_MODE)
			Log.d("pingServerSuccessfull", "received code: " + response.getCode());
		return false;
	}

	public void loadWithoutServer() {
		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		setDeviceStatus(Constants.DEVICE_STATUS_NO_NETWORK);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		setDeviceStatus(Constants.DEVICE_STATUS_READY);
	}

	public String generateServerUrl(String baseQuery) {
		return usedServer + baseQuery;
	}
	
	public String getUsedServer() {
		return usedServer;
	}

	public void loadDataStartup() {
		if (context == null) {
			setDeviceStatus(Constants.DEVICE_STATUS_FATAL_ERROR);
			throw new RuntimeException("Context not defined");
		}

		boolean mustContinue = true;
		setDeviceStatus(Constants.DEVICE_STATUS_REGISTERING_DEVICE);
		
		ServerResponse response = ServerComm.getStartupSynchronous(generateServerUrl(Constants.SERVER_URL_STARTUP), Utils.getID(context));
		switch (response.getCode()) {
			case Constants.DEVICE_CODE_BLOCKED:
				deviceBlocked = true;
				setDeviceStatus(Constants.DEVICE_STATUS_BLOCKED);
				if (Constants.DEBUG_MODE)
					Log.d("ServerDataManager", "device blocked ");
				
				setDeviceStatus(Constants.DEVICE_STATUS_BLOCKED);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				setDeviceStatus(Constants.DEVICE_STATUS_BLOCKED);
				return;
			case Constants.DEVICE_CODE_OK:
				setDeviceStatus(Constants.DEVICE_STATUS_OK);
				mustContinue = true;
				break;
			case Constants.DEVICE_CODE_REGISTERED:
				setDeviceStatus(Constants.DEVICE_STATUS_REGISTERED);
				mustContinue = true;
				break;

			default:
				mustContinue = false;
				break;
		}
		if (Constants.DEBUG_MODE)
			Log.d("ServerDataManager", "device status: " + deviceStatus);
		if (!mustContinue) {
			setDeviceStatus(Constants.DEVICE_STATUS_SERVER_NOT_FOUND);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			setDeviceStatus(Constants.DEVICE_STATUS_READY);
			return;
		}

		setDeviceStatus(Constants.DEVICE_STATUS_UPDATING_LOCAL_DATABASE);
		
		DatabaseHelper databaseHelper = SingletonModel.getInstance().getDatabaseHelper();
		try {
			Thread.sleep(1000);//time to open database
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		JSONObject responseContentObject;
		try {
			responseContentObject = new JSONObject(response.getContent());
		} catch (JSONException e) {
			e.printStackTrace();
			setDeviceStatus(Constants.DEVICE_STATUS_READY);
			return;
		}

		try {
			List<Type> types = downloadTypes(responseContentObject.getJSONArray("contentType"));
			databaseHelper.saveTypes(types);
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		try {
			JSONArray campaignsJson = responseContentObject.getJSONArray("contentCampaign");
			
			List<Campaign> campaigns = downloadCampaigns(campaignsJson);
			databaseHelper.saveCampaigns(campaigns);
			
			List<TypeNumber> objectives = downloadObjectives(campaignsJson);
			databaseHelper.saveObjectives(objectives);
			
			JSONArray statsJson = responseContentObject.getJSONArray("contentStats");
			
			List<TypeNumber> statistics = downloadStatistics(statsJson);
			databaseHelper.saveStatistics(statistics);
			
			List<PersonalStat> personalStats = downloadPersonalStatistics(statsJson);
			databaseHelper.savePersonalStats(personalStats);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Date lastUpdate = databaseHelper.getLastUpdateProducts();
		int newProducts = 0;
		int page = 0;
		do {
			List<ProductBarcode> products = downloadProducts(lastUpdate, page);
			try {
                newProducts = products.size();
            } catch (Exception e) {
                newProducts=0;
            }
			if (newProducts > 0) {
				databaseHelper.saveSearchableProducts(products);
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			page++;
		} while (newProducts > 0);
		databaseHelper.updateLastUpdateProducts(new Date());
		
		lastServerUpdate = System.currentTimeMillis();

		setDeviceStatus(Constants.DEVICE_STATUS_READY);

	}
	
	public void updateNewCampaignData() {
		DatabaseHelper databaseHelper = SingletonModel.getInstance().getDatabaseHelper();
		
		boolean mustContinue = true;
		setDeviceStatus(Constants.DEVICE_STATUS_REGISTERING_DEVICE);
		
		ServerResponse response = ServerComm.getStartupSynchronous(generateServerUrl(Constants.SERVER_URL_STARTUP), Utils.getID(context));
		switch (response.getCode()) {
			case Constants.DEVICE_CODE_BLOCKED:
				deviceBlocked = true;
				setDeviceStatus(Constants.DEVICE_STATUS_BLOCKED);
				if (Constants.DEBUG_MODE)
					Log.d("ServerDataManager", "device blocked ");
				
				setDeviceStatus(Constants.DEVICE_STATUS_BLOCKED);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				setDeviceStatus(Constants.DEVICE_STATUS_BLOCKED);
				return;
			case Constants.DEVICE_CODE_OK:
				setDeviceStatus(Constants.DEVICE_STATUS_OK);
				mustContinue = true;
				break;
			case Constants.DEVICE_CODE_REGISTERED:
				setDeviceStatus(Constants.DEVICE_STATUS_REGISTERED);
				mustContinue = true;
				break;

			default:
				mustContinue = false;
				break;
		}
		if (Constants.DEBUG_MODE)
			Log.d("ServerDataManager", "device status: " + deviceStatus);
		if (!mustContinue) {
			setDeviceStatus(Constants.DEVICE_STATUS_SERVER_NOT_FOUND);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			setDeviceStatus(Constants.DEVICE_STATUS_READY);
			return;
		}

		setDeviceStatus(Constants.DEVICE_STATUS_UPDATING_LOCAL_DATABASE);
		
		
		JSONObject responseContentObject;
		try {
			responseContentObject = new JSONObject(response.getContent());
		} catch (JSONException e) {
			e.printStackTrace();
			setDeviceStatus(Constants.DEVICE_STATUS_READY);
			return;
		}

		try {
			List<Type> types = downloadTypes(responseContentObject.getJSONArray("contentType"));
			databaseHelper.saveTypes(types);
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		try {
			JSONArray campaignsJson = responseContentObject.getJSONArray("contentCampaign");
			
			List<Campaign> campaigns = downloadCampaigns(campaignsJson);
			databaseHelper.saveCampaigns(campaigns);
			
			List<TypeNumber> objectives = downloadObjectives(campaignsJson);
			databaseHelper.saveObjectives(objectives);
			
			JSONArray statsJson = responseContentObject.getJSONArray("contentStats");
			
			List<TypeNumber> statistics = downloadStatistics(statsJson);
			databaseHelper.saveStatistics(statistics);
			
			List<PersonalStat> personalStats = downloadPersonalStatistics(statsJson);
			databaseHelper.savePersonalStats(personalStats);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		setDeviceStatus(Constants.DEVICE_STATUS_READY);
	}

	public void loadData() {
		if (context == null) {
			throw new RuntimeException("Context not defined");
		}
		
		ServerResponse response = ServerComm.getStatisticsSynchronous(generateServerUrl(Constants.SERVER_URL_STATISTICS), Utils.getID(context));
		if (response.getCode()!=Constants.STATS_CODE_OK) {
			if (Constants.DEBUG_MODE)
				Log.d("loadData", "Response code: "+response.getCode());
			return;
		}
		
		JSONArray responseJsonArray;
		try {
			responseJsonArray = new JSONArray(response.getContent());
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		DatabaseHelper databaseHelper = SingletonModel.getInstance().getDatabaseHelper();

		List<TypeNumber> statistics = downloadStatistics(responseJsonArray);
		if (statistics!=null) {
			databaseHelper.saveStatistics(statistics);
		}
		
		List<PersonalStat> personalStats = downloadPersonalStatistics(responseJsonArray);
		if (personalStats!=null) {
			databaseHelper.savePersonalStats(personalStats);
		}	

		Date lastUpdate = databaseHelper.getLastUpdateProducts();
		int newProducts = 0;
		int page = 0;
		boolean updated = false;
		do {
			List<ProductBarcode> products = downloadProducts(lastUpdate, page);
			if (products==null) {
				updated = false;
				break;
			}
			newProducts = products.size();
			try {
                newProducts = products.size();
            } catch (Exception e) {
                newProducts=0;
            }
			if (newProducts > 0) {
				databaseHelper.saveSearchableProducts(products);
				updated = true;
			}
			page++;
		} while (newProducts > 0);
		if (updated) {
			databaseHelper.updateLastUpdateProducts(new Date());
		}		
	}

	private List<Type> downloadTypes(JSONArray content) {
		try {
			List<Type> types = new ArrayList<Type>();
			for (int i = 0; i < content.length(); i++) {
				JSONObject jsonType = (JSONObject) content.get(i);
				types.add(new Type(jsonType.getString("symbol"), null, jsonType.getString("name"), null));
				JSONArray jsonArraySubtypes = jsonType.getJSONArray("subtypes");
				for (int j = 0; j < jsonArraySubtypes.length(); j++) {
					JSONObject jsonSubtype = jsonArraySubtypes.getJSONObject(j);
					if (jsonSubtype.getInt("id") != 0) {
						types.add(new Type(jsonType.getString("symbol"), jsonSubtype.getInt("id"), jsonType.getString("name"), jsonSubtype.getString("name")));
					} else {
						types.add(new Type(jsonType.getString("symbol"), jsonSubtype.getInt("id"), jsonType.getString("name"), jsonType.getString("name")));
					}
				}
			}
			return types;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private List<ProductBarcode> downloadProducts(Date lastUpdate, int page) {
		ServerResponse response = ServerComm.getProductsSynchronous(generateServerUrl(Constants.SERVER_URL_PRODUCTS), Utils.getID(context), lastUpdate.getTime(), page);
		switch (response.getCode()) {
			case Constants.DEVICE_CODE_OK:
				deviceStatus = Constants.DEVICE_STATUS_OK;
				try {
					List<ProductBarcode> products = new ArrayList<ProductBarcode>();
					JSONArray contentJsonArray = new JSONArray(response.getContent());
					for (int i = 0; i < contentJsonArray.length(); i++) {
						JSONObject object = contentJsonArray.getJSONObject(i);
						products.add(new ProductBarcode(object.getString("ean"), object.getString("name"), object.getString("type"), object.getInt("weight"), object
								.getString("unit"), object.getInt("realWeight")));
					}
					return products;
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}

			default:
				return null;
		}
	}

	private List<Campaign> downloadCampaigns(JSONArray content) {
		try {
			List<Campaign> campaigns = new ArrayList<Campaign>();
			for (int i = 0; i < content.length(); i++) {
				JSONObject campaignObject = (JSONObject) content.get(i);
				String mobileScreen = null;
				Long sumWeights = null;
				Integer sumRegistries = null;
				
				try {
					mobileScreen = campaignObject.getString("mobileScreen");
				} catch (Exception e) {
					mobileScreen = null;
				}
				try {
					sumWeights = campaignObject.getLong("sumWeights");
				} catch (Exception e) {
					sumWeights = null;
				}
				try {
					sumRegistries = campaignObject.getInt("sumRegistries");
				} catch (Exception e) {
					sumRegistries = null;
				}
				
				campaigns.add(new Campaign(campaignObject.getString("idCampaign"), campaignObject.getString("name"),
						new Date(campaignObject.getLong("dateStart")), 
						new Date(campaignObject.getLong("dateEnd")), 
						campaignObject.getBoolean("subscribed"), 
						mobileScreen, (sumWeights!=null)?sumWeights:0L, (sumRegistries!=null)?sumRegistries:0));
			}
			return campaigns;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private List<TypeNumber> downloadStatistics(JSONArray content) {
		try {
			List<TypeNumber> statistics = new ArrayList<TypeNumber>();
			
			for (int i = 0; i < content.length(); i++) {
				JSONObject responseObject = content.getJSONObject(i);
				String idCampaign = responseObject.getString("idCampaign");
				if (idCampaign == null) {
					continue;
				}
				
				JSONObject statsValues = responseObject.getJSONObject("stats");
				JSONArray statsValuesNames = statsValues.names();
				for (int j = 0; j < statsValuesNames.length(); j++) {
					String type = (String) statsValuesNames.get(j);
					statistics.add(new TypeNumber(idCampaign, type, statsValues.getInt(type)));
				}				
			}					
			
			return statistics;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private List<PersonalStat> downloadPersonalStatistics(JSONArray content) {
		try {
			List<PersonalStat> personalStats = new ArrayList<PersonalStat>();
			
			for (int i = 0; i < content.length(); i++) {
				JSONObject responseObject = content.getJSONObject(i);
				String idCampaign = responseObject.getString("idCampaign");
				if (idCampaign == null) {
					continue;
				}
				
				Integer sumRegistries = responseObject.getInt("sumRegistries");
				Long sumWeights = responseObject.getLong("sumWeights");
				Integer mySumRegistries = responseObject.getInt("mySumRegistries");
				Long mySumWeights = responseObject.getLong("mySumWeights");
				Integer countDevices = responseObject.getInt("countDevices");
				Integer myRank = responseObject.getInt("myRank");
				
				personalStats.add(new PersonalStat(idCampaign, sumWeights, sumRegistries, mySumWeights, mySumRegistries, countDevices, myRank));
			}					
			
			return personalStats;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private List<TypeNumber> downloadObjectives(JSONArray content) {
		try {
			List<TypeNumber> objectives = new ArrayList<TypeNumber>();
			
			for (int i = 0; i < content.length(); i++) {
				JSONObject responseObject = content.getJSONObject(i);
				String idCampaign = responseObject.getString("idCampaign");
				if (idCampaign == null) {
					continue;
				}
				
				JSONObject statsValues = responseObject.getJSONObject("objectives");
				JSONArray statsValuesNames = statsValues.names();
				for (int j = 0; j < statsValuesNames.length(); j++) {
					String type = (String) statsValuesNames.get(j);
					objectives.add(new TypeNumber(idCampaign, type, statsValues.getInt(type)));
				}
			}	
			return objectives;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public int getDeviceStatus() {
		return deviceStatus;
	}

	public boolean sendProductsBags(String idCampaign, List<ProductsBag> productsBags) {
		if (productsBags == null || idCampaign == null) {
			return false;
		}
		if (productsBags.size() == 0) {
			return false;
		}
		JSONArray jsonArray = new JSONArray();
		if (Constants.DEBUG_MODE)
			Log.d("uploadProductsBags", "size: " + productsBags.size());
		for (int i = 0; i < productsBags.size(); i++) {
			if (Constants.DEBUG_MODE)
				Log.d("uploadProductsBags", "i: " + i);
			try {
				Log.d("sendProductsBags-loc", productsBags.get(i).toString());
				jsonArray.put(new JSONObject(productsBags.get(i).toString()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		ServerResponse response = ServerComm.registerProductsSynchronous(generateServerUrl(Constants.SERVER_URL_REGISTRIES), Utils.getID(context), idCampaign,
				jsonArray.toString());

		if (response.hasException()) {
			response.getException().printStackTrace();
			return false;
		}
		switch (response.getCode()) {
			case Constants.REGISTER_PRODUCT_CODE_OK:
				return true;
			default:
				return false;
		}
	}

	public void startUploaderThread() {
		if (uploadThread != null) {
			if (uploadThread.isAlive()) {
				return;
			}
		}
		mustRunUploader = true;
		uploadThread = new Thread(new Runnable() {

			@Override
			public void run() {
				Random random = new Random(System.nanoTime());
				boolean successLastUpload = true;
				while (mustRunUploader) {
					try {
						if (Constants.DEBUG_MODE)
							Log.d("uploadThread", "1");
						if (successLastUpload) {
							if (forceSend && context!=null) {
								Message msg = new Message();
								Bundle bundle = new Bundle();
								bundle.putString("msg", "Registos enviados com sucesso");
								msg.setData(bundle);
								messageHandler.sendMessage(msg);
								forceSend = false;
							}
							synchronized (awaiterObj) {
								awaiterObj.wait(1000 * 60 * Constants.UPLOAD_THREAD_LONG_WAIT+random.nextInt(180));
							}
						}else {
							if (forceSend && context!=null) {
								Message msg = new Message();
								Bundle bundle = new Bundle();
								bundle.putString("msg", "Falha no envio de registos");
								msg.setData(bundle);
								messageHandler.sendMessage(msg);
								forceSend = false;
							}
							synchronized (awaiterObj) {
								awaiterObj.wait(1000 * 60 * Constants.UPLOAD_THREAD_SHORT_WAIT+random.nextInt(120));
							}
						}						
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}
					
					if (!Utils.isWifiConnected(context) && !Utils.isMobileConnected(context)) {
						successLastUpload = false;
						continue;
					}
					
					if (usedServer == null) {
						if(!findUsableServer()) {
							successLastUpload = false;
							continue;
						}
					}

					try {
						if (Constants.DEBUG_MODE)
							Log.d("uploadThread", "2");
						
						sendNewProducts();
						
						if (Constants.DEBUG_MODE)
							Log.d("uploadThread", "loading data");
						loadData();
						if (Constants.DEBUG_MODE)
							Log.d("uploadThread", "3");
						DatabaseHelper db = SingletonModel.getInstance().getDatabaseHelper();
						Log.d("uploadThread", "4");
						
						

						if (Constants.DEBUG_MODE)
							Log.d("bagsup", "1");
						List<ProductsBag> bags = null;
						Campaign campaignInUse = SingletonModel.getInstance().getCampaignInUse();
						if (campaignInUse != null) {
							bags = db.getBags(campaignInUse.getId(), 100);
						}
						
						if (Constants.DEBUG_MODE)
							Log.d("uploadThread", "bags " + ((bags == null) ? "null" : bags.size()));
						if (Constants.DEBUG_MODE)
							Log.d("bagsup", "2");
						while (bags != null) {
							if (Constants.DEBUG_MODE)
								Log.d("bagsup", "3");
							if (bags.size() == 0) {
								break;
							}
							if (Constants.DEBUG_MODE)
								Log.d("bagsup", "4");
							boolean result = false;
							if (campaignInUse != null) {
								result = sendProductsBags(campaignInUse.getId(), bags);								
							}
							if (Constants.DEBUG_MODE)
								Log.d("bagsup", "5");
							if (Constants.DEBUG_MODE)
								Log.d("uploadThread-result", ""+result);
							if (result) {
								if (Constants.DEBUG_MODE)
									Log.d("bagsup", "6");
								numberRegistriesSent += bags.size();
								List<Long> ids = new ArrayList<Long>();
								StringBuilder idsStringBuilder = new StringBuilder();
								for (ProductsBag bag : bags) {
									ids.add(bag.getRegisteredTime().getTime());
									idsStringBuilder.append(bag.getRegisteredTime().getTime()+" ");
								}
								if (Constants.DEBUG_MODE)
									Log.d("to remove", idsStringBuilder.toString());
								db.removeBags(ids);
								if (Constants.DEBUG_MODE)
									Log.d("bagsup", "7");
								bags = db.getBags(campaignInUse.getId(), 100);
								if (Constants.DEBUG_MODE)
									Log.d("bagsup", "8");
							} else {
								if (Constants.DEBUG_MODE)
									Log.d("bagsup", "9");
								successLastUpload = false;
								break;
							}
							if (Constants.DEBUG_MODE)
								Log.d("bagsup", "10");
						}
						if (Constants.DEBUG_MODE)
							Log.d("bagsup", "11");
						
						
						bags = null;
						campaignInUse = SingletonModel.getInstance().getCampaignInUse();
						if (campaignInUse != null) {
							bags = db.getMassBags(campaignInUse.getId(), 100);
						}
						
						if (Constants.DEBUG_MODE)
							Log.d("uploadThread", "bags " + ((bags == null) ? "null" : bags.size()));
						if (Constants.DEBUG_MODE)
							Log.d("bagsup", "12");
						while (bags != null) {
							if (Constants.DEBUG_MODE)
								Log.d("bagsup", "13");
							if (bags.size() == 0) {
								break;
							}
							if (Constants.DEBUG_MODE)
								Log.d("bagsup", "14");
							boolean result = false;
							if (campaignInUse != null) {
								result = sendProductsBags(campaignInUse.getId(), bags);								
							}
							if (Constants.DEBUG_MODE)
								Log.d("bagsup", "15");
							if (Constants.DEBUG_MODE)
								Log.d("uploadThread-result", ""+result);
							if (result) {
								if (Constants.DEBUG_MODE)
									Log.d("bagsup", "16");
								numberRegistriesSent += bags.size();
								List<Long> ids = new ArrayList<Long>();
								StringBuilder idsStringBuilder = new StringBuilder();
								for (ProductsBag bag : bags) {
									ids.add(bag.getRegisteredTime().getTime());
									idsStringBuilder.append(bag.getRegisteredTime().getTime()+" ");
								}
								if (Constants.DEBUG_MODE)
									Log.d("to remove", idsStringBuilder.toString());
								db.removeBags(ids);
								if (Constants.DEBUG_MODE)
									Log.d("bagsup", "17");
								bags = db.getMassBags(campaignInUse.getId(), 100);
								if (Constants.DEBUG_MODE)
									Log.d("bagsup", "18");
							} else {
								if (Constants.DEBUG_MODE)
									Log.d("bagsup", "19");
								successLastUpload = false;
								break;
							}
							if (Constants.DEBUG_MODE)
								Log.d("bagsup", "20");
						}
						if (Constants.DEBUG_MODE)
							Log.d("bagsup", "21");
						
						loadData();
						
						if (Constants.DEBUG_MODE)
							Log.d("bagsup", "22");
						
						successLastUpload = true;
						lastServerUpdate = System.currentTimeMillis();
					} catch (Exception e) {
						if (Constants.DEBUG_MODE)
							Log.d("bagsup", "23");
						e.printStackTrace();
						successLastUpload = false;
					}

					if (Constants.DEBUG_MODE)
						Log.d("uploadThread-finalresult", ""+successLastUpload);
				}
			}

			private void sendNewProducts() {
				if (context == null) {
					throw new RuntimeException("Context not defined");
				}

				DatabaseHelper databaseHelper = SingletonModel.getInstance().getDatabaseHelper();

				List<ProductBarcode> products = databaseHelper.getNewProducts();
				if (products == null || products.size()==0) {
					return;
				}
				
				JSONArray productsJsonArray = new JSONArray();
				
				for (int i = 0; i < products.size(); i++) {
					productsJsonArray.put(products.get(i).toJsonObject());
				}
				ServerResponse response = ServerComm.registerNewProductsSynchronous(generateServerUrl(Constants.SERVER_URL_REGISTRIES), Utils.getID(context), productsJsonArray);
				
				if (response.hasException()) {
					response.getException().printStackTrace();
				}
				
				if (Constants.DEBUG_MODE) {
					Log.d("sendNewProducts", "response code: "+response.getCode());
				}
				if(response.getCode()==Constants.NEW_PRODUCT_CODE_OK) {
					databaseHelper.removeNewProducts();
				}
			}
		});
		uploadThread.start();
	}

	public void stopThreads() {
		mustRunUploader = false;
		try {
			uploadThread.interrupt();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	public int getNumberRegistriesSent() {
		return numberRegistriesSent;
	}

	public long getLastServerUpdate() {
		return lastServerUpdate;
	}

	public void forceSend() {
		forceSend = true;
		synchronized (awaiterObj) {
			awaiterObj.notify();
		}
	}
}
