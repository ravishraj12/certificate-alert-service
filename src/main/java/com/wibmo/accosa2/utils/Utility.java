package com.wibmo.accosa2.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wibmo.accosa2.acs.common.json.Json;
import com.wibmo.accosa2.common.util.LocalAESEncDec;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class Utility {

	public enum FileUploadStatus {
		SUCCESS, FAILED, IN_PROGRESS;
	}

	private Utility() {

	}

	/** logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(Utility.class);

	/** mapperObj. */
	// public static final ObjectMapper MAPPER_OBJ = Json.serializer().mapper();

	public static final String CONTENT_TYPE_HEADER = "Content-Type";
	public static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

	public static final String COMMA_SEPERATOR = ",";
	private static final String DATE_TIME = "timestamp";
	private static final String YYYY_MM = "yyyyMM";
	private static final String YYYY_MM_DD = "yyyy-MM-dd";
	private static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
	private static int ruleIdLength = 6;

	/** local encrypt and decrypt */
	private static final String ENCRYPTLOCAL = "encryptlocal";
	private static final String DECRYPTLOCAL = "decryptlocal";

	public static final String ORACLE_TYPE = "oracle";
	public static final String SQLSERVER_TYPE = "sqlserver";
	public static final String POSTGRE_TYPE = "postgresql";
	// Expects a lower case
	private static String DB_TYPE = "";

	/**
	 * Method to send response based on the operation performed with respCode.
	 * 
	 * @param routingContext object to access context.
	 * @param obj            needs to be sent to the request initiator.
	 */
	public static void sendResponse(final RoutingContext routingContext, final int respCode, final Object obj) {
		try {
			ObjectMapper mapper = Json.serializer().mapper();
			LOGGER.trace("Ending the request here! {}", obj.toString().replaceAll(CertificateConstants.REG_EXP, ""));
			if (respCode <= 0) {
				routingContext.response().putHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON)
						.end(mapper.writeValueAsString(obj));
			} else {
				routingContext.response().putHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON).setStatusCode(respCode)
						.end(mapper.writeValueAsString(obj));
			}
		} catch (final JsonProcessingException e) {
			LOGGER.error("Error while posting the response back: {}",
					e.getMessage().replaceAll(CertificateConstants.REG_EXP, ""));
		}
	}

	/**
	 * Method to convert date in UTC format.
	 * 
	 * @param routingContext object to access context.
	 * @param obj            needs to be sent to the request initiator.
	 */
	public static String convertDateToUTC(JsonObject jsonObject) {
		final DateFormat utcDateFormat = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
		utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		final String utcDateTime = jsonObject.getValue(DATE_TIME) != null ? jsonObject.getValue(DATE_TIME).toString()
				: utcDateFormat.format(new Date());

		LOGGER.info("UTC DATE TIME: {}", utcDateTime);
		return utcDateTime;
	}

	/**
	 * Method to provide suffix for table name.
	 * 
	 * @param routingContext object to access context.
	 * @param obj            needs to be sent to the request initiator.
	 */
	public static String getDateTimeSufix() {
		final DateFormat utcDateFormat = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
		final DateFormat dateFormatToBeSuffixed = new SimpleDateFormat(YYYY_MM);
		final DateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD);
		final String utcDateTime = utcDateFormat.format(new Date());
		Date date;
		String dateTimeSuffix = null;
		try {
			date = dateFormat.parse(utcDateTime);
			dateTimeSuffix = dateFormatToBeSuffixed.format(date);
		} catch (final ParseException ex) {
			LOGGER.error("====Exception in parsing date===={}", ex);
		}
		return dateTimeSuffix;
	}

	/**
	 * Method to format date to YYYY_MM_DD_HH_MM_SS to string.
	 * 
	 * @param date object to retrieve date value.
	 * @return updated date string.
	 */
	public static String formatDateStrYYYY_MM_DD_HH_MM_SS(final Date date) {
		final SimpleDateFormat sdf = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
		final String dateStr = sdf.format(date);

		return dateStr;
	}

	/**
	 * Method to format date to YYYY_MM_DD to string.
	 * 
	 * @param date object to retrieve date value.
	 * @return updated date string.
	 */
	public static String formatDateYYYY_MM_DD(final Date date) {
		final SimpleDateFormat sdf = new SimpleDateFormat(YYYY_MM_DD);
		final String updatedDate = sdf.format(date);
		LOGGER.info("formatDateYYYY_MM_DD updatedDate: {}", updatedDate);
		return updatedDate;
	}

	/**
	 * Method to provide the table suffix in YYYYMM format.
	 * 
	 * @param date       object to retrieve month and year.
	 * @param suffixType regex to convert to.
	 * @return table suffix once prepared.
	 */
	public static String getTableSouffix(final Date date, final String suffixType) {
		final StringBuffer monthStr = new StringBuffer();
		final Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		final int appendMonthValue = 1;
		final int month = cal.get(Calendar.MONTH) + appendMonthValue;
		if ((month) <= 9) {
			monthStr.append("0" + month);
		} else {
			monthStr.append("" + month);
		}

		final StringBuffer tableSuffix = new StringBuffer();
		if (YYYY_MM.equalsIgnoreCase(suffixType)) {
			tableSuffix.append(cal.get(Calendar.YEAR));
			tableSuffix.append("");
			tableSuffix.append(monthStr.toString());
		}
		return tableSuffix.toString();
	}

	/**
	 * Method to validate data for null or blank check.
	 * 
	 * @param value to validate for all checks
	 * @return true if null or blank else false.
	 */
	public static boolean isNullOrBlank(final Object value) {
		boolean response = false;
		LOGGER.trace("isNullOrBlankCheck - value: {}", value);
		if (null == value) {
			response = true;
		} else if (value instanceof String) {
			LOGGER.trace("If is String");
			if ("".equalsIgnoreCase((String) value)) {
				response = true;
			}
		} else if (value instanceof Integer) {
			LOGGER.trace("If is Integer");
			if (0 == (int) value) {
				LOGGER.trace("If in int");
				response = true;
			}
		} else if (value instanceof Character) {
			LOGGER.trace("If is Char");
			if (' ' == (char) value) {
				LOGGER.trace("If in char");
				response = true;
			}
		}
		LOGGER.info("Response after isNullOrBlank Check: {}", response);
		return response;
	}

	public static String generateRuleId() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < ruleIdLength; i++) {
			buffer.append(generateSecureRandom());
		}
		return buffer.toString();
	}

	private static char generateSecureRandom() {
		SecureRandom objSecureRandom = new SecureRandom();
		return (char) (48 + (int) (objSecureRandom.nextDouble() * 10));
	}

	public static String getFormattedDate(String inputDate) {
		String outputDate = null;
		if (null != inputDate) {
			SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			Date parsedDate = null;
			try {
				parsedDate = inputFormat.parse(inputDate);
				outputDate = outputFormat.format(parsedDate);
			} catch (ParseException e) {

			}
		}
		return outputDate;
	}

	public static Object getObjectFromString(String input, Class cls) {
		ObjectMapper mapper = Json.serializer().mapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Object object = null;
		try {
			object = mapper.readValue(input, cls);
		} catch (Exception e) {
			LOGGER.error("Error occured : {}", e);
		}
		return object;
	}

	public static String get_SHA_1_SecureOTP(String passwordToHash, byte[] salt) {
		String generatedPassword = null;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(salt);
			byte[] bytes = md.digest(passwordToHash.getBytes());
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < bytes.length; i++) {
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			generatedPassword = sb.toString();
		} catch (NoSuchAlgorithmException e) {

		}
		return generatedPassword;
	}

	public static String get_tokenVerified(String loginID, String salt, String opt) {
		String generatedPassword = null;
		String decryptedString = null;
		try {
			String value = loginID + "|" + salt + Instant.now().toEpochMilli();
			if (ENCRYPTLOCAL.equalsIgnoreCase(opt)) {
				generatedPassword = LocalAESEncDec.encrypt(value);
				LOGGER.info("*TOKEN ID***%%%%% :: {}", generatedPassword);
				return generatedPassword;
			} else if (DECRYPTLOCAL.equalsIgnoreCase(opt)) {
				decryptedString = LocalAESEncDec.decrypt(loginID);
				LOGGER.info("*TOKEN DECRYPT***%%%%% :: {}", decryptedString);
				String[] id = decryptedString.split("\\|");
				generatedPassword = id[0];
				LOGGER.info("*USER ID***%%%%% :: {}", id[0]);
				return generatedPassword;
			}
		} catch (Exception e) {
			LOGGER.info("Exception :: {}", e);
		}
		return generatedPassword;
	}

	public static String getCalculatedTimeDifference(String systemProperty, String timeZone) {
		int seconds;
		int seconds2;
		int minutes;
		int hours;
		int hours2seconds;
		int minutes2seconds;

		seconds = TimeZone.getTimeZone(timeZone).getRawOffset() / 1000;

		LOGGER.debug("*** timeZone raw offset ***:", seconds);

		// split(systemProperty)
		String[] timeData = systemProperty.split(":");
		LOGGER.info("hours :" + timeData[0] + " minutes :", timeData[1]);
		hours2seconds = (Integer.parseInt(timeData[0])) * 3600;
		if (timeData[0].startsWith("-")) {
			minutes2seconds = (Integer.parseInt("-" + timeData[1])) * 60;

		} else {
			minutes2seconds = (Integer.parseInt(timeData[1])) * 60;
		}
		LOGGER.debug("*** timeData[0] :" + minutes2seconds);
		LOGGER.debug("hours2 & minutes2 :" + (minutes2seconds + hours2seconds));
		seconds2 = hours2seconds + minutes2seconds;
		LOGGER.debug("*** seconds2 ***:" + seconds2);

		hours = (seconds + seconds2) / 3600;
		minutes = ((seconds + seconds2) % 3600) / 60;

		LOGGER.debug("hours :{}" + hours);
		LOGGER.debug("minutes :{}" + minutes);

		String finalString = "+00:00";
		if ((hours + "").startsWith("-")) {
			finalString = hours + ":" + minutes;
		} else {
			finalString = "+" + hours + ":" + minutes;
		}
		LOGGER.info(" *** Final Value *** :" + finalString);
		return finalString;
	}

	public static final String stmtDtFmtConTzDtTime(String fieldName, String targetTimezone) {
		StringBuilder sqlQuery = new StringBuilder();
		// original mysql -
		// date_format(CONVERT_TZ(date_time,'+00:00','").append(convertedTime2 + "'),
		// '%Y-%m-%d %H:%i:%s:%f')
		if (targetTimezone == null)
			targetTimezone = "+00:00";

		switch (DB_TYPE) {
		// ORACLE
		case ORACLE_TYPE:
			sqlQuery.append(" TO_CHAR(FROM_TZ(cast(" + fieldName + " as timestamp),'UTC') AT TIME ZONE '"
					+ targetTimezone + "','YYYY-MM-DD HH24:MI:SS') ");
			break;
		// SQLSERVER
		case SQLSERVER_TYPE:
			sqlQuery.append(
					" DATE_FORMAT(TODATETIMEOFFSET(" + fieldName + "," + targetTimezone + "),'yyyy-MM-dd HH:mm:ss') ");
			break;
		// POSTGRESQL
		case POSTGRE_TYPE:
			sqlQuery.append(" TO_CHAR(CONVERT_TZ(" + fieldName + ",'+00:00','" + targetTimezone
					+ "'),'YYYY-MM-DD HH24:MI:SS') ");
			break;
		default:
			sqlQuery.append(" DATE_FORMAT(CONVERT_TZ(" + fieldName + ",'+00:00','" + targetTimezone
					+ "'),'%Y-%m-%d %H:%i:%s') ");
		}
		return sqlQuery.toString();
	}

	public static final String stmtLimitOffset(int noOfRows, int offset) {
		StringBuilder sqlQuery = new StringBuilder();

		switch (DB_TYPE) {
		// ORACLE
		case ORACLE_TYPE:
			sqlQuery.append(" OFFSET " + offset + " ROWS FETCH NEXT " + noOfRows + " ROWS ONLY");
			break;
		// SQLSERVER
		case SQLSERVER_TYPE:
			sqlQuery.append(" OFFSET " + offset + " ROWS FETCH NEXT " + noOfRows + " ROWS ONLY");
			break;
		// POSTGRESQL
		case POSTGRE_TYPE:
			// SQLSERVER is same as mysql the default
		default:
			// do nothing alreay set above
			sqlQuery.append(" LIMIT " + noOfRows + " OFFSET " + offset);
		}
		return sqlQuery.toString();
	}

}
