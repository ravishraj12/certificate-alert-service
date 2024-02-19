package com.wibmo.accosa2.config.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wibmo.accosa2.bean.CertificateInventoryBean;
import com.wibmo.accosa2.bean.CertificateJobBean;
import com.wibmo.accosa2.utils.CertificateExpiryDownload;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class CertificateAlertDAODownload {

	private static Logger LOGGER = LoggerFactory.getLogger(CertificateAlertDAODownload.class);

	public Future<List<CertificateInventoryBean>> fetchCertificateAlertData(Vertx vertx,
			Promise<List<CertificateInventoryBean>> certificateInventoryFuture,
			List<CertificateJobBean> certificateJobBeans, String sqlQuery, int expiryDay) {
		LOGGER.info("fetchCertificateAlertData - Fetch Certificate alert data");
		return CertificateExpiryDownload.downloadExpiryCertificateAlertData(vertx, certificateInventoryFuture,
				certificateJobBeans, sqlQuery, expiryDay);
	}

}