package com.modernmotion.safetext;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import java.lang.Thread;

public class STSplash extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.st_splash);
		
		new InitTask().execute();
	}

	private boolean init() {
		return false;
	}

	private class InitTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			boolean doSignup = false;
			try {
				Thread.sleep(1000);
				doSignup = init();
			} catch (InterruptedException ex) {

			}
			return doSignup;
		}
		
		@Override
		protected void onPostExecute(Boolean doSignup) {
			super.onPostExecute(doSignup);
			
			Intent initializer;
			
			if (doSignup) {
				initializer = new Intent(STSplash.this, STSignup.class);
			} else {
				initializer = new Intent(STSplash.this, STStatus.class);
			}
			startActivity(initializer);
			finish();
		}
	}
}
