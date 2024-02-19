package com.wibmo.accosa2.utils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wibmo.accosa2.acs.common.json.Json;
import com.wibmo.accosa2.bean.CertificateInventoryBean;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

public class CertificateAlertDBUtility {

	private static Logger LOGGER = LoggerFactory.getLogger(CertificateAlertDBUtility.class);
	private static CertificateAlertDBUtility certificateAlertDBUtility;
	static List<CertificateInventoryBean> arrayList = new ArrayList<CertificateInventoryBean>();
	static List<CertificateInventoryBean> arrayListData = new ArrayList<>();

	public Future<List<CertificateInventoryBean>> getListOfACSCertificateAlertData(Vertx vertx, String queryToExec) {
		LOGGER.info("*** Inside getListOfACSCertificateAlertData ***");
		Promise<List<CertificateInventoryBean>> certificateInventoryFuture = Promise.promise();

		vertx.executeBlocking(futureLoc -> {
			SQLClient client = DBClientService.getSqlClient();
			if (client != null) {
				client.getConnection(res -> {
					try {
						if (res.succeeded()) {
							try (SQLConnection connection = res.result()) {
								connection.query(queryToExec, resultHandler -> {
									try {
										if (resultHandler.succeeded()) {
											List<JsonObject> results = resultHandler.result().getRows();
											for (JsonObject jsonObj : results) {
												LOGGER.trace("====== Certificate JSON OBJECT: : {}",
														jsonObj.toString());
												try {
													ObjectMapper MAPPER_OBJ = Json.serializer().mapper();
													CertificateInventoryBean certificateInventoryBean = MAPPER_OBJ
															.readValue(jsonObj.toString(),
																	CertificateInventoryBean.class);
													String certType = certificateInventoryBean.getcType();
													LOGGER.trace(
															"getListOfACSCertificateAlertData - Certificate configuration type :{} ",
															certType);
													JsonObject jsonObject = null;

													if (CertificateConstants.CERT.equalsIgnoreCase(certType)) {
														processCert(certificateInventoryBean,
																vertx);
													} else if (CertificateConstants.ALERTEMAIL.equalsIgnoreCase(certType)) {
														String resourceName = certificateInventoryBean
																.getResourceName();
														jsonObject = certificateInventoryBean.getCertName();

														if (jsonObject.containsKey(CertificateConstants.SMTP_SERVER)) {
															JsonObject smtpServerObject = jsonObject
																	.getJsonObject(CertificateConstants.SMTP_SERVER);
															processAlertEmailIfPresent(smtpServerObject,
																	CertificateConstants.KEYSTORE, resourceName, certificateInventoryBean,
																	vertx);
															processAlertEmailIfPresent(smtpServerObject,
																	CertificateConstants.TRUSTSTORE, resourceName,
																	certificateInventoryBean, vertx);
														}
													} else if (CertificateConstants.ALERTSMS.equalsIgnoreCase(certType)) {
														String resourceName = certificateInventoryBean
																.getResourceName();
														jsonObject = certificateInventoryBean.getCertName();

														if (jsonObject.containsKey(CertificateConstants.MESSAGE_SERVER)) {
															JsonObject messageServerObject = jsonObject
																	.getJsonObject(CertificateConstants.MESSAGE_SERVER);
															processAlertSmsIfPresent(messageServerObject,
																	CertificateConstants.KEYSTORE, resourceName, certificateInventoryBean,
																	vertx);
															processAlertSmsIfPresent(messageServerObject,
																	CertificateConstants.TRUSTSTORE, resourceName,
																	certificateInventoryBean, vertx);
														}
													} else if (CertificateConstants.EPURL.equalsIgnoreCase(certType)) {
														jsonObject = certificateInventoryBean.getCertName();
														prcoessEpurlIfPresent(jsonObject, CertificateConstants.KEYSTORE,
																certificateInventoryBean, vertx);
														prcoessEpurlIfPresent(jsonObject, CertificateConstants.TRUSTSTORE,
																certificateInventoryBean, vertx);
													} else {
														continue; // Skip to the next iteration if certType is not
																	// recognized
													}
												} catch (JsonParseException | JsonMappingException e) {
													LOGGER.error("Error parsing/mapping object: {}", e.getMessage()
															.replaceAll(CertificateConstants.REG_EXP, ""));
												} catch (IOException e) {
													LOGGER.error("IO Exception: {}", e.getMessage()
															.replaceAll(CertificateConstants.REG_EXP, ""));
												}
											}
											certificateInventoryFuture.complete(arrayListData);
										} else {
											LOGGER.info("Cause of the issue:: {}",
													resultHandler != null ? resultHandler.cause() : null);
											certificateInventoryFuture.fail("certificateInventoryFuture failed");
										}
									} catch (Exception e) {
										certificateInventoryFuture
												.fail("certificateInventoryFuture failed Exception : " + e);
										LOGGER.error("DB Exception -->: {}, {}", e,
												e != null ? e.getMessage().replaceAll(CertificateConstants.REG_EXP, "")
														: null);
									}
								});
							}
						} else {
							certificateInventoryFuture.fail(res != null ? res.cause() : null);
							client.close();
						}
					} catch (Exception e) {
						certificateInventoryFuture.fail(e);
						LOGGER.error("Error ::  ", e);
					}
				});
			} else {
				LOGGER.info("getListOfACSCertificateAlertData - SQL client object is null");
			}
		}, null);

		return certificateInventoryFuture.future();
	}

