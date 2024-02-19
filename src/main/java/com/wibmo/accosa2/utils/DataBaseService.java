///**
// * 
// */
package com.wibmo.accosa2.utils;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DataBaseService {
	private static Logger logger = LoggerFactory.getLogger(DataBaseService.class);;

	public static Future<Void> initializeDb(Vertx vertx) {

		Promise<Void> promise = Promise.promise();
		DBClientService.setDbSqlClient(vertx);

		promise.complete();
		return promise.future();
	}
}
