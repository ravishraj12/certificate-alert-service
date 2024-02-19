package com.wibmo.accosa2.bean;

import java.io.Serializable;
import java.util.List;

public class CertificateJobBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String intervalStartDate;
	private String intervalEndDate;
	private String configType;
	private String alertStatusType;
	private List<Integer> alertScheduleTime;

	private AlertBean alertBean;

	public AlertBean getAlertBean() {
		return alertBean;
	}

	public void setAlertBean(AlertBean alertBean) {
		this.alertBean = alertBean;
	}

	public String getIntervalStartDate() {
		return intervalStartDate;
	}

	public void setIntervalStartDate(String intervalStartDate) {
		this.intervalStartDate = intervalStartDate;
	}

	public String getIntervalEndDate() {
		return intervalEndDate;
	}

	public void setIntervalEndDate(String intervalEndDate) {
		this.intervalEndDate = intervalEndDate;
	}

	public String getConfigType() {
		return configType;
	}

	public void setConfigType(String configType) {
		this.configType = configType;
	}

	public String getAlertStatusType() {
		return alertStatusType;
	}

	public void setAlertStatusType(String alertStatusType) {
		this.alertStatusType = alertStatusType;
	}

	public List<Integer> getAlertScheduleTime() {
		return alertScheduleTime;
	}

	public void setAlertScheduleTime(List<Integer> alertScheduleTime) {
		this.alertScheduleTime = alertScheduleTime;
	}

	@Override
	public String toString() {
		return "CertificateJobBean [intervalStartDate=" + intervalStartDate + ", intervalEndDate=" + intervalEndDate
				+ ", configType=" + configType + ", alertStatusType=" + alertStatusType + ", alertScheduleTime="
				+ alertScheduleTime + "]";
	}

}