	public Future<List<CertificateInventoryBean>> getConfigResourceData(Vertx vertx, String queryToExec) {

		LOGGER.info("*** Inside getConfigResourceData ***");
		Promise<List<CertificateInventoryBean>> certificateInventoryFuture = Promise.promise();
		vertx.executeBlocking(futureLoc -> {
			SQLClient client = DBClientService.getSqlClient();
			if (client != null) {
				client.getConnection(res -> {

					try {
						if (res.succeeded()) {
							SQLConnection connection = res.result();
							connection.query(queryToExec, resultHandler -> {

								try {
									if (resultHandler.succeeded()) {
										List<JsonObject> results = resultHandler.result().getRows();
										if(results != null) {
											for (JsonObject jsonObj : results) {
												LOGGER.trace("====== Certificate JSON OBJECT: : {}", jsonObj.toString());
												try {
													ObjectMapper MAPPER_OBJ = Json.serializer().mapper();
													CertificateInventoryBean certificateInventoryBean = MAPPER_OBJ
															.readValue(jsonObj.toString(), CertificateInventoryBean.class);

													String certType = certificateInventoryBean.getcType();
													LOGGER.trace(
															"getConfigResourceData - Certificate configuration type :{} ",
															certType);
													JsonObject jsonObject = null;
													if (CertificateConstants.CERT.equalsIgnoreCase(certType)) {
														String resourceName = certificateInventoryBean.getResourceName();
														String modifiedDate = certificateInventoryBean.getModifiedDate();
														LocalDateTime dateTime = LocalDateTime.parse(modifiedDate,
																DateTimeFormatter.ISO_DATE_TIME);
														String formattedDate = dateTime.format(DateTimeFormatter.ISO_DATE);

														jsonObject = certificateInventoryBean.getCertName();
														String certTypeValue = jsonObject.getString(CertificateConstants.CERT_TYPE);

														CertificateAlertDBUtility
																.compareDates(resourceName, certTypeValue, formattedDate)
																.onSuccess(result -> {
																	LOGGER.debug("CERT keystore Result:{} ", result);
																	if (result == 0 || result == 1) {
																		processCert(
																				certificateInventoryBean, vertx);
																	}
																}).onFailure(result -> {
																	LOGGER.info("CERT Failed");
																});
													} else if (CertificateConstants.ALERTEMAIL.equalsIgnoreCase(certType)) {
														String resourceName = certificateInventoryBean.getResourceName();
														String modifiedDate = certificateInventoryBean.getModifiedDate();
														LocalDateTime dateTime = LocalDateTime.parse(modifiedDate,
																DateTimeFormatter.ISO_DATE_TIME);
														String formattedDate = dateTime.format(DateTimeFormatter.ISO_DATE);

														jsonObject = certificateInventoryBean.getCertName();

														if (jsonObject.containsKey(CertificateConstants.SMTP_SERVER)) {
															JsonObject smtpServerObject = jsonObject
																	.getJsonObject(CertificateConstants.SMTP_SERVER);
															if (smtpServerObject.containsKey(CertificateConstants.KEYSTORE)) {
																String certTypeValue = smtpServerObject
																		.getJsonObject(CertificateConstants.KEYSTORE).getString(CertificateConstants.CERT_TYPE);
																if (certTypeValue != null) {
																	CertificateAlertDBUtility.compareDates(resourceName,
																			certTypeValue, formattedDate)
																			.onSuccess(result -> {
																				LOGGER.debug(
																						"ALERT-EMAIL keystore Result:{} ",
																						result);
																				if (result == 0 || result == 1) {
																					processAlertEmail(
																							certificateInventoryBean,
																							certTypeValue, vertx);
																				}
																			}).onFailure(result -> {
																				LOGGER.info("ALERT-EMAIL Failed");
																			});
																}
															}
															if (smtpServerObject.containsKey(CertificateConstants.TRUSTSTORE)) {
																String certTypeValue = smtpServerObject
																		.getJsonObject(CertificateConstants.TRUSTSTORE).getString(CertificateConstants.CERT_TYPE);
																if (certTypeValue != null) {
																	CertificateAlertDBUtility.compareDates(resourceName,
																			certTypeValue, formattedDate)
																			.onSuccess(result -> {
																				LOGGER.debug(
																						"ALERT-EMAIL keystore Result:{} ",
																						result);
																				if (result == 0 || result == 1) {
																					processAlertEmail(
																							certificateInventoryBean,
																							certTypeValue, vertx);
																				}
																			}).onFailure(result -> {
																				LOGGER.info("ALERT-EMAIL Failed");
																			});
																}
															}
														}
													} else if (CertificateConstants.ALERTSMS.equalsIgnoreCase(certType)) {
														String resourceName1 = certificateInventoryBean.getResourceName();
														String modifiedDate = certificateInventoryBean.getModifiedDate();
														LocalDateTime dateTime = LocalDateTime.parse(modifiedDate,
																DateTimeFormatter.ISO_DATE_TIME);
														String formattedDate = dateTime.format(DateTimeFormatter.ISO_DATE);

														jsonObject = certificateInventoryBean.getCertName();

														if (jsonObject.containsKey(CertificateConstants.MESSAGE_SERVER)) {
															JsonObject messageServerObject = jsonObject
																	.getJsonObject(CertificateConstants.MESSAGE_SERVER);
															if (messageServerObject.containsKey(CertificateConstants.KEYSTORE)) {
																String certTypeValue = messageServerObject
																		.getJsonObject(CertificateConstants.KEYSTORE).getString(CertificateConstants.CERT_TYPE);
																if (certTypeValue != null) {
																	CertificateAlertDBUtility.compareDates(resourceName1,
																			certTypeValue, formattedDate)
																			.onSuccess(result -> {
																				LOGGER.debug(
																						"ALERT-SMS keystore Result:{} ",
																						result);
																				if (result == 0 || result == 1) {
																					processAlertSms(
																							certificateInventoryBean,
																							certTypeValue, vertx);
																				}
																			}).onFailure(result -> {
																				LOGGER.info("ALERT-SMS Failed");
																			});
																}
															}
															if (messageServerObject.containsKey(CertificateConstants.TRUSTSTORE)) {
																String certTypeValue = messageServerObject
																		.getJsonObject(CertificateConstants.TRUSTSTORE).getString(CertificateConstants.CERT_TYPE);
																if (certTypeValue != null) {
																	CertificateAlertDBUtility.compareDates(resourceName1,
																			certTypeValue, formattedDate)
																			.onSuccess(result -> {
																				LOGGER.debug(
																						"ALERT-SMS keystore Result:{} ",
																						result);
																				if (result == 0 || result == 1) {
																					processAlertSms(
																							certificateInventoryBean,
																							certTypeValue, vertx);
																				}
																			}).onFailure(result -> {
																				LOGGER.info("ALERT-SMS Failed");
																			});
																}
															}
														}
													} else if (CertificateConstants.EPURL.equalsIgnoreCase(certType)) {
														String resourceName1 = certificateInventoryBean.getResourceName();
														String modifiedDate = certificateInventoryBean.getModifiedDate();
														LocalDateTime dateTime = LocalDateTime.parse(modifiedDate,
																DateTimeFormatter.ISO_DATE_TIME);
														String formattedDate = dateTime.format(DateTimeFormatter.ISO_DATE);

														jsonObject = certificateInventoryBean.getCertName();

														if (jsonObject.containsKey(CertificateConstants.KEYSTORE)) {
															Object keystoreObject = jsonObject.getValue(CertificateConstants.KEYSTORE);
															if (keystoreObject instanceof JsonObject) {
																JsonObject keystoreJsonObject = (JsonObject) keystoreObject;
																String certTypeValue = keystoreJsonObject
																		.getString(CertificateConstants.CERT_TYPE);
																CertificateAlertDBUtility.compareDates(resourceName1,
																		certTypeValue, formattedDate).onSuccess(result -> {
																			LOGGER.debug("EPURL keystore Result:{} ",
																					result);
																			if (result == 0 || result == 1) {
																				processEpurl(
																						certificateInventoryBean,
																						certTypeValue, vertx);
																			}
																		}).onFailure(result -> {
																			LOGGER.info("EPURL Failed");
																		});
															}
														}
														if (jsonObject.containsKey(CertificateConstants.TRUSTSTORE)) {
															Object truststoreObject = jsonObject.getValue(CertificateConstants.TRUSTSTORE);
															if (truststoreObject instanceof JsonObject) {
																JsonObject truststoreJsonObject = (JsonObject) truststoreObject;
																String certTypeValue = truststoreJsonObject
																		.getString(CertificateConstants.CERT_TYPE);
																CertificateAlertDBUtility.compareDates(resourceName1,
																		certTypeValue, formattedDate).onSuccess(result -> {
																			LOGGER.debug("EPURL keystore Result:{} ",
																					result);
																			if (result == 0 || result == 1) {
																				processEpurl(
																						certificateInventoryBean,
																						certTypeValue, vertx);
																			}
																		}).onFailure(result -> {
																			LOGGER.info("EPURL Failed");
																		});
															}
														}
													} else {
														continue;
													}
												} catch (JsonParseException e) {
													LOGGER.error("Error parsing object: {}",
															e.getMessage().replaceAll(CertificateConstants.REG_EXP, ""));
												} catch (JsonMappingException e) {
													LOGGER.error("Error mapping object: {}",
															e.getMessage().replaceAll(CertificateConstants.REG_EXP, ""));
												} catch (IOException e) {
													LOGGER.error("IO Exception: {}",
															e.getMessage().replaceAll(CertificateConstants.REG_EXP, ""));
												} catch (Exception e) {
													LOGGER.error("Exception: {}",
															e.getMessage().replaceAll(CertificateConstants.REG_EXP, ""));
												}
											}
										}else {
											LOGGER.debug("result is empty");
										}
									} else {
										LOGGER.info("Cause of the issue:: {}",
												resultHandler != null ? resultHandler.cause() : null);
										certificateInventoryFuture.fail("certificateInventoryFuture failed");
									}
								} catch (Exception e) {
									certificateInventoryFuture
											.fail("certificateInventoryFuture failed Exception : " + e);
									LOGGER.error("DB Exception -->: {}, {}", e,
											e != null ? e.getMessage().replaceAll(CertificateConstants.REG_EXP, "")
													: null);
								} finally {
									LOGGER.info("closing connection inside finally block");
									if (connection != null)
										connection.close();
								}

							});
						} else {
							certificateInventoryFuture.fail(res != null ? res.cause() : null);
							client.close();

						}
					} catch (Exception e) {
						certificateInventoryFuture.fail(e);
						LOGGER.error("Error ::  ", e);
					}

				});
			} else {
				LOGGER.info("getListOfACSCertificateAlertData - SQL client object is null");
			}

		}, null);

		return certificateInventoryFuture.future();
	}

