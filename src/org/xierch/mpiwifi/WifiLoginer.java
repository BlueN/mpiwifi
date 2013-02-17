package org.xierch.mpiwifi;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.widget.Toast;

public class WifiLoginer {
	public static void loginNamon(Context context, String netId, String pwd, Boolean lessToast) {
    	/* If lessToast is true, only show those message: 
    	 * 		login success, password incorrect and unknown error.
    	 * Use for auto login.
    	 */
		HttpURLConnection urlConn = null;
    	String resp;
    	
    	// Get the URL of login page: 
    	try {
			URL url = new URL("http://www.google.com/noexist");
			urlConn = (HttpURLConnection) url.openConnection();
			urlConn.setConnectTimeout(3000);
			InputStream in = new BufferedInputStream(urlConn.getInputStream());
			byte[] data = new byte[1024];
			int length = in.read(data);
			resp = new String(data, 0, length);
			
		} catch (FileNotFoundException e) {
			if (!lessToast)
				Toast.makeText(context, R.string.err_already, Toast.LENGTH_SHORT).show();
			return;
		} catch (ConnectException e) {
			if (!lessToast)
				Toast.makeText(context, R.string.err_noNet, Toast.LENGTH_SHORT).show();
			return;
		} catch (UnknownHostException e) {
			if (!lessToast)
				Toast.makeText(context, R.string.err_noNet, Toast.LENGTH_SHORT).show();
			return;
		} catch (Exception e) {
			Toast.makeText(context, R.string.err_unknow, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
			return;
		} finally {
			if (urlConn != null) urlConn.disconnect();
		}
    	
    	// Get the login page:
    	String postUrl;
    	String loginUrl;
    	try {
	    	Pattern p = Pattern.compile("'(http://.*)'");
	    	Matcher m = p.matcher(resp);
	    	if (!m.find()) throw new MalformedURLException();
	    	loginUrl = m.group(1) + "&flag=location";
	    	m = Pattern.compile(".*/").matcher(loginUrl);
	    	m.find();
	    	postUrl = m.group() + "user.do?method=login_ajax";
	    	
	    	URL url = new URL(loginUrl);
	    	urlConn = (HttpURLConnection) url.openConnection();
			InputStream in = new BufferedInputStream(urlConn.getInputStream());
			byte[] data = new byte[10240];
			int length = in.read(data);
			resp = new String(data, 0, length);
			
    	} catch (MalformedURLException e) {
    		if (!lessToast)
    			Toast.makeText(context, R.string.err_otherNet, Toast.LENGTH_SHORT).show();
    		return;
    	} catch (Exception e) {
    		Toast.makeText(context, R.string.err_unknow, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
			return;
		} finally {
			if (urlConn != null) urlConn.disconnect();
    	}
    	
    	// Post login form:
    	try {
    		URL url = new URL(postUrl);
    		urlConn = (HttpURLConnection) url.openConnection();
    		urlConn.setDoOutput(true);
    		urlConn.setRequestMethod("POST");
    		urlConn.setInstanceFollowRedirects(true);
    		urlConn.connect();
    		DataOutputStream out = new DataOutputStream(urlConn.getOutputStream());
    		
    		String args = "usernmae=" + URLEncoder.encode(netId, "utf-8")
    				+ "&usernameHidden=" + URLEncoder.encode(netId, "utf-8")
    				+ "&pwd=" + URLEncoder.encode(pwd, "utf-8")
    				+ "&seczone&validcode=no_check";
    		Matcher m = Pattern.compile("id=\"(\\w+?)\" value='(\\w*?)'")
    				.matcher(resp);
    		while(m.find()) {
    			args = args + "&" + m.group(1) + "=" + m.group(2);
    		}
    		
    		out.writeBytes(args);
    		out.flush();
    		out.close();
			InputStream in = new BufferedInputStream(urlConn.getInputStream());
			byte[] data = new byte[10240];
			int length = in.read(data);
			resp = new String(data, 0, length);
    		
    	} catch (Exception e) {
    		Toast.makeText(context, R.string.err_unknow, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
			return;
		} finally {
    		if (urlConn != null) urlConn.disconnect();
    	}
    	
    	// Check result:
    	Matcher m = Pattern.compile("(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}" +
    			"(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)").matcher(resp);
    	if (!m.find())
    		Toast.makeText(context, R.string.err_loginFail, Toast.LENGTH_SHORT).show();
    	else {
    		String msg = context.getString(R.string.login_ok);
    		msg = String.format(msg, m.group());
    		Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    	}
	}
	
}
