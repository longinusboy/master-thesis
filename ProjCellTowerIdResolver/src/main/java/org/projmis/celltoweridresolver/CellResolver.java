/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 *
 */
package org.projmis.celltoweridresolver;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class CellResolver extends BusModBase implements Handler<Message<JsonObject>> {
    protected String address;
    protected String collectionOpenCellID;
    protected String collectionSlashGPS;
    protected String collectionGoogleMmapCache;

    protected EventBus eb;

    public void start() {
        super.start();

        eb = vertx.eventBus();

        address = getOptionalStringConfig("address", "cellid-resolver");
        collectionOpenCellID = getOptionalStringConfig("collectionOpenCellID", "opencellid");
        collectionSlashGPS = getOptionalStringConfig("collectionSlashGPS", "slashgps");
        collectionGoogleMmapCache = getOptionalStringConfig("collectionGoogleMmapCache", "mmapcache");
        
        JsonObject configMongo = new JsonObject();
        configMongo.putString("address", getOptionalStringConfig("mongo_address", "mongodb-persistor-celltower"));
        configMongo.putString("host", getOptionalStringConfig("mongo_host", "localhost"));
        configMongo.putNumber("port", getOptionalIntConfig("mongo_port", 27017));
        configMongo.putNumber("pool_size", getOptionalIntConfig("mongo_pool_size", 10));
        configMongo.putString("db_name", getOptionalStringConfig("mongo_db_name", "celltowersids"));

        container.deployModule("io.vertx~mod-mongo-persistor~2.1.0", configMongo, new Handler<AsyncResult<String>>() {
			
			@Override
			public void handle(AsyncResult<String> event) {
				if (event.succeeded()) {
					eb.registerHandler(address, CellResolver.this);
				}
			}
		});
    }

    @Override
    public void stop() {
        eb.unregisterHandler(address, this);
        super.stop();
    }

    @Override
    public void handle(Message<JsonObject> event) {
        String action = event.body().getString("action");

        if (action == null) {
            sendError(event, "action must be specified");
            return;
        }

        try {
            switch (action) {
                case "resolve":
                    doResolve(event);
                    break;
                case "multi-resolve":
                	doMultiResolve(event);
                	break;
                default:
                    sendError(event, "Invalid action: " + action);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doMultiResolve(final Message<JsonObject> event) {
    	Set<Tower> towersSet = new HashSet<Tower>();
    	
    	final String responseAddress = event.body().getString("responseAddress");
    	if (responseAddress == null) {
    		sendError(event, "Response address not defined");
            return;
		}
    	
    	JsonArray jsonArrayTowers = event.body().getArray("towers");    	
    	if (jsonArrayTowers == null) {
    		sendError(event, "Towers array not defined");
            return;
		}
    	
    	for (int i = 0; i < jsonArrayTowers.size(); i++) {
			JsonObject tower = jsonArrayTowers.get(i);
			Integer mcc = (Integer) tower.getNumber("mcc");
	        Integer mnc = (Integer)tower.getNumber("mnc");
	        Integer lac = (Integer)tower.getNumber("lac");
	        Integer cid = (Integer)tower.getNumber("cid");
	        
	        if (mcc == null || mnc == null || lac == null || cid == null) {
	            sendError(event, "Search terms not properly initialized on element: "+i);
	            return;
	        }
	        
	        towersSet.add(new Tower(mcc, mnc, lac, cid));
		}
    	
    	List<Tower> towersList = new ArrayList<>(towersSet);
    	
    	for (int i = 0; i < towersList.size(); i++) {
			Tower tower = towersList.get(i);
			
			final Integer mcc = tower.getMcc();
	        final Integer mnc = tower.getMnc();
	        final Integer lac = tower.getLac();
	        final Integer cid = tower.getCid();
	        
	        
	        searchTable(collectionOpenCellID, mcc, mnc, lac, cid, new Handler<Message<JsonObject>>() {
	            @Override
	            public void handle(Message<JsonObject> replyEvent) {
	                String status = replyEvent.body().getString("status");
	                if (status != null) {
	                    if (status.contentEquals("ok")) {
	                    	JsonObject result = replyEvent.body().getObject("result");
	                    	if (result != null) {
	                        	JsonObject reply = new JsonObject()
			                        	.putString("status", "more-exist")
			                        	.putNumber("mcc", mcc)
	                                    .putNumber("mnc", mnc)
	                                    .putNumber("lac", lac)
	                                    .putNumber("cid", cid)
	                                    .putNumber("lon", (Double) result.getNumber("lon"))
	                                    .putNumber("lat", (Double) result.getNumber("lat"))
	                                    .putString("source", collectionOpenCellID);
	                            incrementUsageCounter(collectionOpenCellID, mcc, mnc, lac, cid);
	                            eb.send(responseAddress, reply);
	                            return;
	                        }
	                    }
	                }
	                searchTable(collectionSlashGPS, mcc, mnc, lac, cid, new Handler<Message<JsonObject>>() {
	                    @Override
	                    public void handle(Message<JsonObject> replyEvent) {
	                        String status = replyEvent.body().getString("status");
	                        if (status != null) {
	                            if (status.contentEquals("ok")) {
	                                JsonObject result = replyEvent.body().getObject("result");
	                                if (result != null) {
	                                	JsonObject reply = new JsonObject()
			                                	.putString("status", "more-exist")
			                                	.putNumber("mcc", mcc)
			                                    .putNumber("mnc", mnc)
			                                    .putNumber("lac", lac)
			                                    .putNumber("cid", cid)
			                                    .putNumber("lon", (Double) result.getNumber("lon"))
			                                    .putNumber("lat", (Double) result.getNumber("lat"))
			                                    .putString("source", collectionSlashGPS);
	                                    incrementUsageCounter(collectionSlashGPS, mcc, mnc, lac, cid);
	                                    eb.send(responseAddress, reply);
	                                    return;
	                                }
	                            }
	                        }
	                        searchTable(collectionGoogleMmapCache, mcc, mnc, lac, cid, new Handler<Message<JsonObject>>() {
	                            @Override
	                            public void handle(Message<JsonObject> replyEvent) {
	                                String status = replyEvent.body().getString("status");
	                                if (status != null) {
	                                    if (status.contentEquals("ok")) {
	                                    	JsonObject result = replyEvent.body().getObject("result");
	                                        if (result != null) {
	                                        	JsonObject reply = new JsonObject()
	                                        			.putString("status", "more-exist")
			                                        	.putNumber("mcc", mcc)
			    	                                    .putNumber("mnc", mnc)
			    	                                    .putNumber("lac", lac)
			    	                                    .putNumber("cid", cid)
			    	                                    .putNumber("lon", (Double) result.getNumber("lon"))
			    	                                    .putNumber("lat", (Double) result.getNumber("lat"))
			    	                                    .putString("source", collectionGoogleMmapCache);
	                                            incrementUsageCounter(collectionGoogleMmapCache, mcc, mnc, lac, cid);
	                                            eb.send(responseAddress, reply);
	                                            return;
	                                        }
	                                    }
	                                }
	                                
	                                try {
										Thread.sleep(1500);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}

	                                JsonObject reply = searchGoogleMmapServer(mcc, mnc, lac, cid);
	                                
	                                Double lon = (Double) reply.getNumber("lon");
	                                Double lat = (Double) reply.getNumber("lat");
	                                String error = reply.getString("error");
	                                
	                                reply.putNumber("mcc", mcc)
		                                    .putNumber("mnc", mnc)
		                                    .putNumber("lac", lac)
		                                    .putNumber("cid", cid)
		                                    .putString("source", "Mmap-server");
	                                
	                                System.out.println(reply.encode());
	                                
	                                if (error==null) {	                                	
	                                	insertIncGoogleMmapResult(mcc, mnc, lac, cid, lon, lat);
	                                	reply.putString("status", "more-exist");
	                                	eb.send(responseAddress, reply);
									}else {
										reply.putString("status", "error");
										eb.send(responseAddress, reply);
									}
	                            }
	                        });
	                    }
	                });
	            }
	        });	        
		}
    	eb.send(responseAddress, new JsonObject().putString("status", "ok"));
    	sendOK(event);
	}

	private void doResolve(final Message<JsonObject> event) {
        final Integer mcc = (Integer)event.body().getNumber("mcc");
        final Integer mnc = (Integer)event.body().getNumber("mnc");
        final Integer lac = (Integer)event.body().getNumber("lac");
        final Integer cid = (Integer)event.body().getNumber("cid");

        if (mcc == null || mnc == null || lac == null || cid == null) {
            sendError(event, "Search terms not properly initialized");
            return;
        }

        searchTable(collectionOpenCellID, mcc, mnc, lac, cid, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> replyEvent) {
                String status = replyEvent.body().getString("status");
                if (status != null) {
                    if (status.contentEquals("ok")) {
                    	JsonObject result = replyEvent.body().getObject("result");
                    	if (result != null) {
                        	JsonObject reply = new JsonObject()
                                    .putNumber("lon", (Double) result.getNumber("lon"))
                                    .putNumber("lat", (Double) result.getNumber("lat"));
                            reply.putString("source", collectionOpenCellID);
                            incrementUsageCounter(collectionOpenCellID, mcc, mnc, lac, cid);
                            sendOK(event, reply);
                            return;
                        }
                    }
                }
                searchTable(collectionSlashGPS, mcc, mnc, lac, cid, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> replyEvent) {
                        String status = replyEvent.body().getString("status");
                        if (status != null) {
                            if (status.contentEquals("ok")) {
                                JsonObject result = replyEvent.body().getObject("result");
                                if (result != null) {
                                	JsonObject reply = new JsonObject()
		                                    .putNumber("lon", (Double) result.getNumber("lon"))
		                                    .putNumber("lat", (Double) result.getNumber("lat"));
                                    reply.putString("source", collectionSlashGPS);
                                    incrementUsageCounter(collectionSlashGPS, mcc, mnc, lac, cid);
                                    sendOK(event, reply);
                                    return;
                                }
                            }
                        }
                        searchTable(collectionGoogleMmapCache, mcc, mnc, lac, cid, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> replyEvent) {
                                String status = replyEvent.body().getString("status");
                                if (status != null) {
                                    if (status.contentEquals("ok")) {
                                    	JsonObject result = replyEvent.body().getObject("result");
                                        if (result != null) {
                                        	JsonObject reply = new JsonObject()
        		                                    .putNumber("lon", (Double) result.getNumber("lon"))
        		                                    .putNumber("lat", (Double) result.getNumber("lat"));
                                            reply.putString("source", collectionGoogleMmapCache);
                                            incrementUsageCounter(collectionGoogleMmapCache, mcc, mnc, lac, cid);
                                            sendOK(event, reply);
                                            return;
                                        }
                                    }
                                }

                                JsonObject reply = searchGoogleMmapServer(mcc, mnc, lac, cid);
                                
                                Double lon = (Double) reply.getNumber("lon");
                                Double lat = (Double) reply.getNumber("lat");
                                String error = reply.getString("error");
                                
                                System.out.println(reply.encode());
                                
                                if (error==null) {
                                	reply.putString("source", "Mmap-server");
                                	insertIncGoogleMmapResult(mcc, mnc, lac, cid, lon, lat);
                                	sendOK(event, reply);
								}else {
									sendError(event, error);
								}
                            }
                        });
                    }
                });
            }
        });
    }

    private void searchTable(String collection, Integer mcc, Integer mnc, Integer lac, Integer cid, final Handler<Message<JsonObject>> message) {

        JsonObject matcher = new JsonObject()
        		.putNumber("mcc", mcc)
        		.putNumber("mnc", mnc)
        		.putNumber("lac", lac)
        		.putNumber("cid", cid);
        
        JsonObject query = new JsonObject()
		        .putString("collection", collection)
		        .putString("action", "findone")
		        .putObject("matcher", matcher);

        eb.send("mongodb-persistor-celltower", query, message);
    }

    private void incrementUsageCounter(String collection, Integer mcc, Integer mnc, Integer lac, Integer cid) {
    	
    	JsonObject matcher = new JsonObject()
				.putNumber("mcc", mcc)
				.putNumber("mnc", mnc)
				.putNumber("lac", lac)
				.putNumber("cid", cid);
    	
    	JsonObject inc = new JsonObject()
    			.putObject("$inc", new JsonObject()
    			.putNumber("nUsed", 1));
    	
        JsonObject query = new JsonObject()
        		.putString("action", "update")
                .putString("collection", collection)
                .putObject("criteria", matcher)
                .putObject("objNew", inc);
        
        eb.send("mongodb-persistor-celltower", query);
    }
	
	private JsonObject searchGoogleMmapServer(int mcc, int mnc, int lac, int cid) {

		String urlmmap = "http://www.google.com/glm/mmap";

		try {
			URL url = new URL(urlmmap);
			URLConnection conn = url.openConnection();
			HttpURLConnection httpConn = (HttpURLConnection) conn;
			httpConn.setReadTimeout(5000);
			httpConn.setConnectTimeout(5000);
			httpConn.setRequestMethod("POST");
			httpConn.setDoOutput(true);
			httpConn.setDoInput(true);
			httpConn.connect();

			OutputStream outputStream = httpConn.getOutputStream();
			writeMmapData(outputStream, mcc, mnc, lac, cid);

			InputStream inputStream = httpConn.getInputStream();
			DataInputStream dataInputStream = new DataInputStream(inputStream);

			dataInputStream.readShort();
			dataInputStream.readByte();
			int code = dataInputStream.readInt();
			if (code == 0) {
				double lat = (double) dataInputStream.readInt() / 1000000D;
				double lon = (double) dataInputStream.readInt() / 1000000D;
				return new JsonObject().putNumber("lat", lat).putNumber("lon", lon);

			}
			return new JsonObject().putString("error", "Code " + code);
		} catch (IOException e) {
			e.printStackTrace();
			return new JsonObject().putString("error", e.getLocalizedMessage());
		}

	}

	private void writeMmapData(OutputStream out, int mcc, int mnc, int lac, int cid) throws IOException {
		DataOutputStream dataOutputStream = new DataOutputStream(out);
		dataOutputStream.writeShort(21);
		dataOutputStream.writeLong(0);
		dataOutputStream.writeUTF("en");
		dataOutputStream.writeUTF("Android");
		dataOutputStream.writeUTF("1.0");
		dataOutputStream.writeUTF("Web");
		dataOutputStream.writeByte(27);
		dataOutputStream.writeInt(0);
		dataOutputStream.writeInt(0);
		dataOutputStream.writeInt(3);
		dataOutputStream.writeUTF("");

		dataOutputStream.writeInt(cid);
		dataOutputStream.writeInt(lac);

		dataOutputStream.writeInt(mcc);
		dataOutputStream.writeInt(mnc);
		
		dataOutputStream.writeInt(0);
		dataOutputStream.writeInt(0);
		dataOutputStream.flush();
	}    

    private  void insertIncGoogleMmapResult(Integer mcc, Integer mnc, Integer lac, Integer cid, Double lon, Double lat) {
        
        JsonObject document = new JsonObject()
				.putNumber("mcc", mcc)
				.putNumber("mnc", mnc)
				.putNumber("lac", lac)
				.putNumber("cid", cid)
				.putNumber("lon", lon)
				.putNumber("lat", lat)
				.putNumber("nUsed", 1);
		
		JsonObject query = new JsonObject()
        		.putString("action", "save")
		        .putString("collection", collectionGoogleMmapCache)
		        .putObject("document", document);
		
		System.out.println(query.encodePrettily());
		
		eb.send("mongodb-persistor-celltower", query);
    }
    
	private class Tower {
		private int mcc;
		private int mnc;
		private int lac;
		private int cid;

		public Tower(int mcc, int mnc, int lac, int cid) {
			this.mcc = mcc;
			this.mnc = mnc;
			this.lac = lac;
			this.cid = cid;
		}

		public int getMcc() {
			return mcc;
		}

		public int getMnc() {
			return mnc;
		}

		public int getLac() {
			return lac;
		}

		public int getCid() {
			return cid;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Tower) {
				Tower objTower = (Tower) obj;
				if (mcc == objTower.getMcc() && mnc == objTower.getMnc() && lac == objTower.getLac() && cid == objTower.getCid()) {
					return true;
				}
			}
			return false;
		}
	}
}