	private static Future<Integer> compareDates(String resourceName, String certType, String modifiedDate) {
		Promise<Integer> resultFuture = Promise.promise();

		checkPrimaryKeyInDatabase(resourceName, certType).onComplete(exists -> {
			if (exists.succeeded()) {
				if (exists.result()) {
					// Record exists, now check modified_date
					checkModifiedDate(resourceName, certType, modifiedDate).onComplete(match -> {
						if (match.succeeded()) {
							if (match.result()) {
								resultFuture.complete(2); // Modified_date equals last_update_on
								LOGGER.info("compareDates - Modified_date equals last_update_on");
							} else {
								resultFuture.complete(1); // Modified_date is different
								LOGGER.info("compareDates - Modified_date is different");
							}
						} else {
							LOGGER.error("compareDates - Error checking modified_date:", match.cause());
							resultFuture.fail(match.cause());
						}
					});
				} else {
					// Record does not exist
					LOGGER.info("compareDates - Record does not exist");
					resultFuture.complete(0);
				}
			} else {
				LOGGER.error("compareDates - Error checking primary key in the database:", exists.cause());
				resultFuture.fail(exists.cause());
			}
		});

		return resultFuture.future();
	}

	private static Future<Boolean> checkPrimaryKeyInDatabase(String resourceName, String certType) {
		Promise<Boolean> resultFuture = Promise.promise();
		SQLClient sqlClient = DBClientService.getSqlClient();
		StringBuilder query = new StringBuilder();

		query.append(CertificateConstants.FETCH_NAME_TYPE_CERT_INV);
		LOGGER.info("checkPrimaryKeyInDatabase - query:{}", CertificateConstants.FETCH_NAME_TYPE_CERT_INV);

		if (sqlClient != null) {
			sqlClient.getConnection(connection -> {
				if (connection.succeeded()) {
					try (SQLConnection conn = connection.result()) {
						JsonArray params = new JsonArray().add(resourceName).add(certType);

						conn.queryWithParams(query.toString(), params, resultHandler -> {
							if (resultHandler.succeeded()) {
								ResultSet rs = resultHandler.result();
								resultFuture.complete(rs != null && rs.getNumRows() > 0);
								LOGGER.debug("Query execution successful. Rows found: {}", rs.getNumRows());
							} else {
								LOGGER.error("checkPrimaryKeyInDatabase - Query ResultSet FAILED,",
										resultHandler.cause());
								resultFuture.fail(resultHandler.cause());
							}
						});
					} catch (Throwable t) {
						LOGGER.error("checkPrimaryKeyInDatabase - Error while querying database", t);
						resultFuture.fail(t);
					}
				} else {
					LOGGER.error("checkPrimaryKeyInDatabase - Connection failed,", connection.cause());
					resultFuture.fail(connection.cause());
				}
			});
		} else {
			LOGGER.info("checkPrimaryKeyInDatabase - sql client object is null");
			resultFuture.fail("SQL client object is null");
		}

		return resultFuture.future();
	}

