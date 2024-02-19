package com.wibmo.accosa2.handler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wibmo.accosa2.bean.AlertBean;
import com.wibmo.accosa2.bean.CertificateInventoryBean;
import com.wibmo.accosa2.bean.CertificateJobBean;
import com.wibmo.accosa2.config.dao.CertificateAlertDAODownload;
import com.wibmo.accosa2.db.CertificateDataDBUtility;
import com.wibmo.accosa2.utils.CertificateAlertDBUtility;
import com.wibmo.accosa2.utils.CertificateConstants;
import com.wibmo.accosa2.utils.CommonUtility;
import com.wibmo.accosa2.utils.DBClientService;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

public class CertificateAlertHandler {

	private static Logger logger = LoggerFactory.getLogger(CertificateAlertHandler.class);
	static List<CertificateInventoryBean> certificateInventoryList;

	public void insertUpdatedCertificates(Vertx vertx) {
		checkModifiedDates().onComplete(checkModifiedDatesResult -> {
			if (checkModifiedDatesResult.succeeded()) {
				List<String> modifiedNames = checkModifiedDatesResult.result();
				if (!modifiedNames.isEmpty()) {
					// Iterate over modifiedNames and call getListOfACSCertificateAlertData for each
					// name
					for (String modifiedName : modifiedNames) {
						String sqlQuery = "SELECT " + " name AS \"resourceName\"," + " value AS \"certName\","
								+ " type AS \"cType\"," + " group_name AS \"groupName\" " + " FROM "
								+ " config_resource " + " WHERE " + " type IN (RESOURCE_TYPE) "
								+ " AND group_name IN (GROUP_NAME) " + " AND name = '" + modifiedName + "' ";

						logger.debug("insertUpdatedCertificates - sqlQuery:{}", sqlQuery);
						JsonObject config = CommonUtility.getAlertConfig();
						JsonArray groupName = config.getJsonArray("groupName");
						JsonArray resourceType = config.getJsonArray("resourceType");
						logger.debug("insertUpdatedCertificates - sqlQuery:{}, groupName:{}, resourceType:{}", sqlQuery,
								groupName, resourceType);
						String groupNameStr = getConvertedStrVal(groupName.getList());
						String resourcetypeStr = getConvertedStrVal(resourceType.getList());
						String finalQuery = sqlQuery.toString().replace("GROUP_NAME", groupNameStr)
								.replace("RESOURCE_TYPE", resourcetypeStr);
						logger.info("finalQuery: {}", finalQuery);

						CertificateAlertDBUtility certificateAlertDBUtility = CertificateAlertDBUtility.getInstance();
						Future<List<CertificateInventoryBean>> future = certificateAlertDBUtility
								.getListOfACSCertificateAlertData(vertx, finalQuery.toString());
						logger.info("insertUpdatedCertificates - executed successfully");
					}
				} else {
					logger.info("insertUpdatedCertificates - No modified certificates found.");
				}
			} else {
				logger.error("insertUpdatedCertificates - Error checking modified dates:",
						checkModifiedDatesResult.cause());
			}
		});
	}

