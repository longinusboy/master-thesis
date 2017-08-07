package com.longinus.projcaritasand.model.network;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.longinus.projcaritasand.model.Constants;

import android.util.Log;

public class ServerComm {
	public final static int CONNECTION_TIMEOUT = 10000;
	public final static int SOCKET_TIMEOUT = 10000;
	public final static int MAX_RETRIES = 3;
    public static final int FAST_CONNECTION_TIMEOUT = 3000;
    public static final int FAST_SOCKET_TIMEOUT = 3000;
    public static final int FAST_MAX_RETRIES = 2;
    
    protected static long lastServerAccess = -1;

    public static long getLastServerAccess() {
		return lastServerAccess;
	}

	public static ServerResponse pingSynchronous(String url) {
		if(Constants.DEBUG_MODE)
			Log.d("ServerComm-pingAsynchronous", url);
		final String finalUrl = url;

		RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(SOCKET_TIMEOUT).setConnectTimeout(CONNECTION_TIMEOUT)
				.setConnectionRequestTimeout(CONNECTION_TIMEOUT).setStaleConnectionCheckEnabled(true).build();
		CloseableHttpClient httpclient = HttpClients.custom().disableRedirectHandling().setDefaultRequestConfig(defaultRequestConfig)
				.setRetryHandler(new HttpRequestRetryHandler() {

					@Override
					public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
						if (executionCount <= MAX_RETRIES) {
							return true;
						}
						return false;
					}
				}).build();

		HttpGet httpGet = new HttpGet(finalUrl);

