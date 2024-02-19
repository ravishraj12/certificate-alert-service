
package com.wibmo.accosa2.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;

public class DBClientService {
	private static final String supportUTF8ForMySqlJdbcUrl = "?useUnicode=yes&characterEncoding=UTF-8&enabledTLSProtocols=TLSv1.2";
	public static JsonObject dbconfig;

	private static final Logger logger = LoggerFactory.getLogger(DBClientService.class);

	private static SQLClient sqlClient;

	public static SQLClient getSqlClient() {
		return sqlClient;
	}

	public static void setSqlClient(SQLClient sqlClient) {
		DBClientService.sqlClient = sqlClient;
	}

	public static void configureJson(String ipport, String schema, String userName, String password) {
		dbconfig = new JsonObject();
		dbconfig.put("provider_class", "io.vertx.ext.jdbc.spi.impl.HikariCPDataSourceProvider");
		dbconfig.put("driver_class", "com.mysql.jdbc.Driver");
		dbconfig.put("jdbcUrl", "jdbc:mysql://" + ipport + "/" + schema + "" + supportUTF8ForMySqlJdbcUrl);
		dbconfig.put("username", userName);
		dbconfig.put("password", password);
		dbconfig.put("maximumPoolSize", 5);
		dbconfig.put("minimumIdle", 1);
		dbconfig.put("leakDetectionThreshold", 100000);
	}

	public static void setDbSqlClient(Vertx vertx) {
		SQLClient client = prepareJDBCConfig(vertx, dbconfig, "ACS_TXN_SHARED");
		logger.info("setDbSqlClient - {}", client);
		setSqlClient(client);
	}

	public static SQLClient prepareJDBCConfig(Vertx vertx, JsonObject sqlClientConfig, String dsName) {
		SQLClient client = JDBCClient.createShared(vertx, sqlClientConfig, dsName);
		logger.info("sqlClientConfig: {}, dsName: {}, client: {}", sqlClientConfig, dsName, client);
		return JDBCClient.createShared(vertx, sqlClientConfig, dsName);
	}
}
