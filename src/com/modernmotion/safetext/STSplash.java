package com.modernmotion.safetext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class STSplash extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.st_splash);

		new InitTask().execute();
	}

	private boolean init() {
        byte[] pinBytes = new byte[1024];
        try {
            String fileName = getResources().getString(R.string.safetext_pin_file);
            FileInputStream fin = openFileInput(fileName);
            fin.read(pinBytes);
            String pin = new String(pinBytes).trim();

            String pinValue = pin.substring(5, pin.length());
            if ("".equals(pinValue) || pinValue == null) {
                return true;
            }
        } catch (FileNotFoundException ex) {
            createDefaultPin();
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
	}

    private void createDefaultPin() {
        try {
            String fileName = getResources().getString(R.string.safetext_pin_file);
            FileOutputStream fout = openFileOutput(fileName, Context.MODE_PRIVATE);

            StringBuilder file = new StringBuilder();
            file.append("-pin:");
            file.append("0000");
            fout.write(file.toString().getBytes());
            fout.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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
