package org.projmis.evida;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class StatsVerticle extends Verticle {
	static EventBus eb = null;

	@Override
	public void start() {
		System.out.println("Starting " + this.getClass().getName() + " Verticle");
		eb = vertx.eventBus();
		eb.registerHandler(Constants.handlerNameStats, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(final Message<JsonObject> message) {
				JsonObject messageBody = message.body();
				try {
					String op = messageBody.getString("op");
					boolean noBlockCheck = messageBody.getBoolean("noBlockCheck", false);
					if (op.contentEquals("get")) {
						final String idDevice = messageBody.getString("idDevice");
			            if(idDevice==null) {
			            	message.reply(new JsonObject().putNumber("httpCode", 500));
			                return;
			            }
			            
			            if (!noBlockCheck) {
							Utils.isDeviceUnblocked(eb, idDevice, new Handler<Message<JsonObject>>() {

								@Override
								public void handle(Message<JsonObject> messageDevice) {
									if (!messageDevice.body().getString("status").contentEquals("ok")) {
										Utils.log(this.getClass().getName(), "Utils.isDeviceUnblocked", messageDevice.body().encode());
										message.reply(new JsonObject().putNumber("httpCode", 403));
						                return;
									}
									getStats(message, idDevice);
								}
				            }); 
						} else {
							getStats(message, idDevice);
						}			                       
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
	
	public void getStats(final Message<JsonObject> message, final String idDevice) {

		try {			
			JsonObject jsonSearchCampaignsWithIdDevice = new JsonObject()
                    .putString("collection", "campaigns")
                    .putString("action", "find")
                    .putObject("matcher", 
                    		new JsonObject().putObject("devices", new JsonObject().putArray("$in", new JsonArray().addString(idDevice))));
			
			eb.send(Constants.handlerDataBase, jsonSearchCampaignsWithIdDevice, new Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> messageCampaigns) {
					String status = messageCampaigns.body().getString("status");
                    try {
                        if(!status.equalsIgnoreCase("ok")) {
                        	message.reply(new JsonObject().putNumber("httpCode", 500));
                            return;
                        }
                        
                        JsonArray results = messageCampaigns.body().getArray("results", new JsonArray());
                        
                        JsonArray campaignsMatcherOr = new JsonArray();
                        final JsonArray campaigns = new JsonArray();
                        
                        for (int i = 0; i < results.size(); i++) {
                        	JsonObject result = results.get(i);
                        	JsonObject campaign = new JsonObject()
                        			.putString("idCampaign", result.getString("idCampaign"))
                        			.putObject("stats", result.getObject("stats"))
                        			.putNumber("sumWeights", result.getLong("sumWeights"))
                        			.putNumber("sumRegistries", result.getInteger("sumRegistries"));//visible not treated intentionally
							campaigns.add(campaign);
							campaignsMatcherOr.add(new JsonObject().putString("idCampaign", result.getString("idCampaign")));
						}
                        
                        JsonObject jsonSearchStats;
                        
                        if (campaignsMatcherOr.size()>0) {
                        	jsonSearchStats = new JsonObject()
				                    .putString("collection", "personalStats")
				                    .putString("action", "find")
				                    .putObject("matcher", 
				                    		new JsonObject().putArray("$and", new JsonArray().addObject(new JsonObject()
			                    				.putArray("$or", campaignsMatcherOr)).addObject(new JsonObject().putString("idDevice", idDevice))));
						}else {
							message.reply(new JsonObject().putNumber("httpCode", 200).putString("httpResponse", new JsonArray().encode()));
							return;
						}
                        
			            eb.send(Constants.handlerDataBase, jsonSearchStats, new Handler<Message<JsonObject>>() {
			                @Override
			                public void handle(Message<JsonObject> messageMongoDB) {
			                    String status = messageMongoDB.body().getString("status");
			                    try {
			                        if(!status.equalsIgnoreCase("ok")) {
			                        	message.reply(new JsonObject().putNumber("httpCode", 500));
			                            return;
			                        }
			                        
			                        JsonArray results = messageMongoDB.body().getArray("results", new JsonArray());
			                        
			                        for (int i = 0; i < results.size(); i++) {
										JsonObject result = results.get(i);
										String idCampaign = result.getString("idCampaign");
										
										for (int j = 0; j < campaigns.size(); j++) {
											JsonObject campaign = campaigns.get(j);
											
											if (idCampaign.contentEquals(campaign.getString("idCampaign"))) {
												campaign.putNumber("mySumWeights", result.getLong("sumWeights"));
												campaign.putNumber("mySumRegistries", result.getInteger("sumRegistries"));
												break;
											}
										}
									}
			                        
			                        JsonObject jsonSearchRanks = new JsonObject()
						                    .putString("collection", "personalStats")
						                    .putString("action", "find")
						                    .putObject("matcher", 
					                    			new JsonObject().putString("idCampaign", 
					                    					((JsonObject)campaigns.get(0)).getString("idCampaign")))
				                    		.putObject("sort", 
				                    				new JsonObject().putNumber("numRegistries", -1).putNumber("numWeights", -1));
			                        
			                        eb.send(Constants.handlerDataBase, jsonSearchRanks, new RankHandler(message, idDevice, campaigns, 0));
			                        
									return;
			                    }catch(Exception e) {
			                    	Utils.saveException(this.getClass().getName(), e);
			                    	message.reply(new JsonObject().putNumber("httpCode", 500));
			                        return;
			                    }
			                }
			            });
                    	return;
                    }catch(Exception e) {
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

	@Override
	public void stop() {
		eb.unregisterHandler(Constants.handlerNameStats, null);
	}
	
	
	private class RankHandler implements Handler<Message<JsonObject>> {
		 
        private final Message<JsonObject> request;
        private JsonArray campaigns;
        private int index;
        private String idDevice;
 
        private RankHandler(final Message<JsonObject> request, String idDevice, JsonArray campaigns, int index) {
            this.request = request;
            this.campaigns = campaigns;
            this.index = index;
            this.idDevice = idDevice;
        }
        
        class RankInstance implements Comparable<RankInstance> {
        	private Long sumWeights;
        	private Long sumRegistries;
        	private JsonObject content;
        	
			public RankInstance(Long sumWeights, Long sumRegistries, JsonObject content) {
				super();
				this.sumWeights = sumWeights;
				this.sumRegistries = sumRegistries;
				this.content = content;
			}

			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof RankInstance)) {
					return false;
				}
				RankInstance objRank = (RankInstance) obj;
				return sumRegistries.equals(objRank.sumRegistries) && sumWeights.equals(objRank.sumWeights) && content.encode().equals(objRank.content.encode());
			}

			@Override
			public int compareTo(RankInstance arg0) {
				if (sumWeights>arg0.sumWeights) {
					return 1;
				}
				if (sumWeights<arg0.sumWeights) {
					return -1;
				}
				if (sumRegistries>arg0.sumRegistries) {
					return 1;
				}
				if (sumRegistries<arg0.sumRegistries) {
					return -1;
				}
				return 0;
			}
        	
        	public JsonObject getContent() {
				return content;
			}
        }
 
        @Override
        public void handle(final Message<JsonObject> messageMongoDB) {			
        	try {
				if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
					request.reply(new JsonObject().putString("status", "error").putString("error", "Erro a processar dados"));
					return;
				}
								
				JsonArray results = messageMongoDB.body().getArray("results", new JsonArray());
				
				JsonObject campaign = campaigns.get(index);
				
				List<RankInstance> ranks = new ArrayList<RankInstance>();
				for (int i = 0; i < results.size(); i++) {
					JsonObject result = results.get(i);
					ranks.add(new RankInstance(result.getNumber("sumWeights",0).longValue(), result.getNumber("numRegistries", 0).longValue(), result));
				}
				Collections.sort(ranks);
				JsonArray newResults = new JsonArray();
				for (int i = ranks.size()-1; i >= 0; i--) {
					newResults.add(ranks.get(i).getContent());
				}
				
				for (int i = 0; i < newResults.size(); i++) {
					JsonObject result = newResults.get(i);
					if (result.getString("idDevice").contentEquals(idDevice)) {										
						campaign.putNumber("myRank", i+1);
						campaign.putNumber("countDevices", newResults.size());
						break;
					}
				}
				
				if (!campaign.containsField("myRank")) {
					campaign.putNumber("myRank", 0);
				}
				
				if (!campaign.containsField("countDevices")) {
					campaign.putNumber("countDevices", 0);
				}
				
				index++;
				
				if (index<campaigns.size()) {
					JsonObject jsonSearchRanks = new JsonObject()
		                    .putString("collection", "personalStats")
		                    .putString("action", "find")
		                    .putObject("matcher", 
	                    			new JsonObject().putString("idCampaign", 
	                    					((JsonObject)campaigns.get(index)).getString("idCampaign")))
                    		.putObject("sort", 
                    				new JsonObject().putNumber("numRegistries", -1).putNumber("numWeights", -1));
                    
                    eb.send(Constants.handlerDataBase, jsonSearchRanks, new RankHandler(request, idDevice, campaigns, index));
				}else {
					 request.reply(new JsonObject().putNumber("httpCode", 200).putString("httpResponse", campaigns.encode()));
				}
			}catch(Exception e) {
				Utils.saveException(this.getClass().getName(), e);
				request.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
			}
		}
    }

}
