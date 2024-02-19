package com.wibmo.accosa2.utils;

import java.io.Serializable;
import java.util.List;

import com.wibmo.accosa2.bean.AlertBean;

public class AlertConfigInfo implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private List<Integer> remainingDaysList;
	private AlertBean alertBean;

	public AlertConfigInfo(List<Integer> remainingDaysList, AlertBean alertBean) {
		this.remainingDaysList = remainingDaysList;
		this.alertBean = alertBean;
	}

	public List<Integer> getRemainingDaysList() {
		return remainingDaysList;
	}

	public AlertBean getAlertBean() {
		return alertBean;
	}
}