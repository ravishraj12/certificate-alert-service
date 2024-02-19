package com.wibmo.accosa2.observer;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wibmo.accosa2.bean.CertificateInventoryBean;
import com.wibmo.accosa2.handler.CertificateAlertHandler;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import rx.Observer;

public class FirstObserver implements Observer<Object> {

	private static Logger logger = LoggerFactory.getLogger(FirstObserver.class);

	private final Vertx vertx;

	public FirstObserver(Vertx vertx) {
		this.vertx = vertx;
	}

	@SuppressWarnings("deprecation")
	public void insertData(Vertx vertx) {

		logger.info("Inside insertData");

		Promise<List<CertificateInventoryBean>> promise = Promise.promise();
		CertificateAlertHandler.triggerCertificateOperation(vertx, promise); // fetching all the information from
																				// config_resource table
	}

	public static int calculateRemainingDays(LocalDate expiryDate, LocalDate currentDate) {
		return (int) ChronoUnit.DAYS.between(currentDate, expiryDate);
	}

	private static boolean isDayInRange(int day, String startDay, String endDay) {
		return day >= Integer.parseInt(startDay) && day < Integer.parseInt(endDay);
	}

	@Override
	public void onNext(Object t) {
		insertData(vertx);
	}

	@Override
	public void onCompleted() {
		logger.info("Inside onCompleted method at time");
	}

	@Override
	public void onError(Throwable e) {
		logger.error("Inside onError Method", e);
	}

}
