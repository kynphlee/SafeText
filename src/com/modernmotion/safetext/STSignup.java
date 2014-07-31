package com.modernmotion.safetext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class STSignup extends Activity {

	ImageView btnActivate;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.st_signup);

        btnActivate = (ImageView) findViewById(R.id.activate_button);
        btnActivate.setOnClickListener(activateListener);

	}

    private OnClickListener activateListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            EditText pin = (EditText) findViewById(R.id.pin_field);
            String pinValue = pin.getText().toString();

            if (pinValue == null
                    || "".equals(pinValue)
                    || "0000".equals(pinValue)) {
                Toast badPin = Toast.makeText(getApplicationContext(), "Please enter a valid pin.", Toast.LENGTH_SHORT);
                badPin.show();
            } else {
                try {
                    String fileName = getString(R.string.filename);
                    FileOutputStream fout = openFileOutput(fileName, Context.MODE_PRIVATE);

                    StringBuilder file = new StringBuilder();
                    file.append("-pin:");
                    file.append(pinValue);
                    fout.write(file.toString().getBytes());
                    fout.close();

                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                Intent statusIntent = new Intent(STSignup.this, STStatus.class);
                startActivity(statusIntent);
                finish();
            }
        }
    };
}
