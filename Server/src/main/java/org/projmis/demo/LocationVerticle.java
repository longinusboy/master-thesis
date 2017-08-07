package org.projmis.demo;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class LocationVerticle extends Verticle {
    EventBus eb = null;

    @Override
    public void start() {
    	System.out.println("Starting " + this.getClass().getName() + " Verticle");
        eb = vertx.eventBus();
        eb.registerHandler(Constants.handlerNameLocations, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(final Message<JsonObject> message) {
                final JsonObject messageBody = message.body();
                try {
                    String op = messageBody.getString("op");
                    if (op.contentEquals("post")) {
                        final String idDevice = messageBody.getString("idDevice");
                        final String idCampaign = messageBody.getString("idCampaign");
                        final JsonObject location = messageBody.getObject("location");

                        if (idDevice == null) {
                            message.reply(new JsonObject().putString("status", "error").putString("error", "idDevice not defined"));
                            return;
                        }
                        
                        if (idCampaign == null) {
                            message.reply(new JsonObject().putString("status", "error").putString("error", "idCampaign not defined"));
                            return;
                        }
                        
                        if (location == null) {
                            message.reply(new JsonObject().putString("status", "error").putString("error", "locations not defined"));
                            return;
                        }
                        
                        JsonObject jsonUpdateLastLocationField = new JsonObject().putObject("$set", new JsonObject()
                                .putObject("lastLocation",location));

                        JsonObject jsonUpdateLastLocation = new JsonObject()
                                .putString("collection", "devices")
                                .putString("action", "update")
                                .putObject("criteria", new JsonObject().putString("idDevice", idDevice))
                                .putObject("objNew", jsonUpdateLastLocationField);
                        Utils.log(this.getClass().getName(), "update last loc", jsonUpdateLastLocation.encode());
                        eb.send(Constants.handlerDataBase, jsonUpdateLastLocation);

                        JsonObject objNewRegistries = new JsonObject().putObject("$push", new JsonObject()
                                .putObject("locations", location));

                        JsonObject jsonUpdateLocationsList = new JsonObject()
                                .putString("collection", "devices")
                                .putString("action", "update")
                                .putObject("criteria", new JsonObject().putString("idDevice", idDevice))
                                .putObject("objNew", objNewRegistries);

                        eb.send(Constants.handlerDataBase, jsonUpdateLocationsList, new Handler<Message<JsonObject>>() {

                            @Override
                            public void handle(Message<JsonObject> messageUpdate) {
                                try {
                                    if(messageUpdate.body().getString("status").equalsIgnoreCase("ok")) {
                                        message.reply(new JsonObject().putNumber("httpCode", 200));
                                        return;
                                    }else{
                                        message.reply(new JsonObject().putNumber("httpCode", 500));
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
        eb.unregisterHandler(Constants.handlerNameLocations, null);
    }

}
