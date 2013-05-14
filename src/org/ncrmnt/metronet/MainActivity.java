package org.ncrmnt.metronet;

import java.util.List;

import org.ncrmnt.metronet.R;

import android.os.Bundle;
import android.os.Handler;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {

	WifiManager wifi;
	private scanResultsReceiver rcv;
	private TextView status;
	private ProgressBar p;
	
	public void appendLog(String t) {
		status.append(t);
	}
	public void appendLogN(String t) {
		status.append("\n" + t);
	}
	
	public void resetBar() {
		p.setProgress(0);
	}
	
	public void pushBar() {
		p.setProgress(p.getProgress()+1);
	}
	
	private void purgeMtsNetworks(final WifiManager wifiManager) {
		wifi.disconnect();
		final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
		int pri = 0;
		for(final WifiConfiguration config : configurations) {
			if ((config.SSID.contains("MTS")) || (config.SSID.contains("mts"))) {
				appendLog("\n Сносим сеть: " + config.SSID);
					wifi.removeNetwork(config.networkId);
			} else 
				appendLog("\n Оставляем сеть: " + config.SSID);
			}
		wifi.saveConfiguration();
		}
		
	
	private void shutdownAndExit() {
		getApplicationContext().unregisterReceiver(rcv);
		Intent i = new Intent();
		i.setAction("org.ncrmnt.metronet.PingService");
		stopService(i);
		wifi.setWifiEnabled(false);
		finish();
	}
	
    private void updateUi(Intent intent) {
        String text = intent.getStringExtra("text");
        appendLog(text);
       }
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        status = (EditText) findViewById(R.id.log);
        p = (ProgressBar) findViewById(R.id.pbar);
        p.setMax(10);
		rcv = new scanResultsReceiver(this);
		
        Context c = getApplicationContext();
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        IntentFilter iflt = new IntentFilter();
        iflt.addAction(wifi.SCAN_RESULTS_AVAILABLE_ACTION);
        iflt.addAction(scanResultsReceiver.action_ap_dead);
        iflt.addAction(scanResultsReceiver.action_ap_alive);
        iflt.addAction(scanResultsReceiver.action_ping_fail);
        iflt.addAction(scanResultsReceiver.action_ping_ok);
        c.registerReceiver(rcv, iflt);
        int state = wifi.getWifiState();
        if (state == wifi.WIFI_STATE_DISABLED)	
        {
        	appendLogN("Вайфай выключен, включаем");	
        	wifi.setWifiEnabled(true);
        }else
        {
        	appendLogN("Вайфай уже включен, это хорошо...");	
        }
        appendLogN("Начинаю сканирование");
        wifi.disconnect();
        wifi.startScan();
    }

    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.delmts:
        	purgeMtsNetworks(wifi);
            return true;
        case R.id.shdnandexit:
        	shutdownAndExit();
            return true;
        case R.id.menu_settings:
			startActivity(new Intent(this, Prefs.class));
			return true;           
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
