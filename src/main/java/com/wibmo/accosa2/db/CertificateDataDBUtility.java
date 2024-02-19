package com.wibmo.accosa2.db;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wibmo.accosa2.bean.AlertBean;
import com.wibmo.accosa2.bean.CertificateInventoryBean;
import com.wibmo.accosa2.bean.CertificateJobBean;
import com.wibmo.accosa2.utils.CertificateConstants;
import com.wibmo.accosa2.utils.CertificateJobSchedulerUtility;
import com.wibmo.accosa2.utils.DBClientService;
import com.wibmo.accosa2.utils.ProcessExpiryUtility;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

public class CertificateDataDBUtility {
	private static final Logger LOGGER = LoggerFactory.getLogger(CertificateDataDBUtility.class);

	public static Future<List<CertificateJobBean>> fetchAlertConfigsFromDatabase(Vertx vertx) {
		LOGGER.info("fetchAlertConfigsFromDatabase - Fetch alert configuration from the database");
		Promise<List<CertificateJobBean>> resultFuture = Promise.promise();
		SQLClient sqlClient = DBClientService.getSqlClient();
		StringBuilder query = new StringBuilder();
		query.append(CertificateConstants.FETCH_CERT_CONFIG_JOIN_ALERT_QUERY); // Use the join query from
																				// CertificateConstants
		LOGGER.info("Certificate Alert Config Query : {}", query);

		if (sqlClient != null) {
			sqlClient.getConnection(connection -> {
				if (connection.succeeded()) {
					try (SQLConnection conn = connection.result()) {
						conn.queryWithParams(query.toString(), null, resultHandler -> {
							if (resultHandler.succeeded()) {
								ResultSet rs = resultHandler.result();
								if (rs != null) {
									List<JsonObject> objList = rs.getRows();
									List<CertificateJobBean> certificateJobBeans = CertificateJobSchedulerUtility
											.fetchBeanFromList(objList);

									LOGGER.info(
											"fetchAlertConfigsFromDatabase - Fetching Certificate Job Bean Successful");
									resultFuture.complete(certificateJobBeans);
								} else {
									LOGGER.error("fetchAlertConfigsFromDatabase - ResultSet is NULL for query:{}",
											query);
									resultFuture.fail("ResultSet is NULL for query: " + query);
								}
							} else {
								LOGGER.error("fetchAlertConfigsFromDatabase - Query ResultSet FAILED,",
										resultHandler.cause());
								resultFuture.fail(resultHandler.cause());
							}
						});
					} catch (Throwable t) {
						LOGGER.error("fetchAlertConfigsFromDatabase - Error while querying database", t);
						resultFuture.fail(t);
					}
				} else {
					LOGGER.error("fetchAlertConfigsFromDatabase - Connection failed,", connection.cause());
					resultFuture.fail(connection.cause());
				}
			});
		} else {
			LOGGER.error("fetchAlertConfigsFromDatabase - sql client object is null");
			resultFuture.fail("SQL client object is null");
		}

		return resultFuture.future();
	}

	public static void TriggerEmail(Vertx vertx, List<CertificateJobBean> certificateJobBeans, int expiryDay,
			List<CertificateInventoryBean> listCertificateInventoryBean, AlertBean alertBean) {
		LOGGER.debug("TriggerEmail - call to processExpiryDay");
		ProcessExpiryUtility.processExpiryDay(vertx, certificateJobBeans, expiryDay, listCertificateInventoryBean,
				alertBean);
	}

	public static Future<List<LocalDate>> fetchExpiryDatesFromDatabase(Vertx vertx) {
		LOGGER.info("fetchExpiryDatesFromDatabase - Fetching Expiry dates from the database");
		Promise<List<LocalDate>> resultFuture = Promise.promise();
		SQLClient sqlClient = DBClientService.getSqlClient();
		StringBuilder query = new StringBuilder();
		query.append(CertificateConstants.FETCH_EXPIRY_DATE);
		LOGGER.info("fetchExpiryDatesFromDatabase - Certificate Inventory Query: {}",
				CertificateConstants.FETCH_EXPIRY_DATE);

		if (sqlClient != null) {
			sqlClient.getConnection(connection -> {
				if (connection.succeeded()) {
					try (SQLConnection conn = connection.result()) {
						conn.query(query.toString(), resultHandler -> {
							if (resultHandler.succeeded()) {
								ResultSet rs = resultHandler.result();
								if (rs != null) {
									List<JsonObject> objList = rs.getRows();
									List<LocalDate> nearestExpiryDates = fetchNearestExpiryDatesFromList(objList);
									if(nearestExpiryDates != null) {
										LOGGER.debug("fetchExpiryDatesFromDatabase - Nearest Expiry Date List: {}",
												nearestExpiryDates);
										resultFuture.complete(nearestExpiryDates);
									}else {
										LOGGER.debug("nearestExpiryDates is null");
									}
								} else {
									LOGGER.error("fetchExpiryDatesFromDatabase - ResultSet is NULL for query: {}",
											query);
									resultFuture.fail("ResultSet is NULL for query: " + query);
								}
							} else {
								LOGGER.error("fetchExpiryDatesFromDatabase - Query ResultSet FAILED,",
										resultHandler.cause());
								resultFuture.fail(resultHandler.cause());
							}
						});
					} catch (Throwable t) {
						LOGGER.error("fetchExpiryDatesFromDatabase - Error while querying database", t);
						resultFuture.fail(t);
					}
				} else {
					LOGGER.error("fetchExpiryDatesFromDatabase - Connection failed,", connection.cause());
					resultFuture.fail(connection.cause());
				}
			});
		} else {
			LOGGER.error("fetchExpiryDatesFromDatabase - SQL client object is null");
			resultFuture.fail("SQL client object is null");
		}

		return resultFuture.future();
	}

	private static List<LocalDate> fetchNearestExpiryDatesFromList(List<JsonObject> objList) {
		LOGGER.debug("fetchNearestExpiryDatesFromList - Fetching Nearest expiry dates");
		DateTimeFormatter formatter = CertificateConstants.FORMATTER;

		return objList.stream().filter(date -> date != null)
				.map(obj -> parseLocalDate(obj.getString("expiry_date"), formatter))
				.sorted().collect(Collectors.toList());
				
	}

	private static LocalDate parseLocalDate(String dateString, DateTimeFormatter formatter) {
		return dateString != null && !dateString.isEmpty() ? LocalDate.parse(dateString, formatter) : null;
	}
}
