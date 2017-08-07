package org.projmis.demo;

import java.util.List;
import java.util.UUID;

import org.projmis.caritas.data.Registry;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class RegistryVerticle extends Verticle {
	EventBus eb = null;

	@Override
	public void start() {
		System.out.println("Starting " + this.getClass().getName() + " Verticle");
		eb = vertx.eventBus();
		eb.registerHandler(Constants.handlerNameRegistries, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(final Message<JsonObject> message) {
				final JsonObject messageBody = message.body();
				try {
					String op = messageBody.getString("op");
					if (op.contentEquals("post")) {
						final String idDevice = messageBody.getString("idDevice");
						final String content = messageBody.getString("content");
						
			            if (idDevice == null) {
			            	message.reply(new JsonObject().putNumber("httpCode", 500));
			                return;
			            }
			            
			            if (content==null) {
			            	message.reply(new JsonObject().putNumber("httpCode", 400));
			                return;
			            }
			            
			            Utils.isDeviceUnblocked(eb, idDevice, new Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> messageDevice) {
								try {
									if (!messageDevice.body().getString("status").contentEquals("ok")) {
										Utils.log(this.getClass().getName(), "Utils.isDeviceUnblocked", messageDevice.body().encode());
										message.reply(new JsonObject().putNumber("httpCode", 403));
						                return;
									}
									
									List<Registry> registries = null;
									JsonObject jsonContentObject = null;
									JsonArray newProducts = null;
									try {
										jsonContentObject = new JsonObject(content);
			                        	
			                        	registries = Utils.recreateRegistries(idDevice, jsonContentObject);
			                        	
			                        	newProducts = jsonContentObject.getArray("newProducts", new JsonArray());
									} catch (Exception e) {
										Utils.saveException(this.getClass().getName(), e);
										message.reply(new JsonObject().putNumber("httpCode", 400));
	                                    return;
									}
			                        	
									final String localAddressAccepted = UUID.randomUUID().toString()+"-accepted";
		                            
									JsonObject workerDataJsonObject = new JsonObject();
									workerDataJsonObject.putString("accepted-reply", localAddressAccepted);
									workerDataJsonObject.putString("idDevice", idDevice);
									workerDataJsonObject.putString("idCampaign", jsonContentObject.getString("idCampaign"));
									if (registries != null && registries.size()>0) {
										workerDataJsonObject.putArray("registries", Utils.registriesToJsonArray(registries));
									}		                            
		                            if (newProducts != null && newProducts.size()>0) {
		                            	workerDataJsonObject.putArray("newProducts", newProducts);
									}
		                            
		                            eb.registerHandler(localAddressAccepted, new Handler<Message<JsonObject>>() {

										@Override
										public void handle(Message<JsonObject> messageFromWorker) {
											Utils.log(this.getClass().getName(), "From worker", messageFromWorker.address(), messageFromWorker.body().encode());
											if(messageFromWorker.body().getString("status").equalsIgnoreCase("accepted")) {
			                                	message.reply(new JsonObject().putNumber("httpCode", 200));													
			                                }else{
			                                	message.reply(new JsonObject().putNumber("httpCode", 500));
			                                }
											
											eb.unregisterHandler(localAddressAccepted, null);
										}
									});

		                            Utils.log(this.getClass().getName(), "sending to worker", workerDataJsonObject.encode());
                                    
                                    eb.send(Constants.handlerNameRegistriesProcessor, workerDataJsonObject);
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
					message.reply(new JsonObject().putNumber("httpCode", 400));
                    return;
				} catch (Exception e) {
					Utils.saveException(this.getClass().getName(), e);
					message.reply(new JsonObject().putNumber("httpCode", 500));
                    return;
				}
			}
		});
	}

	@Override
	public void stop() {
		eb.unregisterHandler(Constants.handlerNameRegistries, null);
	}

}
