package org.projmis.caritas;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class DeviceVerticle extends Verticle {
	EventBus eb = null;

	@Override
	public void start() {
		System.out.println("Starting " + this.getClass().getName() + " Verticle");
		eb = vertx.eventBus();
		eb.registerHandler(Constants.handlerNameDevices, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(final Message<JsonObject> message) {
				JsonObject messageBody = message.body();
				try {
					String op = messageBody.getString("op");
					if (op.contentEquals("get")) {
						String idDevice = messageBody.getString("idDevice");
			            if (idDevice == null) {
			            	message.reply(new JsonObject().putNumber("httpCode", 500));
			                return;
			            }

			            JsonObject matcher = new JsonObject().putString("idDevice", idDevice);
			            JsonObject json = new JsonObject()
			                    .putString("collection", "devices")
			                    .putString("action", "findone")
			                    .putObject("matcher", matcher);
			            eb.send(Constants.handlerDataBase, json, new Handler<Message<JsonObject>>() {
			                @Override
			                public void handle(Message<JsonObject> messageMongoDB) {
			                    try {
                                    if(!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
			                        	message.reply(new JsonObject().putNumber("httpCode", 500));
			                            return;
			                        }
			                        
			                        JsonObject result = messageMongoDB.body().getObject("result");
			                        if(result==null) {
			                        	message.reply(new JsonObject().putNumber("httpCode", 404));
			                            return;
			                        }
			                        if (!result.getBoolean("isblocked")) {
			                        	message.reply(new JsonObject().putNumber("httpCode", 200));
			                            return;
									}else {
										message.reply(new JsonObject().putNumber("httpCode", 403));
			                            return;
									}
			                    }catch (Exception e) {
			    					Utils.saveException(this.getClass().getName(), e);
			                    	message.reply(new JsonObject().putNumber("httpCode", 500));
			                        return;
			                    }
			                }
			            });			            
			            return;
					}
					if (op.contentEquals("post")) {
						final String idDevice = messageBody.getString("idDevice");
			            if (idDevice == null) {
			            	message.reply(new JsonObject().putNumber("httpCode", 500));
			                return;
			            }
						eb.send(Constants.handlerNameDevices, new JsonObject().putString("op", "get").putString("idDevice", idDevice), new Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> messageDevice) {
								Integer retCode = messageDevice.body().getInteger("httpCode");
								try {
									if (retCode==200) {
										message.reply(new JsonObject().putNumber("httpCode", 200));
	                                    return;
									}
									if (retCode==403) {
										message.reply(new JsonObject().putNumber("httpCode", 403));
	                                    return;
									}
									if (retCode==404) {
										JsonObject doc = new JsonObject()
			                                    .putString("idDevice", idDevice)
			                                    .putBoolean("isblocked", false)
			                                    .putBoolean("ismobile", true)
                                                .putObject("lastLocation", new JsonObject())
			                                    .putArray("locations", new JsonArray())
			                                    .putArray("campaigns", new JsonArray());
			                            JsonObject jsonSaveDevice = new JsonObject()
			                                    .putString("collection", "devices")
			                                    .putString("action", "save")
			                                    .putObject("document", doc);
			                            eb.send(Constants.handlerDataBase, jsonSaveDevice, new Handler<Message<JsonObject>>() {
		
			                                @Override
			                                public void handle(Message<JsonObject> saveMessage) {
			                                    try {
			                                        if(!saveMessage.body().getString("status").equalsIgnoreCase("ok")) {
			                                        	message.reply(new JsonObject().putNumber("httpCode", 500));
			                                            return;
			                                        }
			                                        
			                                        message.reply(new JsonObject().putNumber("httpCode", 201));
				                                    return;
			                                    }catch (Exception e) {
			                    					Utils.saveException(this.getClass().getName(), e);
			                                    	message.reply(new JsonObject().putNumber("httpCode", 500));
			                                        return;
			                                    }
			                                }
			                            });
			                            return;
									}
									message.reply(new JsonObject().putNumber("httpCode", retCode));
					                return;
								} catch (Exception e) {
									Utils.saveException(this.getClass().getName(), e);
									message.reply(new JsonObject().putNumber("httpCode", 500));
                                    return;
								}
							}
						});
						return;
					}
					message.reply(new JsonObject().putNumber("httpCode", 500));
                    return;
				} catch (Exception e) {
					Utils.saveException(this.getClass().getName(), e);
					message.reply(new JsonObject().putNumber("httpCode", 500));
				}
			}
		});
	}

	@Override
	public void stop() {
		eb.unregisterHandler(Constants.handlerNameDevices, null);
	}

}
