package com.wibmo.accosa2.observer;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wibmo.accosa2.bean.AlertBean;
import com.wibmo.accosa2.bean.CertificateJobBean;
import com.wibmo.accosa2.db.CertificateDataDBUtility;
import com.wibmo.accosa2.handler.CertificateAlertHandler;
import com.wibmo.accosa2.utils.AlertConfigInfo;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import rx.Observer;

public class SecondObserver implements Observer<Object> {

	private static Logger logger = LoggerFactory.getLogger(SecondObserver.class);

	private List<Integer> remainingDayExpiryList;
	private List<CertificateJobBean> configList;

	private final Vertx vertx;

	public SecondObserver(Vertx vertx) {
		this.vertx = vertx;
	}

	@SuppressWarnings("deprecation")
	public void triggerAlert(Vertx vertx) {
		logger.info("Inside triggerAlert");

		CertificateAlertHandler certificateAlertHandler = new CertificateAlertHandler();

		certificateAlertHandler.insertUpdatedCertificates(vertx);
		logger.debug("triggerAlert - insertUpdatedCertificates successfull");

		Future<List<CertificateJobBean>> certificateConfigs = CertificateDataDBUtility
				.fetchAlertConfigsFromDatabase(vertx); // Fetching all the information from certificate_alert_config
														// table
		logger.debug("triggerAlert - certificate_alert_config Fetch Successful");

		certificateConfigs.onComplete(ar -> {
			if (ar.succeeded()) {
				configList = ar.result();
			} else {
				Throwable error = ar.cause();
				logger.error("triggerAlert - Failed to fetch certificate alert configurations from the database",
						error);
			}
		});

		LocalDate currentDate = LocalDate.now();

		Future<List<LocalDate>> expiryDateList = CertificateDataDBUtility.fetchExpiryDatesFromDatabase(vertx); // Fetching
																												// inventory
		logger.info("triggerAlert - Expiry date fetch successful");

		expiryDateList.onComplete(resultHandler -> {
			if (resultHandler.succeeded()) {
				List<LocalDate> expiryDates = resultHandler.result();

//		         Calculate remaining days for each expiry date
				remainingDayExpiryList = expiryDates.stream()
						.map(expiryDate -> calculateRemainingDays(expiryDate, currentDate))
						.filter(remainingDays -> remainingDays >= 0 && remainingDays <= 45).distinct()
						.collect(Collectors.toList());

				logger.debug("triggerAlert - Remaining Days list: {}", remainingDayExpiryList);

				logger.debug("triggerAlert - Size of config Map: {}", configList.size());

				Map<String, AlertConfigInfo> resultMap = configList.stream()
						.collect(Collectors.toMap(CertificateJobBean::getConfigType, bean -> {
							List<Integer> remainingDaysList = remainingDayExpiryList.stream().filter(
									day -> isDayInRange(day, bean.getIntervalStartDate(), bean.getIntervalEndDate()))
									.collect(Collectors.toList());

							return new AlertConfigInfo(remainingDaysList, bean.getAlertBean());
						}, (info1, info2) -> {
							List<Integer> mergedList = new ArrayList<>(info1.getRemainingDaysList());
							mergedList.addAll(info2.getRemainingDaysList());
							return new AlertConfigInfo(mergedList, info1.getAlertBean());
						}));

				resultMap.forEach((configType, alertConfigInfo) -> logger.debug("{}: {}", configType, String.join(", ",
						alertConfigInfo.getRemainingDaysList().stream().map(String::valueOf).toArray(String[]::new))));

				resultMap.forEach((configType, alertConfigInfo) -> {
					List<Integer> remainingDaysList = alertConfigInfo.getRemainingDaysList();
					AlertBean alertBean = alertConfigInfo.getAlertBean();

					if (!remainingDaysList.isEmpty()) {
						int expiryDay = remainingDaysList.get(0);
						CertificateAlertHandler.downloadExpiryAlertCertificateSummary(vertx, configList, expiryDay,
								alertBean);
						logger.info("triggerAlert - downloadExpiryAlertCertificateSummary successful");
					} else {
						logger.info("triggerAlert - Remaining Days List is Empty");
					}
				});
			} else {
				Throwable cause = resultHandler.cause();
				logger.error("triggerAlert - Failed to fetch nearest expiry dates from the database: {}", cause);
			}
		});
	}

	public static int calculateRemainingDays(LocalDate expiryDate, LocalDate currentDate) {
		return (int) ChronoUnit.DAYS.between(currentDate, expiryDate);
	}

	private static boolean isDayInRange(int day, String startDay, String endDay) {
		return day >= Integer.parseInt(startDay) && day < Integer.parseInt(endDay);
	}

	@Override
	public void onNext(Object t) {
		triggerAlert(vertx);
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
