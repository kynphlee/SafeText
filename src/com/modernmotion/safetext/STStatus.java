package com.modernmotion.safetext;

import static com.modernmotion.safetext.STConstants.DEBUG_ACCELEROMETER;
import static com.modernmotion.safetext.STConstants.DEBUG_SENSOR_MANAGER;
import static com.modernmotion.safetext.STConstants.DEBUG_STRING;
import static com.modernmotion.safetext.util.STParameters.ST_THRESHOLD;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.modernmotion.safetext.domain.SMSProperties;
import com.modernmotion.safetext.util.MonitorParameters;

public class STStatus extends Activity implements SensorEventListener {

	private boolean serviceEnabled;
	private ImageView serviceSwitch;
	private TextView activationIndicator, smsCount, senderValue, messageValue;

	private SensorManager sensorManager;
	private Sensor linearSensor;
	private static boolean sessionStarted = false;

	// TODO: Implement Acceleration-based switch variables
	private double acceleration;
	private float[] values;
	private double accel_prev;
	private double threshold;
	private long timeStamp;
	private static float NS2S;

	private static final String SERVICE_STATE = "serviceState";
	private static final String RECEIVER_STATE = "receiverState";

	private boolean receiverRegistered = false;
	private IntentFilter smsIntentFilter = new IntentFilter(
			"SMS_MESSAGE_RECEIVED");

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

		// Constants
		NS2S = 1.0f / 1000000000.0f;
		threshold = 0.447;

		// Variables
		acceleration = 0.0;
		accel_prev = 0.0;

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Log.i(DEBUG_SENSOR_MANAGER, "acquired sensor manager");

		linearSensor = sensorManager
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		Log.i(DEBUG_ACCELEROMETER, "sensor: " + linearSensor.getVendor()
				+ ", type: " + linearSensor.getName());

		sensorManager.registerListener(this, linearSensor,
				SensorManager.SENSOR_DELAY_NORMAL);
		Log.i(DEBUG_SENSOR_MANAGER, "registered sensor with normal delay");
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		values = event.values.clone();
		
		acceleration = sqrt(pow(values[0], 2) + pow(values[1], 2)
				+ pow(values[2], 2));

		if (acceleration >= ST_THRESHOLD) {
			Log.d(DEBUG_STRING, "Threshold reached.");
			if (!sessionStarted) {
				sessionStarted = true;
//				SMSProperties properties = SMSProperties.builder()
//						.setAcceleration(acceleration).setSensor(linearSensor)
//						.setSensorManager(sensorManager)
//						.setContext(getApplicationContext()).build();
//				SMSMonitor smsMonitor = new SMSMonitor();
//				sessionStarted = (smsMonitor.execute(properties) != null) ? true
//						: false;
				Log.d(DEBUG_STRING, "Monitor started.");
			} else {
				
			}
		}
	}

	// ------------ LEGACY START ------------------------
	// long timeStamp = event.timestamp;
	// double accelSum = 0;
	//
	// while ((event.timestamp - timeStamp) < 180) {
	// double accelDiff = acceleration - accelSum;
	// accelSum += accelDiff;
	// }
	//
	// // The Manual Switch Logic
	// if (!isEnabled()) {
	// setServiceState(true);
	// } else {
	// if (isEnabled()) {
	// setServiceState(false);
	// }
	// }

	// Update global variables
	// accel_prev = acceleration;
	// ------------ LEGACY END ------------------------

	/*
	 * TODO:Accelerometer Sensor 1.) Create an AsyncTask class to implement the
	 * 3 minute scan window (SMSSession). Potential Types are
	 * AsyncTask<SensorEvent, Void, Boolean>, AsyncTask<STProperties,
	 * Void,Boolean>, and AsyncTask<Double, Void, Boolean>.
	 * 
	 * 1.a.) Create a class SMSMonitor that can sequentially run SMSSession
	 * threads in 3 minute intervals. For each SMSSession thread:
	 * 
	 * 1.a.1.) Compute the acceleration over time as the cumulative sum of
	 * differences. Example: accelCumulative += (acceleration -
	 * prevAcceleration) 1.a.2.)
	 * 
	 * 1.b.) After 3 minutes (180s), if the cumulative acceleration exceeds the
	 * threshold, continue SMSSession by instantiating a new SMSSession.
	 */

	private class SMSMonitor extends AsyncTask<SMSProperties, Void, Boolean> {

		@Override
		protected Boolean doInBackground(SMSProperties... params) {
			Log.d(DEBUG_STRING, "....now in SMSMonitor.doInBackground()...");
			SMSProperties properties = params[0];

			
			return true;
		}
	}

	private BroadcastReceiver smsIntentReceiver = new BroadcastReceiver() {

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
			registerReceiver(smsIntentReceiver, smsIntentFilter);
			receiverRegistered = state;
		} else {
			// End service
			serviceSwitch.setImageResource(R.drawable.st_logo_grey);
			activationIndicator.setText(R.string.st_service_status_disabled);
			
			SMSCaptureService.stopSMSCapture(this);
			serviceEnabled = state;
			unregisterReceiver(smsIntentReceiver);
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
				registerReceiver(smsIntentReceiver, smsIntentFilter);
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
			registerReceiver(smsIntentReceiver, smsIntentFilter);
			receiverRegistered = true;
		}
		sensorManager.registerListener(this, linearSensor,
				SensorManager.SENSOR_DELAY_NORMAL);
		Log.d(DEBUG_SENSOR_MANAGER, "sensor registered");
		Log.d(DEBUG_SENSOR_MANAGER, "  *** sensor start ***  ");
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (receiverRegistered) {
			unregisterReceiver(smsIntentReceiver);
			receiverRegistered = false;
		}
		sensorManager.unregisterListener(this);
		Log.d(DEBUG_SENSOR_MANAGER, "  *** sensor stop ***  ");
		Log.d(DEBUG_SENSOR_MANAGER, "state: onPause(), sensor unregistered");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (receiverRegistered) {
			unregisterReceiver(smsIntentReceiver);
		}
		sensorManager.unregisterListener(this);
		Log.d(DEBUG_SENSOR_MANAGER, "  *** sensor end ***  ");
		Log.d(DEBUG_SENSOR_MANAGER, "state: onDestroy(),sensor unregistered");
	}
}
