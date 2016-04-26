package com.john.ble;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class welcomeActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);
	}
   @Override
	protected void onResume() {
		super.onResume();
		
		Timer t=new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run(){
				final Intent toScan =new Intent(welcomeActivity.this,ScanActivity.class);
				startActivity(toScan);
				finish();
			}};
			t.schedule (task, 1500);
	}
}
