package com.wibmo.accosa2.utils;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wibmo.accosa2.bean.CertificateInventoryBean;
import com.wibmo.accosa2.bean.CertificateJobBean;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

public class CertificateExpiryDownload {

	private static Logger LOGGER = LoggerFactory.getLogger(CertificateExpiryDownload.class);

	public static Future<List<CertificateInventoryBean>> downloadExpiryCertificateAlertData(Vertx vertx,
			Promise<List<CertificateInventoryBean>> resultFuture, List<CertificateJobBean> certificateJobBeans,
			String sqlQuery, int expiryDay) {
		LOGGER.info("Inside downloadExpiryCertificateAlertData");

		SQLClient sqlClient = DBClientService.getSqlClient();
		LOGGER.info("downloadExpiryCertificateAlertData - Certificate Alert Config Query: {}", sqlQuery);

		if (sqlClient != null) {
			sqlClient.getConnection(connection -> {
				if (connection.succeeded()) {
					try (SQLConnection conn = connection.result()) {
						conn.query(sqlQuery.toString(), result -> {
							if (result.succeeded()) {
								ResultSet rs = result.result();
								if (rs != null) {
									List<JsonObject> objList = rs.getRows();
									LOGGER.debug("Retrieved {} rows from the ResultSet", objList.size());
									List<CertificateInventoryBean> list = CertificateJobSchedulerUtility
											.downloadInventoryFromList(objList, certificateJobBeans, expiryDay);
									LOGGER.info("Processed {} items in the list", list.size());
									resultFuture.complete(list);
								} else {
									LOGGER.error("downloadExpiryCertificateAlertData - ResultSet is NULL for query: {}",
											sqlQuery);
									resultFuture
											.fail("downloadExpiryCertificateAlertData - ResultSet is NULL for query: "
													+ sqlQuery);
								}
							} else {
								LOGGER.error("downloadExpiryCertificateAlertData - Query ResultSet FAILED: {}",
										result.cause());
								resultFuture.fail(result.cause());
							}
						});
					} catch (Throwable t) {
						LOGGER.error("Error while connecting to the database", t);
					}
				} else {
					LOGGER.error("downloadExpiryCertificateAlertData - Connection failed: {}", connection.cause());
					resultFuture.fail(connection.cause());
				}
			});
		} else {
			LOGGER.info("downloadExpiryCertificateAlertData - SQL client object is null");
			resultFuture.fail("SQL client object is null");
		}

		return resultFuture.future();
	}
}