package org.projmis.evida;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.projmis.caritas.data.BarcodeProduct;
import org.projmis.caritas.data.IProduct;
import org.projmis.caritas.data.ManualProduct;
import org.projmis.caritas.data.Registry;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.impl.JsonObjectMessage;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class Utils {
	private static Object object = new Object();

	public static void isDeviceUnblocked(EventBus eb, String idDevice, final Handler<Message<JsonObject>> message) {
		eb.send("projcaritas.device", new JsonObject().putString("op", "get").putString("idDevice", idDevice), new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> messageDevice) {
				Integer retCode = messageDevice.body().getInteger("httpCode");
				try {
					if (retCode == 200) {
						message.handle(new JsonObjectMessage(true, messageDevice.address(), new JsonObject().putString("status", "ok")));
					} else {
						message.handle(new JsonObjectMessage(true, messageDevice.address(), new JsonObject().putString("status", "error").putNumber("error", retCode)));
					}
				} catch (Exception e) {
					e.printStackTrace();
					message.handle(new JsonObjectMessage(true, messageDevice.address(), new JsonObject().putString("status", "error").putNumber("error", 500)
							.putString("errorDescription", "httpCode not defined")));
				}
			}
		});
	}

	public static List<Registry> recreateRegistries(String idDevice, JsonObject contentJsonObject) {

		List<Registry> registries = new ArrayList<Registry>();

		String idCampaign = contentJsonObject.getString("idCampaign");

		JsonArray jsonRegistries = contentJsonObject.getArray("registries", new JsonArray());

		for (int i = 0; i < jsonRegistries.size(); i++) {
			Registry registry;
			JsonObject jsonRegistry = jsonRegistries.get(i);
			JsonArray jsonProducts = jsonRegistry.getArray("products");
			List<IProduct> products = new ArrayList<IProduct>();
			for (int j = 0; j < jsonProducts.size(); j++) {
				JsonObject jsonProduct = jsonProducts.get(j);
				IProduct product = null;
				if (jsonProduct.containsField("ean")) {
					product = new BarcodeProduct(jsonProduct.getString("ean"), jsonProduct.getInteger("quantity"));
				} else {
					product = new ManualProduct(jsonProduct.getString("type"), jsonProduct.getInteger("weight"), jsonProduct.getString("unit"),
							jsonProduct.getInteger("realWeight"), jsonProduct.getInteger("quantity"));
				}
				products.add(product);
			}

			JsonObject loc = jsonRegistry.getObject("location");

			Boolean massInput = jsonRegistry.getBoolean("massInput");

			if (massInput != null && massInput) {
				String type = jsonRegistry.getString("type");
				Integer weight = (Integer) jsonRegistry.getNumber("weight");
				String unit = jsonRegistry.getString("unit");
				Integer minWeight = (Integer) jsonRegistry.getNumber("minWeight");
				Integer maxWeight = (Integer) jsonRegistry.getNumber("maxWeight");
				Integer realWeight = (Integer) jsonRegistry.getNumber("realWeight");

				registry = new Registry(idCampaign, idDevice, loc, type, weight, unit, minWeight, maxWeight, realWeight, jsonRegistry.getLong("date"), products);
			} else {
				registry = new Registry(idCampaign, idDevice, loc, jsonRegistry.getLong("date"), products);
			}

			registries.add(registry);
		}
		return registries;
	}

	public static JsonArray registriesToJsonArray(List<Registry> registries) {
		JsonArray jsonArray = new JsonArray();

		for (int i = 0; i < registries.size(); i++) {
			jsonArray.addObject(registries.get(i).toJsonObject());
		}

		return jsonArray;
	}

	public static List<Registry> jsonArrayToRegistries(String idDevice, String idCampaign, JsonArray registriesJsonArray) {
		JsonObject contentJsonObject = new JsonObject().putString("idCampaign", idCampaign).putArray("registries", registriesJsonArray);
		return recreateRegistries(idDevice, contentJsonObject);
	}
	
	public static void logProcessor(String place, String... message) {
		try {
			PrintWriter writer = new PrintWriter(new FileOutputStream(new File("evida-log-processor.txt"), true));
			writer.append("" + new Date() + "\t" + place);
			for (int i = 0; i < message.length; i++) {
				writer.append("\t" + message[i]);
			}
			writer.append("\n");
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void log(String place, String... message) {
		try {
			PrintWriter writer = new PrintWriter(new FileOutputStream(new File("evida-log.txt"), true));
			writer.append("" + new Date() + "\t" + place);
			for (int i = 0; i < message.length; i++) {
				writer.append("\t" + message[i]);
			}
			writer.append("\n");
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void saveException(String local, Exception exception) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		exception.printStackTrace(pw);
		String exceptionString = sw.toString();
		pw.close();
		try {
			sw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		synchronized (object) {
			try {
				PrintWriter writer = new PrintWriter(new FileOutputStream(new File("evida-exceptions.txt"), true));
				writer.append(String.valueOf(new Date()) + "\t" + local + "\t" + exceptionString + "\n");
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void saveException(String local, Throwable throwable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		String exceptionString = sw.toString();
		pw.close();
		try {
			sw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		synchronized (object) {
			try {
				PrintWriter writer = new PrintWriter(new FileOutputStream(new File("evida-exceptions.txt"), true));
				writer.append(String.valueOf(new Date()) + "\t" + local + "\t" + exceptionString + "\n");
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void saveException(String local, String exceptionString) {
		synchronized (object) {
			try {
				PrintWriter writer = new PrintWriter(new FileOutputStream(new File("evida-exceptions.txt"), true));
				writer.append(String.valueOf(new Date()) + "\t" + local + "\t" + exceptionString + "\n");
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String exceptionToString(Exception exception) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		exception.printStackTrace(pw);
		String exceptionString = sw.toString();
		pw.close();
		return exceptionString;
	}
}
