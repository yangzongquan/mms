package com.android.mms.privacy;

import java.util.ArrayList;

public class PrivacyStatus {

	private static boolean sHideMsg = true;

	public interface HideMsgListener {
		public abstract void onStatusChanged(boolean hideMsg);
	}

	private static final ArrayList<HideMsgListener> sListeners = new ArrayList<HideMsgListener>();

	public synchronized static boolean isHideMsg() {
		return sHideMsg;
	}

	public synchronized static void setHideMsg(boolean hide) {
		sHideMsg = hide;
		notifyHideMsgListener(hide);
	}

	public synchronized static void addHideMsgListener(HideMsgListener l) {
		sListeners.add(l);
	}

	public synchronized static void removeHideMsgListener(HideMsgListener l) {
		sListeners.remove(l);
	}

	private static void notifyHideMsgListener(boolean hideMsg) {
		ArrayList<HideMsgListener> listeners;
		listeners = new ArrayList<PrivacyStatus.HideMsgListener>(sListeners);
		for (HideMsgListener l : listeners) {
			l.onStatusChanged(hideMsg);
		}
	}

}
