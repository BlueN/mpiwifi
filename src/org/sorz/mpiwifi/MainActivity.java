package org.sorz.mpiwifi;

import org.sorz.mpiwifi.exceptions.AlreadyConnectedException;
import org.sorz.mpiwifi.exceptions.LoginFailException;
import org.sorz.mpiwifi.exceptions.NetworkException;
import org.sorz.mpiwifi.exceptions.NoNetworkAccessException;
import org.sorz.mpiwifi.exceptions.UnknownNetworkException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {
	private SharedPreferences loginInfo;
	private SharedPreferences settings;

	private class LoginTask extends AsyncTask<Void, Void, Void> {
		private ProgressDialog dialog = null;
		private Context context = null;
		private String netId;
		private String password;
		private Exception exception;
		private String ipAddress;

		public LoginTask setContext(Context context) {
			this.context = context;
			return this;
		}

		public LoginTask setUserInfo(String netId, String password) {
			this.netId = netId;
			this.password = password;
			return this;
		}

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(context);
			dialog.setTitle(R.string.logining_title);
			dialog.setMessage(getString(R.string.logining_message));
			dialog.setIndeterminate(true);
			dialog.setCancelable(false);
			dialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				ipAddress = WifiLoginer.login(netId, password);
			} catch (Exception e) {
				exception = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();

			if (ipAddress != null) {
				String msg = String.format(getString(R.string.login_ok),
						ipAddress);
				Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
			} else if (exception instanceof AlreadyConnectedException)
				Toast.makeText(context, R.string.err_already,
						Toast.LENGTH_SHORT).show();
			else if (exception instanceof NoNetworkAccessException)
				Toast.makeText(context, R.string.err_noNet, Toast.LENGTH_SHORT)
						.show();
			else if (exception instanceof UnknownNetworkException)
				Toast.makeText(context, R.string.err_otherNet,
						Toast.LENGTH_SHORT).show();
			else if (exception instanceof LoginFailException)
				Toast.makeText(context, R.string.err_loginFail,
						Toast.LENGTH_SHORT).show();
			else if (exception instanceof NetworkException)
				Toast.makeText(context, R.string.err_unknow, Toast.LENGTH_SHORT)
						.show();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		loginInfo = getSharedPreferences("loginInfo", MODE_PRIVATE);
		settings = getSharedPreferences("settings", MODE_PRIVATE);

		EditText netId = (EditText) findViewById(R.id.netId);
		EditText pwd = (EditText) findViewById(R.id.pwd);
		CheckBox auto = (CheckBox) findViewById(R.id.autoLogin);

		netId.setText(loginInfo.getString("netId", ""));
		pwd.setText(loginInfo.getString("pwd", ""));
		auto.setChecked(settings.getBoolean("autoLogin", false));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_exit:
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void setAutoLogin(View v) {
		CheckBox auto = (CheckBox) v;
		EditText netIdR = (EditText) findViewById(R.id.netId);
		EditText pwdR = (EditText) findViewById(R.id.pwd);
		String netId = netIdR.getText().toString();
		String pwd = pwdR.getText().toString();

		if (auto.isChecked())
			if (netId.isEmpty() || pwd.isEmpty()) {
				auto.setChecked(false);
				Toast.makeText(this, R.string.err_noEnter, Toast.LENGTH_SHORT)
						.show();
			}

		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("autoLogin", auto.isChecked());
		editor.commit();

		editor = loginInfo.edit();
		editor.putString("netId", netId);
		editor.putString("pwd", pwd);
		editor.commit();
	}

	public void login(View v) {
		EditText netIdR = (EditText) findViewById(R.id.netId);
		EditText pwdR = (EditText) findViewById(R.id.pwd);
		CheckBox autoR = (CheckBox) findViewById(R.id.autoLogin);
		String netId = netIdR.getText().toString();
		String pwd = pwdR.getText().toString();

		// Save login information:
		SharedPreferences.Editor editor = loginInfo.edit();
		editor.putString("netId", netId);
		editor.putString("pwd", pwd);
		editor.commit();

		if (netId.isEmpty() || pwd.isEmpty()) {
			autoR.setChecked(false);
			Toast.makeText(this, R.string.err_noEnter, Toast.LENGTH_SHORT)
					.show();
			return;
		}
		(new LoginTask()).setContext(this).setUserInfo(netId, pwd).execute();
	}
}
