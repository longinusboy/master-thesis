package org.projmis.caritas;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.projmis.caritas.data.BarcodeProduct;
import org.projmis.caritas.data.Coordinate;
import org.projmis.caritas.data.IProduct;
import org.projmis.caritas.data.Registry;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.impl.JsonObjectMessage;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class RegistriesProcessor extends Verticle implements Handler<Message<JsonObject>> {

	private EventBus eb;
	private String address = UUID.randomUUID().toString();
	private JsonArray typesJsonArray;

	@Override
	public void start() {
		System.out.println("Starting " + this.getClass().getName() + " Worker" + address);
		eb = vertx.eventBus();
	    eb.registerHandler(address, this);
	    JsonObject msg = new JsonObject().putString("processor", address);
	    eb.send(Constants.handlerNameRegistriesProcessor+".register", msg);
	}

	@Override
	public void stop() {
		JsonObject msg = new JsonObject().putString("processor", address);
	    eb.send(Constants.handlerNameRegistriesProcessor+".unregister", msg);
	    eb.unregisterHandler(address, this);
	}

	@Override
	public void handle(final Message<JsonObject> verticleMessage) {
		/**
		 * Entry point
		 */
		final String idCampaign = verticleMessage.body().getString("idCampaign");
		final String idDevice = verticleMessage.body().getString("idDevice");

		Utils.logProcessor("mainHand", verticleMessage.body().encode());

		eb.send(Constants.handlerNameTypes, new JsonObject().putString("op", "get"), new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> messageTypes) {
				typesJsonArray = new JsonArray(messageTypes.body().getString("httpResponse"));

				/**
				 * Iterate every registries on message and create ProductBarcode
				 * or ProductManual
				 */
				JsonArray newProducts = verticleMessage.body().getArray("newProducts");
				JsonArray jsonRegistries = verticleMessage.body().getArray("registries");
								
				if (newProducts != null) {
					if (newProducts.size()==0) {
						sendDone(eb, verticleMessage);
						return;
					}
					List<BarcodeProduct> newBarcodeProducts = new ArrayList<BarcodeProduct>();
					
					for (int i = 0; i < newProducts.size(); i++) {
						JsonObject newProduct = newProducts.get(i);
						
						BarcodeProduct barcodeProduct = new BarcodeProduct(newProduct.getString("ean"), 0);
						barcodeProduct.setType(newProduct.getString("type"));
						barcodeProduct.setWeight(newProduct.getInteger("weight"));
						barcodeProduct.setUnit(newProduct.getString("unit"));
						barcodeProduct.setRealWeight(newProduct.getInteger("realWeight"));
						barcodeProduct.setName(newProduct.getString("name"));
						newBarcodeProducts.add(barcodeProduct);
					}
					
					JsonObject jsonSearchProduct = new JsonObject()
							.putString("collection", "products")
							.putString("action", "findone")
							.putObject("matcher", new JsonObject().putString("ean", newBarcodeProducts.get(0).getEan()));
					
					eb.send(Constants.handlerDataBase, jsonSearchProduct, new SaverNewBarcodeProducts(new Handler<Message<JsonObject>>() {
						
						@Override
						public void handle(Message<JsonObject> arg0) {
							sendDone(eb, verticleMessage);
						}
					},idDevice, idCampaign, newBarcodeProducts));
				}

				if (jsonRegistries != null) {
					if (jsonRegistries.size()==0) {
						sendDone(eb, verticleMessage);
						return;
					}
					List<Registry> registries = Utils.jsonArrayToRegistries(idDevice, idCampaign, jsonRegistries);
					for (int i = 0; i < registries.size(); i++) {
						final Registry registry = registries.get(i);
						if (registry.hasTowerLocation() && !registry.hasGpsLocation()) {

							resolveLocation(registry.getLocation(), new Handler<Message<JsonObject>>() {

								@Override
								public void handle(Message<JsonObject> responseLocation) {
									Number lon = responseLocation.body().getNumber("lon");
									Number lat = responseLocation.body().getNumber("lat");
									if (lon != null && lat != null) {//TODO guardar tambem a source
										registry.setGpsLocationResolved(new Coordinate(lon.doubleValue(), lat.doubleValue()));
									}
									saveData(idDevice, idCampaign, registry);
								}
								
							});
							
						}else {
							saveData(idDevice, idCampaign, registry);
						}					
					}
				}
				

				sendDone(eb, verticleMessage);
			}
		});

		return;

	}
	
	private void saveData(String idDevice, String idCampaign, Registry registry) {
		saveLocation(idDevice, idCampaign, registry.getDate(), registry.getCompleteLocation());
		savePresetRegistry(idDevice, idCampaign, registry);
		saveNonPresetRegistry(idDevice, idCampaign, registry);
	}

	private void saveLocation(String idDevice, String idCampaign, long dateRegistry, JsonObject location) {
		if (location.getLong("date", 0)==0) {
			location.putNumber("date", dateRegistry);
		}
		eb.send(Constants.handlerNameLocations,
				new JsonObject().putString("op", "post").putString("idDevice", idDevice).putString("idCampaign", idCampaign).putObject("location", location),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						Utils.log(this.getClass().getName(), "saveLocations", message.body().encode());
					}
				});
	}

	private void resolveLocation(JsonObject locationJsonObject, final Handler<Message<JsonObject>> message) {
		eb.sendWithTimeout(Constants.handlerCellIDResolver, locationJsonObject.putString("action", "resolve"), 10000, new Handler<AsyncResult<Message<JsonObject>>>() {
		
			@Override
			public void handle(AsyncResult<Message<JsonObject>> cellIdResolverMessage) {
				if (cellIdResolverMessage.succeeded()) {
					try {
						if (cellIdResolverMessage.result().body().getString("status").contentEquals("ok")) {
							JsonObject response = new JsonObject()
									.putString("status", "ok")
									.putNumber("lon", cellIdResolverMessage.result().body().getNumber("lon").doubleValue())
									.putNumber("lat", cellIdResolverMessage.result().body().getNumber("lat").doubleValue());
							message.handle(new JsonObjectMessage(true, cellIdResolverMessage.result().address(), response));
							return;
						}else {
							JsonObject response = new JsonObject()
									.putString("status", "ok")
									.putString("error", cellIdResolverMessage.result().body().getString("error"));
							message.handle(new JsonObjectMessage(true, cellIdResolverMessage.result().address(), response));
							return;
						}
					} catch (Exception e) {
						Utils.saveException(this.getClass().getName(), e);
						message.handle(new JsonObjectMessage(true, cellIdResolverMessage.result().address(), new JsonObject().putString("status", "error")
								.putString("error", e.getLocalizedMessage())));
					}
				}else {
					message.handle(new JsonObjectMessage(true, cellIdResolverMessage.result().address(), new JsonObject().putString("status", "error")
							.putString("error", "timeout")));
				}
				
			}
		});
	}
	
	private class SaverNewBarcodeProducts implements Handler<Message<JsonObject>> {
		 
        private final Handler<Message<JsonObject>> request;
        private List<BarcodeProduct> products;
        private String idDevice, idCampaign;
 
        private SaverNewBarcodeProducts(final Handler<Message<JsonObject>> request, String idDevice, String idCampaign, List<BarcodeProduct> products) {
            this.request = request;
            this.products = products;
            this.idDevice = idDevice;
            this.idCampaign = idCampaign;
        }
 
        @Override
        public void handle(final Message<JsonObject> event) {
			try {
				JsonObject result = event.body().getObject("result", null);
				
				BarcodeProduct product = products.get(0);
				
				if (result == null || !result.containsField("ean")) {
					JsonObject jsonAddProduct = new JsonObject()
							.putString("collection", "products")
							.putString("action", "save")
							.putObject("document", new JsonObject()
									.putString("ean", product.getEan())
									.putString("type", product.getType())
									.putNumber("weight", product.getWeight())
									.putString("unit", product.getUnit())
									.putNumber("realWeight", product.getRealWeight())
									.putNumber("modified", new Date().getTime())
									.putString("name", product.getName())
									.putString("src", "mobile")
									.putString("idDevice", idDevice)
									.putString("idCampaign", idCampaign));
					eb.send(Constants.handlerDataBase, jsonAddProduct, new Handler<Message<JsonObject>>() {
						
						@Override
						public void handle(Message<JsonObject> arg0) {
							products.remove(0);
							if (products.size()>0) {
								JsonObject jsonSearchProduct = new JsonObject()
										.putString("collection", "products")
										.putString("action", "findone")
										.putObject("matcher", new JsonObject().putString("ean", products.get(0).getEan()));
								
								eb.send(Constants.handlerDataBase, jsonSearchProduct, new SaverNewBarcodeProducts(request, idDevice, idCampaign, products));
							}else {
								request.handle(new JsonObjectMessage(true, arg0.address(), new JsonObject().putString("status", "ok")));
							}
						}
					});
				}else {
					JsonObject jsonUpdateProduct = new JsonObject()
							.putString("collection", "products")
							.putString("action", "update")
							.putObject("criteria", new JsonObject()
									.putString("ean", product.getEan()))
							.putObject("objNew", new JsonObject()
									.putString("ean", product.getEan())
									.putString("type", product.getType())
									.putNumber("weight", product.getWeight())
									.putString("unit", product.getUnit())
									.putNumber("realWeight", product.getRealWeight())
									.putNumber("modified", new Date().getTime())
									.putString("name", product.getName())
									.putString("src", "mobile")
									.putString("idDevice", idDevice)
									.putString("idCampaign", idCampaign));
					eb.send(Constants.handlerDataBase, jsonUpdateProduct);
					
					
					JsonArray conflictOcurrencies = new JsonArray();
					result.removeField("ean");
					result.removeField("_id");
					conflictOcurrencies.add(result);
					conflictOcurrencies.add(new JsonObject()
							.putString("type", product.getType())
							.putNumber("weight", product.getWeight())
							.putString("unit", product.getUnit())
							.putNumber("realWeight", product.getRealWeight())
							.putNumber("modified", new Date().getTime())
							.putString("name", product.getName())
							.putString("src", "mobile")
							.putString("idDevice", idDevice)
							.putString("idCampaign", idCampaign));
					
					
					JsonObject jsonAddConflict = new JsonObject()
					.putString("collection", "products")
					.putString("action", "save")
					.putObject("document", new JsonObject()
							.putString("ean", product.getEan())
							.putNumber("created", new Date().getTime())
							.putArray("ocurrences", conflictOcurrencies));
					
					eb.send(Constants.handlerDataBase, jsonAddConflict, new Handler<Message<JsonObject>>() {
						
						@Override
						public void handle(Message<JsonObject> arg0) {
							products.remove(0);
							if (products.size()>0) {
								JsonObject jsonSearchProduct = new JsonObject()
										.putString("collection", "products")
										.putString("action", "findone")
										.putObject("matcher", new JsonObject().putString("ean", products.get(0).getEan()));
								
								eb.send(Constants.handlerDataBase, jsonSearchProduct, new SaverNewBarcodeProducts(request, idDevice, idCampaign, products));
							}else {
								request.handle(new JsonObjectMessage(true, arg0.address(), new JsonObject().putString("status", "ok")));
							}
						}
					});
								
				}				
			} catch (Exception e) {
				Utils.saveException(this.getClass().getName(), e);
				request.handle(new JsonObjectMessage(true, event.address(), new JsonObject().putString("status", "error")));
			}
		}
    }
	
	private void saveNonPresetRegistry(final String idDevice, final String idCampaign, final Registry registry) {

		/**
		 * Convert message jsonObject into Json Objects Array for bulk insert
		 */

		Utils.logProcessor("saveNonPresetRegistry", registry.toString());

		if (registry.isPreSet()) {
			return;
		}
		
		List<BarcodeProduct> newBarcodeProducts = extractNewProducts(registry.getProducts());
		if (newBarcodeProducts.size()>0) {
			JsonObject jsonSearchProduct = new JsonObject()
					.putString("collection", "products")
					.putString("action", "findone")
					.putObject("matcher", new JsonObject().putString("ean", newBarcodeProducts.get(0).getEan()));
			
			eb.send(Constants.handlerDataBase, jsonSearchProduct, new SaverNewBarcodeProducts(new Handler<Message<JsonObject>>() {
				
				@Override
				public void handle(Message<JsonObject> arg0) {
					saveNonPresetRegistryCont(idDevice, idCampaign, registry);
				}
			},idDevice, idCampaign, newBarcodeProducts));
		}
		saveNonPresetRegistryCont(idDevice, idCampaign, registry);
	}
		
	
	private void saveNonPresetRegistryCont(final String idDevice, final String idCampaign, final Registry registry) {
		Utils.logProcessor("saveNonPresetRegistryCont", registry.toString());
		resolveBarcodes(registry.getProducts(), new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> resolverResponse) {
				String status = resolverResponse.body().getString("status");
				if (status != null && (status.contentEquals("ok") || status.contentEquals("empty") || status.contentEquals("empty-matchersArray"))) {

					saveSingleRegistries(registry.getIdCampaign(), registry.getIdDevice(), registry.getDate(), registry.getLocation(), registry.getProducts());
					saveUnknownProducts(registry.getIdCampaign(), registry.getIdDevice(), registry.getLocation(), registry.getProducts());

					//Registries bulk insert
					JsonObject jsonBulkInsert = new JsonObject().putString("action", "save").putString("collection", "registries")
							.putObject("document", registry.toJsonObject());
					Utils.logProcessor("nonpreset bulkins registries", jsonBulkInsert.encode());
					eb.send(Constants.handlerDataBase, jsonBulkInsert);

					Map<String, Long> typesIncrement = calculateIncrements(registry.getProducts());
					
					for (Entry<String, Long> entry : typesIncrement.entrySet()) {
						Utils.logProcessor("NonPreset Increments", entry.getKey()+"->"+entry.getValue());
					}

					incrementCampaignStats(idCampaign, typesIncrement, registry.getProducts());
					incrementDeviceStats(idDevice, idCampaign, typesIncrement, registry.getProducts());
					incrementTypes(idCampaign, typesIncrement);
					incrementDetailedTypes(idCampaign, new Date(registry.getDate()), typesIncrement);
				}
			}
		});
	}

	private void savePresetRegistry(final String idDevice, final String idCampaign, final Registry registry) {

		/**
		 * Convert message jsonObject into Json Objects Array for bulk insert
		 */

		Utils.logProcessor("savePresetRegistry", registry.toString());
		
		if (!registry.isPreSet()) {
			return;
		}

		if (registry.getType() == null || registry.getWeight() == null || registry.getUnit() == null || registry.getRealWeight() == null || registry.getProducts().isEmpty()) {
			Utils.logProcessor("savePresetRegistry", "Some field are null: " + registry.toString());
			return;
		}

		resolveBarcodes(registry.getProducts(), new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> arg0) {

				List<BarcodeProduct> conflicts = new ArrayList<BarcodeProduct>();
				List<BarcodeProduct> newProducts = new ArrayList<BarcodeProduct>();
				List<IProduct> registryProducts = new ArrayList<IProduct>();

				String type = registry.getType();
				Integer weight = registry.getWeight();
				String unit = registry.getUnit();
				Integer minWeight = registry.getMinWeight();
				Integer maxWeight = registry.getMaxWeight();
				Integer realWeight = registry.getRealWeight();
				

				for (int j = 0; j < registry.getProducts().size(); j++) {
					BarcodeProduct barcodeProduct;
					if (registry.getProducts().get(j) instanceof BarcodeProduct) {
						barcodeProduct = (BarcodeProduct) registry.getProducts().get(j);
					} else {
						continue;
					}

					if (barcodeProduct.getType() == null) {
						barcodeProduct.setType(type);
						barcodeProduct.setWeight(weight);
						barcodeProduct.setUnit(unit);
						barcodeProduct.setRealWeight(realWeight);
						newProducts.add(barcodeProduct);
					} else {
						if (!barcodeProduct.getType().contentEquals(type) || (barcodeProduct.getWeight() != weight && !registry.hasMinMaxWeight())
								|| !barcodeProduct.getUnit().contentEquals(unit)) {
							barcodeProduct.setInConflic(true);
							barcodeProduct.setType(null);
							barcodeProduct.setWeight(null);
							barcodeProduct.setUnit(null);
						}
						if (registry.hasMinMaxWeight()) {
							if (barcodeProduct.getWeight() < minWeight || barcodeProduct.getWeight() > maxWeight) {
								barcodeProduct.setInConflic(true);
								barcodeProduct.setType(null);
								barcodeProduct.setWeight(null);
								barcodeProduct.setUnit(null);
							}
						}
						if (barcodeProduct.isInConflic()) {
							conflicts.add(new BarcodeProduct(barcodeProduct));
						}
					}
					registryProducts.add(barcodeProduct);
				}

				String typeName = generateName(type, weight, unit, typesJsonArray);

				for (int j = 0; j < newProducts.size(); j++) {
					BarcodeProduct newProduct = newProducts.get(j);
					addProduct(newProduct.getEan(), typeName, type, weight, unit, minWeight, maxWeight, realWeight, idDevice);
				}

				for (int j = 0; j < conflicts.size(); j++) {
					BarcodeProduct conflict = conflicts.get(j);
					addConflict(conflict.getEan(), typeName, type, weight, unit, idDevice);
				}

				saveSingleRegistries(registry.getIdCampaign(), registry.getIdDevice(), registry.getDate(), registry.getLocation(), registryProducts);

				//Registries bulk insert
				JsonObject jsonBulkInsert = new JsonObject().putString("action", "save").putString("collection", "registries")
						.putObject("document", registry.toJsonObject());
				Utils.logProcessor("preset bulkins registries", jsonBulkInsert.encode());
				eb.send(Constants.handlerDataBase, jsonBulkInsert);
				
				Map<String, Long> typesIncrement = calculateIncrements(registryProducts);
				
				for (Entry<String, Long> entry : typesIncrement.entrySet()) {
					Utils.logProcessor("Preset Increments", entry.getKey()+"->"+entry.getValue());
				}

				incrementCampaignStats(idCampaign, typesIncrement, registryProducts);
				incrementDeviceStats(idDevice, idCampaign, typesIncrement, registryProducts);
				incrementTypes(idCampaign, typesIncrement);
				incrementDetailedTypes(idCampaign, new Date(registry.getDate()), typesIncrement);
			}
		});
	}
	
	private List<BarcodeProduct> extractNewProducts(List<IProduct> products) {
		 List<BarcodeProduct> newProducts = new ArrayList<BarcodeProduct>();
		
		if (products == null) {
			return newProducts;
		}
		for (int i = 0; i < products.size(); i++) {
    		if (products.get(i) instanceof BarcodeProduct) {
    			BarcodeProduct product = (BarcodeProduct)products.get(i);
    			if (product.getType()!=null && product.getUnit()!=null && product.getWeight()!=null && product.getRealWeight()!=null) {
					newProducts.add(product);
				}
			}        		
		}
		
		return newProducts;
	}

	private void resolveBarcodes(final List<IProduct> products, final Handler<Message<JsonObject>> message) {
		if (products == null) {
			return;
		}
		if (products.size()>0) {
            /**
             * Obtain type, weight and unit from every ProductBarcode
             */
        	JsonArray matchersArray = new JsonArray();
        	for (int i = 0; i < products.size(); i++) {
        		if (products.get(i) instanceof BarcodeProduct) {
        			matchersArray.addObject(new JsonObject().putString("ean", ((BarcodeProduct)products.get(i)).getEan()));
				}        		
			}
        	
        	if (matchersArray.size()<=0) {
        		Utils.logProcessor("search", "empty-matchersArray");
    			message.handle(new JsonObjectMessage(true, address, new JsonObject().putString("status", "empty-matchersArray")));
				return;
			}

    		JsonObject matcher = new JsonObject().putArray("$or", matchersArray);
            JsonObject json = new JsonObject()
                    .putString("collection", "products")
                    .putString("action", "find")
                    .putObject("matcher", matcher);
            Utils.logProcessor("search barcodes", json.encode());

            eb.send(Constants.handlerDataBase, json, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> mongoProductsSearchMessage) {
                	try {
                		JsonArray jsonResults = mongoProductsSearchMessage.body().getArray("results");
                    	for (int i = 0; i < jsonResults.size(); i++) {
    						JsonObject jsonResult = jsonResults.get(i);
    						for (int j = 0; j < products.size(); j++) {
    							if (products.get(j) instanceof BarcodeProduct) {
	    							BarcodeProduct barcodeProduct = (BarcodeProduct) products.get(j);
	    							if(barcodeProduct.getEan().contentEquals(jsonResult.getString("ean"))) {
	    								barcodeProduct.setType(jsonResult.getString("type"));
	    								barcodeProduct.setWeight(jsonResult.getInteger("weight"));
	    								barcodeProduct.setUnit(jsonResult.getString("unit"));
	    								barcodeProduct.setRealWeight(jsonResult.getInteger("realWeight"));
	    								break;
	    							}
    							}
    						}
    					}
                    	message.handle(new JsonObjectMessage(true, mongoProductsSearchMessage.address(), new JsonObject().putString("status", "ok")));
					} catch (Exception e) {
						Utils.saveException(this.getClass().getName(), e);
						message.handle(new JsonObjectMessage(true, mongoProductsSearchMessage.address(), new JsonObject().putString("status", "error")
								.putString("error", e.getLocalizedMessage())));
					}              	
                }
    		});
		}else {
			Utils.logProcessor("search", "empty");
			message.handle(new JsonObjectMessage(true, address, new JsonObject().putString("status", "empty")));
		}
	}


    private void sendDone(EventBus eb, Message<JsonObject> verticleMessage) {
        verticleMessage.reply();
        String replyAddressDone = verticleMessage.body().getString("done-reply");
        if (replyAddressDone!=null) {
            eb.send(replyAddressDone, new JsonObject().putString("status", "ok"));
        }
    }        
    
    /**
     * Prepare types to increment
     */
	private Map<String, Long> calculateIncrements(List<IProduct> products) {
		Map<String, Long> typesIncrement = new HashMap<String, Long>();

		for (IProduct product : products) {
			try {
				if (product.getType() == null || product.getWeight() == null || product.getUnit() == null || product.getRealWeight() == null) {
					continue;
				}
				
				String mainType = product.getType().split("-")[0];
				Long realValue = typesIncrement.get(mainType);
				if (realValue == null) {
					realValue = 0L;
				}
				realValue += product.getQuantity() * product.getRealWeight();
				typesIncrement.put(mainType, realValue);
				
				Long value = typesIncrement.get(product.getType());
				if (value == null) {
					value = 0L;
				}
				value += product.getQuantity() * product.getWeight();
				typesIncrement.put(product.getType(), value);
			} catch (Exception e) {
				Utils.saveException(this.getClass().getName(), e);
			}			
		}
		return typesIncrement;
	}	
	
	private void incrementCampaignStats(String idCampaign, Map<String, Long> typesIncrement, List<IProduct> registryProducts) {
		long incrementWeight = 0;
		long incrementRegistries = 0;
		
		for (Entry<String, Long> entry : typesIncrement.entrySet()) {
			incrementWeight+=entry.getValue();
		}
		
		for (IProduct product : registryProducts) {
			incrementRegistries = product.getQuantity();
		}
		
		JsonObject inc = new JsonObject().putObject("$inc", new JsonObject().putNumber("sumWeights", incrementWeight).putNumber("sumRegistries", incrementRegistries));
        JsonObject jsonUpdateCampaignStats = new JsonObject()
        		.putString("action", "update")
                .putString("collection", "campaigns")
                .putObject("criteria", new JsonObject().putString("idCampaign", idCampaign))
                .putObject("objNew", inc);

        Utils.logProcessor("update campaign stats", jsonUpdateCampaignStats.encode());

        eb.send(Constants.handlerDataBase, jsonUpdateCampaignStats);	
	}
	
	private void incrementDeviceStats(String idDevice, String idCampaign, Map<String, Long> typesIncrement, List<IProduct> registryProducts) {
		long incrementWeight = 0;
		long incrementRegistries = 0;
		
		for (Entry<String, Long> entry : typesIncrement.entrySet()) {
			incrementWeight+=entry.getValue();
		}
		
		for (IProduct product : registryProducts) {
			incrementRegistries = product.getQuantity();
		}
		
		JsonObject inc = new JsonObject().putObject("$inc", new JsonObject().putNumber("sumWeights", incrementWeight).putNumber("sumRegistries", incrementRegistries));
        JsonObject jsonUpdatePersonalStats = new JsonObject()
        		.putString("action", "update")
                .putString("collection", "personalStats")
                .putObject("criteria", new JsonObject().putString("idCampaign", idCampaign).putString("idDevice", idDevice))
                .putObject("objNew", inc);

        Utils.logProcessor("update personal stats", jsonUpdatePersonalStats.encode());

        eb.send(Constants.handlerDataBase, jsonUpdatePersonalStats);	
	}
    
    /**
     * increment quantity if it exist
     * it will not exist if not a single one product can be resolved
     */
    private void incrementTypes(String idCampaign, Map<String, Long> typesIncrement) {
    	if (typesIncrement.size()>0) {
			JsonObject updateValues = new JsonObject();
			
			for (Entry<String, Long> entry : typesIncrement.entrySet()) {
				updateValues.putNumber("stats."+entry.getKey(), entry.getValue());
			}
			
			JsonObject inc = new JsonObject().putObject("$inc", updateValues);
	        JsonObject jsonUpdateStats = new JsonObject()
	        		.putString("action", "update")
	                .putString("collection", "campaigns")
	                .putObject("criteria", new JsonObject().putString("idCampaign", idCampaign))
	                .putObject("objNew", inc);

	        Utils.logProcessor("update types", jsonUpdateStats.encode());

	        eb.send(Constants.handlerDataBase, jsonUpdateStats);	
		}else {
			Utils.logProcessor("update types", "No increments");
		}
	}

    private void incrementDetailedTypes(String idCampaign, Date date, Map<String, Long> typesIncrement) {
    	if (typesIncrement.size()>0) {
			JsonObject updateValues = new JsonObject();
			
			//String timeSlotName = generateQuarterName(date);
			String timeSlotName = generateHourName(date);
			
			Utils.log(this.getClass().getName(), "generateHourName", timeSlotName);
			
			for (Entry<String, Long> entry : typesIncrement.entrySet()) {
				updateValues.putNumber("detailedStats."+timeSlotName+"."+entry.getKey(), entry.getValue());
			}
			
			JsonObject inc = new JsonObject().putObject("$inc", updateValues);
	        JsonObject jsonUpdateStats = new JsonObject()
	        		.putString("action", "update")
	                .putString("collection", "campaigns")
	                .putObject("criteria", new JsonObject().putString("idCampaign", idCampaign))
	                .putObject("objNew", inc);

	        Utils.logProcessor("incrementDetailedTypes", jsonUpdateStats.encode());

	        eb.send(Constants.handlerDataBase, jsonUpdateStats);	
		}else {
			Utils.logProcessor("incrementDetailedTypes", "No increments");
		}
	}
    
    private void saveSingleRegistries(String idCampaign, String idDevice, long date, JsonObject location, List<IProduct> products) {    	
    	
    	JsonArray jsonDocuments = new JsonArray();
        for (int i = 0; i < products.size(); i++) {
            IProduct product = products.get(i);
            JsonObject productJsonObject = product.toJsonObject().putNumber("date", date).putString("idDevice", idDevice)
                    .putString("idCampaign", idCampaign);
            
            if (location != null) {
            	productJsonObject.putValue("location", location);
			}
            jsonDocuments.add(productJsonObject);
        }

        JsonObject jsonBulkInsert = new JsonObject()
                .putString("action", "command")
                .putString("command", new JsonObject()
                                .putString("insert", "registriesSingle")
                                .putArray("documents", jsonDocuments).encode()
                );
        Utils.logProcessor("saveSingleRegistries bulkins", jsonBulkInsert.encode());

        eb.send(Constants.handlerDataBase, jsonBulkInsert);
    }
    
    private void saveUnknownProducts(final String idCampaign, final String idDevice, JsonObject location, final List<IProduct> products) {    	
    	
		JsonArray jsonDocuments = new JsonArray();
		for (int i = 0; i < products.size(); i++) {
			IProduct product = products.get(i);
			if (product.getType() != null) {
				continue;
			}
			JsonObject productJsonObject = product.toJsonObject().putString("idDevice", idDevice).putString("idCampaign", idCampaign);

			if (location != null) {
				productJsonObject.putValue("location", location);
			}
			jsonDocuments.add(productJsonObject);
		}
		
		if (jsonDocuments.size()==0) {
			return;
		}

        JsonObject jsonBulkInsert = new JsonObject()
                .putString("action", "command")
                .putString("command", new JsonObject()
                                .putString("insert", "unknownProducts")
                                .putArray("documents", jsonDocuments).encode()
                );
        Utils.logProcessor("saveUnknowProducts bulkins", jsonBulkInsert.encode());

        eb.send(Constants.handlerDataBase, jsonBulkInsert);
    }
    
    private String generateName(String type, Integer weight, String unit, JsonArray typesJsonArray) {
		String typeMain = type.split("-")[0];
		int typeSecondary = Integer.valueOf(type.split("-")[1]);
		
		for (int i = 0; i < typesJsonArray.size(); i++) {
			JsonObject typeObject = typesJsonArray.get(i);
			if (typeObject.getString("symbol").contentEquals(typeMain)) {
				JsonArray subtypesArray = typeObject.getArray("subtypes");
				for (int j = 0; j < subtypesArray.size(); j++) {
					JsonObject subtypeObject = subtypesArray.get(j);
					if (subtypeObject.getInteger("id").equals(typeSecondary)) {
						return subtypeObject.getString("name");
					}
				}
			}
		}
		return null;
	}
    
    /*private String generateQuarterName(Date date) {
		
    	Calendar cal1 = Calendar.getInstance();
    	cal1.setTime(date);

    	int unroundedMinutes1 = cal1.get(Calendar.MINUTE);
    	int mod1 = unroundedMinutes1 % 15;
    	cal1.add(Calendar.MINUTE, -mod1);
    	cal1.set(Calendar.SECOND, 0);
    	cal1.set(Calendar.MILLISECOND, 0);
    	
    	Calendar cal2 = Calendar.getInstance();
    	cal2.setTime(date);

    	int unroundedMinutes2 = cal2.get(Calendar.MINUTE);
    	int mod2 = unroundedMinutes2 % 15;
    	cal2.add(Calendar.MINUTE, 15-mod2);
    	cal2.set(Calendar.SECOND, 0);
    	cal2.set(Calendar.MILLISECOND, 0);
    	
    	return String.valueOf(cal1.getTime().getTime())+"-"+String.valueOf(cal2.getTime().getTime());
	}*/
    
    private String generateHourName(Date date) {
		
    	Calendar cal1 = Calendar.getInstance();
    	cal1.setTime(date);

    	//from XX:00:00,000
    	cal1.set(Calendar.MINUTE, 0);
    	cal1.set(Calendar.SECOND, 0);
    	cal1.set(Calendar.MILLISECOND, 0);
    	
    	Calendar cal2 = Calendar.getInstance();
    	cal2.setTime(date);

    	//to XX:59:59,999
    	cal2.add(Calendar.HOUR, 1);
    	cal2.set(Calendar.MINUTE, 0);
    	cal2.set(Calendar.SECOND, 0);
    	cal2.set(Calendar.MILLISECOND, -1);
    	
    	return String.valueOf(cal1.getTime().getTime())+"-"+String.valueOf(cal2.getTime().getTime());
	}
        
    protected void addConflict(final String ean, final String typeName, final String type, final Integer weight, final String unit, final String idDevice) {
				
		JsonObject findConflictReportEntryQuery = new JsonObject()
                .putString("collection", "productConflicts")
                .putString("action", "findone")
                .putObject("matcher", new JsonObject().putString("ean", ean));
		
		Utils.logProcessor("findConflictReportEntryQuery", findConflictReportEntryQuery.encode());
        
        eb.send(Constants.handlerDataBase, findConflictReportEntryQuery, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> messageMongoDB) {
                try {
                    if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
                    	Utils.logProcessor("addConflict", "findConflictReportEntryQuery", messageMongoDB.body().encode());
                    	return;
                    }

					if (messageMongoDB.body().getObject("result") == null) {
						//add new conflict (first instance)
						
						JsonObject matcher = new JsonObject()
								.putString("ean", ean)
								.putString("type", type)
								.putNumber("weight", weight)
								.putString("unit", unit);
						
						JsonObject findFirstInstanceProductJson = new JsonObject()
				                .putString("collection", "products")
				                .putString("action", "findone")
				                .putObject("matcher", matcher);
						
						Utils.logProcessor("findFirstInstanceProductJson", findFirstInstanceProductJson.encode());
				        
				        eb.send(Constants.handlerDataBase, findFirstInstanceProductJson, new Handler<Message<JsonObject>>() {
				            @Override
				            public void handle(Message<JsonObject> messageMongoDB) {
				                try {
				                    if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
				                    	Utils.logProcessor("addConflict", "findFirstInstanceProductJson", messageMongoDB.body().encode());
				                    	return;
				                    }

				                    JsonObject firstInstanceProduct = messageMongoDB.body().getObject("result");
				                    firstInstanceProduct.removeField("_id");
				                    firstInstanceProduct.removeField("ean");
									
				                    JsonArray entries = new JsonArray();
				                    entries.add(firstInstanceProduct);
				                    entries.add(new JsonObject()
											.putString("type", type)
											.putNumber("weight", weight)
											.putString("unit", unit)
											.putNumber("modified", new Date().getTime())
											.putString("name", typeName)
											.putString("owner", idDevice));
				                    
				                    JsonObject document = new JsonObject()
				                    		.putString("ean", ean)
				                    		.putBoolean("resolved", false)
				                    		.putArray("entries", entries);
				                    
				                    JsonObject addFirstDuplicateJsonObject = new JsonObject()
							        		.putString("action", "save")
									        .putString("collection", "productConflicts")
									        .putObject("document", document);
				                    
				                    Utils.logProcessor("addFirstDuplicateJsonObject", addFirstDuplicateJsonObject.encode());
				                    
				                    eb.send(Constants.handlerDataBase, addFirstDuplicateJsonObject);
								} catch (Exception e) {
									Utils.saveException(this.getClass().getName(), e);
									return;
								}
				            }
				        });
				        
					} else {
						//update
						JsonObject newDuplicate = new JsonObject()
								.putString("type", type)
								.putNumber("weight", weight)
								.putString("unit", unit)
								.putNumber("modified", new Date().getTime())
								.putString("name", typeName)
								.putString("owner", idDevice);
						
						JsonObject objUpdateEntries = new JsonObject().putObject("$push", new JsonObject()
                        		.putObject("entries", newDuplicate));
						
						JsonObject updateJsonObject = new JsonObject()
						        .putString("collection", "productConflicts")
						        .putString("action", "update")
						        .putObject("document", objUpdateEntries);
						
						eb.send(Constants.handlerDataBase, updateJsonObject);
					}
				} catch (Exception e) {
					Utils.saveException(this.getClass().getName(), e);
					return;
				}
            }
        });
	}

	protected void addProduct(String ean, String typeName, String type, Integer weight, String unit, Integer minWeight, Integer maxWeight, Integer realWeight, String idDevice) {
		JsonObject document = new JsonObject()
				.putString("ean", ean)
				.putString("type", type)
				.putNumber("weight", weight)
				.putString("unit", unit)
				.putNumber("minWeight", minWeight)
				.putNumber("maxWeight", maxWeight)
				.putNumber("realWeight", realWeight)
				.putNumber("modified", new Date().getTime())
				.putString("name", typeName)
				.putString("owner", idDevice);
		
		JsonObject addProductQuery = new JsonObject()
        		.putString("action", "save")
		        .putString("collection", "products")
		        .putObject("document", document);

		Utils.logProcessor("addProductQuery", addProductQuery.encode());
		
		eb.send(Constants.handlerDataBase, addProductQuery);			
	}

    
}
