package com.wibmo.accosa2.bean;

import java.io.Serializable;

import io.vertx.core.json.JsonObject;

public class CertificateInventoryBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String resourceName;
	private JsonObject certName;
	private String cName;
	private String certType;
	private String cType;
	private String expiryDate;
	private String lastUpdatedOn;
	private String configType;
	private JsonObject configValue;
	private String dateRemaining;
	private String bankId;
	private String modifiedDate;
	private String groupName;

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(String modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	public String getcType() {
		return cType;
	}

	public void setcType(String cType) {
		this.cType = cType;
	}

	public String getBankId() {
		return bankId;
	}

	public void setBankId(String bankId) {
		this.bankId = bankId;
	}

	public String getDateRemaining() {
		return dateRemaining;
	}

	public void setDateRemaining(String dateRemaining) {
		this.dateRemaining = dateRemaining;
	}

	public JsonObject getConfigValue() {
		return configValue;
	}

	public void setConfigValue(JsonObject configValue) {
		this.configValue = configValue;
	}

	public JsonObject getCertName() {
		return certName;
	}

	public void setCertName(JsonObject certName) {
		this.certName = certName;
	}

	public String getcName() {
		return cName;
	}

	public void setcName(String cName) {
		this.cName = cName;
	}

	public String getConfigType() {
		return configType;
	}

	public void setConfigType(String configType) {
		this.configType = configType;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public String getCertType() {
		return certType;
	}

	public void setCertType(String certType) {
		this.certType = certType;
	}

	public String getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(String expiryDate) {
		this.expiryDate = expiryDate;
	}

	public String getLastUpdatedOn() {
		return lastUpdatedOn;
	}

	public void setLastUpdatedOn(String lastUpdatedOn) {
		this.lastUpdatedOn = lastUpdatedOn;
	}

	@Override
	public String toString() {
		return "CertificateInventoryBean [resourceName=" + resourceName + ", certName=" + certName + ", cName=" + cName
				+ ", certType=" + certType + ", expiryDate=" + expiryDate + ", lastUpdatedOn=" + lastUpdatedOn
				+ ", configType=" + configType + ", configValue=" + configValue + ", dateRemaining=" + dateRemaining
				+ ", bankId=" + bankId + "]";
	}
}