		try {
			HttpResponse response = httpclient.execute(httpGet);
			String responseBody = EntityUtils.toString(response.getEntity());
			ServerComm.lastServerAccess = System.currentTimeMillis();
			return new ServerResponse(response.getStatusLine().getStatusCode(), responseBody);
		} catch (Exception e) {
			return new ServerResponse(e);
		}
	}
	
	public static ServerResponse fastPingSynchronous(String url) {
		if(Constants.DEBUG_MODE)
			Log.d("ServerComm-fastPingAsynchronous", url);
        final String finalUrl = url;

        RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(FAST_SOCKET_TIMEOUT).setConnectTimeout(FAST_CONNECTION_TIMEOUT)
                .setConnectionRequestTimeout(FAST_CONNECTION_TIMEOUT).setStaleConnectionCheckEnabled(false).build();
        final CloseableHttpClient httpclient = HttpClients.custom().disableRedirectHandling().setDefaultRequestConfig(defaultRequestConfig)
                .setRetryHandler(new HttpRequestRetryHandler() {

                    @Override
                    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                    	Log.d("retryRequest", "cnt: "+executionCount+((exception!=null)?exception.getLocalizedMessage():""));
                    	if (exception != null) {
							String localizedMessage = exception.getLocalizedMessage();
							if (localizedMessage!=null) {
								if (localizedMessage.contains("refused")) {
									return false;
								}
							}
						}
                        if (executionCount <= FAST_MAX_RETRIES) {
                            return true;
                        }
                        return false;
                    }
                }).build();

        final HttpGet httpGet = new HttpGet(finalUrl);

        try {
        	TimerTask task = new TimerTask() {
			    @Override
			    public void run() {
			        if (httpGet != null) {
			        	try {
			        		httpGet.abort();
							httpclient.close();
						} catch (IOException e) {
						}
			        }
			    }
			};
			new Timer(true).schedule(task, 4*FAST_CONNECTION_TIMEOUT);
			
            HttpResponse response = httpclient.execute(httpGet);
            String responseBody = EntityUtils.toString(response.getEntity());
            ServerComm.lastServerAccess = System.currentTimeMillis();
            return new ServerResponse(response.getStatusLine().getStatusCode(), responseBody);
        } catch (Exception e) {
        	Log.e("retryRequest-main", "exception", e.fillInStackTrace());
            return new ServerResponse(e);
        }
    }

	public static ServerResponse registerCampaignSynchronous(String url, String idDevice, String idCampaign, String password) {
		String finalUrl;
		if (url.endsWith("/")) {
			finalUrl = url + idDevice;
		} else {
			finalUrl = url + "/" + idDevice;
		}

		RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(SOCKET_TIMEOUT).setConnectTimeout(CONNECTION_TIMEOUT)
				.setConnectionRequestTimeout(CONNECTION_TIMEOUT).setStaleConnectionCheckEnabled(true).build();
		CloseableHttpClient httpclient = HttpClients.custom().disableRedirectHandling().setDefaultRequestConfig(defaultRequestConfig)
				.setRetryHandler(new HttpRequestRetryHandler() {

					@Override
					public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
						if (executionCount <= MAX_RETRIES) {
							return true;
						}
						return false;
					}
				}).build();

		HttpPost httpPost = new HttpPost(finalUrl);

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("idCampaign", idCampaign).put("password", sha1(idDevice + password));
		} catch (Exception e) {
			e.printStackTrace();
		}

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("content", jsonObject.toString()));
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		try {
			HttpResponse response = httpclient.execute(httpPost);
			String responseBody = EntityUtils.toString(response.getEntity());
			ServerResponse serverResponse = new ServerResponse(response.getStatusLine().getStatusCode(), responseBody);
			httpclient.close();
			ServerComm.lastServerAccess = System.currentTimeMillis();
			return serverResponse;
		} catch (Exception e) {
			try {
				httpclient.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return new ServerResponse(e);
		}
	}

	public static ServerResponse registerProductsSynchronous(String url, String idDevice, String idCampaign, String registries) {
		String finalUrl;
		if (url.endsWith("/")) {
			finalUrl = url + idDevice;
		} else {
			finalUrl = url + "/" + idDevice;
		}

		RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(SOCKET_TIMEOUT).setConnectTimeout(CONNECTION_TIMEOUT)
				.setConnectionRequestTimeout(CONNECTION_TIMEOUT).setStaleConnectionCheckEnabled(true).build();
		CloseableHttpClient httpclient = HttpClients.custom().disableRedirectHandling().setDefaultRequestConfig(defaultRequestConfig)
				.setRetryHandler(new HttpRequestRetryHandler() {

					@Override
					public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
						if (executionCount <= MAX_RETRIES) {
							return true;
						}
						return false;
					}
				}).build();

		HttpPost httpPost = new HttpPost(finalUrl);

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("idCampaign", idCampaign).put("registries", new JSONArray(registries));
		} catch (Exception e) {
			e.printStackTrace();
		}

		HttpEntity reqEntity = MultipartEntityBuilder.create().addPart("content", new StringBody(jsonObject.toString(), ContentType.TEXT_PLAIN)).build();

		httpPost.setEntity(reqEntity);

		try {
			HttpResponse response = httpclient.execute(httpPost);
			String responseBody = EntityUtils.toString(response.getEntity());
			ServerResponse serverResponse = new ServerResponse(response.getStatusLine().getStatusCode(), responseBody);
			httpclient.close();
			ServerComm.lastServerAccess = System.currentTimeMillis();
			return serverResponse;
		} catch (Exception e) {
			try {
				httpclient.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return new ServerResponse(e);
		}
	}
	
	public static ServerResponse registerNewProductsSynchronous(String url, String idDevice, JSONArray registries) {
		String finalUrl;
		if (url.endsWith("/")) {
			finalUrl = url + idDevice;
		} else {
			finalUrl = url + "/" + idDevice;
		}

		RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(SOCKET_TIMEOUT).setConnectTimeout(CONNECTION_TIMEOUT)
				.setConnectionRequestTimeout(CONNECTION_TIMEOUT).setStaleConnectionCheckEnabled(true).build();
		CloseableHttpClient httpclient = HttpClients.custom().disableRedirectHandling().setDefaultRequestConfig(defaultRequestConfig)
				.setRetryHandler(new HttpRequestRetryHandler() {

					@Override
					public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
						if (executionCount <= MAX_RETRIES) {
							return true;
						}
						return false;
					}
				}).build();

		HttpPost httpPost = new HttpPost(finalUrl);

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("newProducts", registries);
		} catch (Exception e) {
			e.printStackTrace();
		}

		HttpEntity reqEntity = MultipartEntityBuilder.create().addPart("content", new StringBody(jsonObject.toString(), ContentType.TEXT_PLAIN)).build();

		httpPost.setEntity(reqEntity);

		try {
			HttpResponse response = httpclient.execute(httpPost);
			String responseBody = EntityUtils.toString(response.getEntity());
			ServerResponse serverResponse = new ServerResponse(response.getStatusLine().getStatusCode(), responseBody);
			httpclient.close();
			ServerComm.lastServerAccess = System.currentTimeMillis();
			return serverResponse;
		} catch (Exception e) {
			try {
				httpclient.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return new ServerResponse(e);
		}
	}
	
	public static ServerResponse getProductsSynchronous(String url, String idDevice, long lastUpdate, int page) {
		String finalUrl;
		if (url.endsWith("/")) {
			finalUrl = url + idDevice + "/" + lastUpdate + "/" + page;
		} else {
			finalUrl = url + "/" + idDevice + "/" + lastUpdate + "/" + page;
		}
		return genericGetterSynchronous(finalUrl);
	}
	
	public static ServerResponse getStatisticsSynchronous(String url, String idDevice) {
		String finalUrl;
		if (url.endsWith("/")) {
			finalUrl = url + idDevice;
		} else {
			finalUrl = url + "/" + idDevice;
		}
		return genericGetterSynchronous(finalUrl);
	}
	
	public static ServerResponse getStartupSynchronous(String url, String idDevice) {
		String finalUrl;
		if (url.endsWith("/")) {
			finalUrl = url + idDevice;
		} else {
			finalUrl = url + "/" + idDevice;
		}
		return genericGetterSynchronous(finalUrl);
	}

	public static ServerResponse genericGetterSynchronous(String finalUrl) {

		RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(SOCKET_TIMEOUT).setConnectTimeout(CONNECTION_TIMEOUT)
				.setConnectionRequestTimeout(CONNECTION_TIMEOUT).setStaleConnectionCheckEnabled(true).build();
		CloseableHttpClient httpclient = HttpClients.custom().disableRedirectHandling().setDefaultRequestConfig(defaultRequestConfig)
				.setRetryHandler(new HttpRequestRetryHandler() {

					@Override
					public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
						if (executionCount <= MAX_RETRIES) {
							return true;
						}
						return false;
					}
				}).build();

		HttpGet httpGet = new HttpGet(finalUrl);

		try {
			HttpResponse response = httpclient.execute(httpGet);
			String responseBody = EntityUtils.toString(response.getEntity());
			ServerResponse serverResponse = new ServerResponse(response.getStatusLine().getStatusCode(), responseBody);
			httpclient.close();
			ServerComm.lastServerAccess = System.currentTimeMillis();
			return serverResponse;
		} catch (Exception e) {
			try {
				httpclient.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return new ServerResponse(e);
		}
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

class CommWorker extends Observable implements Runnable {
	private CloseableHttpClient httpClient;
	private HttpPost httpPost;
	private HttpGet httpGet;

	public CommWorker(CloseableHttpClient closeableHttpClient, HttpGet httpGet) {
		super();
		this.httpClient = closeableHttpClient;
		this.httpGet = httpGet;
	}

	public CommWorker(CloseableHttpClient closeableHttpClient, HttpPost httpPost) {
		super();
		this.httpClient = closeableHttpClient;
		this.httpPost = httpPost;
	}

	@Override
	public void run() {
		ServerResponse serverResponse = null;

		try {
			HttpResponse response;
			if (httpGet != null) {
				response = httpClient.execute(httpGet);
			} else {
				response = httpClient.execute(httpPost);
			}
			String responseBody = EntityUtils.toString(response.getEntity());
			serverResponse = new ServerResponse(response.getStatusLine().getStatusCode(), responseBody);
		} catch (Exception e) {
			serverResponse = new ServerResponse(e);
		}
		try {
			httpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ServerComm.lastServerAccess = System.currentTimeMillis();

		setChanged();
		notifyObservers(serverResponse);
	}
}
