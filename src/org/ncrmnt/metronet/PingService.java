package org.ncrmnt.metronet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class PingService extends Service implements Runnable {
	MainActivity act;
	WifiManager wifi;
	boolean active = true;
	Thread th;
	static final String TAG = "wifiPingService";
	static String pingError;

	private void doBroadcast(String event) {
		Intent in = new Intent();
		in.setAction(event);
		Log.d(TAG, "Broadcasting: " + event);
		sendBroadcast(in);
	}

	/* Is crappy, doesn't work at all */
	public int doSocket(int count, int delay, String host) {

		Socket socket = null;
		boolean reachable = false;
		int i, ok = 0;
		for (i = 0; i < count; i++) {
			if (!active)
				return ok;
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}
			try {
				socket = new Socket(host, 80);
				ok++;
				doBroadcast(scanResultsReceiver.action_ping_ok);
			} catch (IOException e) {
				doBroadcast(scanResultsReceiver.action_ping_fail);
			} finally {
				if (socket != null)
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}

		}

		return ok;

	}

	/* proper way. works on local ips, sucks on 8.8.8.8 */
	public int doInAvaliable(int count, int delay, String host) {
		InetAddress in;
		in = null;
		int ok = 0;
		int i;
		for (i = 0; i < count; i++) {
			if (!active)
				return ok;
			try {
				in = InetAddress.getByName(host);
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {
				if (in.isReachable(delay)) {
					Log.d(TAG, "Network looks fine");
					doBroadcast(scanResultsReceiver.action_ping_ok);
					ok++;
				} else {
					Log.d(TAG, "Timeout, it's dead");
					doBroadcast(scanResultsReceiver.action_ping_fail);
				}
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				}
			} catch (IOException e) {
				Log.d(TAG, "Caught an exception: " + e.toString());
			}
		}
		return ok;
	}

	/* Dirty way, working way */
	public int doPing(int count, int delay, String host) {
		int i, r;
		int ok = 0;
		for (i = 0; i < count; i++) {
			r = 3;
			if (!active)
				return ok;
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				r = pingHost(host, delay);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (r == 0) {
				doBroadcast(scanResultsReceiver.action_ping_ok);
				ok++;
			} else
				doBroadcast(scanResultsReceiver.action_ping_fail);
		}
		return ok;
	}

	/**
	 * Ping a host and return an int value of 0 or 1 or 2 0=success, 1=fail,
	 * 2=error
	 * 
	 * Does not work in Android emulator and also delay by '1' second if host
	 * not pingable In the Android emulator only ping to 127.0.0.1 works
	 * 
	 * @param String
	 *            host in dotted IP address format
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static int pingHost(String host, int timeout) throws IOException,
			InterruptedException {
		Runtime runtime = Runtime.getRuntime();
		timeout /= 1000;
		String cmd = "ping -c 1 -W " + timeout + " " + host;
		Process proc = runtime.exec(cmd);
		Log.d(TAG, cmd);
		proc.waitFor();
		int exit = proc.exitValue();
		return exit;
	}

	public static String ping(String host) throws IOException,
			InterruptedException {
		StringBuffer echo = new StringBuffer();
		Runtime runtime = Runtime.getRuntime();
		Log.v(TAG, "About to ping using runtime.exec");
		Process proc = runtime.exec("ping -c 1 " + host);
		proc.waitFor();
		int exit = proc.exitValue();
		if (exit == 0) {
			InputStreamReader reader = new InputStreamReader(
					proc.getInputStream());
			BufferedReader buffer = new BufferedReader(reader);
			String line = "";
			while ((line = buffer.readLine()) != null) {
				echo.append(line + "\n");
			}
			return getPingStats(echo.toString());
		} else if (exit == 1) {
			pingError = "failed, exit = 1";
			return null;
		} else {
			pingError = "error, exit = 2";
			return null;
		}
	}

	/**
	 * getPingStats interprets the text result of a Linux ping command
	 * 
	 * Set pingError on error and return null
	 * 
	 * http://en.wikipedia.org/wiki/Ping
	 * 
	 * PING 127.0.0.1 (127.0.0.1) 56(84) bytes of data. 64 bytes from 127.0.0.1:
	 * icmp_seq=1 ttl=64 time=0.251 ms 64 bytes from 127.0.0.1: icmp_seq=2
	 * ttl=64 time=0.294 ms 64 bytes from 127.0.0.1: icmp_seq=3 ttl=64
	 * time=0.295 ms 64 bytes from 127.0.0.1: icmp_seq=4 ttl=64 time=0.300 ms
	 * 
	 * --- 127.0.0.1 ping statistics --- 4 packets transmitted, 4 received, 0%
	 * packet loss, time 0ms rtt min/avg/max/mdev = 0.251/0.285/0.300/0.019 ms
	 * 
	 * PING 192.168.0.2 (192.168.0.2) 56(84) bytes of data.
	 * 
	 * --- 192.168.0.2 ping statistics --- 1 packets transmitted, 0 received,
	 * 100% packet loss, time 0ms
	 * 
	 * # ping 321321. ping: unknown host 321321.
	 * 
	 * 1. Check if output contains 0% packet loss : Branch to success -> Get
	 * stats 2. Check if output contains 100% packet loss : Branch to fail -> No
	 * stats 3. Check if output contains 25% packet loss : Branch to partial
	 * success -> Get stats 4. Check if output contains "unknown host"
	 * 
	 * @param s
	 */
	public static String getPingStats(String s) {
		if (s.contains("0% packet loss")) {
			int start = s.indexOf("/mdev = ");
			int end = s.indexOf(" ms\n", start);
			s = s.substring(start + 8, end);
			String stats[] = s.split("/");
			return stats[2];
		} else if (s.contains("100% packet loss")) {
			pingError = "100% packet loss";
			return null;
		} else if (s.contains("% packet loss")) {
			pingError = "partial packet loss";
			return null;
		} else if (s.contains("unknown host")) {
			pingError = "unknown host";
			return null;
		} else {
			pingError = "unknown error in getPingStats";
			return null;
		}
	}

	public void run() {
		Log.d(TAG, "Starting ");
		int ok = doPing(10, 3000, "8.8.8.8");
		if (ok > 0) {
			doBroadcast(scanResultsReceiver.action_ap_alive);
		} else {
			doBroadcast(scanResultsReceiver.action_ap_dead);
		}

		/* When we get started, we ping the host and send out broadcasts */
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		th = new Thread(this);
		th.start();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		active = false;
		try {
			th.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}

}
