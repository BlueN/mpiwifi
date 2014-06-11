package org.sorz.mpiwifi;

import org.sorz.mpiwifi.exceptions.LoginFailException;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

public class WifiLoginService extends IntentService {
	Handler mHandler;
	
	public WifiLoginService() {
		this("WifiLoginService");
	}

	public WifiLoginService(String name) {
		super(name);
		mHandler = new Handler();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String username = intent.getStringExtra("username");
		String password = intent.getStringExtra("password");

		String ip = null;
		try {
			ip = WifiLoginer.login(username, password);
		} catch (LoginFailException e) {
			mHandler.post(new DisplayToast(R.string.err_loginFail));
		} catch (Exception e) {
			// Ignore
		}

		if (ip != null) {
			String msg = String.format(getString(R.string.login_ok), ip);
			mHandler.post(new DisplayToast(msg));
		}

	}

	private class DisplayToast implements Runnable {
		int mRID;
		String mText;

		public DisplayToast(int RID) {
			mRID = RID;
		}

		public DisplayToast(String text) {
			mText = text;
		}

		public void run() {
			if (mText == null)
				Toast.makeText(WifiLoginService.this, mRID, Toast.LENGTH_SHORT)
						.show();
			else
				Toast.makeText(WifiLoginService.this, mText, Toast.LENGTH_SHORT)
						.show();
		}
	}

}
