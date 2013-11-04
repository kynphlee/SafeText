package com.modernmotion.safetext;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class STSignup extends Activity {

	EditText firstName, lastName, emailAddress;
	CheckBox tac;
	Button btnActivate;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.st_signup);
	}

	public void tacReview(View view) {
		firstName = (EditText) findViewById(R.id.first_name);
		lastName = (EditText) findViewById(R.id.last_name);
		emailAddress = (EditText) findViewById(R.id.email_address);
		tac = (CheckBox) findViewById(R.id.TaC);

		if (!(firstName.getEditableText().toString().isEmpty() || 
				lastName.getEditableText().toString().isEmpty() || 
				emailAddress.getEditableText().toString().isEmpty())
				&& tac.isChecked()) {

			btnActivate = (Button) findViewById(R.id.activate_button);
			btnActivate.setEnabled(true);
		} else {

			btnActivate = (Button) findViewById(R.id.activate_button);
			btnActivate.setEnabled(false);
		}
	}
}
