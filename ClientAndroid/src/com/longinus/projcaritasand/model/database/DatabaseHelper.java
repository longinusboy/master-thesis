package com.longinus.projcaritasand.model.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.longinus.projcaritasand.model.TypeNumber;
import com.longinus.projcaritasand.model.products.IProduct;
import com.longinus.projcaritasand.model.products.MassProductContainer;
import com.longinus.projcaritasand.model.products.ProductBarcode;
import com.longinus.projcaritasand.model.products.ProductManual;
import com.longinus.projcaritasand.model.products.ProductsBag;

@SuppressLint("UseSparseArrays")
public class DatabaseHelper extends SQLiteOpenHelper {
	
	private static final int DB_VERSION = 3;

	//The Android's default system path of your application database.
	//private static String DB_PATH = "/data/data/com.longinus.projcaritasand/databases/";

	private static String ASSET_DB_NAME = "proj_database.imy";
	private static String DB_NAME = "proj_database.sqlite";

	private SQLiteDatabase myDataBase;

	private Object object;

	private final Context myContext;

	/**
	 * Constructor Takes and keeps a reference of the passed context in order to
	 * access to the application assets and resources.
	 * 
	 * @param context
	 */
	public DatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		object = new Object();
		Log.d("dbhelper", "DatabaseHelper");
		this.myContext = context;
		try {
			createDataBase();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a empty database on the system and rewrites it with your own
	 * database.
	 * */
	public void createDataBase() throws IOException {
		Log.d("dbhelper", "createDataBase");

		if (!checkDataBase()) {
			//By calling this method and empty database will be created into the default system path
			//of your application so we are gonna be able to overwrite that database with our database.
			this.getReadableDatabase();

			try {
				copyDataBase();
			} catch (IOException e) {
				e.printStackTrace();
				throw new Error("Error copying database");
			}
		}
	}

	/**
	 * Check if the database already exist to avoid re-copying the file each
	 * time you open the application.
	 * 
	 * @return true if it exists, false if it doesn't
	 */
	private boolean checkDataBase() {
		Log.d("dbhelper", "checkDataBase");

		int sizeDBAsset = 0;
		try {
			sizeDBAsset = myContext.getAssets().open("databases/" + ASSET_DB_NAME).available();
		} catch (IOException e1) {
		}

		long sizeDBLocal = 0;
		File outFile = myContext.getDatabasePath(DB_NAME);
		sizeDBLocal = outFile.length();

		if (sizeDBLocal >= sizeDBAsset) {
			return true;
		}
		return false;

	}

	/**
	 * Copies your database from your local assets-folder to the just created
	 * empty database in the system folder, from where it can be accessed and
	 * handled. This is done by transfering bytestream.
	 * */
	private void copyDataBase() throws IOException {
		Log.d("dbhelper", "copyDataBase");

		//Open your local db as the input stream
		InputStream myInput = myContext.getAssets().open("databases/" + ASSET_DB_NAME);

		// Path to the just created empty db
		File outFile = myContext.getDatabasePath(DB_NAME);
		String outFileName = outFile.getPath();

		//Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(outFileName);

		//transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myInput.read(buffer)) > 0) {
			myOutput.write(buffer, 0, length);
		}

		//Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();
	}

	public void openDataBase() throws SQLException {
		Log.d("dbhelper", "openDataBase");
		//Open the database
		File outFile = myContext.getDatabasePath(DB_NAME);
		String outFileName = outFile.getPath();
		myDataBase = SQLiteDatabase.openDatabase(outFileName, null, SQLiteDatabase.OPEN_READONLY);
	}

	@Override
	public synchronized void close() {
		Log.d("dbhelper", "close");
		if (myDataBase != null)
			myDataBase.close();
		super.close();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d("dbhelper", "onCreate");

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d("dbhelper", "onUpgrade");
		try {
			copyDataBase();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error copying database");
		}
	}