	private Future<List<String>> checkModifiedDates() {
		Promise<List<String>> resultFuture = Promise.promise();

		String currentDate = LocalDate.now().format(CertificateConstants.DATEFORMATTER);

		String joinQuery = "SELECT DISTINCT cr.name FROM config_resource cr "
				+ "JOIN certificate_inventory ci ON cr.name = ci.resource_name " + "WHERE type IN (RESOURCE_TYPE) "
				+ "AND cr.group_name IN (GROUP_NAME) " + "AND DATE(cr.modified_date) = ? "
				+ "AND DATE(ci.last_updated_on) != ? ";

		logger.debug("checkModifiedDates - Executing SQL query: {}", joinQuery);

		JsonObject config = CommonUtility.getAlertConfig();
		JsonArray groupName = config.getJsonArray("groupName");
		JsonArray resourceType = config.getJsonArray("resourceType");
		logger.debug("checkModifiedDates - sqlQuery:{}, groupName:{}, resourceType:{}", joinQuery, groupName,
				resourceType);
		JsonArray params = new JsonArray();
		params.add(currentDate);
		params.add(currentDate);
		String groupNameStr = getConvertedStrVal(groupName.getList());
		String resourceTypeStr = getConvertedStrVal(resourceType.getList());
		String finalQuery = joinQuery.replace("GROUP_NAME", groupNameStr).replace("RESOURCE_TYPE", resourceTypeStr);
		logger.info("finalQuery: {}", finalQuery);

		SQLClient sqlClient = DBClientService.getSqlClient();
		if (sqlClient != null) {
			sqlClient.getConnection(connection -> {
				if (connection.succeeded()) {
					try (SQLConnection conn = connection.result()) {
						conn.queryWithParams(finalQuery, params, queryResult -> {
							if (queryResult.succeeded()) {
								// Extract 'name' from the result
								List<String> names = new ArrayList<>();
								List<JsonObject> rows = queryResult.result().getRows();
								if (rows != null) {
									for (JsonObject row : rows) {
										names.add(row.getString("name"));
									}
									logger.info("checkModifiedDates - Query execution successful. Retrieved names: {}",
											names);
									resultFuture.complete(names);
								} else {
									logger.info("checkModifiedDates - rows is empty");
								}

							} else {
								logger.error("checkModifiedDates - Query execution FAILED,", queryResult.cause());
								resultFuture.fail(queryResult.cause());
							}
						});
					} catch (Throwable t) {
						logger.error("checkModifiedDates - Error while querying database", t);
						resultFuture.fail(t);
					}
				} else {
					logger.error("checkModifiedDates - Connection failed,", connection.cause());
					resultFuture.fail(connection.cause());
				}
			});
		} else {
			logger.info("checkModifiedDates - SQL client object is null");
			resultFuture.fail("SQL client object is null");
		}

		return resultFuture.future();
	}

	public static void triggerCertificateOperation(Vertx vertx, Promise<List<CertificateInventoryBean>> parentPromise) {
		logger.info("Inside triggerCertificateOperation");

		SQLClient client = DBClientService.getSqlClient();
		if (client != null) {

			checkPrimaryKeyInDatabase().onComplete(result -> {
				if (result.succeeded()) {
					boolean isPresent = result.result();
					if (!isPresent) {
						logger.info("triggerCertificateOperation - Primary key not found Update existing entry");
						StringBuilder sqlQuery = new StringBuilder();
						sqlQuery.append(CertificateConstants.FETCH_CERT_AND_QUERY); // Query for fetching
																					// config_resource

						logger.info("triggerCertificateOperation - sqlQuery: {}",
								CertificateConstants.FETCH_CERT_AND_QUERY);

						JsonObject config = CommonUtility.getAlertConfig();
						JsonArray groupName = config.getJsonArray("groupName");
						JsonArray resourceType = config.getJsonArray("resourceType");
						logger.debug("triggerCertificateOperation - sqlQuery:{}, groupName:{}, resourceType:{}",
								sqlQuery, groupName, resourceType);

						String groupNameStr = getConvertedStrVal(groupName.getList());
						String resourcetypeStr = getConvertedStrVal(resourceType.getList());
						String finalQuery = sqlQuery.toString().replace("GROUP_NAME", groupNameStr)
								.replace("RESOURCE_TYPE", resourcetypeStr);
						logger.info("finalQuery: {}", finalQuery);

						CertificateAlertDBUtility certificateAlertDBUtility = CertificateAlertDBUtility.getInstance();

						Future<List<CertificateInventoryBean>> future = certificateAlertDBUtility
								.getListOfACSCertificateAlertData(vertx, finalQuery); // Extracting expiry date, trust
																						// store date and keystore date
																						// from certificates

						future.onComplete(handler2 -> {
							if (handler2.succeeded()) {
								logger.debug(
										"triggerCertificateOperation - extraction and insertion from certificate inventory successfull");
								parentPromise.complete(certificateInventoryList);
							} else
								parentPromise.fail("failed");
						});

					} else {
						logger.info("triggerCertificateOperation - Primary key found. Insertion of New Entry");
						StringBuilder sqlQuery = new StringBuilder();
						sqlQuery.append(CertificateConstants.FETCH_CERT_AND_QUERY);
						logger.info("triggerCertificateOperation - sqlQuery: {}",
								CertificateConstants.FETCH_CERT_AND_QUERY);

						JsonObject config = CommonUtility.getAlertConfig();
						JsonArray groupName = config.getJsonArray("groupName");
						JsonArray resourceType = config.getJsonArray("resourceType");
						logger.debug("triggerCertificateOperation - sqlQuery:{}, groupName:{}, resourceType:{}",
								sqlQuery, groupName, resourceType);
						String groupNameStr = getConvertedStrVal(groupName.getList());
						String resourcetypeStr = getConvertedStrVal(resourceType.getList());
						String finalQuery = sqlQuery.toString().replace("GROUP_NAME", groupNameStr)
								.replace("RESOURCE_TYPE", resourcetypeStr);
						logger.info("finalQuery: {}", finalQuery);

						CertificateAlertDBUtility certificateAlertDBUtility = CertificateAlertDBUtility.getInstance();
						Future<List<CertificateInventoryBean>> future = certificateAlertDBUtility
								.getConfigResourceData(vertx, finalQuery); // Extracting expiry date, trust store date
																			// and keystore date from certificates

						future.onComplete(handler2 -> {
							if (handler2.succeeded()) {
								logger.debug(
										"triggerCertificateOperation - extraction and insertion from certificate inventory successfull");
								parentPromise.complete(certificateInventoryList);
							} else
								parentPromise.fail("failed");
						});

						parentPromise.fail("failed");
					}
				} else {
					logger.error("triggerCertificateOperation - Failed to check primary key in the database: {}",
							result.cause());
					parentPromise.fail("failed");
				}
			});
		} else {
			logger.info("triggerCertificateOperation - sql client object is null");
		}
	}