	private static Future<Boolean> checkModifiedDate(String resourceName, String certType, String modifiedDate) {
		DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		Promise<Boolean> resultFuture = Promise.promise();
		SQLClient sqlClient = DBClientService.getSqlClient();
		StringBuilder query = new StringBuilder();

		query.append(CertificateConstants.FETCH_LAST_UPDATE_CERT_INV);
		LOGGER.info("checkModifiedDate - query for last_update_on: {}",
				CertificateConstants.FETCH_LAST_UPDATE_CERT_INV);

		if (sqlClient != null) {
			sqlClient.getConnection(connection -> {
				if (connection.succeeded()) {
					try (SQLConnection conn = connection.result()) {
						JsonArray params = new JsonArray().add(resourceName).add(certType);
						LOGGER.debug("checkModifiedDate ResourceName: {}, CertType: {}", resourceName, certType);

						conn.queryWithParams(query.toString(), params, resultHandler -> {
							if (resultHandler.succeeded()) {
								ResultSet rs = resultHandler.result();
								List<JsonObject> rows = rs.getRows();
								LOGGER.debug("checkModifiedDate ResourceName: {}, CertType: {}", resourceName,
										certType);

								if (rows != null && !rows.isEmpty()) {
									JsonObject row = rows.get(0);
									String lastUpdatedOnStr = row.getString(CertificateConstants.LAST_UPDATED_ON);

									if (lastUpdatedOnStr != null) {
										LocalDateTime dateTime = LocalDateTime.parse(lastUpdatedOnStr, customFormatter);
										String formattedDate = dateTime.format(DateTimeFormatter.ISO_DATE);
										LOGGER.debug("Comparing modifiedDate: {} with last_update_on: {}", modifiedDate,
												formattedDate);
										resultFuture.complete(modifiedDate.equals(formattedDate));
										LOGGER.debug("Comparison result: {}", modifiedDate.equals(formattedDate));
									} else {
										resultFuture.complete(false);
									}
								} else {
									LOGGER.error("last_update_on is null. Completing with false.");
									resultFuture.complete(false);
								}
							} else {
								LOGGER.error("checkModifiedDate - Query ResultSet FAILED,", resultHandler.cause());
								resultFuture.fail(resultHandler.cause());
							}
						});
					} catch (Throwable t) {
						LOGGER.error("checkModifiedDate - Error while querying database", t);
						resultFuture.fail(t);
					}
				} else {
					LOGGER.error("checkModifiedDate - Connection failed,", connection.cause());
					resultFuture.fail(connection.cause());
				}
			});
		} else {
			LOGGER.info("checkModifiedDate - sql client object is null");
			resultFuture.fail("SQL client object is null");
		}

		return resultFuture.future();
	}

