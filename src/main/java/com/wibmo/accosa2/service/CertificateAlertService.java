/**
 * 
 */
package com.wibmo.accosa2.service;

import static com.wibmo.accosa2.common.constants.CommonConstants.CONTENT_TYPE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wibmo.accosa2.utils.CertificateJobScheduler;
import com.wibmo.accosa2.utils.CommonUtility;
import com.wibmo.accosa2.utils.DBClientService;
import com.wibmo.accosa2.utils.DataBaseService;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

/**
 * 
 */
public class CertificateAlertService extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(CertificateAlertService.class);

	public static void main(String[] args) {
		try {
			final Vertx vertx = Vertx.vertx();
			vertx.deployVerticle(CertificateAlertService.class.getName());
			LOGGER.info("CertificateAlertService deployed successfully.");
		} catch (Exception ex) {
			LOGGER.error("ERROR deploying Verticle:", ex);
		}
	}

	@Override
	public void start(Promise<Void> startPromise) {
		LOGGER.info("Inside start");
		try {
			Router router = Router.router(vertx);
			CommonUtility.loadAlertConfiguration();
			Future<Void> initDBPromise = initDB();
			initDBPromise.onComplete(handler -> {
				setupRouter(router);
				initializeHttpServer(startPromise, router);
				startCertificateJobScheduler(); // Start the job scheduler here
				LOGGER.info("DB initilisation, scheduler configured successfully");
			}).onFailure(e -> {
				LOGGER.error("Exception in init DB", e.getCause().getMessage());
			});

		} catch (Exception ex) {
			LOGGER.error("ERROR starting Verticle:", ex);
			startPromise.fail(ex);
		}
	}

	private void startCertificateJobScheduler() {
		new CertificateJobScheduler().executeJob(vertx)
				.onSuccess(res -> LOGGER.info("configureJobScheduler - Cron job configuration is completed"))
				.onFailure(error -> LOGGER
						.error("configureJobScheduler - Error occurred while configuring job scheduler"));
	}

	private void initializeHttpServer(Promise<Void> startPromise, Router router) {
		int port = Integer.parseInt(System.getProperty("app.port", "6012"));
		vertx.createHttpServer().requestHandler(router).listen(port, http -> {
			if (http.succeeded()) {
				startPromise.complete();
				LOGGER.info("HTTP server started on port {}", port);
			} else {
				LOGGER.error("Failed to start HTTP server:", http.cause());
				startPromise.fail(http.cause());
			}
		});
	}

	private Set<String> setAllowedHeaders() {
		final Set<String> allowedHeaders = new HashSet<>();
		allowedHeaders.add("x-requested-with");
		allowedHeaders.add("Access-Control-Allow-Origin");
		allowedHeaders.add("origin");
		allowedHeaders.add(CONTENT_TYPE);
		allowedHeaders.add("accept");
		allowedHeaders.add("X-PINGARUNER");
		return allowedHeaders;
	}

	private Set<HttpMethod> setAllowedMethods() {
		final Set<HttpMethod> allowedMethods = new HashSet<>();
		allowedMethods.add(HttpMethod.GET);
		allowedMethods.add(HttpMethod.POST);
		return allowedMethods;
	}

	public void setupRouter(Router router) {
		Set<String> allowedHeaders = setAllowedHeaders();
		Set<HttpMethod> allowedMethods = setAllowedMethods();
		router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));

		router.route().handler(BodyHandler.create());

		router.get("/status").handler(ctx -> {
			ctx.response().end(new JsonObject().put("response", "ok").toBuffer());
		});
	}

	public Future<Void> initDB() {
		LOGGER.info("Inside initDB");
		Promise<Void> promise = Promise.promise();
		String dbIpPort = System.getProperty("dbIpPort");
		String schema = System.getProperty("schema");
		String username = System.getProperty("username");

		JsonObject config = CommonUtility.getAlertConfig();
		JsonObject hsmConfig = config.getJsonObject("hsmConfig");

		JsonObject hsmUrlEP = hsmConfig.getJsonObject("hsmUrlEP");

		Map<String, String> map = new HashMap<>();
		map.put("http.clustername", System.getProperty("cluster.name"));
		map.put("http.releaseType", System.getProperty("release.type"));

		map.put("hsm.endpointURL", hsmUrlEP.getString("hsm.endpointURL"));
		map.put("hsm.encKeyAlias", hsmUrlEP.getString("hsm.encKeyAlias"));
		map.put("hsm.providerType", hsmUrlEP.getString("hsm.providerType"));
		map.put("hsm.transformationMode", hsmUrlEP.getString("hsm.transformationMode"));
		map.put("hsm.keyType", hsmUrlEP.getString("hsm.keyType"));
		map.put("hsm.algorthmId", hsmUrlEP.getString("hsm.algorthmId"));

		String pwd = System.getProperty("password");

		Future<String> passFuture = CommonUtility.decryptPwd(vertx, pwd, map);

		passFuture.onComplete(handler -> {
			if (handler.succeeded()) {
				DBClientService.configureJson(dbIpPort, schema, username, handler.result());
				DBClientService.setDbSqlClient(vertx);
				Future<Void> dataBaeFuture = DataBaseService.initializeDb(vertx);
				dataBaeFuture.onComplete(handler1 -> {
					if (handler1.succeeded()) {
						LOGGER.info("database initizalized successfully");
						promise.complete();
					} else {
						LOGGER.error("Failed to initialize Database");
						promise.fail("failed");
					}
				});
				LOGGER.info("Connection Established");
			} else {
				LOGGER.info("After start failed");
			}
		});
		return promise.future();
	}
}
