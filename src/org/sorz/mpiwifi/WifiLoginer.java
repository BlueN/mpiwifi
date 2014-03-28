package org.sorz.mpiwifi;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sorz.mpiwifi.exceptions.AlreadyConnectedException;
import org.sorz.mpiwifi.exceptions.LoginFailException;
import org.sorz.mpiwifi.exceptions.NetworkException;
import org.sorz.mpiwifi.exceptions.NoNetworkAccessException;
import org.sorz.mpiwifi.exceptions.UnknownNetworkException;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

public class WifiLoginer extends IntentService {
	Handler mHandler;

	public WifiLoginer() {
		super("WifiLoginer");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mHandler = new Handler();
	};

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
				Toast.makeText(WifiLoginer.this, mRID, Toast.LENGTH_SHORT)
						.show();
			else
				Toast.makeText(WifiLoginer.this, mText, Toast.LENGTH_SHORT)
						.show();
		}
	}

	public static String login(String netId, String pwd) throws NetworkException,
			UnknownNetworkException, LoginFailException,
			AlreadyConnectedException, NoNetworkAccessException {
		HttpURLConnection urlConn = null;
		String resp;

		// Get the URL of login page:
		try {
			DNSResolver.lookup("client3.google.com", 1000);
			// HttpURLConnection has not a custom timeout during NS lookup.
			URL url = new URL("http://client3.google.com/generate_204");
			urlConn = (HttpURLConnection) url.openConnection();
			urlConn.setConnectTimeout(2000);
			urlConn.setReadTimeout(2000);

			if (urlConn.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT)
				throw new AlreadyConnectedException();

			InputStream in = new BufferedInputStream(urlConn.getInputStream());
			byte[] data = new byte[1024];
			int length = in.read(data);
			resp = new String(data, 0, length);

		} catch (MalformedURLException e) {
			return ""; // Cannot occur
		} catch (IOException e) {
			throw new NoNetworkAccessException(e);
		} finally {
			if (urlConn != null)
				urlConn.disconnect();
		}

		// Get the login page:
		String postUrl;
		String loginUrl;
		try {
			Pattern p = Pattern.compile("'(http://.*)'");
			Matcher m = p.matcher(resp);
			if (!m.find())
				throw new MalformedURLException("URL not found.");
			loginUrl = m.group(1) + "&flag=location";
			m = Pattern.compile(".*/").matcher(loginUrl);
			m.find();
			postUrl = m.group() + "user.do?method=login_ajax";

			URL url = new URL(loginUrl);
			urlConn = (HttpURLConnection) url.openConnection();
			urlConn.setConnectTimeout(3000);
			InputStream in = new BufferedInputStream(urlConn.getInputStream());
			byte[] data = new byte[10240];
			int length = in.read(data);
			resp = new String(data, 0, length);

		} catch (MalformedURLException e) {
			throw new UnknownNetworkException(e);
		} catch (IOException e) {
			throw new NetworkException(e);
		} finally {
			if (urlConn != null)
				urlConn.disconnect();
		}

		// Post login form:
		try {
			URL url = new URL(postUrl);
			urlConn = (HttpURLConnection) url.openConnection();
			urlConn.setDoOutput(true);
			urlConn.setRequestMethod("POST");
			urlConn.setInstanceFollowRedirects(true);
			urlConn.connect();
			DataOutputStream out = new DataOutputStream(
					urlConn.getOutputStream());

			String args = "usernmae=" + URLEncoder.encode(netId, "utf-8")
					+ "&usernameHidden=" + URLEncoder.encode(netId, "utf-8")
					+ "&pwd=" + URLEncoder.encode(pwd, "utf-8")
					+ "&seczone&validcode=no_check";
			Matcher m = Pattern.compile("id=\"(\\w+?)\" value='(\\w*?)'")
					.matcher(resp);
			while (m.find()) {
				args = args + "&" + m.group(1) + "=" + m.group(2);
			}

			out.writeBytes(args);
			out.flush();
			out.close();
			InputStream in = new BufferedInputStream(urlConn.getInputStream());
			byte[] data = new byte[10240];
			int length = in.read(data);
			resp = new String(data, 0, length);

		} catch (IOException e) {
			throw new NetworkException();
		} finally {
			if (urlConn != null)
				urlConn.disconnect();
		}

		// Check result:
		Matcher m = Pattern.compile(
				"(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}"
						+ "(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)").matcher(resp);
		if (!m.find())
			throw new LoginFailException(
					"Can't found IP address on result page.");
		return m.group();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String username = intent.getStringExtra("username");
		String password = intent.getStringExtra("password");

		String ip = null;
		try {
			ip = login(username, password);
		} catch (AlreadyConnectedException e) {
			// Do nothing
		} catch (NoNetworkAccessException e) {
			// Ignore
		} catch (UnknownNetworkException e) {
			// Ignore
		} catch (NetworkException e) {
			// Ignore
		} catch (LoginFailException e) {
			mHandler.post(new DisplayToast(R.string.err_loginFail));
		}
		
		if (ip != null) {
			String msg = String.format(getString(R.string.login_ok), ip);
			mHandler.post(new DisplayToast(msg));
		}

	}

}