	private void prcoessEpurlIfPresent(JsonObject jsonObject, String storeType,
			CertificateInventoryBean certificateInventoryBean, Vertx vertx) {
		if (jsonObject.containsKey(storeType)) {
			Object storeObject = jsonObject.getValue(storeType);
			if (storeObject instanceof JsonObject) {
				JsonObject storeJsonObject = (JsonObject) storeObject;
				String certTypeValue = storeJsonObject.getString(CertificateConstants.CERT_TYPE);
				if (certTypeValue != null) {
					processEpurl(certificateInventoryBean, certTypeValue, vertx);
				}
			}
		}
	}

	private void processAlertSmsIfPresent(JsonObject messageServerObject, String storeType,
			String resourceName, CertificateInventoryBean certificateInventoryBean, Vertx vertx) {
		if (messageServerObject.containsKey(storeType)) {
			Object storeTypeValue = messageServerObject.getValue(storeType);

			if (storeTypeValue instanceof JsonObject) {
				JsonObject storeTypeJsonObject = (JsonObject) storeTypeValue;

				String certTypeValue = storeTypeJsonObject.getString(CertificateConstants.CERT_TYPE);
				if (certTypeValue != null) {
					processAlertSms(certificateInventoryBean, certTypeValue, vertx);
				}
			} else {
				LOGGER.error("Invalid value for {} in messageServerObject. Expected JsonObject.", storeType);
			}
		}
	}

