package org.xierch.mpiwifi;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {
	private SharedPreferences loginInfo;
	private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        loginInfo = getSharedPreferences("loginInfo", MODE_PRIVATE);
        settings = getSharedPreferences("settings", MODE_PRIVATE);
        
    	EditText netId = (EditText) findViewById(R.id.netId);
    	EditText pwd   = (EditText) findViewById(R.id.pwd);
    	CheckBox auto  = (CheckBox) findViewById(R.id.autoLogin);
    	
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
    	switch(item.getItemId()) {
    	case R.id.menu_exit:
    		finish();
    		break;
    	}
    	return super.onOptionsItemSelected(item); 
    }
    
    public void setAutoLogin(View v) {
    	CheckBox auto  = (CheckBox) v;
    	EditText netIdR = (EditText) findViewById(R.id.netId);
    	EditText pwdR   = (EditText) findViewById(R.id.pwd);
    	String   netId  = netIdR.getText().toString();
    	String   pwd    = pwdR.getText().toString();
    	
    	if (auto.isChecked())
        	if (netId.isEmpty() || pwd.isEmpty()) {
        		auto.setChecked(false);
        		Toast.makeText(this, R.string.err_noEnter, Toast.LENGTH_SHORT).show();
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
    	EditText pwdR   = (EditText) findViewById(R.id.pwd);
    	CheckBox autoR  = (CheckBox) findViewById(R.id.autoLogin);
    	String   netId  = netIdR.getText().toString();
    	String   pwd    = pwdR.getText().toString();
    	
    	// Save login information:
    	SharedPreferences.Editor editor = loginInfo.edit();
    	editor.putString("netId", netId);
    	editor.putString("pwd", pwd);
    	editor.commit();
    	
    	if (netId.isEmpty() || pwd.isEmpty()) {
    		autoR.setChecked(false);
    		Toast.makeText(this, R.string.err_noEnter, Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	Intent login = new Intent(this, WifiLoginer.class);
    	login.putExtra("target", "Namon");
    	login.putExtra("username", netId);
    	login.putExtra("password", pwd);
    	login.putExtra("lessToast", false);
    	startService(login);
    }
}
