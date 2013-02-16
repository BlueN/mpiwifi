package org.xierch.mpiwifi;

import android.os.Bundle;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.widget.*;

import java.io.*;
import java.net.*;
import java.util.regex.*;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
    	EditText netId = (EditText) findViewById(R.id.netId);
    	EditText pwd   = (EditText) findViewById(R.id.pwd);
    	SharedPreferences loginInfo = getSharedPreferences("loginInfo", MODE_PRIVATE);
    	
    	netId.setText(loginInfo.getString("netId", ""));
    	pwd.setText(loginInfo.getString("pwd", ""));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    
    public void login(View v) {
    	Button   btn    = (Button)   findViewById(R.id.login);
    	EditText netIdR = (EditText) findViewById(R.id.netId);
    	EditText pwdR   = (EditText) findViewById(R.id.pwd);
    	String   netId  = netIdR.getText().toString();
    	String   pwd    = pwdR.getText().toString();
    	
    	// Save login information:
    	SharedPreferences loginInfo = getSharedPreferences("loginInfo", MODE_PRIVATE);
    	SharedPreferences.Editor editor = loginInfo.edit();
    	editor.putString("netId", netId);
    	editor.putString("pwd", pwd);
    	editor.commit();
    	
    	if (netId.isEmpty() || pwd.isEmpty()) {
    		Toast.makeText(this, R.string.err_noEnter, Toast.LENGTH_SHORT).show();
    		return;
    	}
    	btn.setEnabled(false);
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
			Toast.makeText(this, R.string.err_already, Toast.LENGTH_SHORT).show();
			btn.setEnabled(true);
			return;
		} catch (ConnectException e) {
			Toast.makeText(this, R.string.err_noNet, Toast.LENGTH_SHORT).show();
			btn.setEnabled(true);
			return;
		} catch (UnknownHostException e) {
			Toast.makeText(this, R.string.err_noNet, Toast.LENGTH_SHORT).show();
			btn.setEnabled(true);
			return;
		} catch (Exception e) {
			Toast.makeText(this, R.string.err_unknow, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
			btn.setEnabled(true);
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
    		Toast.makeText(this, R.string.err_otherNet, Toast.LENGTH_SHORT).show();
    		btn.setEnabled(true);
    		return;
    	} catch (Exception e) {
    		Toast.makeText(this, R.string.err_unknow, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
			btn.setEnabled(true);
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
    		Toast.makeText(this, R.string.err_unknow, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
			btn.setEnabled(true);
			return;
		} finally {
    		if (urlConn != null) urlConn.disconnect();
    	}
    	
    	// Check result:
    	Matcher m = Pattern.compile("(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}" +
    			"(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)").matcher(resp);
    	if (!m.find())
    		Toast.makeText(this, R.string.err_loginFail, Toast.LENGTH_SHORT).show();
    	else {
    		String msg = getString(R.string.login_ok);
    		msg = String.format(msg, m.group());
    		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    	}
    	
    	//System.out.println(resp);
    	btn.setEnabled(true);
    }
}