	private void processAlertEmailIfPresent(JsonObject smtpServerObject, String storeType, String resourceName,
			CertificateInventoryBean certificateInventoryBean, Vertx vertx) {
		if (smtpServerObject.containsKey(storeType)) {
			Object storeTypeValue = smtpServerObject.getValue(storeType);

			if (storeTypeValue instanceof JsonObject) {
				JsonObject storeTypeJsonObject = (JsonObject) storeTypeValue;

				String certTypeValue = storeTypeJsonObject.getString(CertificateConstants.CERT_TYPE);
				if (certTypeValue != null) {
					processAlertEmail(certificateInventoryBean, certTypeValue, vertx);
				}
			} else {
				LOGGER.warn("Invalid value for {} in smtpServerObject. Expected JsonObject.", storeType);
			}
		}
	}

	private void processEpurl(CertificateInventoryBean certificateInventoryBean,
			String certTypeValue, Vertx vertx) {
		JsonObject jsonObject = certificateInventoryBean.getCertName();

		if (certTypeValue != null && certTypeValue.equalsIgnoreCase(CertificateConstants.KEYSTORE)) {
			if (jsonObject.containsKey(CertificateConstants.KEYSTORE)) {
				Object keystoreObject = jsonObject.getValue(CertificateConstants.KEYSTORE);
				if (keystoreObject instanceof JsonObject) {
					JsonObject keystoreJsonObject = (JsonObject) keystoreObject;
					Long keyStoreExpiryDate = keystoreJsonObject.getLong(CertificateConstants.EXPIRY_DATE);
					String commonName = keystoreJsonObject.getString(CertificateConstants.COMMON_NAME);
					String resourceName = certificateInventoryBean.getResourceName();
					String groupName = certificateInventoryBean.getGroupName();

					CertificateInventoryBean keystoreJobBean = new CertificateInventoryBean();
					keystoreJobBean.setResourceName(resourceName);
					keystoreJobBean.setCertType(certTypeValue);
					keystoreJobBean.setcName(commonName);
					keystoreJobBean.setExpiryDate(CertificateConstants.DTFORMATTER.format(keyStoreExpiryDate));
					LocalDateTime now = LocalDateTime.now();
					Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
					keystoreJobBean.setLastUpdatedOn(CertificateConstants.DTFORMATTER.format(date));
					keystoreJobBean.setGroupName(groupName);
					insertOrUpdateDataIntoInventory(vertx, keystoreJobBean);
				}
			}
		} else if (certTypeValue != null && certTypeValue.equalsIgnoreCase(CertificateConstants.TRUSTSTORE)) {
			if (jsonObject.containsKey(CertificateConstants.TRUSTSTORE)) {
				Object truststoreObject = jsonObject.getValue(CertificateConstants.TRUSTSTORE);
				if (truststoreObject instanceof JsonObject) {
					JsonObject truststoreJsonObject = (JsonObject) truststoreObject;
					Long keyStoreExpiryDate = truststoreJsonObject.getLong(CertificateConstants.EXPIRY_DATE);
					String commonName = truststoreJsonObject.getString(CertificateConstants.COMMON_NAME);
					String resourceName = certificateInventoryBean.getResourceName();
					String groupName = certificateInventoryBean.getGroupName();

					CertificateInventoryBean truststoreJobBean = new CertificateInventoryBean();
					truststoreJobBean.setResourceName(resourceName);
					truststoreJobBean.setCertType(certTypeValue);
					truststoreJobBean.setcName(commonName);
					truststoreJobBean.setExpiryDate(CertificateConstants.DTFORMATTER.format(keyStoreExpiryDate));
					LocalDateTime now = LocalDateTime.now();
					Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
					truststoreJobBean.setLastUpdatedOn(CertificateConstants.DTFORMATTER.format(date));
					truststoreJobBean.setGroupName(groupName);
					insertOrUpdateDataIntoInventory(vertx, truststoreJobBean);
				}
			}

		} else {
			LOGGER.info("Neither keystore nor truststore is present");
		}
	}

