/**
 * 
 */
package com.wibmo.accosa2.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
/**
 * 
 */
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wibmo.accosa2.acs.common.json.Json;
import com.wibmo.accosa2.async.httpclient.impl.AsyncHttpClient;
import com.wibmo.accosa2.bean.config.EndpointUrl;
import com.wibmo.accosa2.bean.config.HsmConfig;
import com.wibmo.accosa2.bean.web.HttpConfig;
import com.wibmo.accosa2.bean.web.WebRequest;
import com.wibmo.accosa2.bean.web.WebResponse;
import com.wibmo.accosa2.common.util.StringUtil;
import com.wibmo.accosa2.hsm.bean.HsmServerRequest;
import com.wibmo.accosa2.hsm.bean.HsmServerResponse;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class CommonUtility {

	private static final String DEC_URI = "/pkcs11/SD";
	private static final String JSON = "application/json;charset=UTF-8";
	public static ObjectMapper mapper = Json.serializer().mapper();
	public static JsonObject config;

	private static final String USER_INPUT_PROPERTIES_PATH = "config/user-input.properties";
	private static final String PROPERTIES_FILE_PATH_KEY = "PROPERTIES_FILE_PATH";
	public static final Properties properties = loadProperties();

	private static Logger logger = LoggerFactory.getLogger(CommonUtility.class);

	public static String getSystemPropertyString(String key) {

		return System.getProperty(key);
	}

	public static boolean isInvalidString(String str) {
		if (str == null || str.trim().length() == 0) {
			return true;
		}
		if (str == null || str.trim().isEmpty()) {
			return true;
		}
		return false;
	}

	public static String b64Decode(String data) {
		String clearText = null;
		if (data != null && !StringUtil.EMPTY.equalsIgnoreCase(data.trim())) {
			byte[] rawBytes = Base64.getDecoder().decode(data);
			clearText = new String(rawBytes);
		}
		return clearText;
	}

	public static boolean isNullOrEmpty(String input) {

		boolean result = false;
		if (input == null || ("").equals(input.trim())) {
			result = true;
		}
		return result;
	}

	public static String convertObjToJsonString(Object input) {

		String result = null;
		if (input != null) {
			try {
				result = mapper.writeValueAsString(input);
			} catch (Exception ex) {
			}

		}
		return result;
	}

	public static Object convertJsonStringToObject(String jsonStr, Class classType) {

		Object object = null;

		if (!isNullOrEmpty(jsonStr)) {
			try {
				object = mapper.readValue(jsonStr, classType);
			} catch (Exception ex) {
				logger.error("convertJsonStringToObject-{},", ex.getMessage(), ex);
			}
		}
		return object;

	}

	public static Future<String> decryptPwd(Vertx vertx, String password, Map<String, String> map) {
		Promise<String> pwd = Promise.promise();

		HttpConfig httpConfig = new HttpConfig();
		httpConfig.setClusterName(map.get("http.clustername"));
		httpConfig.setReleaseType(map.get("http.releaseType"));
		AsyncHttpClient.initConfig(vertx, httpConfig);

		HsmConfig hsmConfig = new HsmConfig();

		EndpointUrl url = new EndpointUrl();
		url.setConnectTimeout(Long.parseLong(properties.getProperty("CONN_TIMEOUT")));
		url.setEndpointUrl(map.get("hsm.endpointURL"));
		url.setConnectType(EndpointUrl.ConnectTypeEnum.DIRECT);
		hsmConfig.setHsmUrlEP(url);

		hsmConfig.getDataEncKey().setKeyAlias(map.get("hsm.encKeyAlias"));// en_param_key
		hsmConfig.getDataEncKey().setProviderType(map.get("hsm.providerType"));// HK_AC
		hsmConfig.getDataEncKey().setEncMode(map.get("hsm.transformationMode"));// 09
		hsmConfig.getDataEncKey().setKeyType(map.get("hsm.keyType"));// AES256
		hsmConfig.getDataEncKey().setAlgorithm(map.get("hsm.algorthmId"));// HMACSHA256

		Future<String> stringFuture = decrypt(password, hsmConfig);

		stringFuture.onComplete(handle -> {
			if (handle.succeeded()) {
				pwd.complete(handle.result());
				logger.debug("Password: {}", handle.result());
			} else {
				logger.debug("handle fail");
			}

		});

		return pwd.future();
	}

	public static Future<String> decrypt(String data, HsmConfig hsmConfig) {

		String stausCode0 = properties.getProperty("status_code_zero");
		String statusCode503 = properties.getProperty("status_code_503");
		String statusCode500 = properties.getProperty("status_code_500");
		String statusCode408 = properties.getProperty("status_code_408");

		Promise<String> outerFuture = Promise.promise();
		try {
			if (!isNullOrEmpty(data) && hsmConfig != null) {

				HsmServerRequest hsmReqObj = new HsmServerRequest();
				hsmReqObj.setData(data);
				hsmReqObj.setEncId(hsmConfig.getDataEncKey().getEncMode());
				hsmReqObj.setKeyAlias(hsmConfig.getDataEncKey().getKeyAlias());
				hsmReqObj.setKeyAliasType(hsmConfig.getDataEncKey().getKeyType());
				hsmReqObj.setMode(hsmConfig.getDataEncKey().getProviderType());
				hsmReqObj.setAlgorthmId(hsmConfig.getDataEncKey().getAlgorithm());

				Future<WebResponse> result = null;
				String jsonStr = convertObjToJsonString(hsmReqObj);

				AsyncHttpClient webClient = AsyncHttpClient.of(hsmConfig.getHsmUrlEP());
				WebRequest webRequest = new WebRequest();
				webRequest.setBody(jsonStr);
				webRequest.setContentType(JSON);
				webRequest.setUri(DEC_URI);
				webRequest.setRequestId("");
				webRequest.addHeader("x-ivs-request-id", "");

				result = webClient.post(webRequest);

				if (logger.isDebugEnabled())
					logger.debug("decrypt-req:{},URL:{},json:{}", hsmConfig.getHsmUrlEP().getEndpointUrl(), jsonStr);
				result.onComplete(handler -> {
					if (handler.succeeded()) {
						WebResponse response = handler.result();
						HsmServerResponse hsmResponse = (HsmServerResponse) convertJsonStringToObject(
								response.getResponse(), HsmServerResponse.class);
						String responseCode = null;
						if (hsmResponse != null) {
							responseCode = hsmResponse.getResponseCode();
						}
						if (stausCode0.equalsIgnoreCase(responseCode)) {
							outerFuture.complete(b64Decode(hsmResponse.getData()));
						} else if (response.getStatusCode() == Integer.parseInt(statusCode408)
								|| response.getStatusCode() == Integer.parseInt(statusCode503)
								|| response.getStatusCode() == Integer.parseInt(statusCode500)) {
							outerFuture.fail(String.valueOf(response.getStatusCode()));
						} else {
							outerFuture.fail(responseCode);
						}
						if (logger.isDebugEnabled())
							logger.debug("decrypt-req:{},ResCode:{}", responseCode);
					} else {
						logger.error("HSM decrypt failed-:", handler.cause());
						outerFuture.fail(handler.cause());
					}
				});
			}
		} catch (Exception ex) {
			logger.error("{} - decrypt: ", ex);
			outerFuture.fail(ex);
		}
		return outerFuture.future();
	}

	public static void loadAlertConfiguration() {
		config = loadConfigurationFromFile("config/alert-certificate-config.json");
	}

	private static JsonObject loadConfigurationFromFile(String configFile) {
		try {
			byte[] configFileBytes = Files.readAllBytes(Paths.get(configFile));
			return new JsonObject(new String(configFileBytes));
		} catch (IOException e) {
			logger.error("Error reading configuration file: {}", e.getMessage());
			return new JsonObject();
		}
	}

	public static JsonObject getAlertConfig() {
		return config;
	}

	private static String getPropertiesFilePathFromUserInput() {
		Properties userInputProperties = new Properties();
		try (FileInputStream userInputInput = new FileInputStream(USER_INPUT_PROPERTIES_PATH)) {
			userInputProperties.load(userInputInput);
		} catch (IOException e) {
			logger.error("Error loading user input properties from file: " + USER_INPUT_PROPERTIES_PATH, e);
		}

		return userInputProperties.getProperty(PROPERTIES_FILE_PATH_KEY);
	}

	private static Properties loadProperties() {
		Properties prop = new Properties();
		String propertiesFilePath = getPropertiesFilePathFromUserInput();

		try (FileInputStream input = new FileInputStream(propertiesFilePath)) {
			prop.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return prop;
	}

}
