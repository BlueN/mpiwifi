package org.sorz.mpiwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class NetworkBroadcast extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!intent.getAction()
				.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION))
			return;
		if (!intent.getExtras().containsKey(WifiManager.EXTRA_WIFI_INFO))
			return; // The new state is not CONNECTED.
		WifiInfo wifiInfo = (WifiInfo) intent.getExtras().get(
				WifiManager.EXTRA_WIFI_INFO);

		String ssid = wifiInfo.getSSID();
		if (ssid == null)
			return;
		else if (!(ssid.equals("NamOn_Hostel") ||
				ssid.equals("MengTakHostelWiFi")))
			return;

		SharedPreferences settings = context.getSharedPreferences("settings",
				Context.MODE_PRIVATE);
		if (!settings.getBoolean("autoLogin", false))
			return;

		SharedPreferences loginInfo = context.getSharedPreferences("loginInfo",
				Context.MODE_PRIVATE);
		String netId = loginInfo.getString("netId", "");
		String pwd = loginInfo.getString("pwd", "");

		if (netId.isEmpty() || pwd.isEmpty()) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("autoLogin", false);
			editor.commit();
			return;
		}

		Intent login = new Intent(context, WifiLoginService.class);
		login.putExtra("username", netId);
		login.putExtra("password", pwd);
		context.startService(login);
	}

}
