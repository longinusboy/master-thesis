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

import org.projmis.demo.Utils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class DemoAdminVerticle extends Verticle {
	static EventBus eb = null;

	@Override
	public void start() {
		System.out.println("Starting Admin Verticle " + this.getClass().getName());
		eb = vertx.eventBus();
		eb.registerHandler("projdemo.admin", new Handler<Message<JsonObject>>() {

			@Override
			public void handle(final Message<JsonObject> message) {
				JsonObject messageBody = message.body();
				try {
					String op = messageBody.getString("op");
					String page = messageBody.getString("page");
					String idCampaign = messageBody.getString("idCampaign");
					String obj = messageBody.getString("obj");

					if (op.contentEquals("get")) {

						switch (page) {
							case "login":
								doGetLogin(message);
								break;
							case "index":
								doGetIndex(message);
								break;
							case "campaigns":
								doGetCampaigns(message);
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
					message.reply(new JsonObject().putNumber("httpCode", 500));
					return;
				} catch (Exception e) {
					Utils.saveException(this.getClass().getName(), e);
					message.reply(new JsonObject().putNumber("httpCode", 500));
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
					
					eb.send("mongodb-persistor-demo", jsonSearchTypes, new Handler<Message<JsonObject>>() {
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

									eb.send("mongodb-persistor-demo", jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
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
		vertx.fileSystem().readFile("demo/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject()
							.putString("collection", "registriesSingle")
							.putString("action", "find")
							.putObject("matcher", new JsonObject().putString("idCampaign", idCampaign));

					eb.send("mongodb-persistor-demo", jsonSearchCampaigns, new ExportRegReplyHandler(message, idCampaign, new JsonArray()));
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
					message.reply(new JsonObject().putString("status", "error").putString("error", highlightHeader(ar.cause().toString(), 4)));
				}
			}
		});
	}

	protected void doGetExportStats(final Message<JsonObject> message, final String idCampaign) {
		JsonObject jsonSearchTypes = new JsonObject().putString("collection", "types").putString("action", "find");
		
		eb.send("mongodb-persistor-demo", jsonSearchTypes, new Handler<Message<JsonObject>>() {
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

						eb.send("mongodb-persistor-demo", jsonSearchStat, new Handler<Message<JsonObject>>() {
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
					
					eb.send("mongodb-persistor-demo", jsonSearchTypes, new Handler<Message<JsonObject>>() {
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

									eb.send("mongodb-persistor-demo", jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
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

	protected void doGetExport(final Message<JsonObject> message, final String idCampaign) {
		vertx.fileSystem().readFile("demo/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "findone")
							.putObject("matcher", new JsonObject().putString("idCampaign", idCampaign));

					eb.send("mongodb-persistor-demo", jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
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
										sb.append("<a href=\"export.html?idCampaign="+idCampaign+"&obj=stats\">Exportar Estat&iacute;sticas/Tempo</a><br/><br/>");
										sb.append("<a href=\"export.html?idCampaign="+idCampaign+"&obj=reg\">Exportar Registos</a>");
									}									
								}
								message.reply(new JsonObject().putString("status", "ok").putString("result",
										highlightHeader(new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString()), 4)));
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
					message.reply(new JsonObject().putString("status", "error").putString("error", highlightHeader(ar.cause().toString(), 4)));
				}
			}
		});
	}

	protected void doGetExport(final Message<JsonObject> message) {
		vertx.fileSystem().readFile("demo/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "find");

					eb.send("mongodb-persistor-demo", jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
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
										highlightHeader(new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString()), 4)));
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
					message.reply(new JsonObject().putString("status", "error").putString("error", highlightHeader(ar.cause().toString(), 4)));
				}
			}
		});
	}

	protected void doGetObjectives(final Message<JsonObject> message) {
		vertx.fileSystem().readFile("demo/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "find");

					eb.send("mongodb-persistor-demo", jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
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
										highlightHeader(new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString()), 1)));
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
					message.reply(new JsonObject().putString("status", "error").putString("error", highlightHeader(ar.cause().toString(), 1)));
				}
			}
		});
	}

	protected void doGetObjectives(final Message<JsonObject> message, final String idCampaign) {
		vertx.fileSystem().readFile("demo/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "findone")
							.putObject("matcher", new JsonObject().putString("idCampaign", idCampaign));

					eb.send("mongodb-persistor-demo", jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
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
										
										sb.append("function valueChanged(type, value) {}\n");
										
										sb.append("</script>\n");

										sb.append("<table style=\"width: 310px\">");
										sb.append("<tr><th style=\"width: 200px\">Tipo</th><th style=\"width: 98px\">Valor (Kg)</th></tr>");

										sb.append("<tr><td>Leite e Derivados</td><td><input type=\"text\" size=\"7\" onChange=\"valueChanged('a', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("a") / 1000L) + "\" /></td></tr>");
										sb.append("<tr><td>Mercearia</td><td><input type=\"text\" size=\"7\" onChange=\"valueChanged('b', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("b") / 1000f) + "\" /></td></tr>");
										sb.append("<tr><td>Cereais</td><td><input type=\"text\" size=\"7\" onChange=\"valueChanged('c', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("c") / 1000L) + "\" /></td></tr>");
										sb.append("<tr><td>Hortali&ccedil;as e Frutas</td><td><input type=\"text\" size=\"7\" onChange=\"valueChanged('d', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("d") / 1000L) + "\" /></td></tr>");
										sb.append("<tr><td>Enlatados</td><td><input type=\"text\" size=\"7\" onChange=\"valueChanged('e', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("e") / 1000L) + "\" /></td></tr>");
										sb.append("<tr><td>L&iacute;quidos</td><td><input type=\"text\" size=\"7\" onChange=\"valueChanged('f', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("f") / 1000L) + "\" /></td></tr>");
										sb.append("<tr><td>Produtos de Higiene</td><td><input type=\"text\" size=\"7\" onChange=\"valueChanged('g', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("g") / 1000L) + "\" /></td></tr>");
										sb.append("<tr><td>Produtos de Limpeza</td><td><input type=\"text\" size=\"7\" onChange=\"valueChanged('h', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("h") / 1000L) + "\" /></td></tr>");
										sb.append("<tr><td>Charcutaria</td><td><input type=\"text\" size=\"7\" onChange=\"valueChanged('i', this.value)\" value=\""
												+ (int)(objectiveValues.getLong("i") / 1000L) + "\" /></td></tr>");

										sb.append("</table>");
									} catch (Exception e) {
										Utils.saveException(this.getClass().getName(), e);
										sb.append("<span class\"error\">Erro a obter dados</span>");
									}
								}
								message.reply(new JsonObject().putString("status", "ok").putString("result",
										highlightHeader(new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString()), 1)));
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
					message.reply(new JsonObject().putString("status", "error").putString("error", highlightHeader(ar.cause().toString(), 1)));
				}
			}
		});
	}

	protected void doGetStats(final Message<JsonObject> message) {
		vertx.fileSystem().readFile("demo/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "find");

					eb.send("mongodb-persistor-demo", jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
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
										highlightHeader(new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString()), 3)));
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
					message.reply(new JsonObject().putString("status", "error").putString("error", highlightHeader(ar.cause().toString(), 3)));
				}
			}
		});
	}
	
	protected void doGetStats(final Message<JsonObject> message, final String idCampaign) {
		vertx.fileSystem().readFile("demo/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "findone")
							.putObject("matcher", new JsonObject().putString("idCampaign", idCampaign));

					eb.send("mongodb-persistor-demo", jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
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
										JsonObject objetivesValues = stat.getObject("objectives");

										sb.append("<table style=\"width: 350px\">");
										sb.append("<tr><th style=\"width: 200px\">Tipo</th><th style=\"width: 138px\">Valor (Kg)</th></tr>");

										sb.append("<tr><td>Leite e Derivados</td><td>" + String.format("%.1f", (statValues.getLong("a") / 1000f)) + 
												((objetivesValues.getLong("a")>0)?String.format(" / %.0f (%.0f%%)", (objetivesValues.getLong("a") / 1000f), 
														((float)statValues.getLong("a")/(float)objetivesValues.getLong("a"))*100f):"") + "</td></tr>");
										sb.append("<tr><td>Mercearia</td><td>" + String.format("%.1f", (statValues.getLong("b") / 1000f)) + 
												((objetivesValues.getLong("b")>0)?String.format(" / %.0f (%.0f%%)", (objetivesValues.getLong("b") / 1000f), 
														((float)statValues.getLong("b")/(float)objetivesValues.getLong("b"))*100f):"") + "</td></tr>");
										sb.append("<tr><td>Cereais</td><td>" + String.format("%.1f", (statValues.getLong("c") / 1000f)) + 
												((objetivesValues.getLong("c")>0)?String.format(" / %.0f (%.0f%%)", (objetivesValues.getLong("c") / 1000f), 
														((float)statValues.getLong("c")/(float)objetivesValues.getLong("c"))*100f):"") + "</td></tr>");
										sb.append("<tr><td>Hortali&ccedil;as e Frutas</td><td>" + String.format("%.1f", (statValues.getLong("d") / 1000f)) + 
												((objetivesValues.getLong("d")>0)?String.format(" / %.0f (%.0f%%)", (objetivesValues.getLong("d") / 1000f), 
														((float)statValues.getLong("d")/(float)objetivesValues.getLong("d"))*100f):"") + "</td></tr>");
										sb.append("<tr><td>Enlatados</td><td>" + String.format("%.1f", (statValues.getLong("e") / 1000f)) + 
												((objetivesValues.getLong("e")>0)?String.format(" / %.0f (%.0f%%)", (objetivesValues.getLong("e") / 1000f), 
														((float)statValues.getLong("e")/(float)objetivesValues.getLong("e"))*100f):"") + "</td></tr>");
										sb.append("<tr><td>L&iacute;quidos</td><td>" + String.format("%.1f", (statValues.getLong("f") / 1000f)) + 
												((objetivesValues.getLong("f")>0)?String.format(" / %.0f (%.0f%%)", (objetivesValues.getLong("f") / 1000f), 
														((float)statValues.getLong("f")/(float)objetivesValues.getLong("f"))*100f):"") + "</td></tr>");
										sb.append("<tr><td>Produtos de Higiene</td><td>" + String.format("%.1f", (statValues.getLong("g") / 1000f)) + 
												((objetivesValues.getLong("g")>0)?String.format(" / %.0f (%.0f%%)", (objetivesValues.getLong("g") / 1000f), 
														((float)statValues.getLong("g")/(float)objetivesValues.getLong("g"))*100f):"") + "</td></tr>");
										sb.append("<tr><td>Produtos de Limpeza</td><td>" + String.format("%.1f", (statValues.getLong("h") / 1000f)) + 
												((objetivesValues.getLong("h")>0)?String.format(" / %.0f (%.0f%%)", (objetivesValues.getLong("h") / 1000f), 
														((float)statValues.getLong("h")/(float)objetivesValues.getLong("h"))*100f):"") + "</td></tr>");
										sb.append("<tr><td>Charcutaria</td><td>" + String.format("%.1f", (statValues.getLong("i") / 1000f)) + 
												((objetivesValues.getLong("i")>0)?String.format(" / %.0f (%.0f%%)", (objetivesValues.getLong("i") / 1000f), 
														((float)statValues.getLong("i")/(float)objetivesValues.getLong("i"))*100f):"") + "</td></tr>");

										sb.append("</table>");
									} catch (Exception e) {
										Utils.saveException(this.getClass().getName(), e);
										sb.append("<span class\"error\">Erro a obter dados</span>");
									}
								}
								message.reply(new JsonObject().putString("status", "ok").putString("result",
										highlightHeader(new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString()), 3)));
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
					message.reply(new JsonObject().putString("status", "error").putString("error", highlightHeader(ar.cause().toString(), 3)));
				}
			}
		});
	}

	protected void doGetDevices(final Message<JsonObject> message) {
		vertx.fileSystem().readFile("demo/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "devices").putString("action", "find");

					eb.send("mongodb-persistor-demo", jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
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
									
									sb.append("function checkClicked(idDevice, checkbox) {}\n");
									
									sb.append("</script>\n");
									
									sb.append("<table border=\"1\">");
									sb.append("<tr><th>ID</th><th>Bloqueado</th><th>&Uacute;ltima localização</th></tr>");
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
													sb.append("<a target=\"_blank\" href=\"https://www.google.pt/maps/@40.1923717,"
															+ "-8.4128137,18z?hl=en\">Coordenadas ocultas</a>");
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
										highlightHeader(new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString()), 2)));
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
					message.reply(new JsonObject().putString("status", "error").putString("error", highlightHeader(ar.cause().toString(), 2)));
				}
			}
		});
	}
	
	public static String capitalizeString(String string) {
		return Character.toUpperCase(string.charAt(0)) + string.substring(1);
	}

	protected void doGetCampaigns(final Message<JsonObject> message) {
		vertx.fileSystem().readFile("demo/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(final AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "find");

					eb.send("mongodb-persistor-demo", jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
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
										
										sb.append("function addCampaign() {}\n");
										sb.append("</script>\n");
										
										sb.append("<input type='button' id='btnAdd' value='Nova campanha (demo)' disabled />"+
												"<div id=\"addForms\" style=\"display: none;\">"+
												"<table>"+
												"<tr>"+
												"<th>Nome</th><td><input type=\"text\" id=\"name\"/></td>"+
												"</tr>"+
												"<tr>"+
												"<th>Password</th><td><input type=\"text\" id=\"password\" /></td>"+
												"</tr>"+
												"<tr>"+
												"<th>Armaz&eacute;m</th><td><input type=\"text\" id=\"warehouse\" /></td>"+
												"</tr>"+
												"<tr>"+
												"<th>In&iacute;cio</th><td><input type=\"text\" id=\"startDate\" placeholder=\"dd/mm/aaaa\"/></td>"+
												"</tr>"+
												"<tr>"+
												"<th>Fim</th><td><input type=\"text\" id=\"endDate\" placeholder=\"dd/mm/aaaa\"/></td>"+
												"</tr>"+
												"</table>"+
												"<input type='button' onclick=\"addCampaign()\" value='Adicionar campanha (demo)' />"+
												"</div>");
										
									} else {
										SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
										
										sb.append("<script language=\"JavaScript\">\n");
										
										sb.append("function checkClicked(idCampaign, checkbox) {}\n");
										sb.append("</script>\n");
										
										sb.append("<input type='button' id='btnAdd' value='Nova campanha (demo)' disabled />"+
											"<div id=\"addForms\" style=\"display: none;\">"+
											"<table>"+
											"<tr>"+
											"<th>Nome</th><td><input type=\"text\" id=\"name\"/></td>"+
											"</tr>"+
											"<tr>"+
											"<th>Password</th><td><input type=\"text\" id=\"password\" /></td>"+
											"</tr>"+
											"<tr>"+
											"<th>Armaz&eacute;m</th><td><input type=\"text\" id=\"warehouse\" /></td>"+
											"</tr>"+
											"<tr>"+
											"<th>In&iacute;cio</th><td><input type=\"text\" id=\"startDate\" placeholder=\"dd/mm/aaaa\"/></td>"+
											"</tr>"+
											"<tr>"+
											"<th>Fim</th><td><input type=\"text\" id=\"endDate\" placeholder=\"dd/mm/aaaa\"/></td>"+
											"</tr>"+
											"</table>"+
											"<input type='button' onclick=\"addCampaign()\" value='Adicionar campanha' />"+
											"</div>");
										
										sb.append("<br /><table border=\"1\">");
										sb.append("<tr><th>Nome</th><th>Password</th><th>Armaz&eacute;m</th><th>In&iacute;cio</th><th>Fim</th>"
												+ "<th>Vis&iacute;vel</th><th>N&ordm; Disp.</th><th>&nbsp;</th><th>&nbsp;</th></tr>");
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
													+campaign.getString("name")+"');\" title=\"Eliminar\" /></td>");
											sb.append("<td><a href=\"objectives.html?idCampaign="+campaign.getString("idCampaign")+"\">Definir Objetivos<a/></td>");
											sb.append("</tr>");
										}
										sb.append("</table>");
									}
								}
								message.reply(new JsonObject().putString("status", "ok").putString("result",
										highlightHeader(new String(ar.result().getBytes()).replace("%DATA_CONTENT%", sb.toString()),1)));
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
					message.reply(new JsonObject().putString("status", "error").putString("error", highlightHeader(ar.cause().toString(), 1)));
				}
			}
		});
	}
	
	protected void doGetLogin(final Message<JsonObject> message) {
		
		Date now = new Date();
		
		JsonArray searchCampaignsConditions = new JsonArray();
		searchCampaignsConditions.addObject(new JsonObject().putObject("dateStart", new JsonObject().putNumber("$lt", now.getTime())));
		searchCampaignsConditions.addObject(new JsonObject().putObject("dateEnd", new JsonObject().putNumber("$gt", now.getTime())));
		searchCampaignsConditions.addObject(new JsonObject().putBoolean("viewBrief", true));
		
		JsonObject jsonSearchCampaigns = new JsonObject().putString("collection", "campaigns").putString("action", "findone")
				.putObject("matcher", new JsonObject().putArray("$and", searchCampaignsConditions));
		
		eb.send("mongodb-persistor-demo", jsonSearchCampaigns, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> messageMongoDB) {
				try {
					final JsonObject campaign = messageMongoDB.body().getObject("result");
					if (campaign==null) {
						vertx.fileSystem().readFile("demo/login-nostats.html", new AsyncResultHandler<Buffer>() {
							public void handle(final AsyncResult<Buffer> ar) {
								if (ar.succeeded()) {
									message.reply(new JsonObject().putString("status", "ok").putString("result", new String(ar.result().getBytes())));
								} else {
									message.reply(new JsonObject().putString("status", "error").putString("error", "Error loading login template"));
								}
							}
						});
					}else{
						vertx.fileSystem().readFile("demo/login-stats.html", new AsyncResultHandler<Buffer>() {
							public void handle(final AsyncResult<Buffer> ar) {
								if (ar.succeeded()) {
									
									JsonObject statValues = campaign.getObject("stats");
									JsonObject objetivesValues = campaign.getObject("objectives");
									
									String [] categories = {"a", "b", "c", "d", "e", "f", "g", "h", "i"};
									String [] categoriesReplaceable = {"%A_VAL%", "%B_VAL%", "%C_VAL%", "%D_VAL%", "%E_VAL%", "%F_VAL%", "%G_VAL%", "%H_VAL%", "%I_VAL%"};
									
									String templateFile = new String(ar.result().getBytes());
									
									for (int i = 0; i < categories.length; i++) {
										int value = (int) (((float)statValues.getLong(categories[i])/(float)objetivesValues.getLong(categories[i]))*100f);
										if(value<0)
											value = 0;
										if (value>100) 
											value = 100;
										templateFile = templateFile.replace(categoriesReplaceable[i], String.valueOf(value));
									}
									
									message.reply(new JsonObject().putString("status", "ok").putString("result", templateFile));
								}else {
									vertx.fileSystem().readFile("demo/login-nostats.html", new AsyncResultHandler<Buffer>() {
										public void handle(final AsyncResult<Buffer> ar) {
											if (ar.succeeded()) {
												message.reply(new JsonObject().putString("status", "ok").putString("result", new String(ar.result().getBytes())));
											}else {
												message.reply(new JsonObject().putString("status", "error").putString("error", "Error loading stats template"));
											}
										}
									});
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
	}

	protected void doGetIndex(final Message<JsonObject> message) {
		vertx.fileSystem().readFile("demo/template.html", new AsyncResultHandler<Buffer>() {
			public void handle(AsyncResult<Buffer> ar) {
				if (ar.succeeded()) {
					message.reply(new JsonObject().putString("status", "ok").putString("result", highlightHeader(new String(ar.result().getBytes()).replace("%DATA_CONTENT%", 
							"Bem-vindo ao painel de administra&ccedil;&atilde;o do projeto Recolha de Bens.<br /><br />"+
							"Aqui poder&aacute; experimentar simular campanhas de recolha e testar no seu dispositivo Android 4.0+.<br />" +
							"Use o painel superior para navega&ccedil;&atilde;o no site."), 0)));
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
					message.reply(new JsonObject().putString("status", "error").putString("error", highlightHeader(ar.cause().toString(), 0)));
				}
			}
		});
	}
	
	private String highlightHeader(String content, int index) {
		switch (index) {
			case 1:
				return content.replace("%H1%", "class=\"active\"").replace("%H2%", "").replace("%H3%", "").replace("%H4%", "");
			case 2:
				return content.replace("%H1%", "").replace("%H2%", "class=\"active\"").replace("%H3%", "").replace("%H4%", "");
			case 3:
				return content.replace("%H1%", "").replace("%H2%", "").replace("%H3%", "class=\"active\"").replace("%H4%", "");
			case 4:
				return content.replace("%H1%", "").replace("%H2%", "").replace("%H3%", "").replace("%H4%", "class=\"active\"");

			default:
				return content.replace("%H1%", "").replace("%H2%", "").replace("%H3%", "").replace("%H4%", "");
		}
	}

	@Override
	public void stop() {
		eb.unregisterHandler("projdemo.admin", null);
	}

}
