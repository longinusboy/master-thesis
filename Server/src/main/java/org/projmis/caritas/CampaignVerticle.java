package org.projmis.caritas;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class CampaignVerticle extends Verticle {
	EventBus eb = null;

	@Override
	public void start() {
		System.out.println("Starting " + this.getClass().getName() + " Verticle");
		eb = vertx.eventBus();
		eb.registerHandler(Constants.handlerNameCampaigns, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(final Message<JsonObject> message) {
				final JsonObject messageBody = message.body();
				try {
					String op = messageBody.getString("op");
					boolean noBlockCheck = messageBody.getBoolean("noBlockCheck", false);
					if (op.contentEquals("get")) {
						final String idDevice = messageBody.getString("idDevice");
						
						if (!noBlockCheck) {
							Utils.isDeviceUnblocked(eb, idDevice, new Handler<Message<JsonObject>>() {

								@Override
								public void handle(Message<JsonObject> messageDevice) {
									if (!messageDevice.body().getString("status").contentEquals("ok")) {
										Utils.log(this.getClass().getName(), "Utils.isDeviceUnblocked", messageDevice.body().encode());
										message.reply(new JsonObject().putNumber("httpCode", 403));
						                return;
									}
									
									getCampaigns(message, idDevice);
								}
							});
						} else {
							getCampaigns(message, idDevice);
						}
						return;
					}
					
					
					if (op.contentEquals("post")) {
						final String idDevice = messageBody.getString("idDevice");
						String content = messageBody.getString("content");
												
						if(idDevice==null) {
							message.reply(new JsonObject().putNumber("httpCode", 500));
                            return;
                        }
						
						if(content==null) {
							message.reply(new JsonObject().putNumber("httpCode", 400));
                            return;
                        }

                        try {
                        	final String idCampaign;
                        	final String hasedPassword;
                        	try {
                        		JsonObject jsonObjectContent = new JsonObject(content);
                                idCampaign = jsonObjectContent.getString("idCampaign");
                                hasedPassword = jsonObjectContent.getString("password");
							} catch (Exception e) {
								Utils.saveException(this.getClass().getName(), e);
								message.reply(new JsonObject().putNumber("httpCode", 400));
								return;
							}
                        	
                        	eb.send(Constants.handlerNameDevices, new JsonObject().putString("op", "get").putString("idDevice", idDevice), new Handler<Message<JsonObject>>() {

    							@Override
    							public void handle(Message<JsonObject> messageDevice) {
    								try {
    									if (messageDevice.body().getInteger("httpCode")!=200) {
    										message.reply(new JsonObject().putNumber("httpCode", 403));
    						                return;
    									}
    									
    									JsonObject matcher = new JsonObject().putString("idCampaign", idCampaign);
    		            				JsonObject json = new JsonObject()
    		            						.putString("collection", "campaigns")
    		            						.putString("action", "findone")
    		            						.putObject("matcher", matcher);

    		            				eb.send(Constants.handlerDataBase, json, new Handler<Message<JsonObject>>() {
    		            					@Override
    		            					public void handle(Message<JsonObject> messageFindCampaign) {
    		            						try {
    		            							if (!messageFindCampaign.body().getString("status").equalsIgnoreCase("ok")) {
    		            								message.reply(new JsonObject().putNumber("httpCode", 500));
    		            								return;
    		            							}
    		            						} catch (Exception e) {
    		    									Utils.saveException(this.getClass().getName(), e);
    		            							message.reply(new JsonObject().putNumber("httpCode", 500));
    		            							return;
    		            						}

    		            						try {
    		        								JsonObject result = messageFindCampaign.body().getObject("result");
    		        								if (result==null) {
    		        									message.reply(new JsonObject().putNumber("httpCode", 412));
    		                							return;
    												}
    		        								
    		        								Date now = new Date();    		        								
													if (result.getLong("dateStart") > now.getTime() || result.getLong("dateEnd") < now.getTime()) {
														message.reply(new JsonObject().putNumber("httpCode", 500));
														return;
													}
													
    		        								if(sha1(idDevice+result.getString("password")).contentEquals(hasedPassword)) {
    		        									
    		        									JsonObject jsonUpdateCampaignNumDevices = new JsonObject()
			                                    				.putString("collection", "campaigns")
			                                    				.putString("action", "update")
			                                    				.putObject("criteria", new JsonObject().putString("idCampaign", idCampaign))
			                                    				.putObject("objNew", new JsonObject().putObject("$inc", new JsonObject()
			                                    					.putNumber("numDevices", 1)));
		    	                                        eb.send(Constants.handlerDataBase, jsonUpdateCampaignNumDevices);
		    	                                        
		    	                                        JsonObject jsonUpdateCampaignDevices = new JsonObject()
			                                    				.putString("collection", "campaigns")
			                                    				.putString("action", "update")
			                                    				.putObject("criteria", new JsonObject().putString("idCampaign", idCampaign))
			                                    				.putObject("objNew", new JsonObject().putObject("$push", new JsonObject()
			                                    					.putString("devices", idDevice)));
		    	                                        eb.send(Constants.handlerDataBase, jsonUpdateCampaignDevices);
		    	                                        
		    	                                        JsonObject jsonSavePersonatStats = new JsonObject()
			                                    				.putString("collection", "personalStats")
			                                    				.putString("action", "save")
			                                    				.putObject("document", new JsonObject()
			                                    					.putString("idCampaign", idCampaign)
			                                    					.putString("idDevice", idDevice)
			                                    					.putNumber("sumWeights", 0L)
			                                    					.putNumber("sumRegistries", 0));
		    	                                        eb.send(Constants.handlerDataBase, jsonSavePersonatStats);
		    	                                        
		    	                                        JsonObject jsonUpdateDeviceCampaigns = new JsonObject()
			                                    				.putString("collection", "devices")
			                                    				.putString("action", "update")
			                                    				.putObject("criteria", new JsonObject().putString("idDevice", idDevice))
			                                    				.putObject("objNew", new JsonObject().putObject("$push", new JsonObject()
			                                    					.putString("campaigns", idCampaign)));
		    	                                        eb.send(Constants.handlerDataBase, jsonUpdateDeviceCampaigns, new Handler<Message<JsonObject>>() {
			        										
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
    		        								}else {
    		        									message.reply(new JsonObject().putNumber("httpCode", 401));
    												}
    		        								return;
    		            						} catch (Exception e) {
    		    									Utils.saveException(this.getClass().getName(), e);
    		            							message.reply(new JsonObject().putNumber("httpCode", 500));
    		            							return;
    		            						}
    		            					}
    		            				});
    									return;    									
    								} catch (Exception e) {
    									Utils.saveException(this.getClass().getName(), e);
    									message.reply(new JsonObject().putNumber("httpCode", 500));
    					                return;
    								}
    							}
                        	});                        	
                            return;
                        }catch (Exception e) {
							Utils.saveException(this.getClass().getName(), e);
                        	message.reply(new JsonObject().putNumber("httpCode", 500));
                            return;
                        }
					}
					message.reply(new JsonObject().putNumber("httpCode", 400));
                    return;
				} catch (Exception e) {
					Utils.saveException(this.getClass().getName(), e);
					message.reply(new JsonObject().putNumber("httpCode", 500));
				}
			}
		});
	
	}
	
	public void getCampaigns(final Message<JsonObject> message, final String idDevice) {

		try {
			JsonObject jsonSearchCampaigns = new JsonObject()
					.putString("collection", "campaigns")
					.putString("action", "find")
					.putObject("matcher", new JsonObject().putBoolean("visible", true));

			eb.send(Constants.handlerDataBase, jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> messageMongoDB) {
					try {
						if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
							message.reply(new JsonObject().putNumber("httpCode", 500));
							return;
						}
						
						if(messageMongoDB.body().getInteger("number")==0) {
							message.reply(new JsonObject().putNumber("httpCode", 200)
									.putString("httpResponse", new JsonArray().encode()));
							return;
						}
						
						JsonArray resultCampaigns = messageMongoDB.body().getArray("results");
						for (int i = 0; i < resultCampaigns.size(); i++) {
							JsonObject campagin = resultCampaigns.get(i);
							JsonArray devices = campagin.getArray("devices", new JsonArray());
							for (int j = 0; j < devices.size(); j++) {
								try {
									String device = devices.get(j);
									if (device.contentEquals(idDevice)) {
										campagin.putBoolean("subscribed", true);
										break;
									}
								} catch (Exception e) {
									Utils.saveException(this.getClass().getName(), e);
									e.printStackTrace();
								}														
							}
							if (!campagin.containsField("subscribed")) {
								campagin.putBoolean("subscribed", false);
							}
							campagin.removeField("_id");
							campagin.removeField("password");
							campagin.removeField("devices");													
						}
						
						message.reply(new JsonObject().putNumber("httpCode", 200).putString("httpResponse", resultCampaigns.encode()));
						return;
					} catch (Exception e) {
						Utils.saveException(this.getClass().getName(), e);
						message.reply(new JsonObject().putNumber("httpCode", 500));
						return;
					}
				}
			});
			return;
			
		} catch (Exception e) {
			Utils.saveException(this.getClass().getName(), e);
			message.reply(new JsonObject().putNumber("httpCode", 500));
            return;
		}
	
	}

	@Override
	public void stop() {
		eb.unregisterHandler(Constants.handlerNameCampaigns, null);
	}
	
	private static String sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        byte[] result = mDigest.digest(input.getBytes());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        }
         
        return sb.toString();
    }

}
