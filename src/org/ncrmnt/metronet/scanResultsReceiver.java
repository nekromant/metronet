package org.ncrmnt.metronet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Stack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

public class scanResultsReceiver extends BroadcastReceiver {

	static final String TAG = "____wifi";
	WifiManager wifi;
	int activeNetId=-1;
	MainActivity mainActivity;
	Stack<ScanResult> queue = new Stack<ScanResult>(); /* Networks queued for checking */
	static boolean active = true;
	public static final String action_ap_dead = "org.ncrmnt.metronet.ap_dead";
	public static final String action_ap_alive = "org.ncrmnt.metronet.ap_alive";
	public static final String action_ping_ok = "org.ncrmnt.metronet.ping_ok";
	public static final String action_ping_fail = "org.ncrmnt.metronet.ping_fail";
	public static final String action_ping = "org.ncrmnt.metronet.PingService";
	

	boolean isMTSNetwork(String net) {
		if (net.contains("MTS") || net.contains("mts") || net.contains("Beeline"))
			return true;
		return false;
	}

	private static int getMaxPriority(final WifiManager wifiManager) {
		final List<WifiConfiguration> configurations = wifiManager
				.getConfiguredNetworks();
		int pri = 0;
		for (final WifiConfiguration config : configurations) {
			if (config.priority > pri) {
				pri = config.priority;
			}
		}
		return pri;
	}

	private static String convertToQuotedString(String string) {
		if (TextUtils.isEmpty(string)) {
			return "";
		}

		final int lastPos = string.length() - 1;
		if (lastPos < 0
				|| (string.charAt(0) == '"' && string.charAt(lastPos) == '"')) {
			return string;
		}

		return "\"" + string + "\"";
	}

	public void tryAccessPoint(Context cntx, ScanResult scanResult) {
		if (activeNetId >= 0)
				wifi.removeNetwork(activeNetId);
		Log.d(TAG, "setting network " + scanResult.SSID + " as the default one");
		mainActivity.appendLogN("Пробую законнектиться к " + scanResult.SSID);
		mainActivity.appendLogN("Адрес базовой станции: " + scanResult.BSSID);
		WifiConfiguration wc = new WifiConfiguration();
		wc.SSID = convertToQuotedString(scanResult.SSID);
		wc.BSSID = scanResult.BSSID;
		wc.priority = getMaxPriority(wifi)+1;
		wc.allowedKeyManagement.set(KeyMgmt.NONE);
		wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
		wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		int netId = wifi.addNetwork(wc);
		activeNetId = netId;
		wifi.disconnect();
		wifi.enableNetwork(netId, true);
		wifi.reconnect();
		Intent i = new Intent();
		i.setAction(action_ping);
		mainActivity.appendLogN("Пингуем интернеты: ");
		cntx.startService(i);
	}

	private void checkNext(Context cntx) {
		mainActivity.resetBar();
		/* Traverse through the list of queued networks */
		if (queue.isEmpty()) {
			active = true;
			return;
		}
		ScanResult r = queue.pop();
		
		Log.d(TAG, "Picking network " + r.SSID + " from the queue");
		if (isMTSNetwork(r.SSID)) {
			tryAccessPoint(cntx, r);
		} else {
			checkNext(cntx);	
		}
	}

	scanResultsReceiver(MainActivity act)
	{
		mainActivity = act;
		active = true;
	}
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "Got action: " + intent.getAction());
		if (intent.getAction().equals("android.net.wifi.SCAN_RESULTS")) {
			if (active) {
				wifi = (WifiManager) context
						.getSystemService(Context.WIFI_SERVICE);
				queue.addAll(wifi.getScanResults());
				active = false;
				checkNext(context);
			}
		} else if (intent.getAction().equals(action_ap_dead)) { 
			WifiInfo w = wifi.getConnectionInfo();
			mainActivity.appendLogN("Точка " + w.getSSID() + " мертва , пробуем другую. ");
			if (activeNetId>=0) wifi.disableNetwork(activeNetId);
				checkNext(context);
		} else if (intent.getAction().equals(action_ap_alive)) {
			mainActivity.appendLogN("Точка жива, интернеты есть =).");
		} else if (intent.getAction().equals(action_ping_fail)) {		
			mainActivity.appendLog(" - ");
			mainActivity.pushBar();
		} else if (intent.getAction().equals(action_ping_ok)) {		
			mainActivity.appendLog(" + ");
			mainActivity.pushBar();
		} 
	}
}
