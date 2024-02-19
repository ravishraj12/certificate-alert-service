
package com.wibmo.accosa2.utils;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wibmo.accosa2.bean.AlertBean;
import com.wibmo.accosa2.bean.CertificateInventoryBean;
import com.wibmo.accosa2.bean.CertificateJobBean;

import io.vertx.core.Vertx;

public class ProcessExpiryUtility {

	private static Logger logger = LoggerFactory.getLogger(CertificateJobSchedulerUtility.class);

	public static void processExpiryDay(Vertx vertx, List<CertificateJobBean> certificateJobBeans, int expiryDay,
			List<CertificateInventoryBean> listCertificateInventoryBean, AlertBean alertBean) {

		logger.info("Inside processExpiryDay");
		for (CertificateJobBean jobBean : certificateJobBeans) {
			if (expiryDay >= Integer.parseInt(jobBean.getIntervalStartDate())
					&& expiryDay < Integer.parseInt(jobBean.getIntervalEndDate())) {
				if (jobBean.getAlertStatusType().equalsIgnoreCase(CertificateConstants.HOURLY)) {
					logger.info("processExpiryDay - Hourly Mail Triggered");
					TriggerEmailAlerts.sendEmail(vertx, listCertificateInventoryBean, alertBean);
					break;
				} else {
					if (checkAlertSchedule(jobBean.getAlertScheduleTime())) {
						logger.info("processExpiryDay - Mail Triggered for Config Type: {}", jobBean.getConfigType());
						TriggerEmailAlerts.sendEmail(vertx, listCertificateInventoryBean, alertBean);
						break; // Stop processing for this expiry day
					}
				}
			} else {
				logger.info("processExpiryDay - Your expiry does not scheduled for this hour");
			}
		}
	}

	public static boolean checkAlertSchedule(List<Integer> alertScheduleTime) {
		LocalDateTime now = LocalDateTime.now();
		int currentHour = now.getHour();
//        int currentMinute = now.getMinute();
//        int currentSecond = now.getSecond();

		boolean isHourMatch = alertScheduleTime.contains(currentHour);
//        boolean isMinuteAndSecondZero = currentMinute == 0 && currentSecond == 0;

//        return isHourMatch && isMinuteAndSecondZero;
		return isHourMatch;
	}
}