	private void processAlertSms(CertificateInventoryBean certificateInventoryBean,
			String certTypeValue, Vertx vertx) {
		JsonObject jsonObject = certificateInventoryBean.getCertName();
		Long expiryDateKeyStore = null;
		Long expiryDateTrustStore = null;

		JsonObject messageServer = jsonObject.getJsonObject(CertificateConstants.MESSAGE_SERVER);

		// Processing keystore data
		if (certTypeValue.equalsIgnoreCase(CertificateConstants.KEYSTORE)) {
			Object keystoreObject = messageServer.getValue(CertificateConstants.KEYSTORE);
			if (keystoreObject instanceof JsonObject) {
				JsonObject keystore = (JsonObject) keystoreObject;
				expiryDateKeyStore = keystore.getLong(CertificateConstants.EXPIRY_DATE);
				String commonName = keystore.getString(CertificateConstants.COMMON_NAME);
				processStoreData(vertx, certificateInventoryBean, expiryDateKeyStore, commonName, keystore);
			}
		}

		// Processing truststore data
		if (certTypeValue.equalsIgnoreCase(CertificateConstants.TRUSTSTORE)) {
			Object truststoreObject = messageServer.getValue(CertificateConstants.TRUSTSTORE);
			if (truststoreObject instanceof JsonObject) {
				JsonObject truststore = (JsonObject) truststoreObject;
				expiryDateTrustStore = truststore.getLong(CertificateConstants.EXPIRY_DATE);
				String commonName = truststore.getString(CertificateConstants.COMMON_NAME);
				processStoreData(vertx, certificateInventoryBean, expiryDateTrustStore, commonName, truststore);
			}
		}
//    }
	}

	private void processAlertEmail(CertificateInventoryBean certificateInventoryBean,
			String certTypeValue, Vertx vertx) {
		JsonObject jsonObject = certificateInventoryBean.getCertName();
		Long expiryDateKeyStore = null;
		Long expiryDateTrustStore = null;

		JsonObject smtpServer = jsonObject.getJsonObject(CertificateConstants.SMTP_SERVER);

		// Processing keystore data
		if (certTypeValue.equalsIgnoreCase(CertificateConstants.KEYSTORE)) {
			Object keystoreObject = smtpServer.getValue(CertificateConstants.KEYSTORE);
			if (keystoreObject instanceof JsonObject) {
				JsonObject keystore = (JsonObject) keystoreObject;
				expiryDateKeyStore = keystore.getLong(CertificateConstants.EXPIRY_DATE);
				String commonName = keystore.getString(CertificateConstants.COMMON_NAME);
				processStoreData(vertx, certificateInventoryBean, expiryDateKeyStore, commonName, keystore);
			}
		}

		// Processing truststore data
		if (certTypeValue.equalsIgnoreCase(CertificateConstants.TRUSTSTORE)) {
			Object truststoreObject = smtpServer.getValue(CertificateConstants.TRUSTSTORE);
			if (truststoreObject instanceof JsonObject) {
				JsonObject truststore = (JsonObject) truststoreObject;
				expiryDateTrustStore = truststore.getLong(CertificateConstants.EXPIRY_DATE);
				String commonName = truststore.getString(CertificateConstants.COMMON_NAME);
				processStoreData(vertx, certificateInventoryBean, expiryDateTrustStore, commonName, truststore);
			}
		}
	}