	private ProductBarcode getProductNotSync(SQLiteDatabase db, String ean) {
		Log.d("dbhelper", "getProductNotSync");
		ProductBarcode product = null;

		Cursor cursor = db.rawQuery("SELECT _id, name, type, weight, unit FROM products WHERE _id=?", new String[]{ean});

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				product = new ProductBarcode(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3), cursor.getString(4), 1);
			}
			cursor.close();
		}
		Log.d("dbhelper", "getProductNotSync-close");

		return product;
	}

	public void saveSearchableProducts(List<ProductBarcode> products) {
		Log.d("dbhelper", "saveProducts");
		if (products == null) {
			Log.e("dbhelper->saveProducts", "products can not be empty");
			return;
		}
		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			for (int i = 0; i < products.size(); i++) {
				ProductBarcode product = products.get(i);
				ContentValues values = new ContentValues();
				values.put("_id", product.getEan());
				values.put("type", product.getType());
				values.put("weight", product.getWeight());
				values.put("unit", product.getUnit());
				values.put("realWeight", product.getRealWeight());
				values.put("name", product.getName());
				values.put("system", 1);
				//db.insert("products", null, values);
				db.insertWithOnConflict("products", null, values, SQLiteDatabase.CONFLICT_REPLACE);
				//db.update("products", values, "_id=?", new String[]{product.getEan()});
			}
			Log.d("dbhelper", "saveProducts-close");
			db.close();
		}
	}

	private Type getTypeNotSync(SQLiteDatabase db, String typeSymbol) {
		Log.d("dbhelper", "getTypeNotSync");
		Type type = null;

		Cursor cursor = db.rawQuery("SELECT _id, name, subname FROM types WHERE _id=?", new String[]{typeSymbol});

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				String[] idParts = cursor.getString(0).split("-");
				if (idParts.length > 1) {
					type = new Type(idParts[0], Integer.valueOf(idParts[1]), cursor.getString(1), cursor.getString(2));
				} else {
					type = new Type(idParts[0], null, cursor.getString(1), null);
				}
			}
			cursor.close();
		}
		Log.d("dbhelper", "getTypeNotSync-close");

		return type;
	}

	public List<Type> getPrimaryTypes() {
		Log.d("dbhelper", "getPrimaryTypes");
		List<Type> types = null;

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT _id, name FROM types WHERE length(types._id)=1", null);

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					types = new ArrayList<Type>();
					do {
						types.add(new Type(cursor.getString(0), null, cursor.getString(1), null));
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
			Log.d("dbhelper", "getPrimaryTypes-close");
			db.close();
		}

		return types;
	}
	
	public List<Type> getTypes() {
		Log.d("dbhelper", "getTypes");
		List<Type> types = null;

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT _id, name, subname FROM types", null);

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					types = new ArrayList<Type>();
					do {
						types.add(new Type(cursor.getString(0), null, cursor.getString(1), cursor.getString(2)));
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
			Log.d("dbhelper", "getTypes-close");
			db.close();
		}

		return types;
	}
	
	public Type getType(String symbol) {
		Log.d("dbhelper", "getType: "+symbol);
		if (symbol!=null && symbol.contentEquals("undefined")) {
			Log.d("dbhelper", "getType-close-undefined");
			return null;
		}
		Type type = null;

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT name, subname FROM types WHERE _id=?", new String[]{symbol});

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					String idPrimary = null;
					Integer idSecundary = null;
					String [] symbolParts = symbol.split("-");
					if (symbolParts != null) {
						if (symbolParts.length>1) {
							idPrimary = symbolParts[0];
						}
						if (symbolParts.length>2) {
							try {						
								idSecundary = Integer.valueOf(symbol.split("-")[1]);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					
					type = new Type(idPrimary, idSecundary, cursor.getString(0), cursor.getString(1));
				}
				cursor.close();
			}
			Log.d("dbhelper", "getType-close");
			db.close();
		}

		return type;
	}
	
	public List<Campaign> getCampaigns() {
		Log.d("dbhelper", "getCampaigns");
		List<Campaign> campaigns = null;

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT _id, name, startDate, endDate, subscribed, screen, sumWeights, sumRegistries FROM campaigns", null);

			if (cursor != null) {
				campaigns = new ArrayList<Campaign>();
				if (cursor.moveToFirst()) {
					do {
						campaigns.add(new Campaign(cursor.getString(0), cursor.getString(1), new Date(cursor.getLong(2)), new Date(cursor.getLong(3)),
								cursor.getInt(4)!=0, cursor.getString(5), cursor.getLong(6), cursor.getInt(7)));
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
			Log.d("dbhelper", "getCampaigns-close");
			db.close();
		}

		return campaigns;
	}

	public void saveCampaigns(List<Campaign> campaigns) {
		Log.d("dbhelper", "saveCampaigns");
		if (campaigns == null) {
			Log.e("dbhelper->saveCampaigns", "campaigns can not be empty");
			return;
		}

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			db.execSQL("DELETE FROM campaigns");
			for (int i = 0; i < campaigns.size(); i++) {
				Campaign campaign = campaigns.get(i);
				ContentValues values = new ContentValues();
				values.put("_id", campaign.getId());
				values.put("name", campaign.getName());
				values.put("startDate", campaign.getStartDate().getTime());
				values.put("endDate", campaign.getEndDate().getTime());
				values.put("subscribed", (campaign.isSubscribed())?1:0);
				values.put("screen", campaign.getScreen());
				values.put("sumWeights", campaign.getSumWeights());
				values.put("sumRegistries", campaign.getSumRegistries());
				db.insert("campaigns", null, values);
			}
			Log.d("dbhelper", "saveCampaigns-close");
			db.close();
		}
	}
	
	public void setCampaignsSubscriptions(List<Campaign> campaigns) {
		Log.d("dbhelper", "setCampaignsSubscriptions");
		if (campaigns == null) {
			Log.e("dbhelper->setCampaignsSubscriptions", "campaigns can not be empty");
			return;
		}

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			for (int i = 0; i < campaigns.size(); i++) {
				Campaign campaign = campaigns.get(i);
				ContentValues values = new ContentValues();
				values.put("subscribed", (campaign.isSubscribed())?1:0);
				db.update("campaigns", values, "_id=?", new String[]{campaign.getId()});
			}
			Log.d("dbhelper", "setCampaignsSubscriptions-close");
			db.close();
		}
	}

	public void saveObjectives(List<TypeNumber> objectives) {
		Log.d("dbhelper", "saveObjectives");
		if (objectives == null) {
			Log.e("dbhelper->saveObjectives", "objectives can not be empty");
			return;
		}

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			db.execSQL("DELETE FROM objectives");
			for (int i = 0; i < objectives.size(); i++) {
				TypeNumber objective = objectives.get(i);
				ContentValues values = new ContentValues();
				values.put("_id", i);
				values.put("idCampaign", objective.getIdCampaign());
				values.put("symbol", objective.getType());
				values.put("quantity", objective.getValue());
				db.insert("objectives", null, values);
			}
			Log.d("dbhelper", "saveObjectives-close");
			db.close();
		}
	}

	public void saveStatistics(List<TypeNumber> statistics) {
		Log.d("dbhelper", "saveStatistics");
		if (statistics == null) {
			Log.e("dbhelper->saveStatistics", "statistics can not be empty");
			return;
		}

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			db.execSQL("DELETE FROM statistics");
			for (int i = 0; i < statistics.size(); i++) {
				TypeNumber statistic = statistics.get(i);
				ContentValues values = new ContentValues();
				values.put("_id", i);
				values.put("idCampaign", statistic.getIdCampaign());
				values.put("symbol", statistic.getType());
				values.put("quantity", statistic.getValue());
				db.insert("statistics", null, values);
			}
			Log.d("dbhelper", "saveStatistics-close");
			db.close();
		}
	}
	
	public void savePersonalStats(List<PersonalStat> personalStats) {
		Log.d("dbhelper", "savePersonalStatistics");
		if (personalStats == null) {
			Log.e("dbhelper->savePersonalStatistics", "statistics can not be empty");
			return;
		}

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			db.execSQL("DELETE FROM personal_stats");
			for (int i = 0; i < personalStats.size(); i++) {
				PersonalStat statistic = personalStats.get(i);
				ContentValues values = new ContentValues();
				values.put("_id", i * 6 + 0);
				values.put("idCampaign", statistic.getIdCampaign());
				values.put("idType", 0);
				values.put("value", statistic.getSumRegistries());
				db.insert("personal_stats", null, values);

				values = new ContentValues();
				values.put("_id", i * 6 + 1);
				values.put("idCampaign", statistic.getIdCampaign());
				values.put("idType", 1);
				values.put("value", statistic.getSumWeights());
				db.insert("personal_stats", null, values);

				if (statistic.getMySumRegistries() != null) {
					values = new ContentValues();
					values.put("_id", i * 6 + 2);
					values.put("idCampaign", statistic.getIdCampaign());
					values.put("idType", 2);
					values.put("value", statistic.getMySumRegistries());
					db.insert("personal_stats", null, values);
				}			
				
				if (statistic.getMySumWeights() != null) {
					values = new ContentValues();
					values.put("_id", i * 6 + 3);
					values.put("idCampaign", statistic.getIdCampaign());
					values.put("idType", 3);
					values.put("value", statistic.getMySumWeights());
					db.insert("personal_stats", null, values);
				}
				
				if (statistic.getCountDevices() != null) {
					values = new ContentValues();
					values.put("_id", i * 6 + 4);
					values.put("idCampaign", statistic.getIdCampaign());
					values.put("idType", 4);
					values.put("value", statistic.getCountDevices());
					db.insert("personal_stats", null, values);
				}
				
				if (statistic.getMyRank() != null) {
					values = new ContentValues();
					values.put("_id", i * 6 + 5);
					values.put("idCampaign", statistic.getIdCampaign());
					values.put("idType", 5);
					values.put("value", statistic.getMyRank());
					db.insert("personal_stats", null, values);
				}
			}
			Log.d("dbhelper", "savePersonalStatistics-close");
			db.close();
		}
	}
	
	public PersonalStat getPersonalStat(String idCampaign) {
		Log.d("dbhelper", "getPersonalStat");
		PersonalStat stat = null;
		
		if (idCampaign == null) {
			return null;
		}

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT idType, value FROM personal_stats WHERE personal_stats.idCampaign=?",
					new String[] { idCampaign });
			
			Integer sumRegistries = null;
			Long sumWeights = null;
			Integer mySumRegistries = null;
			Long mySumWeights = null;
			Integer countDevices = null;
			Integer myRank = null;

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						switch (cursor.getInt(0)) {
							case 0:
								sumRegistries = cursor.getInt(1);
								break;
							case 1:
								sumWeights = cursor.getLong(1);
								break;
							case 2:
								mySumRegistries = cursor.getInt(1);
								break;
							case 3:
								mySumWeights = cursor.getLong(1);
								break;
							case 4:
								countDevices = cursor.getInt(1);
								break;
							case 5:
								myRank = cursor.getInt(1);
								break;

							default:
								break;
						}
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
			
			if (sumRegistries != null && sumWeights != null && mySumRegistries != null && mySumWeights != null && countDevices != null && myRank != null) {
				stat = new PersonalStat(idCampaign, sumWeights, sumRegistries, mySumWeights, mySumRegistries, countDevices, myRank);
			}else if (sumRegistries != null && sumWeights != null) {
				stat = new PersonalStat(idCampaign, sumWeights, sumRegistries);
			}else {
				stat = new PersonalStat(idCampaign, null, null, null, null, null, null);//XXX dirty hack detected
			}
			
			Log.d("dbhelper", "getPersonalStat-close");
			db.close();
		}

		return stat;
	}

	public void saveTypes(List<Type> types) {
		Log.d("dbhelper", "saveTypes");
		if (types == null) {
			Log.e("dbhelper->saveTypes", "types can not be empty");
			return;
		}

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			db.execSQL("DELETE FROM types");
			for (int i = 0; i < types.size(); i++) {
				Type type = types.get(i);
				ContentValues values = new ContentValues();
				values.put("_id", type.getType());
				values.put("name", type.getName());
				values.put("subname", type.getSubname());
				db.insert("types", null, values);
			}
			Log.d("dbhelper", "saveTypes-close");
			db.close();
		}
	}

	public List<TypeNumber> getPrimaryStats(Campaign campaign) {
		Log.d("dbhelper", "getPrimaryStats");
		List<TypeNumber> stats = null;
		
		if (campaign == null) {
			return null;
		}

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT symbol, quantity FROM statistics WHERE statistics.idCampaign=? and length(statistics.symbol)=1",
					new String[] { campaign.getId() });

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					stats = new ArrayList<TypeNumber>();
					do {
						stats.add(new TypeNumber(campaign.getId(), cursor.getString(0), cursor.getInt(1)));
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
			Log.d("dbhelper", "getPrimaryStats-close");
			db.close();
		}

		return stats;
	}

	public List<TypeNumber> getPrimaryObjectives(Campaign campaign) {
		Log.d("dbhelper", "getPrimaryObjectives");
		List<TypeNumber> objectives = null;
		
		if (campaign == null) {
			return null;
		}

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT symbol, quantity FROM objectives WHERE objectives.idCampaign=? and length(objectives.symbol)=1",
					new String[] { campaign.getId() });

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					objectives = new ArrayList<TypeNumber>();
					do {
						objectives.add(new TypeNumber(campaign.getId(), cursor.getString(0), cursor.getInt(1)));
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
			Log.d("dbhelper", "getPrimaryObjectives-close");
			db.close();
		}

		return objectives;
	}

	public Date getLastUpdateProducts() {
		Log.d("dbhelper", "getLastUpdateProducts");
		long value = 0;

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT value FROM settings WHERE _id='lastUpdateProducts'", new String[] {});

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						try {
							value = Long.parseLong(cursor.getString(0));
						} catch (Exception e) {
							e.printStackTrace();
						}
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
			Log.d("dbhelper", "getType-close");
			db.close();
		}

		return new Date(value);
	}

	public void updateLastUpdateProducts(Date date) {
		Log.d("dbhelper", "updateLastUpdateProducts");

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			db.rawQuery("INSERT OR IGNORE INTO settings (_id, value) VALUES ('lastUpdateProducts', ?);", new String[] { String.valueOf(date.getTime()) });
			db.rawQuery("UPDATE settings SET value=? WHERE _id='lastUpdateProducts';", new String[] { String.valueOf(date.getTime()) });
			Log.d("dbhelper", "getLastUpdateProducts-close");
			db.close();
		}
	}
	
	public void saveBag(String idCampaign, ProductsBag bag) {
		Log.d("dbhelper", "saveBag");

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			
			long rowId = 0;

			List<IProduct> products = bag.getProducts();
			if (products.size() > 0) {
				ContentValues values = new ContentValues();
				values.put("registeredTime", bag.getRegisteredTime().getTime());
				values.put("idCampaign", idCampaign);
				GeoLocation geoLocation = bag.getGeoLocation();
				if (geoLocation != null) {
					values.put("lon", geoLocation.getLon());
					values.put("lat", geoLocation.getLat());
					if (geoLocation.getSource()!=null) {
						values.put("src", geoLocation.getSource());
					}
				}
				MobileLocation mobileLocation = bag.getMobileLocation();
				if (mobileLocation != null) {
					values.put("mcc", mobileLocation.getMcc());
					values.put("mnc", mobileLocation.getMnc());
					values.put("cid", mobileLocation.getCid());
					values.put("lac", mobileLocation.getLac());
				}
				if (bag.isMassInput()) {
					values.put("onmass", 1);
					values.put("type", bag.getType());
					values.put("weight", bag.getWeight());
					values.put("minWeight", bag.getMinWeight());
					values.put("maxWeight", bag.getMaxWeight());
					values.put("unit", bag.getUnit());
					values.put("realWeight", bag.getRealWeight());
				}else {
					values.put("onmass", 0);
				}
				
				rowId = db.insert("registries", null, values);
			}

			if (rowId>-1) {
				for (int i = 0; i < products.size(); i++) {
					IProduct product = products.get(i);
					if (product instanceof ProductBarcode) {
						ProductBarcode productBarcode = (ProductBarcode) product;
						ContentValues values = new ContentValues();
						values.put("idReg", rowId);
						values.put("ean", productBarcode.getEan());
						values.put("quantity", productBarcode.getQuantity());
						db.insert("registries_auto", null, values);
					}
					if (product instanceof ProductManual) {
						ProductManual productManual = (ProductManual) product;
						ContentValues values = new ContentValues();
						values.put("idReg", rowId);
						values.put("type", productManual.getType());
						values.put("weight", productManual.getWeight());
						values.put("unit", productManual.getUnit());
						values.put("quantity", productManual.getQuantity());
						db.insert("registries_man", null, values);
					}
				}
			}			

			Log.d("dbhelper", "saveBag-close");
			db.close();
		}
	}

	public List<ProductsBag> getBags(String idCampaign, int limit) {
		Log.d("dbhelper", "getBags");

		List<ProductsBag> bags = new ArrayList<ProductsBag>();

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT _id, registeredTime, locationTime, lon, lat, src, mcc, mnc, cid, lac FROM registries WHERE idCampaign=? AND onmass=0 "
					+ "ORDER BY _id LIMIT " + limit, new String[] { idCampaign });

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						ProductsBag bag = new ProductsBag(new Date(cursor.getLong(1)));
						if (!cursor.isNull(3) && !cursor.isNull(4)) {
							bag.setGeoLocation(new GeoLocation(cursor.getLong(2), cursor.getDouble(3), cursor.getDouble(4), cursor.getString(5)));
						}
						if (!cursor.isNull(6) && !cursor.isNull(7) && !cursor.isNull(8) && !cursor.isNull(9)) {
							bag.setMobileLocation(new MobileLocation(cursor.getLong(1), cursor.getInt(6), cursor.getInt(7), cursor.getInt(8), cursor.getInt(9)));
						}
						bags.add(bag);

					} while (cursor.moveToNext());
				}
				cursor.close();
			}

			for (ProductsBag bag : bags) {
				cursor = db.rawQuery("SELECT ean, quantity FROM registries_auto, registries WHERE idReg=registries._id AND registeredTime="
						+ bag.getRegisteredTime().getTime(), null);

				if (cursor != null) {
					if (cursor.moveToFirst()) {
						do {
							bag.getProducts().add(new ProductBarcode(cursor.getString(0), cursor.getInt(1)));
						} while (cursor.moveToNext());
					}
					cursor.close();
				}

				cursor = db.rawQuery("SELECT registries_man.type, registries_man.weight, registries_man.unit, quantity FROM registries_man, registries WHERE idReg=registries._id AND registeredTime="
						+ bag.getRegisteredTime().getTime(), null);

				if (cursor != null) {
					if (cursor.moveToFirst()) {
						do {
							bag.getProducts().add(new ProductManual(cursor.getString(0), cursor.getInt(1), cursor.getString(2), cursor.getInt(1), cursor.getInt(3)));
						} while (cursor.moveToNext());
					}
					cursor.close();
				}
			}

			Log.d("dbhelper", "getBags-close");
			db.close();
		}

		return bags;
	}
	
	public List<ProductsBag> getMassBags(String idCampaign, int limit) {
		Log.d("dbhelper", "getMassBags");

		//List<ProductsBag> bags = new ArrayList<ProductsBag>();

		Map<Long, ProductsBag> entriesBags = new HashMap<Long, ProductsBag>();
		
		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT _id, registeredTime, locationTime, lon, lat, src, mcc, mnc, cid, lac, type, weight, minWeight, maxWeight, unit, "
					+ "realWeight FROM registries WHERE idCampaign=? AND onmass=1 ORDER BY _id LIMIT " + limit, new String[] { idCampaign });

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						MassProductContainer container = new MassProductContainer(cursor.getString(10), cursor.getInt(11), cursor.getInt(12), cursor.getInt(13),
								cursor.getString(14), cursor.getInt(15), null);
						ProductsBag bag = new ProductsBag(new Date(cursor.getLong(1)), container);
						if (!cursor.isNull(3) && !cursor.isNull(4)) {
							bag.setGeoLocation(new GeoLocation(cursor.getLong(2), cursor.getDouble(3), cursor.getDouble(4), cursor.getString(5)));
						}
						if (!cursor.isNull(6) && !cursor.isNull(7) && !cursor.isNull(8) && !cursor.isNull(9)) {
							bag.setMobileLocation(new MobileLocation(cursor.getLong(1), cursor.getInt(6), cursor.getInt(7), cursor.getInt(8), cursor.getInt(9)));
						}
						entriesBags.put(cursor.getLong(0), bag);
					} while (cursor.moveToNext());
				}
				cursor.close();
			}

			for (Map.Entry<Long, ProductsBag> entryBag : entriesBags.entrySet()) {
				cursor = db.rawQuery("SELECT ean, quantity FROM registries_auto WHERE idReg=" + entryBag.getKey(), null);//XXX

				if (cursor != null) {
					if (cursor.moveToFirst()) {
						do {
							entryBag.getValue().getProducts().add(new ProductBarcode(cursor.getString(0), cursor.getInt(1)));
						} while (cursor.moveToNext());
					}
					cursor.close();
				}
			}

			Log.d("dbhelper", "getMassBags-close");
			db.close();
		}

		return new ArrayList<ProductsBag>(entriesBags.values());
	}

	public void removeBags(List<Long> times) {
		Log.d("dbhelper", "removeBags");

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			for (Long time : times) {
				
				Cursor cursor = db.rawQuery("SELECT _id FROM registries WHERE registeredTime=" + time, null);

				if (cursor != null) {
					if (cursor.moveToFirst()) {
						do {
							db.delete("registries", "_id=" + cursor.getLong(0), null);
							db.delete("registries_auto", "idReg=" + cursor.getLong(0), null);
							db.delete("registries_man", "idReg=" + cursor.getLong(0), null);
						} while (cursor.moveToNext());
					}
					cursor.close();
				}				
			}
			Log.d("dbhelper", "removeBags-close");
			db.close();
		}
	}

	public int getNumberRegistriesAwaiting() {
		int number = 0;
		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT Count(*) FROM registries", new String[] {});

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					try {
						number = cursor.getInt(0);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				cursor.close();
			}
		}
		return number;
	}
	
	public void addProductOnScreen(IProduct product) {
		Log.d("dbhelper", "addProductOnScreen");

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			boolean exists = false;
			
			if (product instanceof ProductBarcode) {
				ProductBarcode productBarcode = (ProductBarcode) product;
				Cursor cursor = db.rawQuery("SELECT _id FROM onscreen_registries WHERE ean=?", new String[] {productBarcode.getEan()});

				if (cursor != null) {
					if (cursor.moveToFirst()) {
						exists = true;
					}
					cursor.close();
				}
				
				if (!exists) {
					ProductBarcode databaseProduct = getProductNotSync(db, productBarcode.getEan());					
					ContentValues values = new ContentValues();
					values.put("_id", new Date().getTime());
					values.put("ean", productBarcode.getEan());
					values.put("quantity", productBarcode.getQuantity());
					
					if (databaseProduct != null) {
						values.put("type", databaseProduct.getType());
						values.put("name", databaseProduct.getName());
						values.put("weight", databaseProduct.getWeight());
						values.put("unit", databaseProduct.getUnit());
					}
					db.insert("onscreen_registries", null, values);
				}else {
					db.execSQL("UPDATE onscreen_registries SET quantity=quantity+1 WHERE ean=?", new String[] {productBarcode.getEan()});
				}				
			}else if (product instanceof ProductManual) {
				ProductManual productManual = (ProductManual) product;
				Cursor cursor = db.rawQuery("SELECT _id FROM onscreen_registries WHERE type='" + product.getType()
						+ "' AND weight=" + product.getWeight() + " AND unit='" + product.getUnit() + "'", null);

				if (cursor != null) {
					if (cursor.moveToFirst()) {
						exists = true;
					}
					cursor.close();
				}
				
				if (!exists) {
					Type type = getTypeNotSync(db, productManual.getType());
					ContentValues values = new ContentValues();
					values.put("_id", new Date().getTime());
					if (type!=null) {
						values.put("name", type.getName() + " (" + type.getSubname() + ")");
					}
					values.put("type", productManual.getType());
					values.put("weight", productManual.getWeight());
					values.put("unit", productManual.getUnit());
					values.put("quantity", productManual.getQuantity());					
					db.insert("onscreen_registries", null, values);
				}else {
					db.execSQL("UPDATE onscreen_registries SET quantity=quantity+"+product.getQuantity()+" WHERE type=? AND weight=" + product.getWeight() + 
							" AND unit=?", new String[] {product.getType(), product.getUnit()});
				}				
			}

			Log.d("dbhelper", "addProductOnScreen-close");
			db.close();
		}
	}
	
	public void removeProductsOnScreen() {
		Log.d("dbhelper", "removeProductsOnScreen");

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			db.execSQL("DELETE FROM onscreen_registries");
			Log.d("dbhelper", "removeProductsOnScreen-close");
			db.close();
		}
	}
	
	public void removeProductOnScreen(IProduct product) {
		Log.d("dbhelper", "removeProductOnScreen");

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();

			if (product instanceof ProductBarcode) {
				db.delete("onscreen_registries", "ean=?", new String[] { ((ProductBarcode) product).getEan() });
			} else if (product instanceof ProductManual) {
				db.delete("onscreen_registries", "type='" + product.getType() + "' AND weight=" + product.getWeight() + " AND unit='" + product.getUnit() + "'", null);
			}

			Log.d("dbhelper", "removeProductOnScreen-close");
			db.close();
		}
	}
	
	public void setCustomProduct(ProductBarcode product) {
		Log.d("dbhelper", "setCustomProduct");

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			
			Type type = getTypeNotSync(db, product.getType());
			
			ContentValues values = new ContentValues();
			values.put("_id", product.getEan());
			values.put("type", product.getType());
			values.put("weight", product.getWeight());
			values.put("unit", product.getUnit());
			values.put("realWeight", product.getWeight());
			values.put("name", type.getSubname() + " (" + type.getName() + ")");
			values.put("system", 0);
			db.insertWithOnConflict("products", null, values, SQLiteDatabase.CONFLICT_REPLACE);
			
			values = new ContentValues();
			values.put("type", product.getType());
			values.put("weight", product.getWeight());
			values.put("unit", product.getUnit());
			values.put("realWeight", product.getWeight());
			values.put("name", type.getSubname() + " (" + type.getName() + ")");			
			db.update("onscreen_registries", values, "ean=?", new String[]{product.getEan()});

			Log.d("dbhelper", "setCustomProduct-close");
			db.close();
		}
	}
	
	public void setQuantityProductOnScreen(IProduct product) {
		Log.d("dbhelper", "setQuantityProductOnScreen");

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();

			if (product instanceof ProductBarcode) {
				ContentValues values = new ContentValues();
				values.put("quantity", product.getQuantity());
				db.update("onscreen_registries", values, "ean=?", new String[] { ((ProductBarcode) product).getEan() });
			} else if (product instanceof ProductManual) {
				ContentValues values = new ContentValues();
				values.put("quantity", product.getQuantity());
				db.update("onscreen_registries", values, "type='" + product.getType() + "' AND weight=" + product.getWeight() + " AND unit='" + product.getUnit() + "'", null);
			}

			Log.d("dbhelper", "setQuantityProductOnScreen-close");
			db.close();
		}
	}
	
	public List<IProduct> getProductsOnScreen() {
		Log.d("dbhelper", "getProductsOnScreen");
		
		List<IProduct> products = null;

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT _id, ean, type, weight, unit, quantity, realWeight, name FROM onscreen_registries", null);

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					products = new ArrayList<IProduct>();
					do {
						if (!cursor.isNull(1)) {
							products.add(new ProductBarcode(cursor.getString(1), cursor.getString(7), cursor.getString(2), cursor.getInt(3), cursor.getString(4), cursor.getInt(6), cursor.getInt(5)));
						}else {
							products.add(new ProductManual(cursor.getString(2), cursor.getInt(3), cursor.getString(4), cursor.getInt(6), cursor.getInt(5)));
						}
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
			Log.d("dbhelper", "getProductsOnScreen-close");
			db.close();
		}
		return products;
	}
	
	public IProduct getProductOnScreen(int index) {
		Log.d("dbhelper", "getProductsOnScreen");
		
		IProduct product = null;
		int i=0;

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT _id, ean, type, weight, unit, quantity, realWeight, name FROM onscreen_registries", null);

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						if (i==index) {
							if (!cursor.isNull(1)) {
								product = new ProductBarcode(cursor.getString(1), cursor.getString(7), cursor.getString(2), cursor.getInt(3), cursor.getString(4), cursor.getInt(6), cursor.getInt(5));
							}else {
								product = new ProductManual(cursor.getString(2), cursor.getInt(3), cursor.getString(4), cursor.getInt(6), cursor.getInt(5));
							}
						}
						i++;
					} while (cursor.moveToNext() && product==null);
				}
				cursor.close();
			}
			Log.d("dbhelper", "getProductsOnScreen-close");
			db.close();
		}
		return product;
	}
	
	public boolean isBagItemRecognized(int index) {
		Log.d("dbhelper", "isBagItemRecognized");
		
		IProduct product = null;
		int i=0;
		boolean recognized = false;
		String ean;

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT ean FROM onscreen_registries", null);

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						if (i==index) {
							if (!cursor.isNull(0)) {
								ean = cursor.getString(0);
								recognized = getProductNotSync(db, ean) != null;								
							}else {
								recognized = true;
							}
							break;
						}
						i++;
					} while (cursor.moveToNext() && product==null);
				}
				cursor.close();
			}
			Log.d("dbhelper", "isBagItemRecognized-close");
			db.close();
		}
		
		return recognized;
	}
	
	public List<ProductBarcode> getNewProducts() {
		Log.d("dbhelper", "getNewProducts");
		
		List<ProductBarcode> products = null;

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT _id, type, weight, unit, realWeight, name FROM products WHERE system=0", null);

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					products = new ArrayList<ProductBarcode>();
					do {
						products.add(new ProductBarcode(cursor.getString(0), cursor.getString(5), cursor.getString(1), cursor.getInt(2), cursor.getString(3), cursor.getInt(4), 0));
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
			Log.d("dbhelper", "getNewProducts-close");
			db.close();
		}
		return products;
	}
	
	public void removeNewProducts() {
		Log.d("dbhelper", "removeNewProducts");

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			db.execSQL("DELETE FROM products WHERE system=0");
			Log.d("dbhelper", "removeNewProducts-close");
			db.close();
		}
	}
	
	
	public void addProductOnMass(String type, int weight, int minWeight, int maxWeight, String unit, int realWeight, ProductBarcode product) {
		Log.d("dbhelper", "addProductOnMass");

		Log.d("args", "type "+type+" weight "+weight+" minWeight "+minWeight+" maxWeight "+maxWeight+" unit "+unit+" realWeight "+realWeight+" product "+product);
		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			
			boolean exists = false;
			
			Cursor cursor = db.rawQuery("SELECT _id FROM onmass_registries WHERE ean=? AND type=? AND weight="+weight+" AND unit=?", new String[] {product.getEan(), type, unit});
			if (cursor == null) {
				Log.d("addProductOnMass", "cursor null");
			}
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					exists = true;
				}
				cursor.close();
			}
			
			if (!exists) {
				Log.d("addProductOnMass", "add");
				ProductBarcode databaseProduct = getProductNotSync(db, product.getEan());					
				
				ContentValues values = new ContentValues();
				values.put("_id", new Date().getTime());
				values.put("ean", product.getEan());
				if (databaseProduct != null) {
					values.put("name", databaseProduct.getName());
				}
				values.put("type", type);
				values.put("weight", weight);
				values.put("minWeight", minWeight);
				values.put("maxWeight", maxWeight);
				values.put("unit", unit);
				values.put("realWeight", realWeight);
				values.put("quantity", product.getQuantity());
				Log.d("addProductOnMass-values", values.toString());
				Log.d("addProductOnMass-result", String.valueOf(db.insertOrThrow("onmass_registries", null, values)));
			}else {
				Log.d("addProductOnMass", "update");
				db.execSQL("UPDATE onmass_registries SET quantity=quantity+1 WHERE ean=? AND type=? AND weight="+weight+" AND unit=?", new String[] {product.getEan(), type, unit});
			}

			Log.d("dbhelper", "addProductOnMass-close");
			db.close();
		}
	}
	
	public void removeProductsOnMass() {
		Log.d("dbhelper", "removeProductsOnMass");

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();
			db.execSQL("DELETE FROM onmass_registries");
			Log.d("dbhelper", "removeProductsOnMass-close");
			db.close();
		}
	}
	
	public void removeProductOnMass(ProductBarcode product) {
		Log.d("dbhelper", "removeProductOnMass");

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();

			db.delete("onmass_registries", "ean=? AND type=? AND weight="+product.getWeight()+" AND unit=?", 
					new String[]{product.getEan(), product.getType(), product.getUnit()});

			Log.d("dbhelper", "removeProductOnMass-close");
			db.close();
		}
	}
	
	public void setQuantityProductOnMass(String type, int weight, String unit, ProductBarcode product) {
		Log.d("dbhelper", "setQuantityProductOnMass");

		synchronized (object) {
			SQLiteDatabase db = this.getWritableDatabase();

			ContentValues values = new ContentValues();
			values.put("quantity", product.getQuantity());
			db.update("onmass_registries", values, "ean=? AND type=? AND weight="+weight+" AND unit=?", 
					new String[]{product.getEan(), type, unit});

			Log.d("dbhelper", "setQuantityProductOnMass-close");
			db.close();
		}
	}
	
	public List<MassProductContainer> getProductsOnMass() {
		Log.d("dbhelper", "getProductsOnMass");

		List<MassProductContainer> entries = new ArrayList<MassProductContainer>();
		List<ProductBarcode> products = null;

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT _id, ean, type, weight, minWeight, maxWeight, unit, realWeight, quantity FROM onmass_registries ORDER BY type, weight, "
					+ "unit", null);

			if (cursor != null) {
				Log.d("getProductsOnMass", "count: "+cursor.getCount());
				if (cursor.moveToFirst()) {
					String lastType = cursor.getString(2);
					Integer lastWeight = cursor.getInt(3);
					Integer lastMinWeight = cursor.getInt(4);
					Integer lastMaxWeight = cursor.getInt(5);
					String lastUnit = cursor.getString(6);
					Integer lastRealWeight = cursor.getInt(7);
					products = new ArrayList<ProductBarcode>();

					do {
						if ((!lastType.contentEquals(cursor.getString(2)) || lastWeight != cursor.getInt(3) || !lastUnit.contentEquals(cursor.getString(6)))
								&& products.size() > 0) {
							entries.add(new MassProductContainer(lastType, lastWeight, lastMinWeight, lastMaxWeight, lastUnit, lastRealWeight, products));
							products = new ArrayList<ProductBarcode>();
						}
						/*products.add(new ProductBarcode(cursor.getString(1), cursor.getString(2), cursor.getInt(3), cursor.getString(4), cursor.getInt(5), 
								cursor.getInt(6)));*/
						products.add(new ProductBarcode(cursor.getString(1), cursor.getString(2), cursor.getInt(3), cursor.getString(6), cursor.getInt(7), cursor.getInt(8)));
						lastType = cursor.getString(2);
						lastWeight = cursor.getInt(3);
						lastMinWeight = cursor.getInt(4);
						lastMaxWeight = cursor.getInt(5);
						lastUnit = cursor.getString(6);
						lastRealWeight = cursor.getInt(7);
					} while (cursor.moveToNext());
					
					if (products.size() > 0) {
						entries.add(new MassProductContainer(lastType, lastWeight, lastMinWeight, lastMaxWeight, lastUnit, lastRealWeight, products));
					}
				}
				cursor.close();
			}
			Log.d("dbhelper", "getProductsOnMass-close");
			db.close();
		}
		return entries;
	}
	
	public int getProductsOnMassSize(String type, Integer weight, String unit) {
		Log.d("dbhelper", "getProductOnMassSize");
		
		int number=0;

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT _id, name, ean, type, weight, unit, quantity FROM onmass_registries WHERE type=? AND weight="+weight+" AND unit=? "
					+ "ORDER BY type, weight, unit", new String[]{type, unit});

			if (cursor != null) {
				number = cursor.getCount();
				cursor.close();
			}
			Log.d("dbhelper", "getProductOnMassSize-close");
			db.close();
		}
		return number;
	}
	
	public List<ProductBarcode> getProductsOnMass(String type, Integer weight, String unit) {
		Log.d("dbhelper", "getProductsOnMass(3args)");

		List<ProductBarcode> products = null;

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT _id, ean, name, type, weight, unit, realWeight, quantity FROM onmass_registries WHERE type=? "
					+ "AND weight="+weight+" AND unit=? ORDER BY type, weight, unit", new String[]{type, unit});

			if (cursor != null) {
				Log.d("getProductsOnMass(3args)", "count: "+cursor.getCount());
				products = new ArrayList<ProductBarcode>();
				if (cursor.moveToFirst()) {
					products = new ArrayList<ProductBarcode>();
					do {
						products.add(new ProductBarcode(cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getInt(4), cursor.getString(5), cursor.getInt(6), cursor.getInt(7)));
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
			Log.d("dbhelper", "getProductsOnMass(3args)-close");
			db.close();
		}
		return products;
	}
	
	public ProductBarcode getProductOnMass(String type, Integer weight, String unit, int index) {
		Log.d("dbhelper", "getProductOnMass");
		
		ProductBarcode product = null;
		int i=0;

		synchronized (object) {
			SQLiteDatabase db = this.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT _id, ean, name, type, weight, unit, quantity FROM onmass_registries WHERE type=? AND weight="+weight+" AND unit=? "
					+ "ORDER BY type, weight, unit", new String[]{type, unit});

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						if (i == index) {
							product = new ProductBarcode(cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getInt(4), cursor.getString(5),
									cursor.getInt(4), cursor.getInt(6));
						}
						i++;
					} while (cursor.moveToNext() && product==null);
				}
				cursor.close();
			}
			Log.d("dbhelper", "getProductOnMass-close");
			db.close();
		}
		return product;
	}
}
