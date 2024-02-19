package com.wibmo.accosa2.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wibmo.accosa2.bean.AlertBean;
import com.wibmo.accosa2.bean.CertificateInventoryBean;
import com.wibmo.accosa2.bean.CertificateJobBean;
import com.wibmo.accosa2.observer.FirstObserver;

import io.vertx.core.json.JsonObject;

public class CertificateJobSchedulerUtility {

	private static Logger logger = LoggerFactory.getLogger(CertificateJobSchedulerUtility.class);

	private CertificateJobSchedulerUtility() {

	}

	public static List<CertificateJobBean> fetchBeanFromList(List<JsonObject> list) {
		logger.info("Inside fetchBeanFromList");

		List<CertificateJobBean> infoList = null;
		CertificateJobBean certificateInfo = null;

		if (list != null && !list.isEmpty()) {
			infoList = new ArrayList<>();
			for (JsonObject obj : list) {
				certificateInfo = new CertificateJobBean();

				certificateInfo.setIntervalStartDate(obj.getString(CertificateConstants.INTERVAL_START));
				certificateInfo.setIntervalEndDate(obj.getString(CertificateConstants.INTERVAL_END));
				certificateInfo.setConfigType(obj.getString(CertificateConstants.CONFIG_TYPE));
				certificateInfo.setAlertStatusType(obj.getString(CertificateConstants.ALERT_STATUS));

				String alertScheduleTimeArray = obj.getString(CertificateConstants.ALERT_TIME);
				List<Integer> alertScheduleList = Arrays.stream(alertScheduleTimeArray.split(","))
						.filter(s -> !s.isEmpty()).map(Integer::parseInt).collect(Collectors.toList());
				certificateInfo.setAlertScheduleTime(alertScheduleList);
				logger.debug("fetchBeanFromList - Certificate Config Details - fetchBeanFromList:{}", certificateInfo);

				AlertBean alertBean = new AlertBean();
				alertBean.setPriority(obj.getString(CertificateConstants.PRIORITY));
				alertBean.setPrimaryRecipient(obj.getString(CertificateConstants.PRIMARY_RECIPIENT));
				alertBean.setCcRecipient(obj.getString(CertificateConstants.CC_RECIPIENT));
				alertBean.setEmailSubject(obj.getString(CertificateConstants.EMAIL_SUB));
				alertBean.setCreatedDate(LocalDateTime.parse(obj.getString(CertificateConstants.CREATED_DATE), CertificateConstants.DTFORMAT));
				alertBean.setModifiedDate(LocalDateTime.parse(obj.getString(CertificateConstants.MODIFIED_DATE), CertificateConstants.DTFORMAT));
				alertBean.setCreatedBy(obj.getString(CertificateConstants.CREATED_BY));
				alertBean.setModifiedBy(obj.getString(CertificateConstants.MODIFIED_BY));

				// Set the embedded AlertBean
				certificateInfo.setAlertBean(alertBean);

				infoList.add(certificateInfo);

			}
		}
		return infoList;
	}

	public static List<CertificateInventoryBean> fetchInventoryFromList(List<JsonObject> objList) {
		logger.info("Inside fetchInventoryFromList");
		List<CertificateInventoryBean> inventoryList = null;
		CertificateInventoryBean certificateInventory = null;

		if (objList != null && !objList.isEmpty()) {
			inventoryList = new ArrayList<>();
			for (JsonObject obj : objList) {
				certificateInventory = new CertificateInventoryBean();
				certificateInventory.setResourceName(obj.getString(CertificateConstants.RESOURCE_NAME));
				certificateInventory.setcName(obj.getString(CertificateConstants.CNAME));
				certificateInventory.setCertType(obj.getString(CertificateConstants.CERTIFICATE_TYPE));
				certificateInventory.setLastUpdatedOn(obj.getString(CertificateConstants.LAST_UPDATED_ON));
				certificateInventory.setExpiryDate(obj.getString(CertificateConstants.EXP_DATE));
				logger.info("Certificate Inventory Details - fetchInventoryFromList:{}", certificateInventory);
				inventoryList.add(certificateInventory);

			}
		}
		return inventoryList;
	}

	public static List<CertificateInventoryBean> downloadInventoryFromList(List<JsonObject> objList,
			List<CertificateJobBean> certificateJobBeans, int expiryDay) {
		logger.info("Inside downloadInventoryFromList");
		List<CertificateInventoryBean> downloadList = new ArrayList<>();
		int intervalStartDay = 0;
		int intervalEndDay = 0;

		LocalDate currentDate = LocalDate.now();

		for (CertificateJobBean jobBean : certificateJobBeans) {
			if (expiryDay >= Integer.parseInt(jobBean.getIntervalStartDate())
					&& expiryDay < Integer.parseInt(jobBean.getIntervalEndDate())) {
				intervalStartDay = Integer.parseInt(jobBean.getIntervalStartDate());
				intervalEndDay = Integer.parseInt(jobBean.getIntervalEndDate());
				break; // Stop processing for this expiry day
			}
		}

		for (JsonObject obj : objList) {

			LocalDateTime dateTime = LocalDateTime.parse(obj.getString(CertificateConstants.EXPIRY_DATE), CertificateConstants.FORMATTER);

			// Extract the LocalDate from the LocalDateTime
			String expiryDate = dateTime.format(DateTimeFormatter.ISO_DATE);
			LocalDate formattedDate = dateTime.toLocalDate();

			if (formattedDate != null) {
				int remainingDays = FirstObserver.calculateRemainingDays(formattedDate, LocalDate.now());

				if (remainingDays >= intervalStartDay && remainingDays < intervalEndDay) {
					CertificateInventoryBean downloadInventory = new CertificateInventoryBean();
					downloadInventory.setResourceName(obj.getString(CertificateConstants.CERT_NAME));
					downloadInventory.setcName(obj.getString(CertificateConstants.COMMON_NAME));
					downloadInventory.setCertType(obj.getString(CertificateConstants.CRT_TYPE));
					downloadInventory.setExpiryDate(expiryDate);
					downloadInventory.setDateRemaining(String.valueOf(remainingDays));
					downloadInventory.setGroupName(obj.getString(CertificateConstants.GROUP_NAME));

					downloadList.add(downloadInventory);
				}
			}
		}

		return downloadList;
	}
}
