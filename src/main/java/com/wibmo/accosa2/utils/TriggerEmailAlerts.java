package com.wibmo.accosa2.utils;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wibmo.accosa2.bean.AlertBean;
import com.wibmo.accosa2.bean.CertificateInventoryBean;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;

public class TriggerEmailAlerts {

	private static Logger logger = LoggerFactory.getLogger(TriggerEmailAlerts.class);

	// For sending email alert
	public static void sendEmail(Vertx vertx, List<CertificateInventoryBean> listCertificateInventoryBean,
			AlertBean alertBean) {
		logger.info("Inside sendMail");
		Promise<Integer> emailPromise = Promise.promise();
		Properties properties = CommonUtility.properties;

		MailConfig config = new MailConfig();

		config.setHostname(properties.getProperty("mail.smtp.host"));
		config.setPort(Integer.parseInt(properties.getProperty("mail.smtp.port")));
		config.setUsername(properties.getProperty("mail.smtp.user"));
		config.setMaxPoolSize(Integer.parseInt(properties.getProperty("max.pool.size")));

		logger.info("SMTP_PROPERTIES - Host - {}, Port - {}, User - {}", config.getHostname(), config.getPort(),
				config.getUsername());

		MailMessage emailMessage = new MailMessage();
		logger.debug("Email Details: {}", alertBean);

		if (alertBean != null) {
			emailMessage.setFrom(properties.getProperty("MSG_FROM"));
			String toAddresses = alertBean.getPrimaryRecipient();
			List<String> toAddressList = Arrays.asList(toAddresses.split("\\s*,\\s*"));
			emailMessage.setTo(toAddressList);

//			Process and set the "CC" addresses
			String ccAddresses = alertBean.getCcRecipient();
			List<String> ccAddressList = Arrays.asList(ccAddresses.split("\\s*,\\s*"));
			emailMessage.setCc(ccAddressList);

			// Set the subject
			emailMessage.setSubject(alertBean.getEmailSubject());
		} else {
			logger.error("alertBean is empty");
		}

		String emailMessageText = substituteParams();
		logger.debug("Email Message Text - {}", emailMessageText);

		StringBuilder htmlContentBuilder = new StringBuilder();

		// Add table header
		htmlContentBuilder.append(
				"<table border='1'><tr><th>Group Name</th><th>Resource Name</th><th>Common Name</th><th>Certificate Type</th><th>Expiry Date</th><th>Days Remaining</th></tr>");

		// Add table rows from listCertificateInventoryBean
		for (CertificateInventoryBean certificate : listCertificateInventoryBean) {
			htmlContentBuilder.append("<tr>");
			htmlContentBuilder.append("<td>").append(certificate.getGroupName()).append("</td>");
			htmlContentBuilder.append("<td>").append(certificate.getResourceName()).append("</td>");
			htmlContentBuilder.append("<td>").append(certificate.getcName()).append("</td>");
			htmlContentBuilder.append("<td>").append(certificate.getCertType()).append("</td>");
			htmlContentBuilder.append("<td>").append(certificate.getExpiryDate()).append("</td>");
			htmlContentBuilder.append("<td>").append(certificate.getDateRemaining()).append("</td>");
			htmlContentBuilder.append("</tr>");
		}

		htmlContentBuilder.append("</table>");

		// Set the HTML content to the email message
		emailMessage.setHtml(emailMessageText.replace("EMAIL_SIGN_OFF", htmlContentBuilder.toString()));

		MailClient mailClient = MailClient.create(vertx, config);

		mailClient.sendMail(emailMessage, result -> {
			if (result.succeeded()) {
				closeMailClient(mailClient);
				logger.info("Email Response - {}", result.result());
				emailPromise.complete(OK.code());

			}
			if (result.failed()) {
				closeMailClient(mailClient);
				Throwable cause = result.cause();
				logger.error("Exception while sending the email - {}", cause.getMessage(), cause);
				emailPromise.complete(INTERNAL_SERVER_ERROR.code());
			}
		});
	}

	public static String substituteParams() {
		try {
			String templateContent = new String(Files.readAllBytes(Paths.get("config/default-email-template.html")));
			return templateContent;
		} catch (Exception e) {
			logger.error("Error reading email template file", e);
		}
		return null;
	}

	public static void closeMailClient(MailClient mailClient) {
		try {
			mailClient.close();
			logger.info("Mail client successfully closed.");
		} catch (Exception e) {
			logger.error("Error while closing the Mail Client - ", e);
			mailClient = null;
		}
	}
}
