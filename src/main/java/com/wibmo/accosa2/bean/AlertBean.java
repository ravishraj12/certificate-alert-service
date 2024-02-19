package com.wibmo.accosa2.bean;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AlertBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String priority;
	private String primaryRecipient;
	private String ccRecipient;
	private String emailSubject;
	private LocalDateTime createdDate;
	private LocalDateTime modifiedDate;
	private String createdBy;
	private String modifiedBy;

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public String getPrimaryRecipient() {
		return primaryRecipient;
	}

	public void setPrimaryRecipient(String primaryRecipient) {
		this.primaryRecipient = primaryRecipient;
	}

	public String getCcRecipient() {
		return ccRecipient;
	}

	public void setCcRecipient(String ccRecipient) {
		this.ccRecipient = ccRecipient;
	}

	public String getEmailSubject() {
		return emailSubject;
	}

	public void setEmailSubject(String emailSubject) {
		this.emailSubject = emailSubject;
	}

	public LocalDateTime getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(LocalDateTime createdDate) {
		this.createdDate = createdDate;
	}

	public LocalDateTime getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(LocalDateTime modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	@Override
	public String toString() {
		return "AlertBean [priority=" + priority + ", primaryRecipient=" + primaryRecipient + ", ccRecipient="
				+ ccRecipient + ", emailSubject=" + emailSubject + ", createDate=" + createdDate + ", modifiedDate="
				+ modifiedDate + ", createdBy=" + createdBy + ", modifiedBy=" + modifiedBy + "]";
	}
}