	private static String getConvertedStrVal(List<Object> groupName) {
		return groupName.stream().filter(Objects::nonNull).map(String::valueOf)
				.collect(Collectors.joining("','", "'", "'"));
	}

	private static Future<Boolean> checkPrimaryKeyInDatabase() {
		Promise<Boolean> resultFuture = Promise.promise();

		SQLClient sqlClient = DBClientService.getSqlClient();
		StringBuilder query = new StringBuilder();
		query.append(CertificateConstants.FETCH_ROWS_CERT_INV);
		logger.info("checkPrimaryKeyInDatabase - query: {}", CertificateConstants.FETCH_ROWS_CERT_INV);

		if (sqlClient != null) {
			sqlClient.getConnection(connection -> {
				if (connection.succeeded()) {
					try (SQLConnection conn = connection.result()) {
						conn.query(query.toString(), resultHandler -> {
							if (resultHandler.succeeded()) {
								ResultSet rs = resultHandler.result();
								if (rs != null && rs.getNumRows() > 0) {
									resultFuture.complete(true);
								} else {
									resultFuture.complete(false);
								}
							} else {
								logger.error("checkPrimaryKeyInDatabase - Query execution FAILED,",
										resultHandler.cause());
								resultFuture.fail(resultHandler.cause());
							}
						});
					} catch (Throwable t) {
						logger.error("checkPrimaryKeyInDatabase - Error while querying database", t);
						resultFuture.fail(t);
					}
				} else {
					logger.error("checkPrimaryKeyInDatabase - Connection failed,", connection.cause());
					resultFuture.fail(connection.cause());
				}
			});
		} else {
			logger.error("checkPrimaryKeyInDatabase - sql client object is null");
			resultFuture.fail("SQL client object is null");
		}

		return resultFuture.future();
	}

	public static void downloadExpiryAlertCertificateSummary(Vertx vertx, List<CertificateJobBean> certificateJobBeans,
			int expiryDay, AlertBean alertBean) {

		logger.info("Inside downloadExpiryAlertCertificateSummary - Download Alert certificate query: {}",
				CertificateConstants.FETCH_CERT_ALERT_QUERY_DOWNLOAD);

		CertificateAlertDAODownload certificateAlertDAO = new CertificateAlertDAODownload();
		Promise<List<CertificateInventoryBean>> certificateInventoryFuture = Promise.promise();
		Future<List<CertificateInventoryBean>> expiryDetail = certificateAlertDAO.fetchCertificateAlertData(vertx,
				certificateInventoryFuture, certificateJobBeans, CertificateConstants.FETCH_CERT_ALERT_QUERY_DOWNLOAD,
				expiryDay);

		expiryDetail.onComplete(handler -> {
			if (handler.succeeded()) {
				List<CertificateInventoryBean> listOfCertificateData = handler.result();

				if (listOfCertificateData.size() > 0) {
					CertificateDataDBUtility.TriggerEmail(vertx, certificateJobBeans, expiryDay, listOfCertificateData,
							alertBean);
				} else {
					logger.info("downloadExpiryAlertCertificateSummary - certificate_inventory table is empty");
				}
			} else {
				logger.error(
						"downloadExpiryAlertCertificateSummary - Failed to Retrieve data from certificate_inventory table");
			}
		});
	}
}