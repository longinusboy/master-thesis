package org.projmis.evida;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class TypeVerticle extends Verticle {
	EventBus eb = null;

	@Override
	public void start() {
		System.out.println("Starting " + this.getClass().getName() + " Verticle");
		eb = vertx.eventBus();
		eb.registerHandler(Constants.handlerNameTypes, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(final Message<JsonObject> message) {
				JsonObject messageBody = message.body();
				try {
					String op = messageBody.getString("op");
					if (op.contentEquals("get")) {

			            JsonObject json = new JsonObject()
			                    .putString("collection", "types")
			                    .putString("action", "find")
			                    .putObject("matcher", new JsonObject());
			            eb.send(Constants.handlerDataBase, json, new Handler<Message<JsonObject>>() {
			                @Override
			                public void handle(Message<JsonObject> messageMongoDB) {
			                    String status = messageMongoDB.body().getString("status");
			                    try {
			                        if (!status.equalsIgnoreCase("ok")) {
			                        	message.reply(new JsonObject().putNumber("httpCode", 500));
			                            return;
			                        }
			                        if(messageMongoDB.body().getInteger("number")==0) {
			                        	message.reply(new JsonObject().putNumber("httpCode", 404).putString("httpResponse","[]"));
			                            return;
			                        }
			                        
									message.reply(new JsonObject().putNumber("httpCode", 200).putString("httpResponse",
											messageMongoDB.body().getArray("results").encode()));
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
		eb.unregisterHandler(Constants.handlerNameTypes, null);
	}

}
