package com.modernmotion.safetext;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class STStatus extends Activity implements SensorEventListener {

	private boolean serviceEnabled;
	private ImageView serviceSwitch;
	private TextView activationIndicator, smsCount, senderValue, messageValue;

	private SensorManager sensorManager;
	private Sensor accelerometer;

	// private String SENT = "SMS_SENT";
	// private String RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

	private static final String SERVICE_STATE = "serviceState";
	private static final String RECEIVER_STATE = "receiverState";

	private boolean receiverRegistered = false;
	private IntentFilter intentFilter = new IntentFilter("SMS_MESSAGE_RECEIVED");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.st_status);

		serviceSwitch = (ImageView) findViewById(R.id.st_service_switch);
		smsCount = (TextView) (findViewById(R.id.st_sms_count));
		activationIndicator = (TextView) findViewById(R.id.st_service_status_indicator);
		senderValue = (TextView) findViewById(R.id.st_sms_sender_value);
		messageValue = (TextView) findViewById(R.id.st_sms_message_value);

		senderValue.setText("");
		messageValue.setText("");
		messageValue.setMovementMethod(new ScrollingMovementMethod());
		
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Log.i(STConstants.DEBUG_SENSOR_MANAGER, "acquired sensor manager");
		
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		Log.i(STConstants.DEBUG_ACCELEROMETER, "acquired linear accelerometer");

		serviceSwitch.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!isEnabled()) {
					setServiceState(true);
				} else {
					setServiceState(false);
				}
			}
		});
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		
	}

	private BroadcastReceiver intentReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals("SMS_MESSAGE_RECEIVED")) {
				int count = intent.getExtras().getInt("smsCount");
				String message = intent.getExtras().getString("message");
				String sender = intent.getExtras().getString("sender");

				smsCount.setText(String.valueOf(count));
				senderValue.setText(sender);
				messageValue.setText(message);
			}
		}
	};

	private boolean isEnabled() {
		return serviceEnabled;
	}

	private void setServiceState(boolean state) {
		if (state) {
			// Start service
			serviceSwitch.setImageResource(R.drawable.st_logo_orange);
			activationIndicator.setText(R.string.st_service_status_enabled);
			SMSCaptureService.startSMSCapture(this);
			serviceEnabled = state;
			registerReceiver(intentReceiver, intentFilter);
			receiverRegistered = state;
		} else {
			// End service
			serviceSwitch.setImageResource(R.drawable.st_logo_grey);
			activationIndicator.setText(R.string.st_service_status_disabled);
			SMSCaptureService.stopSMSCapture(this);
			serviceEnabled = state;
			unregisterReceiver(intentReceiver);
			receiverRegistered = state;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(SERVICE_STATE, serviceEnabled);
		outState.putBoolean(RECEIVER_STATE, receiverRegistered);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		serviceEnabled = savedInstanceState.getBoolean(SERVICE_STATE);
		receiverRegistered = savedInstanceState.getBoolean(RECEIVER_STATE);
		if (serviceEnabled) {
			serviceSwitch.setImageResource(R.drawable.st_logo_orange);
			activationIndicator.setText(R.string.st_service_status_enabled);
			if (!receiverRegistered) {
				registerReceiver(intentReceiver, intentFilter);
				receiverRegistered = true;
			}
		} else {
			serviceSwitch.setImageResource(R.drawable.st_logo_grey);
			activationIndicator.setText(R.string.st_service_status_disabled);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (serviceEnabled) {
			registerReceiver(intentReceiver, intentFilter);
			receiverRegistered = true;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (receiverRegistered) {
			unregisterReceiver(intentReceiver);
			receiverRegistered = false;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (receiverRegistered) {
			unregisterReceiver(intentReceiver);
		}
	}
}
