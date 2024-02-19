package com.wibmo.accosa2.utils;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

public class CertificateConstants {

	public static final String REG_EXP = "[\r\n]";
	public static final String ORDER_ASC = "asc";
	public static final String ORDER_DESC = "desc";
	
	public static final String HOURLY = "HOURLY";
	public static final String CERT = "CERT";
	public static final String EPURL = "EPURL";
	public static final String ALERTEMAIL = "ALERT-EMAIL";
	public static final String ALERTSMS = "ALERT-SMS";
	
	public static final String CERT_TYPE = "certType";
	public static final String SMTP_SERVER = "smtpServer";
	public static final String KEYSTORE = "keystore";
	public static final String TRUSTSTORE = "truststore";
	public static final String MESSAGE_SERVER = "messageServer";
	public static final String LAST_UPDATED_ON = "last_updated_on";
	public static final String EXPIRY_DATE = "expiryDate";
	public static final String COMMON_NAME = "commonName";
	
	public static final String INTERVAL_START = "intervalStart";
	public static final String INTERVAL_END = "intervalEnd";
	public static final String CONFIG_TYPE = "configType";
	
	public static final String ALERT_STATUS = "alertStatus";
	public static final String ALERT_TIME = "alertTime";
	public static final String PRIORITY = "priority";
	public static final String PRIMARY_RECIPIENT = "primaryRecipient";
	public static final String CC_RECIPIENT = "ccRecipient";
	public static final String EMAIL_SUB = "emailSubject";
	public static final String CREATED_DATE = "createdDate";
	public static final String MODIFIED_DATE = "modifiedDate";
	public static final String CREATED_BY = "createdBy";
	public static final String MODIFIED_BY = "modifiedBy";
	public static final String RESOURCE_NAME = "resource_name";
	public static final String CNAME = "cName";
	public static final String CERTIFICATE_TYPE = "cert_type";
	public static final String EXP_DATE = "expiry_date";
	public static final String CERT_NAME = "certificateName";
	public static final String CRT_TYPE = "certificateType";
	public static final String GROUP_NAME = "groupName";
	
	
	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	public static final SimpleDateFormat DTFORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final DateTimeFormatter DTFORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
	public static final DateTimeFormatter DATEFORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public static final String FETCH_CERT_AND_QUERY = "SELECT " + " name AS \"resourceName\","
			+ " value AS \"certName\"," + " type AS \"cType\"," + " group_name AS \"groupName\","
			+ " modified_date AS \"modifiedDate\" " + " FROM " + " config_resource " + " WHERE "
			+ " type IN (RESOURCE_TYPE) " + " and group_name IN (GROUP_NAME);";

	public static final String FETCH_CERT_CONFIG = "SELECT" + "interval_start_day as \"intervalStart\","
			+ " interval_end_day as \"intervalEnd\"," + " config_type as \"configType\","
			+ " alert_status_type as \"alertStatus\"," + " alert_schedule_time as \"alertTime\" "
			+ " FROM certificate_alert_config ";

	public static final String FETCH_CERT_ALERT_QUERY_DOWNLOAD = "SELECT " + "ci.resource_name AS \"certificateName\", "
			+ "ci.cname AS \"commonName\", " + "ci.cert_type AS \"certificateType\", "
			+ "ci.expiry_date AS \"expiryDate\", " + "ci.group_name AS \"groupName\" "
			+ "FROM certificate_inventory ci ";

	public static final String FETCH_CERT_CONFIG_JOIN_ALERT_QUERY = "SELECT "
			+ "c.interval_start_day as \"intervalStart\", " + "c.interval_end_day as \"intervalEnd\", "
			+ "c.config_type as \"configType\", " + "c.alert_status_type as \"alertStatus\", "
			+ "c.alert_schedule_time as \"alertTime\", " + "n.priority as \"priority\", "
			+ "n.primary_recipient as \"primaryRecipient\", " + "n.cc_recipient as \"ccRecipient\", "
			+ "n.email_subject as \"emailSubject\", " + "n.created_date as \"createdDate\", "
			+ "n.modified_date as \"modifiedDate\", " + "n.created_by as \"createdBy\", "
			+ "n.modified_by as \"modifiedBy\" " + "FROM certificate_alert_config c "
			+ "JOIN certificate_alert_notification_details n ON c.config_type = n.priority";

	public static final String FETCH_EXPIRY_DATE = "SELECT " + "expiry_date from certificate_inventory";

	public static final String FETCH_ROWS_CERT_INV = "SELECT * FROM certificate_inventory";

	public static final String FETCH_NAME_TYPE_CERT_INV = "SELECT resource_name, cert_type FROM certificate_inventory "
			+ "WHERE resource_name = ? AND cert_type = ?";

	public static final String FETCH_LAST_UPDATE_CERT_INV = "SELECT last_updated_on FROM certificate_inventory "
			+ "WHERE resource_name = ? AND cert_type = ?";

	public static final String UPSERT_CERTIFICATE_INVENTORY = "INSERT INTO certificate_inventory "
			+ "(resource_name, cname, cert_type, expiry_date, last_updated_on, group_name) "
			+ "VALUES (?, ?, ?, ?, ?, ?) "
			+ "ON DUPLICATE KEY UPDATE expiry_date = VALUES(expiry_date), "
			+ "last_updated_on = VALUES(last_updated_on)";
}
