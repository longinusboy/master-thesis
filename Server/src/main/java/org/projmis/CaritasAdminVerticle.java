package org.projmis;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.projmis.caritas.Constants;
import org.projmis.caritas.Utils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class CaritasAdminVerticle extends Verticle {
	static EventBus eb = null;

	@Override
	public void start() {
		System.out.println("Starting Admin Verticle " + this.getClass().getName());
		eb = vertx.eventBus();
		eb.registerHandler(Constants.handlerNameAdmin, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(final Message<JsonObject> message) {
				JsonObject messageBody = message.body();
				try {
					String op = messageBody.getString("op");
					String page = messageBody.getString("page");
					String idCampaign = messageBody.getString("idCampaign");
					String idDevice = messageBody.getString("idDevice");
					String obj = messageBody.getString("obj");
					String stat = messageBody.getString("stat");
					String type = messageBody.getString("type");
					Integer value = messageBody.getInteger("value");
					String action = messageBody.getString("action");

					if (op.contentEquals("get")) {

						switch (page) {
							case "index":
								doGetIndex(message);
								break;
							case "campaigns":
								doGetCampaigns(message);
								break;
							case "conflicts":
								doGetConflicts(message);
								break;
							case "devices":
								doGetDevices(message);
								break;
							case "stats":
								if (idCampaign != null) {
									doGetStats(message, idCampaign);
								} else {
									doGetStats(message);
								}
								break;
							case "objectives":
								if (idCampaign != null) {
									doGetObjectives(message, idCampaign);
								} else {
									doGetObjectives(message);
								}
								break;
							case "export":
								if (idCampaign == null) {
									doGetExport(message);
								} else {
									if (obj == null) {
										doGetExport(message, idCampaign);
									} else {
										switch (obj) {
											case "prod":
												doGetExportProd(message, idCampaign);
												break;
											case "stats":
												doGetExportStats(message, idCampaign);
												break;
											case "reg":
												doGetExportReg(message, idCampaign);
												break;
											default:
												break;
										}
									}
								}
								break;

							default:
								Utils.log(this.getClass().getName(), "GET", "Page not found: "+op);
								break;
						}
						return;
					}

					if (op.contentEquals("post")) {
						switch (page) {
							case "campaigns":
								switch (action) {
									case "vis":
										doPostCampaignVisibility(message, idCampaign, stat);
										break;
									case "new":
										doPostCampaignNew(message, messageBody.getString("name"), messageBody.getString("password"), messageBody.getString("warehouse"), 
												messageBody.getLong("startDate"), messageBody.getLong("endDate"));
										break;
									case "del":
										doPostCampaignDel(message, idCampaign);
										break;
									default:
										break;
								}
								
								break;
							case "conflicts":
								doPostConflicts(message, obj, value);
								break;
							case "devices":
								doPostDevices(message, idDevice, stat);
								break;
							case "objectives":
								doPostObjectives(message, idCampaign, type, value);
								break;
							default:
								Utils.log(this.getClass().getName(), "POST", "Page not found: "+op);
								break;
						}

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

	protected void doPostCampaignDel(Message<JsonObject> message, String idCampaign) {
		JsonObject jsonCampaigns = new JsonObject()
				.putString("collection", "campaigns")
				.putString("action", "delete")
				.putObject("matcher", new JsonObject().putString("idCampaign", idCampaign));
		eb.send(Constants.handlerDataBase, jsonCampaigns);
		JsonObject jsonRegistries = new JsonObject()
				.putString("collection", "registries")
				.putString("action", "delete")
				.putObject("matcher", new JsonObject().putString("idCampaign", idCampaign));
		eb.send(Constants.handlerDataBase, jsonRegistries);
		JsonObject jsonRegistriesSingle = new JsonObject()
				.putString("collection", "registriesSingle")
				.putString("action", "delete")
				.putObject("matcher", new JsonObject().putString("idCampaign", idCampaign));
		eb.send(Constants.handlerDataBase, jsonRegistriesSingle);
		JsonObject jsonUnknowProducts = new JsonObject()
				.putString("collection", "unknowProducts")
				.putString("action", "delete")
				.putObject("matcher", new JsonObject().putString("idCampaign", idCampaign));
		eb.send(Constants.handlerDataBase, jsonUnknowProducts);
		
		message.reply(new JsonObject().putString("status", "ok"));
	}

	protected void doPostCampaignNew(final Message<JsonObject> message, final String name, final String password, final String warehouse, final Long startDateLong, final Long endDateLong) {
		final String idCampaign = UUID.randomUUID().toString();
		JsonObject jsonSearchCampaign = new JsonObject().putString("collection", "campaigns").putString("action", "findone")
				.putObject("matcher", new JsonObject().putString("name", name));

		eb.send(Constants.handlerDataBase, jsonSearchCampaign, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> messageMongoDB) {
				try {
					if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
						message.reply(new JsonObject().putString("status", "error").putString("error", ""));
						return;
					} else {
						JsonObject campaign = messageMongoDB.body().getObject("result");
						if (campaign == null || campaign.getString("name")==null) {
							JsonObject jsonCampaigns = new JsonObject()
									.putString("collection", "campaigns")
									.putString("action", "save")
									.putObject("document", new JsonObject().putString("idCampaign", idCampaign).putString("name", name).putString("password", password)
											.putString("warehouse", warehouse).putNumber("dateStart", startDateLong).putNumber("dateEnd", endDateLong)
											.putBoolean("visible", false).putNumber("numDevices", 0)
											.putObject("objectives", new JsonObject().putNumber("a", (long)0).putNumber("b", (long)0).putNumber("c", (long)0)
													.putNumber("d", (long)0).putNumber("e", (long)0).putNumber("f", (long)0).putNumber("g", (long)0)
													.putNumber("h", (long)0).putNumber("i", (long)0))
											.putObject("stats", new JsonObject().putNumber("a", (long)0).putNumber("b", (long)0).putNumber("c", (long)0)
													.putNumber("d", (long)0).putNumber("e", (long)0).putNumber("f", (long)0).putNumber("g", (long)0)
													.putNumber("h", (long)0).putNumber("i", (long)0))
											.putNumber("sumWeights", 0L)
											.putNumber("sumRegistries", 0L)
											.putObject("detailedStats", new JsonObject()));
							eb.send(Constants.handlerDataBase, jsonCampaigns);
		
							message.reply(new JsonObject().putString("status", "ok"));
							return;
						}else {
							message.reply(new JsonObject().putString("status", "error").putString("error", "dup"));
							return;
						}
					}
				} catch (Exception e) {
					Utils.saveException(this.getClass().getName(), e);
					message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
				}
			}
		});		
	}

	protected void doPostObjectives(final Message<JsonObject> message, String idCampaign, String type, Integer value) {
		JsonObject jsonUpdateObjective = new JsonObject()
				.putString("collection", "campaigns")
				.putString("action", "update")
				.putObject("criteria", new JsonObject().putString("idCampaign", idCampaign))
				.putObject("objNew", new JsonObject().putObject("$set", new JsonObject().putNumber("objectives."+type, (long)value*1000)))
				.putBoolean("upsert", true)
				.putBoolean("multi", false);
		eb.send(Constants.handlerDataBase, jsonUpdateObjective, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> messageMongoDB) {
				try {
					if (messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
						message.reply(new JsonObject().putString("status", "ok"));
					}else {
						message.reply(new JsonObject().putString("status", "error"));
					}
				}catch(Exception e) {
					Utils.saveException(this.getClass().getName(), e);
					message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
				}
			}
		});
	}
	
	protected void doPostConflicts(final Message<JsonObject> message, final String obj, final Integer value) {
		JsonObject jsonFindConflict = new JsonObject()
				.putString("collection", "productConflicts")
				.putString("action", "findone")
				.putObject("matcher", new JsonObject().putObject("_id", new JsonObject().putString("$oid", obj)));
		
		eb.send(Constants.handlerDataBase, jsonFindConflict, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> messageMongoDB) {
				try {
					if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
						message.reply(new JsonObject().putString("status", "error"));
						return;
					}
					
					JsonObject conflict = messageMongoDB.body().getObject("result");
					if (conflict == null) {
						message.reply(new JsonObject().putString("status", "error"));
						return;
					}
					
					String ean = conflict.getString("ean");
					JsonArray entries = conflict.getArray("entries");
					JsonObject choosedEntry = entries.get(value);
					
					JsonObject jsonUpdateConflict = new JsonObject()
							.putString("collection", "productConflicts")
							.putString("action", "update")
							.putObject("criteria", new JsonObject().putObject("_id", new JsonObject().putString("$oid", obj)))
							.putObject("objNew", new JsonObject()
									.putObject("$set", new JsonObject()
											.putBoolean("resolved", true)
											.putNumber("choosed", value)))
							.putBoolean("upsert", false)
							.putBoolean("multi", false);
					
					eb.send(Constants.handlerDataBase, jsonUpdateConflict);
					
					JsonObject jsonUpdateProduct = new JsonObject()
							.putString("collection", "products")
							.putString("action", "update")
							.putObject("criteria", new JsonObject().putString("ean", ean))
							.putObject("objNew", new JsonObject()
									.putObject("$set", new JsonObject()
											.putString("type", choosedEntry.getString("type"))
											.putNumber("weight", choosedEntry.getInteger("weight"))
											.putString("unit", choosedEntry.getString("unit"))
											.putNumber("realWeight", choosedEntry.getInteger("realWeight"))
											.putNumber("modified", choosedEntry.getLong("modified"))
											.putString("name", choosedEntry.getString("name"))
											.putString("owner", choosedEntry.getString("owner"))))
							.putBoolean("upsert", false)
							.putBoolean("multi", false);
					
					eb.send(Constants.handlerDataBase, jsonUpdateProduct, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> messageMongoDB) {
							try {
								if (messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
									message.reply(new JsonObject().putString("status", "ok"));
								}else {
									message.reply(new JsonObject().putString("status", "error"));
								}
								return;								
							}catch(Exception e) {
								Utils.saveException(this.getClass().getName(), e);
								message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
							}
						}
					});
				}catch(Exception e) {
					Utils.saveException(this.getClass().getName(), e);
					message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
				}
			}
		});
	}

	protected void doPostDevices(final Message<JsonObject> message, String idDevice, String stat) {
		JsonObject jsonUpdateDevice;
		if (stat.contentEquals("true")) {
			jsonUpdateDevice = new JsonObject()
					.putString("collection", "devices")
					.putString("action", "update")
					.putObject("criteria", new JsonObject().putString("idDevice", idDevice))
					.putObject("objNew", new JsonObject().putObject("$set", new JsonObject().putBoolean("isblocked", true)))
					.putBoolean("upsert", false)
					.putBoolean("multi", false);
		}else {
			jsonUpdateDevice = new JsonObject()
					.putString("collection", "devices")
					.putString("action", "update")
					.putObject("criteria", new JsonObject().putString("idDevice", idDevice))
					.putObject("objNew", new JsonObject().putObject("$set", new JsonObject().putBoolean("isblocked", false)))
					.putBoolean("upsert", false)
					.putBoolean("multi", false);
		}
		
		eb.send(Constants.handlerDataBase, jsonUpdateDevice, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> messageMongoDB) {
				try {
					if (messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
						message.reply(new JsonObject().putString("status", "ok"));
					}else {
						message.reply(new JsonObject().putString("status", "error"));
					}
				}catch(Exception e) {
					Utils.saveException(this.getClass().getName(), e);
					message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
				}
			}
		});
	}

	protected void doPostCampaignVisibility(final Message<JsonObject> message, String idCampaign, String stat) {
		JsonObject jsonUpdateCampaign;
		if (stat.contentEquals("true")) {
			jsonUpdateCampaign = new JsonObject()
					.putString("collection", "campaigns")
					.putString("action", "update")
					.putObject("criteria", new JsonObject().putString("idCampaign", idCampaign))
					.putObject("objNew", new JsonObject().putObject("$set", new JsonObject().putBoolean("visible", true)))
					.putBoolean("upsert", false)
					.putBoolean("multi", false);
		}else {
			jsonUpdateCampaign = new JsonObject()
					.putString("collection", "campaigns")
					.putString("action", "update")
					.putObject("criteria", new JsonObject().putString("idCampaign", idCampaign))
					.putObject("objNew", new JsonObject().putObject("$set", new JsonObject().putBoolean("visible", false)))
					.putBoolean("upsert", false)
					.putBoolean("multi", false);
		}
		
		eb.send(Constants.handlerDataBase, jsonUpdateCampaign, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> messageMongoDB) {
				try {
					if (messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
						message.reply(new JsonObject().putString("status", "ok"));
					}else {
						message.reply(new JsonObject().putString("status", "error"));
					}
				}catch(Exception e) {
					Utils.saveException(this.getClass().getName(), e);
					message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
				}
			}
		});
	}

	private static class ExportRegReplyHandler implements Handler<Message<JsonObject>> {
		 
        private final Message<JsonObject> request;
        private JsonArray data;
        private String idCampaign;
 
        private ExportRegReplyHandler(final Message<JsonObject> request, String idCampaign, JsonArray data) {
            this.request = request;
            this.data = data;
            this.idCampaign = idCampaign;
        }
 
        @Override
        public void handle(final Message<JsonObject> event) {			
			try {
				if (event.body().getString("status").equalsIgnoreCase("more-exist")) {
					JsonArray products = event.body().getArray("results");
					for (int i = 0; i < products.size(); i++) {
						data.add(products.get(i));
					}
					event.reply(new JsonObject(), new ExportProdReplyHandler(request, idCampaign, data));
					return;
				}
				if (!event.body().getString("status").equalsIgnoreCase("ok")) {
					request.reply(new JsonObject().putString("status", "ok").putString("result", "Erro a processar dados"));
					return;
				} else {
					JsonObject jsonSearchTypes = new JsonObject().putString("collection", "types").putString("action", "find");
					
					eb.send(Constants.handlerDataBase, jsonSearchTypes, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> messageMongoDB) {
							try {
								if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
									request.reply(new JsonObject().putString("status", "ok").putString("result", "Erro a processar dados"));
									return;
								} else {
									final HashMap<String, String> types = new HashMap<String, String>();
									JsonArray resultsTypes = messageMongoDB.body().getArray("results");
									for (int i = 0; i < resultsTypes.size(); i++) {
										JsonObject resultType = resultsTypes.get(i);
										JsonArray subtypes = resultType.getArray("subtypes");
										for (int j = 0; j < subtypes.size(); j++) {
											JsonObject typeObject = subtypes.get(j);
											types.put(resultType.getString("symbol")+"-"+typeObject.getInteger("id"), 
													replaceUrlEncodes(resultType.getString("name")+" - "+typeObject.getString("name")));
										}
									}
									
									SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
									
									JsonArray products = event.body().getArray("results");
									for (int i = 0; i < products.size(); i++) {
										data.add(products.get(i));
									}
									
									final StringBuilder sb = new StringBuilder();
									
									sb.append("Data;EAN;Quant.;Tipo;Valor unidade;Peso real;Dispositivo\n");
									
									for (int i = 0; i < data.size(); i++) {
										JsonObject registry = data.get(i);
										long date = registry.getLong("date", 0);
										sb.append("\""+((date!=0)?df.format(new Date(date)):"") + "\";");
										String ean = registry.getString("ean");
										sb.append("\""+ ((ean!=null)?("'"+ean):"") + "\";");
										sb.append(registry.getInteger("quantity") + ";");
										String type = registry.getString("type");
										String typeName = types.get(type);
										sb.append("\""+((typeName!=null)?typeName:"") + "\";");
										if (registry.getInteger("weight") != null && registry.getString("unit") != null) {
											sb.append(registry.getInteger("weight") + registry.getString("unit") + ";");
										}else {
											sb.append(";");
										}										
										Integer realWeight = registry.getInteger("realWeight");						
										sb.append(((realWeight!=null)?registry.getInteger("realWeight"):"") + ";");
										sb.append("\""+registry.getString("idDevice") + "\"\n");
									}
									
									JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "findone")
											.putObject("matcher", new JsonObject().putString("idCampaign", idCampaign));

									eb.send(Constants.handlerDataBase, jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> messageMongoDB) {
											JsonObject requestResponse = new JsonObject().putString("status", "ok").putString("result",sb.toString())
													.putBoolean("download", true);
											if (messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
												requestResponse.putString("campaignName", messageMongoDB.body().getObject("result").getString("name"));
											} 
											request.reply(requestResponse);
										}
									});
								}
							}catch(Exception e) {
								Utils.saveException(this.getClass().getName(), e);
								request.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
							}
						}
					});					
				}
				return;
			} catch (Exception e) {
				Utils.saveException(this.getClass().getName(), e);
				request.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
			}
		}
    }

	protected void doGetExportReg(final Message<JsonObject> message, final String idCampaign) {
		vertx.fileSystem().readFile("caritas/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject()
							.putString("collection", "registriesSingle")
							.putString("action", "find")
							.putObject("matcher", new JsonObject().putString("idCampaign", idCampaign));

					eb.send(Constants.handlerDataBase, jsonSearchCampaigns, new ExportRegReplyHandler(message, idCampaign, new JsonArray()));
				} else {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ar.cause().printStackTrace(pw);
					String exceptionString = sw.toString();
					pw.close();
					try {
						sw.close();
					} catch (Exception e) {
					}
					Utils.saveException(this.getClass().getName(), exceptionString);
					message.reply(new JsonObject().putString("status", "error").putString("error", ar.cause().toString()));
				}
			}
		});
	}

	protected void doGetExportStats(final Message<JsonObject> message, final String idCampaign) {
		JsonObject jsonSearchTypes = new JsonObject().putString("collection", "types").putString("action", "find");
		
		eb.send(Constants.handlerDataBase, jsonSearchTypes, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> messageMongoDB) {
				try {
					if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
						message.reply(new JsonObject().putString("status", "ok").putString("result", "Erro a processar dados"));
						return;
					} else {
						final HashMap<String, String> types = new HashMap<String, String>();
						JsonArray resultsTypes = messageMongoDB.body().getArray("results");
						for (int i = 0; i < resultsTypes.size(); i++) {
							JsonObject resultType = resultsTypes.get(i);
							JsonArray subtypes = resultType.getArray("subtypes");
							for (int j = 0; j < subtypes.size(); j++) {
								JsonObject typeObject = subtypes.get(j);
								types.put(resultType.getString("symbol")+"-"+typeObject.getInteger("id"), resultType.getString("name")+" - "+typeObject.getString("name"));
							}
						}
						
						JsonObject jsonSearchStat = new JsonObject().putString("collection", "campaigns").putString("action", "findone")
								.putObject("matcher", new JsonObject().putString("idCampaign", idCampaign));

						eb.send(Constants.handlerDataBase, jsonSearchStat, new Handler<Message<JsonObject>>() {
							@SuppressWarnings("unchecked")
							@Override
							public void handle(Message<JsonObject> messageMongoDB) {
								StringBuilder sb = new StringBuilder();
								try {
									if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
										message.reply(new JsonObject().putString("status", "ok").putString("result", "Erro a processar dados"));
										return;
									} else {
										SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
										
										JsonObject resultStat = messageMongoDB.body().getObject("result");
										JsonObject detaildedValues = resultStat.getObject("detailedStats");
										
										//header start
										sb.append("\"Hora inicio\";\"Hora fim\"");
										Iterator<Map.Entry<String, String>> itTypesHeader = types.entrySet().iterator();
										while (itTypesHeader.hasNext()) {
											sb.append(";\""+replaceUrlEncodes(itTypesHeader.next().getValue())+"\"");
										}
										sb.append("\n");
										//header end
										
										Map<String, Object> detailedStats = detaildedValues.toMap();
										Map<String, Object> sortedDetailedValues = new TreeMap<String, Object>(detailedStats);
										
										Iterator<Map.Entry<String, Object>> it = sortedDetailedValues.entrySet().iterator();
										while (it.hasNext()) {
											Entry<String, Object> entry = it.next();
											String timesString = entry.getKey();
											sb.append("\""+df.format(new Date(Long.valueOf(timesString.split("-")[0])))+"\"");
											sb.append(";\""+df.format(new Date(Long.valueOf(timesString.split("-")[1])))+"\"");
											
											Iterator<Map.Entry<String, String>> itTypes = types.entrySet().iterator();
											while (itTypes.hasNext()) {
												try {
													Object value = ((LinkedHashMap<String, Object>)entry.getValue()).get(itTypes.next().getKey());
													if (value instanceof Integer || value instanceof Long || value instanceof Double) {
														sb.append(";"+((value==null)?"0":value));
													}else {
														sb.append(";0");
													}
													
												} catch (Exception e) {
													Utils.saveException(this.getClass().getName(), e);
													sb.append(";0");
												}
												
											}
											sb.append("\n");
										}
										JsonObject requestResponse = new JsonObject().putString("status", "ok").putString("result",sb.toString())
												.putBoolean("download", true).putString("campaignName", messageMongoDB.body().getObject("result").getString("name"));
										message.reply(requestResponse);
									}
									message.reply(new JsonObject().putString("status", "ok").putString("result",sb.toString()).putBoolean("download", true));
								} catch (Exception e) {
									Utils.saveException(this.getClass().getName(), e);
									message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
								}
							}
						});
						return;
					}
				} catch (Exception e) {
					Utils.saveException(this.getClass().getName(), e);
					message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
				}
			}
		});
	}
	
	private static class ExportProdReplyHandler implements Handler<Message<JsonObject>> {
		 
        private final Message<JsonObject> request;
        private JsonArray data;
        private String idCampaign;
 
        private ExportProdReplyHandler(final Message<JsonObject> request, String idCampaign, JsonArray data) {
            this.request = request;
            this.data = data;
            this.idCampaign = idCampaign;
        }
 
        @Override
        public void handle(final Message<JsonObject> event) {
			try {
				if (event.body().getString("status").equalsIgnoreCase("more-exist")) {
					JsonArray products = event.body().getArray("results");
					for (int i = 0; i < products.size(); i++) {
						data.add(products.get(i));
					}
					event.reply(new JsonObject(), new ExportProdReplyHandler(request, idCampaign, data));
					return;
				}
				if (!event.body().getString("status").equalsIgnoreCase("ok")) {
					request.reply(new JsonObject().putString("status", "ok").putString("result","Erro a processar dados"));
					return;
				} else {
					
					JsonObject jsonSearchTypes = new JsonObject().putString("collection", "types").putString("action", "find");
					
					eb.send(Constants.handlerDataBase, jsonSearchTypes, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> messageMongoDB) {
							try {
								if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
									request.reply(new JsonObject().putString("status", "ok").putString("result", "Erro a processar dados"));
									return;
								} else {
									final StringBuilder sb = new StringBuilder();
									
									final HashMap<String, String> types = new HashMap<String, String>();
									JsonArray resultsTypes = messageMongoDB.body().getArray("results");
									for (int i = 0; i < resultsTypes.size(); i++) {
										JsonObject resultType = resultsTypes.get(i);
										JsonArray subtypes = resultType.getArray("subtypes");
										for (int j = 0; j < subtypes.size(); j++) {
											JsonObject typeObject = subtypes.get(j);
											types.put(resultType.getString("symbol")+"-"+typeObject.getInteger("id"), 
													replaceUrlEncodes(resultType.getString("name")+" - "+typeObject.getString("name")));
										}
									}
									SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
									
									JsonArray products = event.body().getArray("results");
									for (int i = 0; i < products.size(); i++) {
										data.add(products.get(i));
									}
									
									sb.append("EAN;Tipo;Tipo (Nome);Valor;Unid.;Peso real;\u00DAltima modif.;Nome\n");
									
									for (int i = 0; i < data.size(); i++) {
										JsonObject product = data.get(i);
										sb.append("\""+product.getString("ean") + "\";");
										sb.append(product.getString("type") + ";");
										sb.append("\""+replaceUrlEncodes(types.get(product.getString("type")))+"\";");
										sb.append(product.getInteger("weight") + ";");
										sb.append("\""+product.getString("unit") + "\";");
										sb.append(product.getInteger("realWeight") + ";");
										sb.append(df.format(new Date(product.getLong("modified"))) + ";");
										sb.append("\""+replaceUrlEncodes(product.getString("name"))+"\"\n");
									}
									
									JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "findone")
											.putObject("matcher", new JsonObject().putString("idCampaign", idCampaign));

									eb.send(Constants.handlerDataBase, jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> messageMongoDB) {
											JsonObject requestResponse = new JsonObject().putString("status", "ok").putString("result",sb.toString())
													.putBoolean("download", true);
											if (messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
												requestResponse.putString("campaignName", messageMongoDB.body().getObject("result").getString("name"));
											} 
											request.reply(requestResponse);
										}
									});									
								}
							}catch(Exception e) {
								Utils.saveException(this.getClass().getName(), e);
								request.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
							}
						}
					});
					
					
				}				
			} catch (Exception e) {
				Utils.saveException(this.getClass().getName(), e);
				request.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
			}
		}
    }
	
	private static String replaceUrlEncodes(String string) {
		String localString = new String(string.toLowerCase());
		
		String[][] replacements = {
				{"&aacute;", "\u00E1"}, 
                {"&agrave;", "\u00E0"},
                {"&acirc;", "\u00E2"},
                {"&atilde;", "\u00E3"},
                {"&eacute;", "\u00E9"}, 
                {"&egrave;", "\u00E8"},
                {"&ecirc;", "\u00EA"},
                {"&iacute;", "\u00ED"},
                {"&oacute;", "\u00F3"},
                {"&otilde;", "\u00F5"},
                {"&uacute;", "\u00Fa"},
				{"&Aacute;", "\u00C1"}, 
                {"&Agrave;", "\u00C0"},
                {"&Acirc;", "\u00C2"},
                {"&Atilde;", "\u00C3"},
                {"&Eacute;", "\u00C9"}, 
                {"&Egrave;", "\u00C8"},
                {"&Ecirc;", "\u00CA"},
                {"&Iacute;", "\u00CD"},
                {"&Oacute;", "\u00D3"},
                {"&Otilde;", "\u00D5"},
                {"&Uacute;", "\u00DA"},
                {"&ccedil;", "\u00E7"},
		};

		for(String[] replacement: replacements) {
			localString = localString.replace(replacement[0], replacement[1]);
		}
		
		return localString;
	}

	protected void doGetExportProd(final Message<JsonObject> message, String idCampaign) {
		//Actually idCampaign does nothing
		JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "products").putString("action", "find");
		
		eb.send(Constants.handlerDataBase, jsonSearchCampaigns, new ExportProdReplyHandler(message, idCampaign, new JsonArray()));
	}

	protected void doGetExport(final Message<JsonObject> message, final String idCampaign) {
		vertx.fileSystem().readFile("caritas/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "findone")
							.putObject("matcher", new JsonObject().putString("idCampaign", idCampaign));

					eb.send(Constants.handlerDataBase, jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> messageMongoDB) {
							StringBuilder sb = new StringBuilder();
							try {
								if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
									sb.append("<span class\"error\">Erro a processar dados</span>");
								} else {
									JsonObject campaign = messageMongoDB.body().getObject("result");
									if (campaign == null || campaign.getString("idCampaign")==null) {
										sb.append("<span class\"error\">Campanha inexistente</span>");
									}else {
										sb.append("<script language=\"JavaScript\">\n");
										sb.append("function loadPage(url) {\n");
										sb.append("location.href=\"url\"\n");
										sb.append("}\n");
										sb.append("</script>\n");
										sb.append("<a href=\"export.html?idCampaign="+idCampaign+"&obj=prod\">Exportar Produtos</a><br/><br/>");
										sb.append("<a href=\"export.html?idCampaign="+idCampaign+"&obj=stats\">Exportar Estat&iacute;sticas/Tempo</a><br/><br/>");
										sb.append("<a href=\"export.html?idCampaign="+idCampaign+"&obj=reg\">Exportar Registos</a>");
									}									
								}
								message.reply(new JsonObject().putString("status", "ok").putString("result",
										new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString())));
							} catch (Exception e) {
								Utils.saveException(this.getClass().getName(), e);
								message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
							}
						}
					});
					return;
				} else {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ar.cause().printStackTrace(pw);
					String exceptionString = sw.toString();
					pw.close();
					try {
						sw.close();
					} catch (Exception e) {
					}
					Utils.saveException(this.getClass().getName(), exceptionString);
					message.reply(new JsonObject().putString("status", "error").putString("error", ar.cause().toString()));
				}
			}
		});
	}

	protected void doGetExport(final Message<JsonObject> message) {
		vertx.fileSystem().readFile("caritas/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "find");

					eb.send(Constants.handlerDataBase, jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> messageMongoDB) {
							StringBuilder sb = new StringBuilder();
							try {
								if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
									sb.append("<span class\"error\">Erro a processar dados</span>");
								} else if (messageMongoDB.body().getInteger("number") == 0) {
									sb.append("Nenhuma campanha disponivel");
								} else {
									sb.append("<script language=\"JavaScript\">\n");
									sb.append("function loadPage(list) {\n");
									sb.append("location.href=\"export.html?idCampaign=\" + list.options[list.selectedIndex].value\n");
									sb.append("}\n");
									sb.append("</script>\n");

									sb.append("<form>");
									sb.append("Escolha a campanha<br /><br />");
									sb.append("<select onChange=\"loadPage(this.form.elements[0])\">");
									sb.append("<option></option>");
									JsonArray resultCampaigns = messageMongoDB.body().getArray("results");
									for (int i = 0; i < resultCampaigns.size(); i++) {
										JsonObject campaign = resultCampaigns.get(i);
										sb.append("<option value=\"" + URLEncoder.encode(campaign.getString("idCampaign"), "UTF-8") + "\">");
										sb.append(campaign.getString("name") + "</option>");
									}
									sb.append("</select></table>");
								}
								message.reply(new JsonObject().putString("status", "ok").putString("result",
										new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString())));
							} catch (Exception e) {
								Utils.saveException(this.getClass().getName(), e);
								message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
							}
						}
					});
					return;
				} else {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ar.cause().printStackTrace(pw);
					String exceptionString = sw.toString();
					pw.close();
					try {
						sw.close();
					} catch (Exception e) {
					}
					Utils.saveException(this.getClass().getName(), exceptionString);
					message.reply(new JsonObject().putString("status", "error").putString("error", ar.cause().toString()));
				}
			}
		});
	}

	protected void doGetObjectives(final Message<JsonObject> message) {
		vertx.fileSystem().readFile("caritas/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "find");

					eb.send(Constants.handlerDataBase, jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> messageMongoDB) {
							StringBuilder sb = new StringBuilder();
							try {
								if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
									sb.append("<span class\"error\">Erro a processar dados</span>");
								} else if (messageMongoDB.body().getInteger("number") == 0) {
									sb.append("Nenhuma campanha disponivel");
								} else {
									sb.append("<script language=\"JavaScript\">\n");
									sb.append("function loadPage(list) {\n");
									sb.append("location.href=\"objectives.html?idCampaign=\" + list.options[list.selectedIndex].value\n");
									sb.append("}\n");
									sb.append("</script>\n");

									sb.append("<form>");
									sb.append("Escolha a campanha<br /><br />");
									sb.append("<select onChange=\"loadPage(this.form.elements[0])\">");
									sb.append("<option></option>");
									JsonArray resultCampaigns = messageMongoDB.body().getArray("results");
									for (int i = 0; i < resultCampaigns.size(); i++) {
										JsonObject campaign = resultCampaigns.get(i);
										sb.append("<option value=\"" + URLEncoder.encode(campaign.getString("idCampaign"), "UTF-8") + "\">");
										sb.append(campaign.getString("name") + "</option>");
									}
									sb.append("</select></table>");
								}
								message.reply(new JsonObject().putString("status", "ok").putString("result",
										new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString())));
							} catch (Exception e) {
								Utils.saveException(this.getClass().getName(), e);
								message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
							}
						}
					});
					return;
				} else {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ar.cause().printStackTrace(pw);
					String exceptionString = sw.toString();
					pw.close();
					try {
						sw.close();
					} catch (Exception e) {
					}
					Utils.saveException(this.getClass().getName(), exceptionString);
					message.reply(new JsonObject().putString("status", "error").putString("error", ar.cause().toString()));
				}
			}
		});
	}

	protected void doGetObjectives(final Message<JsonObject> message, final String idCampaign) {
		vertx.fileSystem().readFile("caritas/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "findone")
							.putObject("matcher", new JsonObject().putString("idCampaign", idCampaign));

					eb.send(Constants.handlerDataBase, jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> messageMongoDB) {
							StringBuilder sb = new StringBuilder();
							try {
								if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
									sb.append("<span class\"error\">Erro a processar dados</span>");
								} else {
									try {
										JsonObject objective = messageMongoDB.body().getObject("result");
										JsonObject objectiveValues = objective.getObject("objectives");
										
										sb.append("<script language=\"JavaScript\">\n");
										
										sb.append("function ajaxRequest(){\n");
										sb.append("var activexmodes=[\"Msxml2.XMLHTTP\", \"Microsoft.XMLHTTP\"]\n");
										sb.append("if (window.ActiveXObject){\n");
										sb.append("try{ return new ActiveXObject(activexmodes[i]);}catch(e){}\n");
										sb.append("}else if (window.XMLHttpRequest) return new XMLHttpRequest(); else return false;}\n");
										
										sb.append("function valueChanged(type, value) {\n");
										sb.append("var myrequest=new ajaxRequest()\n");
										sb.append("myrequest.open(\"POST\", \"objectives.html?idCampaign="+idCampaign+"&type=\"+type+\"&value=\"+value, true)\n");
										sb.append("myrequest.send()}\n");
										
										sb.append("</script>\n");

										sb.append("<table style=\"width: 310px\">");
										sb.append("<tr><td style=\"width: 200px\">Tipo</td><td style=\"width: 98px\">Valor (Kg)</td></tr>");

										sb.append("<tr><td>Leite e Derivados</td><td><input type=\"text\" onChange=\"valueChanged('a', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("a") / 1000L) + "\" /></td></tr>");
										sb.append("<tr><td>Mercearia</td><td><input type=\"text\" onChange=\"valueChanged('b', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("b") / 1000f) + "\" /></td></tr>");
										sb.append("<tr><td>Cereais</td><td><input type=\"text\" onChange=\"valueChanged('c', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("c") / 1000L) + "\" /></td></tr>");
										sb.append("<tr><td>Hortali&ccedil;as e Frutas</td><td><input type=\"text\" onChange=\"valueChanged('d', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("d") / 1000L) + "\" /></td></tr>");
										sb.append("<tr><td>Enlatados</td><td><input type=\"text\" onChange=\"valueChanged('e', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("e") / 1000L) + "\" /></td></tr>");
										sb.append("<tr><td>L&iacute;quidos</td><td><input type=\"text\" onChange=\"valueChanged('f', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("f") / 1000L) + "\" /></td></tr>");
										sb.append("<tr><td>Produtos de Higiene</td><td><input type=\"text\" onChange=\"valueChanged('g', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("g") / 1000L) + "\" /></td></tr>");
										sb.append("<tr><td>Produtos de Limpeza</td><td><input type=\"text\" onChange=\"valueChanged('h', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("h") / 1000L) + "\" /></td></tr>");
										sb.append("<tr><td>Charcutaria</td><td><input type=\"text\" onChange=\"valueChanged('i', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("i") / 1000L) + "\" /></td></tr>");

										sb.append("</table>");
									} catch (Exception e) {
										Utils.saveException(this.getClass().getName(), e);
										sb.append("<span class\"error\">Erro a obter dados</span>");
									}
								}
								message.reply(new JsonObject().putString("status", "ok").putString("result",
										new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString())));
							} catch (Exception e) {
								Utils.saveException(this.getClass().getName(), e);
								message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
							}
						}
					});
					return;
				} else {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ar.cause().printStackTrace(pw);
					String exceptionString = sw.toString();
					pw.close();
					try {
						sw.close();
					} catch (Exception e) {
					}
					Utils.saveException(this.getClass().getName(), exceptionString);
					message.reply(new JsonObject().putString("status", "error").putString("error", ar.cause().toString()));
				}
			}
		});
	}

	protected void doGetStats(final Message<JsonObject> message) {
		vertx.fileSystem().readFile("caritas/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "find");

					eb.send(Constants.handlerDataBase, jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> messageMongoDB) {
							StringBuilder sb = new StringBuilder();
							try {
								if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
									sb.append("<span class\"error\">Erro a processar dados</span>");
								} else if (messageMongoDB.body().getInteger("number") == 0) {
									sb.append("Nenhuma campanha disponivel");
								} else {
									sb.append("<script language=\"JavaScript\">\n");
									sb.append("function loadPage(list) {\n");
									sb.append("location.href=\"stats.html?idCampaign=\" + list.options[list.selectedIndex].value\n");
									sb.append("}\n");
									sb.append("</script>\n");

									sb.append("<form>");
									sb.append("Escolha a campanha<br /><br />");
									sb.append("<select onChange=\"loadPage(this.form.elements[0])\">");
									sb.append("<option></option>");
									JsonArray resultCampaigns = messageMongoDB.body().getArray("results");
									for (int i = 0; i < resultCampaigns.size(); i++) {
										JsonObject campaign = resultCampaigns.get(i);
										sb.append("<option value=\"" + URLEncoder.encode(campaign.getString("idCampaign"), "UTF-8") + "\">");
										sb.append(campaign.getString("name") + "</option>");
									}
									sb.append("</select></table>");
								}
								message.reply(new JsonObject().putString("status", "ok").putString("result",
										new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString())));
							} catch (Exception e) {
								Utils.saveException(this.getClass().getName(), e);
								message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
							}
						}
					});
					return;
				} else {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ar.cause().printStackTrace(pw);
					String exceptionString = sw.toString();
					pw.close();
					try {
						sw.close();
					} catch (Exception e) {
					}
					Utils.saveException(this.getClass().getName(), exceptionString);
					message.reply(new JsonObject().putString("status", "error").putString("error", ar.cause().toString()));
				}
			}
		});
	}
	
	protected void doGetStats(final Message<JsonObject> message, final String idCampaign) {
		vertx.fileSystem().readFile("caritas/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "findone")
							.putObject("matcher", new JsonObject().putString("idCampaign", idCampaign));

					eb.send(Constants.handlerDataBase, jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> messageMongoDB) {
							StringBuilder sb = new StringBuilder();
							try {
								if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
									sb.append("<span class\"error\">Erro a processar dados</span>");
								} else {
									try {
										JsonObject stat = messageMongoDB.body().getObject("result");
										JsonObject statValues = stat.getObject("stats");

										sb.append("<table style=\"width: 310px\">");
										sb.append("<tr><td style=\"width: 200px\">Tipo</td><td style=\"width: 98px\">Valor (Kg)</td></tr>");

										sb.append("<tr><td>Leite e Derivados</td><td>" + String.format("%.3f", (statValues.getLong("a") / 1000f)) + "</td></tr>");
										sb.append("<tr><td>Mercearia</td><td>" + String.format("%.3f", (statValues.getLong("b") / 1000f)) + "</td></tr>");
										sb.append("<tr><td>Cereais</td><td>" + String.format("%.3f", (statValues.getLong("c") / 1000f)) + "</td></tr>");
										sb.append("<tr><td>Hortali&ccedil;as e Frutas</td><td>" + String.format("%.3f", (statValues.getLong("d") / 1000f)) + "</td></tr>");
										sb.append("<tr><td>Enlatados</td><td>" + String.format("%.3f", (statValues.getLong("e") / 1000f)) + "</td></tr>");
										sb.append("<tr><td>L&iacute;quidos</td><td>" + String.format("%.3f", (statValues.getLong("f") / 1000f)) + "</td></tr>");
										sb.append("<tr><td>Produtos de Higiene</td><td>" + String.format("%.3f", (statValues.getLong("g") / 1000f)) + "</td></tr>");
										sb.append("<tr><td>Produtos de Limpeza</td><td>" + String.format("%.3f", (statValues.getLong("h") / 1000f)) + "</td></tr>");
										sb.append("<tr><td>Charcutaria</td><td>" + String.format("%.3f", (statValues.getLong("i") / 1000f)) + "</td></tr>");

										sb.append("</table>");
									} catch (Exception e) {
										Utils.saveException(this.getClass().getName(), e);
										sb.append("<span class\"error\">Erro a obter dados</span>");
									}
								}
								message.reply(new JsonObject().putString("status", "ok").putString("result",
										new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString())));
							} catch (Exception e) {
								Utils.saveException(this.getClass().getName(), e);
								message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
							}
						}
					});
					return;
				} else {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ar.cause().printStackTrace(pw);
					String exceptionString = sw.toString();
					pw.close();
					try {
						sw.close();
					} catch (Exception e) {
					}
					Utils.saveException(this.getClass().getName(), exceptionString);
					message.reply(new JsonObject().putString("status", "error").putString("error", ar.cause().toString()));
				}
			}
		});
	}

	protected void doGetDevices(final Message<JsonObject> message) {
		vertx.fileSystem().readFile("caritas/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "devices").putString("action", "find");

					eb.send(Constants.handlerDataBase, jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> messageMongoDB) {
							StringBuilder sb = new StringBuilder();
							try {
								if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
									sb.append("<span class\"error\">Erro a processar dados</span>");
								} else if (messageMongoDB.body().getInteger("number") == 0) {
									sb.append("Nenhum dispositivo adicionado");
								} else {
									SimpleDateFormat df = new SimpleDateFormat("dd/MM HH:mm");
									sb.append("<script language=\"JavaScript\">\n");
									
									sb.append("function ajaxRequest(){\n");
									sb.append("var activexmodes=[\"Msxml2.XMLHTTP\", \"Microsoft.XMLHTTP\"]\n");
									sb.append("if (window.ActiveXObject){\n");
									sb.append("try{ return new ActiveXObject(activexmodes[i]);}catch(e){}\n");
									sb.append("}else if (window.XMLHttpRequest) return new XMLHttpRequest(); else return false;}\n");
									
									sb.append("function checkClicked(idDevice, checkbox) {\n");
									sb.append("var myrequest=new ajaxRequest()\n");
									sb.append("if (checkbox.checked)\n");
									sb.append("myrequest.open(\"POST\", \"devices.html?idDevice=\"+idDevice+\"&stat=true\", true)\n");
									sb.append("else myrequest.open(\"POST\", \"devices.html?idDevice=\"+idDevice+\"&stat=false\", true)\n");
									sb.append("myrequest.send()}\n");
									
									sb.append("</script>\n");
									
									sb.append("<table border=\"1\">");
									sb.append("<tr><td>ID</td><td>Bloqueado</td><td>&Uacute;ltima localização</td></tr>");
									JsonArray resultDevices = messageMongoDB.body().getArray("results");
									for (int i = 0; i < resultDevices.size(); i++) {
										JsonObject device = resultDevices.get(i);
										sb.append("<tr>");
										sb.append("<td>" + device.getString("idDevice") + "</td>");
										sb.append("<td><input type='checkbox' onclick=\"checkClicked('"+device.getString("idDevice")+"',this);\" "
												+ (device.getBoolean("isblocked", false) ? "checked=true" : "") + "></td>");
										JsonObject lastLocation = device.getObject("lastLocation");
										if (lastLocation != null) {
											try {
												Long dateValue = lastLocation.getLong("date");
												Number lonNumber = lastLocation.getNumber("lon");
												Number latNumber = lastLocation.getNumber("lat");

												sb.append("<td>");
												if (dateValue != null) {
													sb.append(df.format(new Date(lastLocation.getLong("date"))) + "<br />");
												}

												if (lonNumber != null && latNumber != null) {
													sb.append("<a target=\"_blank\" href=\"https://www.google.pt/maps/@" + latNumber.doubleValue() + "," + 
															lonNumber.doubleValue()	+ ",15z?hl=en\">Ver no mapa</a>");
												}else {
													sb.append("Indispon&iacute;vel");
												}
												sb.append("</td>");
												
											} catch (Exception e) {
												Utils.saveException(this.getClass().getName(), e);
												sb.append("<td>&nbsp;</td>");
											}
										}
										sb.append("</tr>");
									}
									sb.append("</table>");
								}
								message.reply(new JsonObject().putString("status", "ok").putString("result",
										new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString())));
							} catch (Exception e) {
								Utils.saveException(this.getClass().getName(), e);
								message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
							}
						}
					});
					return;
				} else {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ar.cause().printStackTrace(pw);
					String exceptionString = sw.toString();
					pw.close();
					try {
						sw.close();
					} catch (Exception e) {
					}
					Utils.saveException(this.getClass().getName(), exceptionString);
					message.reply(new JsonObject().putString("status", "error").putString("error", ar.cause().toString()));
				}
			}
		});
	}
	
	protected void doGetConflicts(final Message<JsonObject> message) {
		vertx.fileSystem().readFile("caritas/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject()
							.putString("collection", "productConflicts")
							.putString("action", "find")
							.putObject("matcher", new JsonObject().putBoolean("resolved", false));

					eb.send(Constants.handlerDataBase, jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(final Message<JsonObject> messageMongoDB) {
							try {
								if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
									message.reply(new JsonObject().putString("status", "ok").putString("result",
											new String(ar.result().getBytes()).replace("%DATA_CONTENT%", "<span class\"error\">Erro a processar dados</span>")));
									return;
								} else if (messageMongoDB.body().getInteger("number") == 0) {
									message.reply(new JsonObject().putString("status", "ok").putString("result",
											new String(ar.result().getBytes()).replace("%DATA_CONTENT%", "Nenhum conflito encontrado")));
									return;
								} else {
									JsonObject jsonSearchTypes = new JsonObject().putString("collection", "types").putString("action", "find");
									
									eb.send(Constants.handlerDataBase, jsonSearchTypes, new Handler<Message<JsonObject>>() {
										@Override
										public void handle(Message<JsonObject> messageMongoDBTypes) {
											try {
												if (!messageMongoDBTypes.body().getString("status").equalsIgnoreCase("ok")) {
													message.reply(new JsonObject().putString("status", "ok").putString("error", "Erro a processar dados"));
													return;
												} else {
													
													final HashMap<String, String> types = new HashMap<String, String>();
													JsonArray resultsTypes = messageMongoDBTypes.body().getArray("results");
													for (int i = 0; i < resultsTypes.size(); i++) {
														JsonObject resultType = resultsTypes.get(i);
														JsonArray subtypes = resultType.getArray("subtypes");
														for (int j = 0; j < subtypes.size(); j++) {
															JsonObject typeObject = subtypes.get(j);
															types.put(resultType.getString("symbol") + "-" + typeObject.getInteger("id"),
																	replaceUrlEncodes(capitalizeString(resultType.getString("name")) + "<br />"
																			+ capitalizeString(typeObject.getString("name"))));
														}
													}
													
													StringBuilder sb = new StringBuilder();
													
													SimpleDateFormat df = new SimpleDateFormat("MM/dd HH:mm:ss");
													sb.append("<script language=\"JavaScript\">\n");
													
													sb.append("function ajaxRequest(){\n");
													sb.append("var activexmodes=[\"Msxml2.XMLHTTP\", \"Microsoft.XMLHTTP\"]\n");
													sb.append("if (window.ActiveXObject){\n");
													sb.append("try{ return new ActiveXObject(activexmodes[i]);}catch(e){}\n");
													sb.append("}else if (window.XMLHttpRequest) return new XMLHttpRequest(); else return false;}\n");
													
													sb.append("function radioClicked(conflictId, value) {\n");
													sb.append("var myrequest=new ajaxRequest()\n");
													sb.append("myrequest.open(\"POST\", \"conflicts.html?obj=\"+conflictId+\"&value=\"+value, true)\n");
													sb.append("myrequest.send()}\n");
													
													sb.append("</script>\n");
													
													
													JsonArray resultConflicts = messageMongoDB.body().getArray("results");
													for (int i = 0; i < resultConflicts.size(); i++) {
														JsonObject conflict = resultConflicts.get(i);
														sb.append("<form>\n");
														sb.append("<table border=\"1\">\n");
														sb.append("<tr><td>EAN</td><td>Tipo</td><td>Nome</td><td>Autor</td><td>Data</td><td>&nbsp;</td></tr>\n");
														sb.append("<tr>");

														JsonArray conflictEntries = conflict.getArray("entries");
														sb.append("<td rowspan=\"" + conflictEntries.size() + "\">" + conflict.getString("ean") + "</td>\n");

														for (int j = 0; j < conflictEntries.size(); j++) {
															JsonObject conflictEntry = conflictEntries.get(j);

															if (j != 0) {
																sb.append("<tr>\n");
															}

															sb.append("<td>" + capitalizeString(types.get(conflictEntry.getString("type"))) + "</td>\n");
															sb.append("<td>" + conflictEntry.getString("name") + "</td>\n");
															sb.append("<td>" + conflictEntry.getString("owner") + "</td>\n");
															sb.append("<td>" + df.format(new Date(conflictEntry.getLong("modified"))) + "</td>\n");
															String objId = conflict.getObject("_id").getString("$oid");
															sb.append("<td><input type=\"radio\" onclick=\"radioClicked('" + objId + "', '" + j + "')\" name=\"" + objId
																	+ "\" value=\"" + j + "\" />\n");
															sb.append("</tr>");
														}

														sb.append("</table>\n");
														sb.append("</form>\n");
														sb.append("<br/>\n");
													}
													
													message.reply(new JsonObject().putString("status", "ok").putString("result",
															new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString())));
												}
											}catch(Exception e) {
												Utils.saveException(this.getClass().getName(), e);
												message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
											}
										}
									});
								}
								
							} catch (Exception e) {
								Utils.saveException(this.getClass().getName(), e);
								message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
							}
						}
					});
					return;
				} else {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ar.cause().printStackTrace(pw);
					String exceptionString = sw.toString();
					pw.close();
					try {
						sw.close();
					} catch (Exception e) {
					}
					Utils.saveException(this.getClass().getName(), exceptionString);
					message.reply(new JsonObject().putString("status", "error").putString("error", ar.cause().toString()));
				}
			}
		});
	}
	
	public static String capitalizeString(String string) {
		return Character.toUpperCase(string.charAt(0)) + string.substring(1);
	}

	protected void doGetCampaigns(final Message<JsonObject> message) {
		vertx.fileSystem().readFile("caritas/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "find");

					eb.send(Constants.handlerDataBase, jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> messageMongoDB) {
							StringBuilder sb = new StringBuilder();
							try {
								if (!messageMongoDB.body().getString("status").equalsIgnoreCase("ok")) {
									sb.append("<span class\"error\">Erro a processar dados</span>");
								} else {
									if (messageMongoDB.body().getInteger("number") == 0) {
								
										sb.append("Nenhuma campanha adicionada<br /><br />");
										
										sb.append("<script language=\"JavaScript\">\n");
										
										sb.append("function ajaxRequest(){\n");
										sb.append("var activexmodes=[\"Msxml2.XMLHTTP\", \"Microsoft.XMLHTTP\"]\n");
										sb.append("if (window.ActiveXObject){\n");
										sb.append("try{ return new ActiveXObject(activexmodes[i]);}catch(e){}\n");
										sb.append("}else if (window.XMLHttpRequest) return new XMLHttpRequest(); else return false;}\n");
										
										sb.append("function addCampaign() {\n");
										sb.append("var myrequest=new ajaxRequest()\n");
										sb.append("var myName = document.getElementById('name');\n");
										sb.append("var myPassword = document.getElementById('password');\n");
										sb.append("var myWarehouse = document.getElementById('warehouse');\n");
										sb.append("var myStartDate = document.getElementById('startDate');\n");
										sb.append("var myEndDate = document.getElementById('endDate');\n");
										sb.append("myrequest.onreadystatechange=function(){if (myrequest.readyState==4 && (myrequest.status==200 || myrequest.status==500)){\n");
										sb.append("if(myrequest.responseText==\"ok\"){window.location=\"campaigns.html\"}else{"
												+ "alert(\"Erro na introdução dos dados\");}}}\n");
										sb.append("myrequest.open(\"POST\", \"newcampaign.html\", true)\n");
										sb.append("myrequest.setRequestHeader(\"Content-type\",\"application/x-www-form-urlencoded\");\n");
										sb.append("myrequest.send(\"newName=\"+myName.value+\"&newPassword=\"+myPassword.value+\"&newWarehouse=\"+myWarehouse.value+"
												+ "\"&newStartDate=\"+myStartDate.value+\"&newEndDate=\"+myEndDate.value)}\n");
										sb.append("var toggle = function() {"
												+ " var mydiv = document.getElementById('addForms');\n");
										sb.append(" if (mydiv.style.display === 'block' || mydiv.style.display === '') "
												+ "mydiv.style.display = 'none'; "
												+ "else "
												+ "mydiv.style.display = 'block'}\n");
										sb.append("</script>\n");
										
										sb.append("<input type='button' id='btnAdd' onclick=\"toggle()\" value='Nova campanha' />"+
												"<div id=\"addForms\" style=\"display: none;\">"+
												"<table>"+
												"<tr>"+
												"<td>Nome</td><td><input type=\"text\" id=\"name\"/></td>"+
												"</tr>"+
												"<tr>"+
												"<td>Password</td><td><input type=\"text\" id=\"password\" /></td>"+
												"</tr>"+
												"<tr>"+
												"<td>Armaz&eacute;m</td><td><input type=\"text\" id=\"warehouse\" /></td>"+
												"</tr>"+
												"<tr>"+
												"<td>In&iacute;cio</td><td><input type=\"text\" id=\"startDate\" placeholder=\"dd/mm/aaaa\"/></td>"+
												"</tr>"+
												"<tr>"+
												"<td>Fim</td><td><input type=\"text\" id=\"endDate\" placeholder=\"dd/mm/aaaa\"/></td>"+
												"</tr>"+
												"</table>"+
												"<input type='button' onclick=\"addCampaign()\" value='Adicionar campanha' />"+
												"</div>");
										
									} else {
										SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
										
										sb.append("<script language=\"JavaScript\">\n");
										
										sb.append("function ajaxRequest(){\n");
										sb.append("var activexmodes=[\"Msxml2.XMLHTTP\", \"Microsoft.XMLHTTP\"]\n");
										sb.append("if (window.ActiveXObject){\n");
										sb.append("try{ return new ActiveXObject(activexmodes[i]);}catch(e){}\n");
										sb.append("}else if (window.XMLHttpRequest) return new XMLHttpRequest(); else return false;}\n");
										
										sb.append("function checkClicked(idCampaign, checkbox) {\n");
										sb.append("var myrequest=new ajaxRequest()\n");
										sb.append("if (checkbox.checked)\n");
										sb.append("myrequest.open(\"POST\", \"campaigns.html?idCampaign=\"+idCampaign+\"&stat=true\", true)\n");
										sb.append("else myrequest.open(\"POST\", \"campaigns.html?idCampaign=\"+idCampaign+\"&stat=false\", true)\n");
										sb.append("myrequest.send()}\n");
										
										sb.append("function addCampaign() {\n");
										sb.append("var myrequest=new ajaxRequest()\n");
										sb.append("var myName = document.getElementById('name');\n");
										sb.append("var myPassword = document.getElementById('password');\n");
										sb.append("var myWarehouse = document.getElementById('warehouse');\n");
										sb.append("var myStartDate = document.getElementById('startDate');\n");
										sb.append("var myEndDate = document.getElementById('endDate');\n");
										sb.append("myrequest.onreadystatechange=function(){if (myrequest.readyState==4 && (myrequest.status==200 || myrequest.status==500)){\n");
										sb.append("if(myrequest.responseText==\"ok\"){window.location=\"campaigns.html\"}else{"
												+ "alert(\"Erro na introdução dos dados\");}}}\n");
										sb.append("myrequest.open(\"POST\", \"newcampaign.html\", true)\n");
										sb.append("myrequest.setRequestHeader(\"Content-type\",\"application/x-www-form-urlencoded\");\n");
										sb.append("myrequest.send(\"newName=\"+myName.value+\"&newPassword=\"+myPassword.value+\"&newWarehouse=\"+myWarehouse.value+"
												+ "\"&newStartDate=\"+myStartDate.value+\"&newEndDate=\"+myEndDate.value)}\n");
										
										sb.append("var toggle = function() {"
												+ " var mydiv = document.getElementById('addForms');\n");
										sb.append(" if (mydiv.style.display === 'block' || mydiv.style.display === '') "
												+ "mydiv.style.display = 'none'; "
												+ "else "
												+ "mydiv.style.display = 'block'}\n");
										sb.append("function delClicked(idCampaign, name) { var ret = confirm(\"Todos os dados da campanha '\"+name+\"' serão eliminados, "
												+ "tem certeza que pretende prosseguir?\");\n");
										sb.append("if(ret == true) { var myrequest=new ajaxRequest();\n");
										sb.append("myrequest.open(\"POST\", \"delcampaign.html?idCampaign=\"+idCampaign, false);\n");
										sb.append("myrequest.send();");
										sb.append("window.location=\"campaigns.html\"\n}}");
										sb.append("</script>\n");
										
										sb.append("<input type='button' id='btnAdd' onclick=\"toggle()\" value='Nova campanha' />"+
											"<div id=\"addForms\" style=\"display: none;\">"+
											"<table>"+
											"<tr>"+
											"<td>Nome</td><td><input type=\"text\" id=\"name\"/></td>"+
											"</tr>"+
											"<tr>"+
											"<td>Password</td><td><input type=\"text\" id=\"password\" /></td>"+
											"</tr>"+
											"<tr>"+
											"<td>Armaz&eacute;m</td><td><input type=\"text\" id=\"warehouse\" /></td>"+
											"</tr>"+
											"<tr>"+
											"<td>In&iacute;cio</td><td><input type=\"text\" id=\"startDate\" placeholder=\"dd/mm/aaaa\"/></td>"+
											"</tr>"+
											"<tr>"+
											"<td>Fim</td><td><input type=\"text\" id=\"endDate\" placeholder=\"dd/mm/aaaa\"/></td>"+
											"</tr>"+
											"</table>"+
											"<input type='button' onclick=\"addCampaign()\" value='Adicionar campanha' />"+
											"</div>");
										
										sb.append("<br /><table border=\"1\">");
										sb.append("<tr><td>Nome</td><td>Password</td><td>Armaz&eacute;m</td><td>In&iacute;cio</td><td>Fim</td>"
												+ "<td>Vis&iacute;vel</td><td>N&ordm; Disp.</td><td>&nbsp;</td></tr>");
										JsonArray resultCampaigns = messageMongoDB.body().getArray("results");
										for (int i = 0; i < resultCampaigns.size(); i++) {
											JsonObject campaign = resultCampaigns.get(i);
											sb.append("<tr>");
											sb.append("<td>" + campaign.getString("name") + "</td>");
											sb.append("<td>" + campaign.getString("password") + "</td>");
											sb.append("<td>" + campaign.getString("warehouse") + "</td>");
											sb.append("<td>" + df.format(new Date(campaign.getLong("dateStart"))) + "</td>");
											sb.append("<td>" + df.format(new Date(campaign.getLong("dateEnd"))) + "</td>");
											sb.append("<td><input type='checkbox' onclick=\"checkClicked('"+campaign.getString("idCampaign")+"',this);\" "
													+ (campaign.getBoolean("visible", false) ? "checked=true" : "") + " /></td>");
											sb.append("<td>" + campaign.getInteger("numDevices", 0) + "</td>");
											sb.append("<td><img src=\"images/delete.png\" onclick=\"delClicked('"+campaign.getString("idCampaign")+"', '"
													+campaign.getString("name")+"');\" title=\"Apagar\" /></td>");
											sb.append("</tr>");
										}
										sb.append("</table>");
									}
								}
								message.reply(new JsonObject().putString("status", "ok").putString("result",
										new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString())));
							} catch (Exception e) {
								Utils.saveException(this.getClass().getName(), e);
								message.reply(new JsonObject().putString("status", "error").putString("error", Utils.exceptionToString(e)));
							}
						}
					});
					return;
				} else {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ar.cause().printStackTrace(pw);
					String exceptionString = sw.toString();
					pw.close();
					try {
						sw.close();
					} catch (Exception e) {
					}
					Utils.saveException(this.getClass().getName(), exceptionString);
					message.reply(new JsonObject().putString("status", "error").putString("error", ar.cause().toString()));
				}
			}
		});
	}

	protected void doGetIndex(final Message<JsonObject> message) {
		vertx.fileSystem().readFile("caritas/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					message.reply(new JsonObject().putString("status", "ok").putString("result", new String(ar.result().getBytes()).replace("%DATA_CONTENT%", 
							"Bem-vindo ao painel de administra&ccedil;&atilde;o do projeto Recolha de Bens.<br /><br /><br />" +
							"Use o painel lateral para navega&ccedil;&atilde;o no site.")));
				} else {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ar.cause().printStackTrace(pw);
					String exceptionString = sw.toString();
					pw.close();
					try {
						sw.close();
					} catch (Exception e) {
					}
					Utils.saveException(this.getClass().getName(), exceptionString);
					message.reply(new JsonObject().putString("status", "error").putString("error", ar.cause().toString()));
				}
			}
		});
	}

	@Override
	public void stop() {
		eb.unregisterHandler(Constants.handlerNameAdmin, null);
	}

}
