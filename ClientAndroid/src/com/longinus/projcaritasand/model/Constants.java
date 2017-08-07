package com.longinus.projcaritasand.model;

public class Constants {
	public final static boolean DEBUG_MODE = true;
	
	public final static int CONECTION_STATUS_OK = 0;
	public final static int CONECTION_STATUS_ERROR = 1;
	public final static int CONECTION_STATUS_ASKING = 2;
	public final static int CONECTION_STATUS_DELAY = 3;
	public final static int CONECTION_STATUS_UNKNOWN = 4;
	
	public final static int DEVICE_STATUS_OK = 0;
	public final static int DEVICE_STATUS_REGISTERED = 1;
	public final static int DEVICE_STATUS_BLOCKED = 2;
	public final static int DEVICE_STATUS_NOTFOUND = 3;
	public final static int DEVICE_STATUS_INITIALIZING = 4;
	public final static int DEVICE_STATUS_SEARCHING_SERVERS = 5;
	public final static int DEVICE_STATUS_FOUND_SERVER = 6;
	public final static int DEVICE_STATUS_SERVER_NOT_FOUND = 7;
	public final static int DEVICE_STATUS_REGISTERING_DEVICE = 8;
	public final static int DEVICE_STATUS_UPDATING_LOCAL_DATABASE = 9;
	public final static int DEVICE_STATUS_UNKNOWN = 10;
	public final static int DEVICE_STATUS_READY = 11;
	public final static int DEVICE_STATUS_NO_NETWORK = 12;
	public final static int DEVICE_STATUS_FATAL_ERROR = 20;
	
	public final static int PING_CODE_OK = 200;
	
	public final static int DEVICE_CODE_OK = 200;
	public final static int DEVICE_CODE_REGISTERED = 201;
	public final static int DEVICE_CODE_BLOCKED = 403;
	public final static int DEVICE_CODE_NOTFOUND = 404;
	public final static int DEVICE_CODE_OTHER_ERROR = 500;
	
	public final static int NEW_PRODUCT_CODE_OK = 200;
	public final static int NEW_PRODUCT_CODE_OTHER_ERROR = 500;
	
	public final static int STATS_CODE_OK = 200;
	public final static int STATS_CODE_OTHER_ERROR = 500;
	
	public final static int REGISTER_PRODUCT_CODE_OK = 200;
	public final static int REGISTER_PRODUCT_CODE_INVALID_CONTENT = 400;
	public final static int REGISTER_PRODUCT_CODE_BLOCKED = 403;
	public final static int REGISTER_PRODUCT_CODE_OTHER_ERROR = 500;
	
	public final static int REGISTER_CAMPAIGN_OK = 0;
	public final static int REGISTER_CAMPAIGN_WRONG_PASSWORD = 1;
	public final static int REGISTER_CAMPAIGN_ERROR = 2;
	
	public final static int REGISTER_CAMPAIGN_CODE_OK = 200;
	public final static int REGISTER_CAMPAIGN_CODE_REGISTERED = 201;
	public final static int REGISTER_CAMPAIGN_CODE_PASSWORD_MISMATCH = 401;
	public final static int REGISTER_CAMPAIGN_CODE_NOTFOUND = 412;
	public final static int REGISTER_CAMPAIGN_CODE_INVALID_CONTENT = 400;
	public final static int REGISTER_CAMPAIGN_CODE_BLOCKED = 403;
	public final static int REGISTER_CAMPAIGN_CODE_OTHER_ERROR = 500;
	
	public final static String[] serversList  = new String[] {
		//"http://192.168.2.10:8080",
		"http://kenobi.dei.uc.pt:8080",
		"http://kenobi.dei.uc.pt:8080",
		"http://projectos.isec.pt/deis/mis/a21170222",
		
		//"http://10.65.131.71:8080",
		//"http://192.168.2.7:8080", 
		//"http://10.65.131.71:8080",
		//"http://10.65.131.71:8080",
		//"http://192.168.2.10:8080",
		//"http://10.202.192.36:8080"
	};
	
	public final static String SERVER_URL_CAMPAIGNS = "/caritas/campaigns/";
	public final static String SERVER_URL_PRODUCTS = "/caritas/products/";
	public final static String SERVER_URL_REGISTRIES = "/caritas/registries/";
	public static final String SERVER_URL_STARTUP = "/caritas/startup/";
	public final static String SERVER_URL_STATISTICS = "/caritas/stats/";
	
	public final static int UPLOAD_THREAD_LONG_WAIT = 10;
	public final static int UPLOAD_THREAD_SHORT_WAIT = 5;
	
	public final static int GPS_LISTENER_INTERVAL = 3;
	public final static long MAX_TIME_LOCATION = 1000*60*15;
	
	public final static String[] monthNamesWithPrefix = new String[] {
		" de Janeiro",
		" de Fevereiro",
		" de Março",
		" de Abril",
		" de Maio",
		" de Junho",
		" de Julho",
		" de Agosto",
		" de Setembro",
		" de Outubro",
		" de Novembro",
		" de Dezembro"
	};

}