	private void processCert(CertificateInventoryBean certificateInventoryBean, Vertx vertx) {
		JsonObject jsonObject = certificateInventoryBean.getCertName();
		Long expiryDate = (Long) jsonObject.getValue(CertificateConstants.EXPIRY_DATE);
		String commonName = jsonObject.getString(CertificateConstants.COMMON_NAME);
		String certTypeValue = jsonObject.getString(CertificateConstants.CERT_TYPE);
		String groupName = certificateInventoryBean.getGroupName();

		if (expiryDate != null) {
			certificateInventoryBean.setcName(commonName);
			certificateInventoryBean.setCertType(certTypeValue);
			certificateInventoryBean.setExpiryDate(CertificateConstants.DTFORMATTER.format(expiryDate));
			LocalDateTime now = LocalDateTime.now();
			Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
			certificateInventoryBean.setLastUpdatedOn(CertificateConstants.DTFORMATTER.format(date));
			certificateInventoryBean.setGroupName(groupName);
			insertOrUpdateDataIntoInventory(vertx, certificateInventoryBean);
		}
	}

	private static void processStoreData(Vertx vertx, CertificateInventoryBean certificateInventoryBean,
			Long expiryDate, String commonName, JsonObject storeObject) {
		if (expiryDate != null && commonName != null) {
			String certTypeValue = storeObject.getString(CertificateConstants.CERT_TYPE);
			String resourceName = certificateInventoryBean.getResourceName();
			String groupName = certificateInventoryBean.getGroupName();

			CertificateInventoryBean storeJobBean = new CertificateInventoryBean();
			storeJobBean.setResourceName(resourceName);
			storeJobBean.setCertType(certTypeValue);
			storeJobBean.setcName(commonName);
			storeJobBean.setExpiryDate(CertificateConstants.DTFORMATTER.format(expiryDate));
			LocalDateTime now = LocalDateTime.now();
			Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
			storeJobBean.setLastUpdatedOn(CertificateConstants.DTFORMATTER.format(date));
			storeJobBean.setGroupName(groupName);
			insertOrUpdateDataIntoInventory(vertx, storeJobBean);
		}
	}

	private static void insertOrUpdateDataIntoInventory(Vertx vertx,
			CertificateInventoryBean certificateInventoryBean) {
		LOGGER.info("Inside insertOrUpdateDataIntoInventory");

		SQLClient sqlClient = DBClientService.getSqlClient();
		StringBuilder query = new StringBuilder();
		query.append(CertificateConstants.UPSERT_CERTIFICATE_INVENTORY);

		LOGGER.info("insertOrUpdateDataIntoInventory - Certificate Alert Config Query: {}",
				CertificateConstants.UPSERT_CERTIFICATE_INVENTORY);

		if (sqlClient != null) {
			sqlClient.getConnection(connection -> {
				if (connection.succeeded()) {
					try (SQLConnection conn = connection.result()) {
						JsonArray params = new JsonArray().add(certificateInventoryBean.getResourceName())
								.add(certificateInventoryBean.getcName()).add(certificateInventoryBean.getCertType())
								.add(certificateInventoryBean.getExpiryDate())
								.add(certificateInventoryBean.getLastUpdatedOn())
								.add(certificateInventoryBean.getGroupName());

						conn.updateWithParams(query.toString(), params, result -> {
							if (result.succeeded()) {
								LOGGER.info("Data inserted or updated successfully into certificate_inventory");
							} else {
								LOGGER.error("Failed to insert or update data into certificate_inventory: {}",
										result.cause());
							}
						});
					} catch (Throwable t) {
						LOGGER.error("Error while connecting to the database", t);
					}
				} else {
					LOGGER.error("insertOrUpdateDataIntoInventory - Connection failed: {}", connection.cause());
				}
			});
		} else {
			LOGGER.info("insertOrUpdateDataIntoInventory - SQL client object is null");
		}
	}

	public static synchronized CertificateAlertDBUtility getInstance() {
		if (certificateAlertDBUtility == null) {
			synchronized (CertificateAlertDBUtility.class) {
				if (certificateAlertDBUtility == null) {
					certificateAlertDBUtility = new CertificateAlertDBUtility();
				}
			}
		}
		return certificateAlertDBUtility;
	}

}