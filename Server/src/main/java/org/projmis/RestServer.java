package org.projmis;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.file.FileProps;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/**
 * Created by Long on 07-04-2014.
 */
public class RestServer extends Verticle {
	final int numWorkProcessors = 10;
	final int TIMEOUT_RESPONSE = 2500;
	int listeningPort;
	EventBus eb;
	HttpServer httpServer;

	@Override
	public void start() {

		final JsonObject appConfig = container.config();
		
		listeningPort = appConfig.getInteger("listeningPort", 8080);
		
		container.deployModule("io.vertx~mod-mongo-persistor~2.1.0", appConfig.getObject("caritas").getObject("mongo-persistor"), 1, new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> asyncResult) {
				if (asyncResult.succeeded()) {
					System.out.println("Mongo persistor deployed");
				}else {
					asyncResult.cause().printStackTrace();
				}
			}
		});
		
		container.logger().debug("Starting server...");
		
		container.deployModule("io.vertx~mod-mongo-persistor~2.1.0", appConfig.getObject("evida").getObject("mongo-persistor"), 1, new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> asyncResult) {
				if (asyncResult.succeeded()) {
					System.out.println("Mongo persistor deployed (evida)");
				}else {
					asyncResult.cause().printStackTrace();
				}
			}
		});
		
		container.deployModule("io.vertx~mod-mongo-persistor~2.1.0", appConfig.getObject("demo").getObject("mongo-persistor"), 1, new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> asyncResult) {
				if (asyncResult.succeeded()) {
					System.out.println("Mongo persistor deployed (demo)");
				}else {
					asyncResult.cause().printStackTrace();
				}
			}
		});
		
		container.deployModule("org.projmis~mod-celltowerid-resolver~1.0-SNAPSHOT", appConfig.getObject("cellid-resolver"), 1, new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> asyncResult) {
				if (asyncResult.succeeded()) {
					System.out.println("CelltowerID Resolver deployed");
				}else {
					asyncResult.cause().printStackTrace();
				}
			}
		});
		
		container.deployModule("io.vertx~mod-work-queue~2.0.0-CR2", appConfig.getObject("caritas").getObject("work-queue-registries"), 1, new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> asyncResult) {
				if (asyncResult.succeeded()) {
					container.deployVerticle(org.projmis.caritas.RegistriesProcessor.class.getName(), appConfig.getObject("work-queue-registries"), numWorkProcessors,
							new Handler<AsyncResult<String>>() {

								@Override
								public void handle(AsyncResult<String> asyncResult) {
									if (asyncResult.succeeded()) {
										System.out.println("Registries Worker deployed");
									} else {
										asyncResult.cause().printStackTrace();
									}
								}
							});
				} else {
					asyncResult.cause().printStackTrace();
				}
			}
		});
		
		container.deployModule("io.vertx~mod-work-queue~2.0.0-CR2", appConfig.getObject("demo").getObject("work-queue-registries"), 1, new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> asyncResult) {
				if (asyncResult.succeeded()) {
					container.deployVerticle(org.projmis.demo.RegistriesProcessor.class.getName(), appConfig.getObject("demo").getObject("work-queue-registries"), numWorkProcessors,
							new Handler<AsyncResult<String>>() {

								@Override
								public void handle(AsyncResult<String> asyncResult) {
									if (asyncResult.succeeded()) {
										System.out.println("Registries Worker deployed (demo)");
									} else {
										asyncResult.cause().printStackTrace();
									}
								}
							});
				} else {
					asyncResult.cause().printStackTrace();
				}
			}
		});
		
		container.deployModule("io.vertx~mod-work-queue~2.0.0-CR2", appConfig.getObject("evida").getObject("work-queue-registries"), 1, new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> asyncResult) {
				if (asyncResult.succeeded()) {
					container.deployVerticle(org.projmis.evida.RegistriesProcessor.class.getName(), appConfig.getObject("demo").getObject("work-queue-registries"), 
							numWorkProcessors, new Handler<AsyncResult<String>>() {

								@Override
								public void handle(AsyncResult<String> asyncResult) {
									if (asyncResult.succeeded()) {
										System.out.println("Registries Worker deployed (eVida)");
									} else {
										asyncResult.cause().printStackTrace();
									}
								}
							});
				} else {
					asyncResult.cause().printStackTrace();
				}
			}
		});

		container.deployVerticle(org.projmis.caritas.TypeVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.caritas.StatsVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.caritas.DeviceVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.caritas.ProductVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.caritas.CampaignVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.caritas.LocationVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.caritas.RegistryVerticle.class.getName(), 1);
		
		container.deployVerticle(org.projmis.evida.TypeVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.evida.StatsVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.evida.DeviceVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.evida.ProductVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.evida.CampaignVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.evida.LocationVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.evida.RegistryVerticle.class.getName(), 1);
		
		container.deployVerticle(org.projmis.demo.TypeVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.demo.StatsVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.demo.DeviceVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.demo.ProductVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.demo.CampaignVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.demo.LocationVerticle.class.getName(), 1);
		container.deployVerticle(org.projmis.demo.RegistryVerticle.class.getName(), 1);
		
		container.deployVerticle(CaritasAdminVerticle.class.getName(), 1);
		container.deployVerticle(EVidaAdminVerticle.class.getName(), 1);
		container.deployVerticle(DemoAdminVerticle.class.getName(), 1);

		eb = vertx.eventBus();

		eb.setDefaultReplyTimeout(30000);

		httpServer = vertx.createHttpServer();
		RouteMatcher matcher = new RouteMatcher();
		
		//XXX start frontpage
		
		matcher.get("/", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {				
				setNoCache(httpServerRequest);
				httpServerRequest.response().putHeader("Location", "frontpage/index.html");
				httpServerRequest.response().setStatusCode(301).end();
				return;
			}
		});
		
		matcher.get("/frontpage", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {				
				setNoCache(httpServerRequest);
				httpServerRequest.response().putHeader("Location", "frontpage/index.html");
				httpServerRequest.response().setStatusCode(301).end();
				return;
			}
		});
		
		matcher.get("/frontpage/", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {				
				setNoCache(httpServerRequest);
				httpServerRequest.response().putHeader("Location", "index.html");
				httpServerRequest.response().setStatusCode(301).end();
				return;
			}
		});		
		
		matcher.get("/frontpage/index.html", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest req) {				
				vertx.fileSystem().props("frontpage/index.html", new Handler<AsyncResult<FileProps>>() {
					
					@Override
					public void handle(AsyncResult<FileProps> event) {
						if (event.succeeded()) {
							SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
						    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
							req.response().putHeader("Last-Modified", dateFormat.format(event.result().lastAccessTime()));
							req.response().sendFile("frontpage/index.html");
						}else {
							req.response().setStatusCode(404).end();
						}
					}
				});
			}
		});
		matcher.get("/frontpage/css/:file", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				serverHttpFile(req, "css");
			}
		});
		matcher.get("/frontpage/fonts/:file", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest req) {
				serverHttpFile(req, "fonts");
			}
		});
		matcher.get("/frontpage/images/:file", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest req) {
				serverHttpFile(req, "images");
			}
		});
		matcher.get("/frontpage/js/:file", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest req) {
				serverHttpFile(req, "js");
			}
		});
		
		
		
		matcher.get("/admin/images/:image", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				vertx.fileSystem().props("images/"+req.params().get("image"), new Handler<AsyncResult<FileProps>>() {
					
					@Override
					public void handle(AsyncResult<FileProps> event) {
						if (event.succeeded()) {
							SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
						    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
							req.response().putHeader("Last-Modified", dateFormat.format(event.result().lastAccessTime()));
							req.response().sendFile("images/"+req.params().get("image"));
						}else {
							req.response().setStatusCode(404).end();
						}
					}
				});
			}
		});

		matcher.get("/ping", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				JsonObject jsonPingMongoDB = new JsonObject().putString("action", "command").putString("command", "{ ping: 1 }");

				org.projmis.caritas.Utils.log("ping", jsonPingMongoDB.encode());

				eb.sendWithTimeout("mongodb-persistor", jsonPingMongoDB, TIMEOUT_RESPONSE, new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> event) {
						if (event.succeeded()) {
							try {
								if (event.result().body().getString("status").contentEquals("ok")) {
									httpServerRequest.response().setStatusCode(200).end("pong");
								} else {
									httpServerRequest.response().setStatusCode(500).end();
								}
							} catch (Exception e) {
								httpServerRequest.response().setStatusCode(500).end();
							}
						} else {
							httpServerRequest.response().setStatusCode(500).end();
						}
					}
				});
				return;
			}
		});
		
		matcher.get("/pingevida", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				JsonObject jsonPingMongoDB = new JsonObject().putString("action", "command").putString("command", "{ ping: 1 }");

				org.projmis.evida.Utils.log("ping", jsonPingMongoDB.encode());

				eb.sendWithTimeout(org.projmis.evida.Constants.handlerDataBase, jsonPingMongoDB, TIMEOUT_RESPONSE, new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> event) {
						if (event.succeeded()) {
							try {
								if (event.result().body().getString("status").contentEquals("ok")) {
									httpServerRequest.response().setStatusCode(200).end("pong");
								} else {
									httpServerRequest.response().setStatusCode(500).end();
								}
							} catch (Exception e) {
								httpServerRequest.response().setStatusCode(500).end();
							}
						} else {
							httpServerRequest.response().setStatusCode(500).end();
						}
					}
				});
				return;
			}
		});
		
		matcher.get("/pingdemo", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				JsonObject jsonPingMongoDB = new JsonObject().putString("action", "command").putString("command", "{ ping: 1 }");

				org.projmis.demo.Utils.log("ping", jsonPingMongoDB.encode());

				eb.sendWithTimeout(org.projmis.demo.Constants.handlerDataBase, jsonPingMongoDB, TIMEOUT_RESPONSE, new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> event) {
						if (event.succeeded()) {
							try {
								if (event.result().body().getString("status").contentEquals("ok")) {
									httpServerRequest.response().setStatusCode(200).end("pong");
								} else {
									httpServerRequest.response().setStatusCode(500).end();
								}
							} catch (Exception e) {
								httpServerRequest.response().setStatusCode(500).end();
							}
						} else {
							httpServerRequest.response().setStatusCode(500).end();
						}
					}
				});
				return;
			}
		});
		
		//XXX start webservice caritas
		
		matcher.get("/caritas/startup/:idDevice", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				final String idDevice = httpServerRequest.params().get("idDevice");

				org.projmis.caritas.Utils.log("startup", (new JsonObject().putString("op", "get").putString("idDevice", idDevice)).encode());

				eb.send("projcaritas.device", new JsonObject().putString("op", "post").putString("idDevice", idDevice), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						final int deviceCode = message.body().getInteger("httpCode", 0);
						
						if (deviceCode == 200 || deviceCode == 201) {
							
							eb.send("projcaritas.type", new JsonObject().putString("op", "get"), new Handler<Message<JsonObject>>() {

								@Override
								public void handle(Message<JsonObject> message) {
									final int typeCode = message.body().getInteger("httpCode", 0);
									final String  typeResponse = message.body().getString("httpResponse", "[]");
									
									eb.send("projcaritas.campaign", new JsonObject().putString("op", "get").putString("idDevice", idDevice)
											.putBoolean("noBlockCheck", true), new Handler<Message<JsonObject>>() {

										@Override
										public void handle(Message<JsonObject> message) {
											final int campaignCode = message.body().getInteger("httpCode", 0);
											final String  campaignResponse = message.body().getString("httpResponse", "[]");
											
											eb.send("projcaritas.stats", new JsonObject().putString("op", "get").putString("idDevice", idDevice)
													.putBoolean("noBlockCheck", true), new Handler<Message<JsonObject>>() {

												@Override
												public void handle(Message<JsonObject> message) {
													int statsCode = message.body().getInteger("httpCode", 0);
													String  statsResponse = message.body().getString("httpResponse", "[]");
													
													sendReplyMessage(httpServerRequest, deviceCode, 
															new JsonObject().putNumber("statusDevice", deviceCode)
																.putNumber("statusType", typeCode).putArray("contentType", new JsonArray(typeResponse))
																.putNumber("statusCampaign", campaignCode).putArray("contentCampaign", new JsonArray(campaignResponse))
																.putNumber("statusStats", statsCode).putArray("contentStats", new JsonArray(statsResponse))
																.encode());
												}
											});
										}
									});
								}
							});
						}else{
							sendReplyMessage(httpServerRequest, deviceCode, null);
						}
					}
				});
			}
		});		
		
		matcher.post("/caritas/campaigns/:idDevice", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				try {
					httpServerRequest.expectMultiPart(true);
					httpServerRequest.endHandler(new Handler<Void>() {

						@Override
						public void handle(Void aVoid) {
							List<Map.Entry<String, String>> entries = null;
							String content = null;

							String idDevice = httpServerRequest.params().get("idDevice");
							if (idDevice == null) {
								httpServerRequest.response().setStatusCode(500).end();
								return;
							}

							try {
								entries = httpServerRequest.formAttributes().entries();
								for (Map.Entry<String, String> entry : entries) {
									if (entry.getKey().equalsIgnoreCase("content")) {
										content = entry.getValue();
										break;
									}
								}

								if (content == null) {
									httpServerRequest.response().setStatusCode(400).end();
									return;
								}
							} catch (Exception e) {
								httpServerRequest.response().setStatusCode(400).end();
								return;
							}

							org.projmis.caritas.Utils.log("campaign", (new JsonObject().putString("op", "post").putString("idDevice", idDevice).putString("content", content)).encode());

							eb.send("projcaritas.campaign", new JsonObject().putString("op", "post").putString("idDevice", idDevice).putString("content", content),
									new Handler<Message<JsonObject>>() {

										@Override
										public void handle(Message<JsonObject> message) {
											sendReplyMessage(httpServerRequest, message.body().getInteger("httpCode"), message.body().getString("httpResponse"));
										}
									});
						}
					});
				} catch (Exception e) {
					httpServerRequest.response().setStatusCode(500).end();
				}
			}
		});

		matcher.get("/caritas/products/:idDevice/:date/:page", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				String idDevice = httpServerRequest.params().get("idDevice");
				String date = httpServerRequest.params().get("date");
				String page = httpServerRequest.params().get("page");

				org.projmis.caritas.Utils.log("product", (new JsonObject().putString("op", "get").putString("idDevice", idDevice).putString("date", date).putString("page", page)).encode());

				eb.send("projcaritas.product", new JsonObject().putString("op", "get").putString("idDevice", idDevice).putString("date", date).putString("page", page),
						new Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> message) {
								sendReplyMessage(httpServerRequest, message.body().getInteger("httpCode"), message.body().getString("httpResponse"));
							}
						});
			}
		});

		matcher.post("/caritas/registries/:idDevice", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				try {
					httpServerRequest.expectMultiPart(true);
					httpServerRequest.endHandler(new Handler<Void>() {

						@Override
						public void handle(Void aVoid) {
							List<Map.Entry<String, String>> entries = null;
							String content = null;

							String idDevice = httpServerRequest.params().get("idDevice");
							if (idDevice == null) {
								httpServerRequest.response().setStatusCode(500).end();
								return;
							}

							try {
								entries = httpServerRequest.formAttributes().entries();
								for (Map.Entry<String, String> entry : entries) {
									if (entry.getKey().equalsIgnoreCase("content")) {
										content = entry.getValue();
										break;
									}
								}

								if (content == null) {
									httpServerRequest.response().setStatusCode(400).end();
									return;
								}
							} catch (Exception e) {
								httpServerRequest.response().setStatusCode(400).end();
								return;
							}

							org.projmis.caritas.Utils.log("registry", (new JsonObject().putString("op", "post").putString("idDevice", idDevice).putString("content", content)).encode());

							eb.send("projcaritas.registry", new JsonObject().putString("op", "post").putString("idDevice", idDevice).putString("content", content),
									new Handler<Message<JsonObject>>() {

										@Override
										public void handle(Message<JsonObject> message) {
											sendReplyMessage(httpServerRequest, message.body().getInteger("httpCode"), message.body().getString("httpResponse"));
										}
									});
						}
					});
				} catch (Exception e) {
					httpServerRequest.response().setStatusCode(500).end();
				}
			}
		});

		matcher.get("/caritas/stats/:idDevice", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				String idDevice = httpServerRequest.params().get("idDevice");

				org.projmis.caritas.Utils.log("stats", (new JsonObject().putString("op", "get").putString("idDevice", idDevice)).encode());

				eb.send("projcaritas.stats", new JsonObject().putString("op", "get").putString("idDevice", idDevice), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						sendReplyMessage(httpServerRequest, message.body().getInteger("httpCode"), message.body().getString("httpResponse"));
					}
				});
			}
		});
		
		//XXX start webservice evida
		
		matcher.get("/evida/startup/:idDevice", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				final String idDevice = httpServerRequest.params().get("idDevice");

				org.projmis.evida.Utils.log("startup", (new JsonObject().putString("op", "get").putString("idDevice", idDevice)).encode());

				eb.sendWithTimeout("projevida.device", new JsonObject().putString("op", "post").putString("idDevice", idDevice), TIMEOUT_RESPONSE, 
						new Handler<AsyncResult<Message<JsonObject>>>() {

							@Override
							public void handle(AsyncResult<Message<JsonObject>> event) {
								if (event.failed()) {
									org.projmis.evida.Utils.saveException(this.getClass().getName(), event.cause());
									sendReplyMessage(httpServerRequest, 500, null);
									return;
								}
								
								Message<JsonObject> message = event.result();

								final int deviceCode = message.body().getInteger("httpCode", 0);
								
								if (deviceCode == 200 || deviceCode == 201) {
									
									eb.sendWithTimeout("projevida.type", new JsonObject().putString("op", "get"), TIMEOUT_RESPONSE, 
											new Handler<AsyncResult<Message<JsonObject>>>() {

										@Override
										public void handle(AsyncResult<Message<JsonObject>> event) {
											if (event.failed()) {
												org.projmis.evida.Utils.saveException(this.getClass().getName(), event.cause());
												sendReplyMessage(httpServerRequest, 500, null);
												return;
											}
											
											Message<JsonObject> message = event.result();
											
											final int typeCode = message.body().getInteger("httpCode", 0);
											final String  typeResponse = message.body().getString("httpResponse", "[]");
											
											eb.sendWithTimeout("projevida.campaign", new JsonObject().putString("op", "get").putString("idDevice", idDevice).
													putBoolean("noBlockCheck", true), TIMEOUT_RESPONSE, new Handler<AsyncResult<Message<JsonObject>>>() {

												@Override
												public void handle(AsyncResult<Message<JsonObject>> event) {
													if (event.failed()) {
														org.projmis.evida.Utils.saveException(this.getClass().getName(), event.cause());
														sendReplyMessage(httpServerRequest, 500, null);
														return;
													}
													
													Message<JsonObject> message = event.result();

													final int campaignCode = message.body().getInteger("httpCode", 0);
													final String  campaignResponse = message.body().getString("httpResponse", "[]");
													
													eb.sendWithTimeout("projevida.stats", new JsonObject().putString("op", "get").putString("idDevice", idDevice)
															.putBoolean("noBlockCheck", true), TIMEOUT_RESPONSE, new Handler<AsyncResult<Message<JsonObject>>>() {

														@Override
														public void handle(AsyncResult<Message<JsonObject>> event) {
															if (event.failed()) {
																org.projmis.evida.Utils.saveException(this.getClass().getName(), event.cause());
																sendReplyMessage(httpServerRequest, 500, null);
																return;
															}
															
															Message<JsonObject> message = event.result();

															int statsCode = message.body().getInteger("httpCode", 0);
															String statsResponse = message.body().getString("httpResponse", "[]");
															
															sendReplyMessage(httpServerRequest, deviceCode, 
																	new JsonObject().putNumber("statusDevice", deviceCode)
																		.putNumber("statusType", typeCode).putArray("contentType", new JsonArray(typeResponse))
																		.putNumber("statusCampaign", campaignCode).putArray("contentCampaign", new JsonArray(campaignResponse))
																		.putNumber("statusStats", statsCode).putArray("contentStats", new JsonArray(statsResponse))
																		.encode());
														}
													});
												}
											});
										}
									});
								}else{
									sendReplyMessage(httpServerRequest, deviceCode, null);
								}
								
							}
				
				
				});
						
			}
		});		
		
		matcher.post("/evida/campaigns/:idDevice", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				try {
					httpServerRequest.expectMultiPart(true);
					httpServerRequest.endHandler(new Handler<Void>() {

						@Override
						public void handle(Void aVoid) {
							List<Map.Entry<String, String>> entries = null;
							String content = null;

							String idDevice = httpServerRequest.params().get("idDevice");
							if (idDevice == null) {
								httpServerRequest.response().setStatusCode(500).end();
								return;
							}

							try {
								entries = httpServerRequest.formAttributes().entries();
								for (Map.Entry<String, String> entry : entries) {
									if (entry.getKey().equalsIgnoreCase("content")) {
										content = entry.getValue();
										break;
									}
								}

								if (content == null) {
									httpServerRequest.response().setStatusCode(400).end();
									return;
								}
							} catch (Exception e) {
								httpServerRequest.response().setStatusCode(400).end();
								return;
							}

							org.projmis.evida.Utils.log("campaign", 
									(new JsonObject().putString("op", "post").putString("idDevice", idDevice).putString("content", content)).encode());

							eb.send("projevida.campaign", new JsonObject().putString("op", "post").putString("idDevice", idDevice).putString("content", content),
									new Handler<Message<JsonObject>>() {

										@Override
										public void handle(Message<JsonObject> message) {
											sendReplyMessage(httpServerRequest, message.body().getInteger("httpCode"), message.body().getString("httpResponse"));
										}
									});
						}
					});
				} catch (Exception e) {
					httpServerRequest.response().setStatusCode(500).end();
				}
			}
		});

		matcher.get("/evida/products/:idDevice/:date/:page", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				String idDevice = httpServerRequest.params().get("idDevice");
				String date = httpServerRequest.params().get("date");
				String page = httpServerRequest.params().get("page");

				org.projmis.evida.Utils.log("product", (new JsonObject().putString("op", "get").putString("idDevice", idDevice).putString("date", date).putString("page", page)).encode());

				eb.send("projevida.product", new JsonObject().putString("op", "get").putString("idDevice", idDevice).putString("date", date).putString("page", page),
						new Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> message) {
								sendReplyMessage(httpServerRequest, message.body().getInteger("httpCode"), message.body().getString("httpResponse"));
							}
						});
			}
		});

		matcher.post("/evida/registries/:idDevice", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				try {
					httpServerRequest.expectMultiPart(true);
					httpServerRequest.endHandler(new Handler<Void>() {

						@Override
						public void handle(Void aVoid) {
							List<Map.Entry<String, String>> entries = null;
							String content = null;

							String idDevice = httpServerRequest.params().get("idDevice");
							if (idDevice == null) {
								httpServerRequest.response().setStatusCode(500).end();
								return;
							}

							try {
								entries = httpServerRequest.formAttributes().entries();
								for (Map.Entry<String, String> entry : entries) {
									if (entry.getKey().equalsIgnoreCase("content")) {
										content = entry.getValue();
										break;
									}
								}

								if (content == null) {
									httpServerRequest.response().setStatusCode(400).end();
									return;
								}
							} catch (Exception e) {
								httpServerRequest.response().setStatusCode(400).end();
								return;
							}

							org.projmis.evida.Utils.log("registry", (new JsonObject().putString("op", "post").putString("idDevice", idDevice).putString("content", content)).encode());

							eb.send("projevida.registry", new JsonObject().putString("op", "post").putString("idDevice", idDevice).putString("content", content),
									new Handler<Message<JsonObject>>() {

										@Override
										public void handle(Message<JsonObject> message) {
											sendReplyMessage(httpServerRequest, message.body().getInteger("httpCode"), message.body().getString("httpResponse"));
										}
									});
						}
					});
				} catch (Exception e) {
					httpServerRequest.response().setStatusCode(500).end();
				}
			}
		});

		matcher.get("/evida/stats/:idDevice", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				String idDevice = httpServerRequest.params().get("idDevice");

				org.projmis.evida.Utils.log("stats", (new JsonObject().putString("op", "get").putString("idDevice", idDevice)).encode());

				eb.send("projevida.stats", new JsonObject().putString("op", "get").putString("idDevice", idDevice), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						sendReplyMessage(httpServerRequest, message.body().getInteger("httpCode"), message.body().getString("httpResponse"));
					}
				});
			}
		});
		
		//XXX start webservice demo
		
		matcher.get("/demorb/startup/:idDevice", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				final String idDevice = httpServerRequest.params().get("idDevice");

				org.projmis.demo.Utils.log("startup", (new JsonObject().putString("op", "get").putString("idDevice", idDevice)).encode());

				eb.sendWithTimeout("projdemo.device", new JsonObject().putString("op", "post").putString("idDevice", idDevice), TIMEOUT_RESPONSE, 
						new Handler<AsyncResult<Message<JsonObject>>>() {

							@Override
							public void handle(AsyncResult<Message<JsonObject>> event) {
								if (event.failed()) {
									org.projmis.demo.Utils.saveException(this.getClass().getName(), event.cause());
									sendReplyMessage(httpServerRequest, 500, null);
									return;
								}
								
								Message<JsonObject> message = event.result();

								final int deviceCode = message.body().getInteger("httpCode", 0);
								
								if (deviceCode == 200 || deviceCode == 201) {
									
									eb.sendWithTimeout("projdemo.type", new JsonObject().putString("op", "get"), TIMEOUT_RESPONSE, 
											new Handler<AsyncResult<Message<JsonObject>>>() {

										@Override
										public void handle(AsyncResult<Message<JsonObject>> event) {
											if (event.failed()) {
												org.projmis.demo.Utils.saveException(this.getClass().getName(), event.cause());
												sendReplyMessage(httpServerRequest, 500, null);
												return;
											}
											
											Message<JsonObject> message = event.result();
											
											final int typeCode = message.body().getInteger("httpCode", 0);
											final String  typeResponse = message.body().getString("httpResponse", "[]");
											
											eb.sendWithTimeout("projdemo.campaign", new JsonObject().putString("op", "get").putString("idDevice", idDevice).
													putBoolean("noBlockCheck", true), TIMEOUT_RESPONSE, new Handler<AsyncResult<Message<JsonObject>>>() {

												@Override
												public void handle(AsyncResult<Message<JsonObject>> event) {
													if (event.failed()) {
														org.projmis.demo.Utils.saveException(this.getClass().getName(), event.cause());
														sendReplyMessage(httpServerRequest, 500, null);
														return;
													}
													
													Message<JsonObject> message = event.result();

													final int campaignCode = message.body().getInteger("httpCode", 0);
													final String  campaignResponse = message.body().getString("httpResponse", "[]");
													
													eb.sendWithTimeout("projdemo.stats", new JsonObject().putString("op", "get").putString("idDevice", idDevice)
															.putBoolean("noBlockCheck", true), TIMEOUT_RESPONSE, new Handler<AsyncResult<Message<JsonObject>>>() {

														@Override
														public void handle(AsyncResult<Message<JsonObject>> event) {
															if (event.failed()) {
																org.projmis.demo.Utils.saveException(this.getClass().getName(), event.cause());
																sendReplyMessage(httpServerRequest, 500, null);
																return;
															}
															
															Message<JsonObject> message = event.result();

															int statsCode = message.body().getInteger("httpCode", 0);
															String  statsResponse = message.body().getString("httpResponse", "[]");
															
															sendReplyMessage(httpServerRequest, deviceCode, 
																	new JsonObject().putNumber("statusDevice", deviceCode)
																		.putNumber("statusType", typeCode).putArray("contentType", new JsonArray(typeResponse))
																		.putNumber("statusCampaign", campaignCode).putArray("contentCampaign", new JsonArray(campaignResponse))
																		.putNumber("statusStats", statsCode).putArray("contentStats", new JsonArray(statsResponse))
																		.encode());
														}
													});
												}
											});
										}
									});
								}else{
									sendReplyMessage(httpServerRequest, deviceCode, null);
								}
								
							}
				
				
				});
						
			}
		});		
		
		matcher.post("/demorb/campaigns/:idDevice", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				try {
					httpServerRequest.expectMultiPart(true);
					httpServerRequest.endHandler(new Handler<Void>() {

						@Override
						public void handle(Void aVoid) {
							List<Map.Entry<String, String>> entries = null;
							String content = null;

							String idDevice = httpServerRequest.params().get("idDevice");
							if (idDevice == null) {
								httpServerRequest.response().setStatusCode(500).end();
								return;
							}

							try {
								entries = httpServerRequest.formAttributes().entries();
								for (Map.Entry<String, String> entry : entries) {
									if (entry.getKey().equalsIgnoreCase("content")) {
										content = entry.getValue();
										break;
									}
								}

								if (content == null) {
									httpServerRequest.response().setStatusCode(400).end();
									return;
								}
							} catch (Exception e) {
								httpServerRequest.response().setStatusCode(400).end();
								return;
							}

							org.projmis.demo.Utils.log("campaign", (new JsonObject().putString("op", "post").putString("idDevice", idDevice).putString("content", content)).encode());

							eb.send("projdemo.campaign", new JsonObject().putString("op", "post").putString("idDevice", idDevice).putString("content", content),
									new Handler<Message<JsonObject>>() {

										@Override
										public void handle(Message<JsonObject> message) {
											sendReplyMessage(httpServerRequest, message.body().getInteger("httpCode"), message.body().getString("httpResponse"));
										}
									});
						}
					});
				} catch (Exception e) {
					httpServerRequest.response().setStatusCode(500).end();
				}
			}
		});

		matcher.get("/demorb/products/:idDevice/:date/:page", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				String idDevice = httpServerRequest.params().get("idDevice");
				String date = httpServerRequest.params().get("date");
				String page = httpServerRequest.params().get("page");

				org.projmis.demo.Utils.log("product", (new JsonObject().putString("op", "get").putString("idDevice", idDevice).putString("date", date).putString("page", page)).encode());

				eb.send("projdemo.product", new JsonObject().putString("op", "get").putString("idDevice", idDevice).putString("date", date).putString("page", page),
						new Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> message) {
								sendReplyMessage(httpServerRequest, message.body().getInteger("httpCode"), message.body().getString("httpResponse"));
							}
						});
			}
		});

		matcher.post("/demorb/registries/:idDevice", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				try {
					httpServerRequest.expectMultiPart(true);
					httpServerRequest.endHandler(new Handler<Void>() {

						@Override
						public void handle(Void aVoid) {
							List<Map.Entry<String, String>> entries = null;
							String content = null;

							String idDevice = httpServerRequest.params().get("idDevice");
							if (idDevice == null) {
								httpServerRequest.response().setStatusCode(500).end();
								return;
							}

							try {
								entries = httpServerRequest.formAttributes().entries();
								for (Map.Entry<String, String> entry : entries) {
									if (entry.getKey().equalsIgnoreCase("content")) {
										content = entry.getValue();
										break;
									}
								}

								if (content == null) {
									httpServerRequest.response().setStatusCode(400).end();
									return;
								}
							} catch (Exception e) {
								httpServerRequest.response().setStatusCode(400).end();
								return;
							}

							org.projmis.demo.Utils.log("registry", (new JsonObject().putString("op", "post").putString("idDevice", idDevice).putString("content", content)).encode());

							eb.send("projdemo.registry", new JsonObject().putString("op", "post").putString("idDevice", idDevice).putString("content", content),
									new Handler<Message<JsonObject>>() {

										@Override
										public void handle(Message<JsonObject> message) {
											sendReplyMessage(httpServerRequest, message.body().getInteger("httpCode"), message.body().getString("httpResponse"));
										}
									});
						}
					});
				} catch (Exception e) {
					httpServerRequest.response().setStatusCode(500).end();
				}
			}
		});

		matcher.get("/demorb/stats/:idDevice", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest httpServerRequest) {
				String idDevice = httpServerRequest.params().get("idDevice");

				org.projmis.demo.Utils.log("stats", (new JsonObject().putString("op", "get").putString("idDevice", idDevice)).encode());

				eb.send("projdemo.stats", new JsonObject().putString("op", "get").putString("idDevice", idDevice), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						sendReplyMessage(httpServerRequest, message.body().getInteger("httpCode"), message.body().getString("httpResponse"));
					}
				});
			}
		});
		
		//XXX start caritas page

		matcher.get("/admin", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				req.response().putHeader("Location", "admin/");
				req.response().setStatusCode(301).end();
				return;
			}
		});
		matcher.get("/admin/", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveCaritasCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				eb.send(org.projmis.caritas.Constants.handlerNameAdmin, new JsonObject().putString("op", "get").putString("page", "index"), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, message.body().getString("result"));
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		matcher.get("/admin/index.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveCaritasCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				eb.send(org.projmis.caritas.Constants.handlerNameAdmin, new JsonObject().putString("op", "get").putString("page", "index"), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, message.body().getString("result"));
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.get("/favicon.ico", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				req.response().sendFile("images/favicon.ico");
			}
		});
		matcher.get("/images/favicon.ico", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				req.response().sendFile("images/favicon.ico");
			}
		});
		matcher.get("/admin/images/favicon.ico", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				req.response().sendFile("images/favicon.ico");
			}
		});
		
		matcher.get("/admin/login.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (haveCaritasCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				req.response().sendFile("caritas/login.html");
			}
		});
		
		matcher.post("/admin/login.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				
				req.expectMultiPart(true);
				req.bodyHandler(new Handler<Buffer>() {
					
					@Override
					public void handle(Buffer query) {
						Map<String, String> queryMap = getQueryMap(new String(query.getBytes()));
						
						String username = queryMap.get("username");
						String password = queryMap.get("password");
						
						org.projmis.caritas.Utils.log(this.getClass().getName(), "post /admin/login.html", "username", username, "password", password);
						
						if (username == null || password == null) {
							req.response().putHeader("Location", "login.html");
							req.response().setStatusCode(301).end();
							return;
						}
						
						try {
							String adminUsername = container.config().getObject("caritas").getObject("admin").getString("username");
							String adminPassword = container.config().getObject("caritas").getObject("admin").getString("password");
							
							if (adminUsername.contentEquals(username) && adminPassword.contentEquals(password)) {
								req.response().putHeader("Set-Cookie", "user=1");
								req.response().putHeader("Location", "index.html");
								req.response().setStatusCode(301).end();
							} else {
								req.response().putHeader("Location", "login.html");
								req.response().setStatusCode(301).end();
							}
						} catch (Exception e) {
							org.projmis.caritas.Utils.saveException(this.getClass().getName(), e);
							req.response().putHeader("Location", "login.html");
							req.response().setStatusCode(301).end();
						}
					}
				});
			}
		});
		
		matcher.get("/admin/logout.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				req.response().putHeader("Set-Cookie", "user=deleted");
				req.response().sendFile("caritas/login.html");
			}
		});
		
		matcher.get("/admin/images/:image", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				vertx.fileSystem().props("images/"+req.params().get("image"), new Handler<AsyncResult<FileProps>>() {
					
					@Override
					public void handle(AsyncResult<FileProps> event) {
						if (event.succeeded()) {
							SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
						    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
							req.response().putHeader("Last-Modified", dateFormat.format(event.result().lastAccessTime()));
							req.response().sendFile("images/"+req.params().get("image"));
						}else {
							req.response().setStatusCode(404).end();
						}
					}
				});
			}
		});
		
		matcher.get("/admin/campaigns.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveCaritasCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				eb.send(org.projmis.caritas.Constants.handlerNameAdmin, new JsonObject().putString("op", "get").putString("page", "campaigns"), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, message.body().getString("result"));
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.post("/admin/campaigns.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveCaritasCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				eb.send(org.projmis.caritas.Constants.handlerNameAdmin, new JsonObject().putString("op", "post").putString("page", "campaigns").putString("action", "vis")
						.putString("idCampaign", req.params().get("idCampaign")).putString("stat", req.params().get("stat")), 
						new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, "ok");
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.post("/admin/newcampaign.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveCaritasCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				req.expectMultiPart(true);
				req.bodyHandler(new Handler<Buffer>() {
					
					@Override
					public void handle(Buffer query) {
						Map<String, String> queryMap = getQueryMap(new String(query.getBytes()));
						
						String name = queryMap.get("newName");
						String password = queryMap.get("newPassword");
						String warehouse = queryMap.get("newWarehouse");
						String startDateString = queryMap.get("newStartDate");
						String endDateString = queryMap.get("newEndDate");
						
						if (name == null || password == null || warehouse == null || startDateString == null || endDateString == null) {
							org.projmis.caritas.Utils.log(this.getClass().getName(), "newcampaign-bodyHandler", "name " + name + " password " + password + " warehouse "
									+ warehouse + " startDateString " + startDateString + " endDateString " + endDateString);
							req.response().setStatusCode(500).end("field");
							return;
						}
						
						SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
						Date startDate;
						Date endDateInput;
						try {
							startDate = df.parse(startDateString);
							endDateInput = df.parse(endDateString);
						} catch (Exception e) {
							org.projmis.caritas.Utils.saveException(this.getClass().getName(), e);
							req.response().setStatusCode(500).end("date");
							return;
						}
						
						Calendar cal = Calendar.getInstance();
						cal.setTime(endDateInput);
						cal.add(Calendar.DAY_OF_MONTH, 1);
						cal.add(Calendar.MILLISECOND, -1);
						
						Date endDate = cal.getTime();
						
						if (endDate.getTime()<=startDate.getTime()) {
							req.response().setStatusCode(500).end("date_interval");
							return;
						}
						
						eb.send(org.projmis.caritas.Constants.handlerNameAdmin, new JsonObject().putString("op", "post").putString("page", "campaigns")
								.putString("action", "new").putString("name", name).putString("password", password).putString("warehouse", warehouse)
								.putNumber("startDate", startDate.getTime()).putNumber("endDate", endDate.getTime()), new Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> message) {
								String status = message.body().getString("status");
								if (status.contentEquals("ok")) {
									sendReplyMessage(req, 200, "ok");
								}else {
									sendReplyMessage(req, 500, "error");
								}						
							}
						});
					}
				});
			}
		});
		
		matcher.post("/admin/delcampaign.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveCaritasCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send(org.projmis.caritas.Constants.handlerNameAdmin, new JsonObject().putString("op", "post").putString("page", "campaigns").putString("action", "del")
						.putString("idCampaign", req.params().get("idCampaign")), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, "ok");
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.get("/admin/devices.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveCaritasCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send(org.projmis.caritas.Constants.handlerNameAdmin, new JsonObject().putString("op", "get").putString("page", "devices"), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, message.body().getString("result"));
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.post("/admin/devices.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveCaritasCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send(org.projmis.caritas.Constants.handlerNameAdmin, new JsonObject().putString("op", "post").putString("page", "devices")
						.putString("idDevice", req.params().get("idDevice")).putString("stat", req.params().get("stat")), 
						new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, "");
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.get("/admin/conflicts.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveCaritasCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send(org.projmis.caritas.Constants.handlerNameAdmin, new JsonObject().putString("op", "get").putString("page", "conflicts"), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, message.body().getString("result"));
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.post("/admin/conflicts.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveCaritasCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send(org.projmis.caritas.Constants.handlerNameAdmin, new JsonObject().putString("op", "post").putString("page", "conflicts")
						.putString("obj", req.params().get("obj")).putNumber("value", Integer.parseInt(req.params().get("value"))), 
						new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, "");
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.get("/admin/stats.html", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveCaritasCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send(org.projmis.caritas.Constants.handlerNameAdmin, new JsonObject().putString("op", "get").putString("page", "stats").putString("idCampaign", req.params().get("idCampaign")),
						new Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> message) {
								String status = message.body().getString("status");
								if (status.contentEquals("ok")) {
									sendReplyMessage(req, 200, message.body().getString("result"));
								} else {
									sendReplyMessage(req, 500, "");
								}
							}
						});
			}
		});
		
		matcher.get("/admin/objectives.html", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveCaritasCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send(org.projmis.caritas.Constants.handlerNameAdmin,
						new JsonObject().putString("op", "get").putString("page", "objectives").putString("idCampaign", req.params().get("idCampaign")),
						new Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> message) {
								String status = message.body().getString("status");
								if (status.contentEquals("ok")) {
									sendReplyMessage(req, 200, message.body().getString("result"));
								} else {
									sendReplyMessage(req, 500, "");
								}
							}
						});
			}
		});
		
		matcher.post("/admin/objectives.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveCaritasCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				try {
					String valueString = req.params().get("value");
					Object numberObject = null;
					try {
						numberObject = Double.valueOf(valueString);
					} catch (Exception e) {
						try {
							numberObject = Long.valueOf(valueString);
						} catch (Exception e2) {
							numberObject = Integer.valueOf(valueString);
						}
					}
					if (numberObject == null) {
						sendReplyMessage(req, 500, "");
					}
					eb.send(org.projmis.caritas.Constants.handlerNameAdmin, new JsonObject().putString("op", "post").putString("page", "objectives")
							.putString("idCampaign", req.params().get("idCampaign")).putString("type", req.params().get("type"))
							.putNumber("value", ((Number) numberObject).intValue()),
							new Handler<Message<JsonObject>>() {

								@Override
								public void handle(Message<JsonObject> message) {
									String status = message.body().getString("status");
									if (status.contentEquals("ok")) {
										sendReplyMessage(req, 200, message.body().getString("result"));
									} else {
										sendReplyMessage(req, 500, "");
									}
								}
							});
				} catch (Exception e) {
					org.projmis.caritas.Utils.saveException(this.getClass().getName(), e);
					sendReplyMessage(req, 500, "");
				}
			}
		});
		
		matcher.get("/admin/export.html", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveCaritasCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send(org.projmis.caritas.Constants.handlerNameAdmin, new JsonObject().putString("op", "get").putString("page", "export").putString("idCampaign", req.params().get("idCampaign"))
						.putString("obj", req.params().get("obj")), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							if (!message.body().getBoolean("download", false)) {
								sendReplyMessage(req, 200, message.body().getString("result"));
							} else {
								String campaignName = message.body().getString("campaignName");
								try {
									req.response().putHeader("Content-Type", "text/csv").putHeader("Content-Disposition","attachment; filename=export_" 
											+ ((campaignName!=null)?URLEncoder.encode(campaignName, "UTF-8") :req.params().get("idCampaign")) 
											+ "_" + req.params().get("obj") + ".csv").setStatusCode(200).end("\uFEFF"+message.body().getString("result"));
								} catch (UnsupportedEncodingException e) {
									org.projmis.caritas.Utils.saveException(this.getClass().getName(), campaignName+" "+e);
									sendReplyMessage(req, 500, "");
								}
							}

						} else {
							sendReplyMessage(req, 500, "");
						}
					}
				});
			}
		});
		
		//XXX start page evida
		
		matcher.get("/evida", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				req.response().putHeader("Location", "demo/");
				req.response().setStatusCode(301).end();
				return;
			}
		});
		matcher.get("/evida/", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveEVidaCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				eb.send(org.projmis.evida.Constants.handlerNameAdmin, new JsonObject().putString("op", "get").putString("page", "index"), 
						new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, message.body().getString("result"));
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		matcher.get("/evida/index.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveEVidaCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				eb.send(org.projmis.evida.Constants.handlerNameAdmin, new JsonObject().putString("op", "get").putString("page", "index"), 
						new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, message.body().getString("result"));
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.get("/evida/login.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				eb.send(org.projmis.evida.Constants.handlerNameAdmin, new JsonObject().putString("op", "get").putString("page", "login"), 
						new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, message.body().getString("result"));
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.post("/evida/login.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				
				req.expectMultiPart(true);
				req.bodyHandler(new Handler<Buffer>() {
					
					@Override
					public void handle(Buffer query) {
						Map<String, String> queryMap = getQueryMap(new String(query.getBytes()));
						
						String username = queryMap.get("username");
						String password = queryMap.get("password");
						
						org.projmis.demo.Utils.log(this.getClass().getName(), "post /evida/login.html", "username", username, "password", password);
						
						if (username == null || password == null) {
							req.response().putHeader("Location", "login.html");
							req.response().setStatusCode(301).end();
							return;
						}
						
						try {
							String adminUsername = container.config().getObject("demo").getObject("admin").getString("username");
							String adminPassword = container.config().getObject("demo").getObject("admin").getString("password");
							
							if (adminUsername.contentEquals(username) && adminPassword.contentEquals(password)) {
								req.response().putHeader("Set-Cookie", "user=1101");
								req.response().putHeader("Location", "index.html");
								req.response().setStatusCode(301).end();
							} else {
								req.response().putHeader("Location", "login.html");
								req.response().setStatusCode(301).end();
							}
						} catch (Exception e) {
							org.projmis.demo.Utils.saveException(this.getClass().getName(), e);
							req.response().putHeader("Location", "login.html");
							req.response().setStatusCode(301).end();
						}
					}
				});
			}
		});
		
		matcher.get("/evida/logout.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				req.response().putHeader("Set-Cookie", "user=deleted");
				req.response().putHeader("Location", "login.html");
				req.response().setStatusCode(301).end();
			}
		});
		
		matcher.get("/evida/images/:image", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				vertx.fileSystem().props("images/"+req.params().get("image"), new Handler<AsyncResult<FileProps>>() {
					
					@Override
					public void handle(AsyncResult<FileProps> event) {
						if (event.succeeded()) {
							SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
						    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
							req.response().putHeader("Last-Modified", dateFormat.format(event.result().lastAccessTime()));
							req.response().sendFile("images/"+req.params().get("image"));
						}else {
							req.response().setStatusCode(404).end();
						}
					}
				});
			}
		});
		
		matcher.get("/evida/campaigns.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveEVidaCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				eb.send(org.projmis.evida.Constants.handlerNameAdmin, new JsonObject().putString("op", "get").putString("page", "campaigns"), 
						new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, message.body().getString("result"));
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.post("/evida/campaigns.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveEVidaCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				eb.send(org.projmis.evida.Constants.handlerNameAdmin, new JsonObject().putString("op", "post").putString("page", "campaigns").putString("action", "vis")
						.putString("idCampaign", req.params().get("idCampaign")).putString("stat", req.params().get("stat")), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, "ok");
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.post("/evida/newcampaign.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveEVidaCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				req.expectMultiPart(true);
				req.bodyHandler(new Handler<Buffer>() {
					
					@Override
					public void handle(Buffer query) {
						Map<String, String> queryMap = getQueryMap(new String(query.getBytes()));
						
						String name = queryMap.get("newName");
						String password = queryMap.get("newPassword");
						String warehouse = queryMap.get("newWarehouse");
						String startDateString = queryMap.get("newStartDate");
						String endDateString = queryMap.get("newEndDate");
						
						if (name == null || password == null || warehouse == null || startDateString == null || endDateString == null) {
							org.projmis.demo.Utils.log(this.getClass().getName(), "newcampaign-bodyHandler", "name " + name + " password " + password + " warehouse " 
									+ warehouse + " startDateString " + startDateString + " endDateString " + endDateString);
							req.response().setStatusCode(500).end("field");
							return;
						}
						
						SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
						Date startDate;
						Date endDateInput;
						try {
							startDate = df.parse(startDateString);
							endDateInput = df.parse(endDateString);
						} catch (Exception e) {
							org.projmis.demo.Utils.saveException(this.getClass().getName(), e);
							req.response().setStatusCode(500).end("date");
							return;
						}
						
						Calendar cal = Calendar.getInstance();
						cal.setTime(endDateInput);
						cal.add(Calendar.DAY_OF_MONTH, 1);
						cal.add(Calendar.MILLISECOND, -1);
						
						Date endDate = cal.getTime();
						
						if (endDate.getTime()<=startDate.getTime()) {
							req.response().setStatusCode(500).end("date_interval");
							return;
						}
						
						eb.send(org.projmis.evida.Constants.handlerNameAdmin, new JsonObject().putString("op", "post").putString("page", "campaigns")
								.putString("action", "new").putString("name", name).putString("password", password).putString("warehouse", warehouse)
								.putNumber("startDate", startDate.getTime()).putNumber("endDate", endDate.getTime()), new Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> message) {
								String status = message.body().getString("status");
								if (status.contentEquals("ok")) {
									sendReplyMessage(req, 200, "ok");
								}else {
									sendReplyMessage(req, 500, "error");
								}						
							}
						});
					}
				});
			}
		});
		
		matcher.post("/evida/delcampaign.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveEVidaCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send(org.projmis.evida.Constants.handlerNameAdmin, new JsonObject().putString("op", "post").putString("page", "campaigns").putString("action", "del")
						.putString("idCampaign", req.params().get("idCampaign")), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, "ok");
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.get("/evida/devices.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveEVidaCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send(org.projmis.evida.Constants.handlerNameAdmin, new JsonObject().putString("op", "get").putString("page", "devices"), 
						new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, message.body().getString("result"));
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.post("/evida/devices.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveEVidaCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send(org.projmis.evida.Constants.handlerNameAdmin, new JsonObject().putString("op", "post").putString("page", "devices")
						.putString("idDevice", req.params().get("idDevice")).putString("stat", req.params().get("stat")), 
						new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, "");
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.get("/evida/stats.html", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveEVidaCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send(org.projmis.evida.Constants.handlerNameAdmin, new JsonObject().putString("op", "get").putString("page", "stats")
						.putString("idCampaign", req.params().get("idCampaign")), new Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> message) {
								String status = message.body().getString("status");
								if (status.contentEquals("ok")) {
									sendReplyMessage(req, 200, message.body().getString("result"));
								} else {
									sendReplyMessage(req, 500, "");
								}
							}
						});
			}
		});
		
		matcher.get("/evida/objectives.html", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveEVidaCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send(org.projmis.evida.Constants.handlerNameAdmin,
						new JsonObject().putString("op", "get").putString("page", "objectives").putString("idCampaign", req.params().get("idCampaign")),
						new Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> message) {
								String status = message.body().getString("status");
								if (status.contentEquals("ok")) {
									sendReplyMessage(req, 200, message.body().getString("result"));
								} else {
									sendReplyMessage(req, 500, "");
								}
							}
						});
			}
		});
		
		matcher.post("/evida/objectives.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveEVidaCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				try {
					String valueString = req.params().get("value");
					Object numberObject = null;
					try {
						numberObject = Double.valueOf(valueString);
					} catch (Exception e) {
						try {
							numberObject = Long.valueOf(valueString);
						} catch (Exception e2) {
							numberObject = Integer.valueOf(valueString);
						}
					}
					if (numberObject == null) {
						sendReplyMessage(req, 500, "");
					}
					eb.send(org.projmis.evida.Constants.handlerNameAdmin, new JsonObject().putString("op", "post").putString("page", "objectives")
							.putString("idCampaign", req.params().get("idCampaign")).putString("type", req.params().get("type"))
							.putNumber("value", ((Number) numberObject).intValue()),
							new Handler<Message<JsonObject>>() {

								@Override
								public void handle(Message<JsonObject> message) {
									String status = message.body().getString("status");
									if (status.contentEquals("ok")) {
										sendReplyMessage(req, 200, message.body().getString("result"));
									} else {
										sendReplyMessage(req, 500, "");
									}
								}
							});
				} catch (Exception e) {
					org.projmis.demo.Utils.saveException(this.getClass().getName(), e);
					sendReplyMessage(req, 500, "");
				}
			}
		});
		
		matcher.get("/evida/export.html", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveEVidaCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send(org.projmis.evida.Constants.handlerNameAdmin, new JsonObject().putString("op", "get").putString("page", "export")
						.putString("idCampaign", req.params().get("idCampaign")).putString("obj", req.params().get("obj")), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							if (!message.body().getBoolean("download", false)) {
								sendReplyMessage(req, 200, message.body().getString("result"));
							} else {
								String campaignName = message.body().getString("campaignName");
								try {
									req.response().putHeader("Content-Type", "text/csv").putHeader("Content-Disposition","attachment; filename=export_" + 
											((campaignName!=null)?URLEncoder.encode(campaignName, "UTF-8") :req.params().get("idCampaign")) + "_" + 
											req.params().get("obj") + ".csv").setStatusCode(200).end("\uFEFF"+message.body().getString("result"));
								} catch (UnsupportedEncodingException e) {
									org.projmis.demo.Utils.saveException(this.getClass().getName(), campaignName+" "+e);
									sendReplyMessage(req, 500, "");
								}
							}
						} else {
							sendReplyMessage(req, 500, "");
						}
					}
				});
			}
		});
		
		//XXX start page demo
		
		matcher.get("/demo", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				req.response().putHeader("Location", "demo/");
				req.response().setStatusCode(301).end();
				return;
			}
		});
		matcher.get("/demo/", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveDemoCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				eb.send("projdemo.admin", new JsonObject().putString("op", "get").putString("page", "index"), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, message.body().getString("result"));
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		matcher.get("/demo/index.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveDemoCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				eb.send("projdemo.admin", new JsonObject().putString("op", "get").putString("page", "index"), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, message.body().getString("result"));
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.get("/demo/login.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				eb.send("projdemo.admin", new JsonObject().putString("op", "get").putString("page", "login"), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, message.body().getString("result"));
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.post("/demo/login.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				
				req.expectMultiPart(true);
				req.bodyHandler(new Handler<Buffer>() {
					
					@Override
					public void handle(Buffer query) {
						Map<String, String> queryMap = getQueryMap(new String(query.getBytes()));
						
						String username = queryMap.get("username");
						String password = queryMap.get("password");
						
						org.projmis.demo.Utils.log(this.getClass().getName(), "post /demo/login.html", "username", username, "password", password);
						
						if (username == null || password == null) {
							req.response().putHeader("Location", "login.html");
							req.response().setStatusCode(301).end();
							return;
						}
						
						try {
							String adminUsername = container.config().getObject("demo").getObject("admin").getString("username");
							String adminPassword = container.config().getObject("demo").getObject("admin").getString("password");
							
							if (adminUsername.contentEquals(username) && adminPassword.contentEquals(password)) {
								req.response().putHeader("Set-Cookie", "user=1001");
								req.response().putHeader("Location", "index.html");
								req.response().setStatusCode(301).end();
							} else {
								req.response().putHeader("Location", "login.html");
								req.response().setStatusCode(301).end();
							}
						} catch (Exception e) {
							org.projmis.demo.Utils.saveException(this.getClass().getName(), e);
							req.response().putHeader("Location", "login.html");
							req.response().setStatusCode(301).end();
						}
					}
				});
			}
		});
		
		matcher.get("/demo/logout.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				req.response().putHeader("Set-Cookie", "user=deleted");
				req.response().putHeader("Location", "login.html");
				req.response().setStatusCode(301).end();
			}
		});
		
		matcher.get("/demo/images/:image", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				vertx.fileSystem().props("images/"+req.params().get("image"), new Handler<AsyncResult<FileProps>>() {
					
					@Override
					public void handle(AsyncResult<FileProps> event) {
						if (event.succeeded()) {
							SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
						    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
							req.response().putHeader("Last-Modified", dateFormat.format(event.result().lastAccessTime()));
							req.response().sendFile("images/"+req.params().get("image"));
						}else {
							req.response().setStatusCode(404).end();
						}
					}
				});
			}
		});
		
		matcher.get("/demo/campaigns.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveDemoCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				eb.send("projdemo.admin", new JsonObject().putString("op", "get").putString("page", "campaigns"), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, message.body().getString("result"));
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.post("/demo/campaigns.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveDemoCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				eb.send("projdemo.admin", new JsonObject().putString("op", "post").putString("page", "campaigns").putString("action", "vis")
						.putString("idCampaign", req.params().get("idCampaign")).putString("stat", req.params().get("stat")), 
						new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, "ok");
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.get("/demo/devices.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveDemoCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send("projdemo.admin", new JsonObject().putString("op", "get").putString("page", "devices"), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, message.body().getString("result"));
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.post("/demo/devices.html", new Handler<HttpServerRequest>() {
			
			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveDemoCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send("projdemo.admin", new JsonObject().putString("op", "post").putString("page", "devices")
						.putString("idDevice", req.params().get("idDevice")).putString("stat", req.params().get("stat")), 
						new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							sendReplyMessage(req, 200, "");
						}else {
							sendReplyMessage(req, 500, "");
						}						
					}
				});
			}
		});
		
		matcher.get("/demo/stats.html", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveDemoCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send("projdemo.admin", new JsonObject().putString("op", "get").putString("page", "stats").putString("idCampaign", req.params().get("idCampaign")),
						new Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> message) {
								String status = message.body().getString("status");
								if (status.contentEquals("ok")) {
									sendReplyMessage(req, 200, message.body().getString("result"));
								} else {
									sendReplyMessage(req, 500, "");
								}
							}
						});
			}
		});
		
		matcher.get("/demo/objectives.html", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveDemoCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send("projdemo.admin", new JsonObject().putString("op", "get").putString("page", "objectives").putString("idCampaign", req.params().get("idCampaign")),
						new Handler<Message<JsonObject>>() {

							@Override
							public void handle(Message<JsonObject> message) {
								String status = message.body().getString("status");
								if (status.contentEquals("ok")) {
									sendReplyMessage(req, 200, message.body().getString("result"));
								} else {
									sendReplyMessage(req, 500, "");
								}
							}
						});
			}
		});
		
		matcher.get("/demo/export.html", new Handler<HttpServerRequest>() {

			@Override
			public void handle(final HttpServerRequest req) {
				setNoCache(req);
				if (!haveDemoCookie(req.headers().get("Cookie"))) {
					req.response().putHeader("Location", "login.html");
					req.response().setStatusCode(301).end();
					return;
				}
				
				eb.send("projdemo.admin", new JsonObject().putString("op", "get").putString("page", "export").putString("idCampaign", req.params().get("idCampaign"))
						.putString("obj", req.params().get("obj")), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> message) {
						String status = message.body().getString("status");
						if (status.contentEquals("ok")) {
							if (!message.body().getBoolean("download", false)) {
								sendReplyMessage(req, 200, message.body().getString("result"));
							} else {
								String campaignName = message.body().getString("campaignName");
								try {
									req.response().putHeader("Content-Type", "text/csv").putHeader("Content-Disposition", "attachment; filename=export_" 
											+ ((campaignName!=null)?URLEncoder.encode(campaignName, "UTF-8") :req.params().get("idCampaign")) 
											+ "_" + req.params().get("obj") + ".csv").setStatusCode(200).end("\uFEFF"+message.body().getString("result"));
								} catch (UnsupportedEncodingException e) {
									org.projmis.demo.Utils.saveException(this.getClass().getName(), campaignName+" "+e);
									sendReplyMessage(req, 500, "");
								}
							}

						} else {
							sendReplyMessage(req, 500, "");
						}
					}
				});
			}
		});

		matcher.noMatch(new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest httpServerRequest) {

				org.projmis.demo.Utils.log("noMatch", httpServerRequest.remoteAddress().getAddress().getHostAddress()+":"+httpServerRequest.remoteAddress().getPort(), 
						httpServerRequest.method(), httpServerRequest.path(), httpServerRequest.query());
				httpServerRequest.response().setStatusCode(500).end();
			}
		});
		
		httpServer.requestHandler(matcher).listen(listeningPort, new Handler<AsyncResult<HttpServer>>() {
			
			@Override
			public void handle(AsyncResult<HttpServer> event) {
				if (event.succeeded()) {
					System.out.println("Listening on port "+listeningPort);
				}else {
					System.err.println("Error binding port "+listeningPort);
					System.err.println(event.cause());
				}
			}
		});
	}
	
	private void serverHttpFile(final HttpServerRequest req, final String folder) {
		vertx.fileSystem().props("frontpage/"+folder+"/"+req.params().get("file"), new Handler<AsyncResult<FileProps>>() {
			
			@Override
			public void handle(AsyncResult<FileProps> event) {
				if (event.succeeded()) {
					SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
				    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
					req.response().putHeader("Last-Modified", dateFormat.format(event.result().lastAccessTime()));
					req.response().sendFile("frontpage/"+folder+"/"+req.params().get("file"));
				}else {
					req.response().setStatusCode(404).end();
				}
			}
		});
	}
	
	protected void setNoCache(HttpServerRequest req) {
		req.response().putHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		req.response().putHeader("Pragma", "no-cache");
		req.response().putHeader("Expires", "0");
	}

	protected boolean haveCaritasCookie(String cookie) {
		try {
			Map<String, String> content = getCookiesMap(cookie);
			String user = content.get("user");
			if (user == null) {
				return false;
			}
			if (user.contentEquals("1")) {
				return true;
			}
		} catch (Exception e) {
			org.projmis.demo.Utils.saveException(this.getClass().getName(), e);
		}
		
		return false;
	}
	
	protected boolean haveEVidaCookie(String cookie) {
		/*try {
			Map<String, String> content = getCookiesMap(cookie);
			String user = content.get("user");
			if (user == null) {
				return false;
			}
			if (user.contentEquals("1101")) {
				return true;
			}
		} catch (Exception e) {
			org.projmis.demo.Utils.saveException(this.getClass().getName(), e);
		}*/
		
		return true;
	}
	
	protected boolean haveDemoCookie(String cookie) {
		try {
			Map<String, String> content = getCookiesMap(cookie);
			String user = content.get("user");
			if (user == null) {
				return false;
			}
			if (user.contentEquals("1001")) {
				return true;
			}
		} catch (Exception e) {
			org.projmis.demo.Utils.saveException(this.getClass().getName(), e);
		}
		
		return false;
	}
	
	private static Map<String, String> getCookiesMap(String query) {
		Map<String, String> map = new HashMap<String, String>();
		try {
			String[] params = query.split("&|;");
		    
		    for (String param : params) {
		        String name = param.split("=")[0];

		        try {
		            map.put(name, URLDecoder.decode(param.split("=")[1], "UTF-8"));
		        } catch (Exception e) {
		        	org.projmis.demo.Utils.saveException(RestServer.class.getName(), e);
		        }
		    }
		} catch (Exception e) {			
		}

	    return map;
	}

	private static Map<String, String> getQueryMap(String query) {
		Map<String, String> map = new HashMap<String, String>();
		try {
			String[] params = query.split("&");
		    
		    for (String param : params) {
		        String name = param.split("=")[0];

		        try {
		            map.put(name, URLDecoder.decode(param.split("=")[1], "UTF-8"));
		        } catch (Exception e) {
		        	org.projmis.demo.Utils.saveException(RestServer.class.getName(), e);
		        }
		    }
		} catch (Exception e) {			
		}

	    return map;
	}

	public void sendReplyMessage(HttpServerRequest httpServerRequest, Integer httpCode, String httpResponse) {
		httpServerRequest.response().setStatusCode((httpCode != null) ? httpCode : 500);
		if (httpResponse != null) {
			httpServerRequest.response().end(httpResponse);
		} else {
			httpServerRequest.response().end();
		}
	}

	@Override
	public void stop() {
		try {
			httpServer.close(new Handler<AsyncResult<Void>>() {
				@Override
				public void handle(AsyncResult<Void> voidAsyncResult) {
					if (voidAsyncResult.succeeded()) {
						System.out.println("Server halted");
					}
				}
			});
		} catch (Exception e) {
		}
	}
}
