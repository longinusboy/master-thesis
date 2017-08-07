package org.projmis.demo;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class ProductVerticle extends Verticle {
	EventBus eb = null;
		
	@Override
	public void start() {
		System.out.println("Starting " + this.getClass().getName() + " Verticle");
		eb = vertx.eventBus();
		eb.registerHandler(Constants.handlerNameProducts, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(final Message<JsonObject> message) {
				final JsonObject messageBody = message.body();
				try {
					String op = messageBody.getString("op");
					if (op.contentEquals("get")) {
						String idDevice = messageBody.getString("idDevice");
			            if (idDevice == null) {
			            	message.reply(new JsonObject().putNumber("httpCode", 500));
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
									
									int page;
									long date;
									try {
										page = Integer.valueOf(messageBody.getString("page"));
										date = Long.valueOf(messageBody.getString("date"));
									} catch (Exception e) {
										Utils.saveException(this.getClass().getName(), e);
										message.reply(new JsonObject().putNumber("httpCode", 500));
						                return;
									}
									
									JsonObject matcher = new JsonObject().putObject("modified", new JsonObject().putNumber("$gt", date));
									
									JsonObject json = new JsonObject()
						                    .putString("collection", "products")
						                    .putString("action", "find")
						                    .putNumber("skip", 100*page)
						                    .putNumber("limit", 100)
						                    .putObject("matcher", matcher);

						            
						            eb.send(Constants.handlerDataBase, json, new Handler<Message<JsonObject>>() {
						                @Override
						                public void handle(Message<JsonObject> messageMongoDB) {
						                    try {
						                        if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok") &&
						                        		!messageMongoDB.body().getString("status").equalsIgnoreCase("more-exist")) {
						                        	message.reply(new JsonObject().putNumber("httpCode", 500));
						                            return;
						                        }
						                        
												if (!messageMongoDB.body().containsField("results")) {
													message.reply(new JsonObject().putNumber("httpCode", 200).putString("httpResponse", new JsonArray().encode()));
													return;
												}

												message.reply(new JsonObject().putNumber("httpCode", 200).putString("httpResponse",
														messageMongoDB.body().getArray("results").encode()));
											} catch (Exception e) {
												Utils.saveException(this.getClass().getName(), e);
												message.reply(new JsonObject().putNumber("httpCode", 500));
												return;
											}
						                }
						            });
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
					return;
				}
			}
		});
	}

	@Override
	public void stop() {
		eb.unregisterHandler(Constants.handlerNameProducts, null);
	}
}
