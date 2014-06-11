package org.sorz.mpiwifi;

import org.sorz.mpiwifi.exceptions.AlreadyConnectedException;
import org.sorz.mpiwifi.exceptions.LoginFailException;
import org.sorz.mpiwifi.exceptions.NetworkException;
import org.sorz.mpiwifi.exceptions.NoNetworkAccessException;
import org.sorz.mpiwifi.exceptions.UnknownNetworkException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {
	private SharedPreferences settings;

	private EditText netIdEdit;
	private EditText passwordEdit;
	private CheckBox autoLogin;

	private class LoginTask extends AsyncTask<String, Void, String> {
		private ProgressDialog dialog = null;
		private Context context = null;
		private Exception exception;

		public LoginTask setContext(Context context) {
			this.context = context;
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
		protected String doInBackground(String... params) {
			try {
				return WifiLoginer.login(params[0], params[1]);
			} catch (Exception e) {
				exception = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(String ipAddress) {
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

		settings = getSharedPreferences("settings", MODE_PRIVATE);

		netIdEdit = (EditText) findViewById(R.id.netId);
		passwordEdit = (EditText) findViewById(R.id.pwd);
		autoLogin = (CheckBox) findViewById(R.id.autoLogin);

		netIdEdit.setText(settings.getString("netId", ""));
		passwordEdit.setText(settings.getString("pwd", ""));
		autoLogin.setChecked(settings.getBoolean("autoLogin", false));
		
		TextWatcher loginInfoChangedWatcher = new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (s.length() == 0)
					autoLogin.setChecked(false);
			}
		};
		netIdEdit.addTextChangedListener(loginInfoChangedWatcher);
		passwordEdit.addTextChangedListener(loginInfoChangedWatcher);
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveSettings();
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
		case R.id.menu_clean:
			clean();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void clean() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.clean_title)
			.setMessage(R.string.clean_message)
			.setNeutralButton(R.string.clean_ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							netIdEdit.getText().clear();
							passwordEdit.getText().clear();
							autoLogin.setSelected(false);
							SharedPreferences.Editor editor = settings.edit();
							editor.clear();
							editor.apply();
						}
					}).show();
		
	}
	
	private void saveSettings() {
		SharedPreferences.Editor editor = settings.edit();;
		String netId = netIdEdit.getText().toString();
		String password = passwordEdit.getText().toString();
		
		editor.putString("netId", netId);
		editor.putString("pwd", password);
		editor.putBoolean("autoLogin", autoLogin.isChecked());
		editor.commit();
	}
	

	public void setAutoLogin(View v) {
		if (autoLogin.isChecked())
			if (passwordEdit.length() == 0) {
				passwordEdit.setError(getText(R.string.err_password_required));
				autoLogin.setChecked(false);
			}
			if (netIdEdit.length() == 0) {
				netIdEdit.setError(getText(R.string.err_netid_required));
				autoLogin.setChecked(false);
			}
	}

	public void login(View v) {
		String netId = netIdEdit.getText().toString();
		String pwd = passwordEdit.getText().toString();
		if (netId.isEmpty())
			netIdEdit.setError(getText(R.string.err_netid_required));
		if (pwd.isEmpty())
			passwordEdit.setError(getText(R.string.err_password_required));
		if (! (netId.isEmpty() || pwd.isEmpty()))
			(new LoginTask()).setContext(this).execute(netId, pwd);
	}
}
