package com.modernmotion.safetext;

import java.util.Date;

import com.modernmotion.safetext.domain.SMSProperties;

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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import static java.lang.Math.sqrt;
import static java.lang.Math.pow;
import static com.modernmotion.safetext.STConstants.*;
import static com.modernmotion.safetext.util.STAttributes.ST_THRESHOLD;

public class STStatus extends Activity implements SensorEventListener {

	private boolean serviceEnabled;
	private ImageView serviceSwitch;
	private TextView activationIndicator, smsCount, senderValue, messageValue;

	private SensorManager sensorManager;
	private Sensor sensor;
	private boolean isSensorRegistered;

	// TODO: Implement Acceleration-based switch variables
	private double acceleration;
	private double accel_prev;
	private double threshold;
	private long timeStamp;
	private static float NS2S;

	private static final String SERVICE_STATE = "serviceState";
	private static final String RECEIVER_STATE = "receiverState";

	private boolean receiverRegistered = false;
	private IntentFilter smsIntentFilter = new IntentFilter("SMS_MESSAGE_RECEIVED");

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

		sensor = sensorManager
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		Log.i(DEBUG_ACCELEROMETER, "sensor: " + sensor.getVendor() + ", type: "
				+ sensor.getName());

		isSensorRegistered = sensorManager.registerListener(this, sensor,
				SensorManager.SENSOR_DELAY_NORMAL);
		Log.i(DEBUG_SENSOR_MANAGER, "registered sensor with normal delay");

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		/* TODO:Accelerometer Sensor
		 * 1) Create an AsyncTask class to implement the 3 minute scan 
		 * window (SMSSession). Potential Types are 
		 * AsyncTask<SensorEvent, Void, Boolean>,
		 * AsyncTask<STProperties, Void, Boolean>, 
		 * AsyncTask<Double, Void, Boolean>.
		 * 
		 * 	1a) Compute the acceleration over time as the cumulative sum 
		 * 		of differences. Example:
		 * 		accelCumulative += (acceleration - prevAcceleration)
		 * 
		 * 	1b)	After 3 minutes (180s), if the cumulative acceleration
		 * 		exceeds the threshold, continue SMSSession by 
		 * 		instantiating a new SMSSession.
		 */
		
		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			
			acceleration = sqrt(pow(event.values[0], 2) + pow(event.values[1], 2)
					+ pow(event.values[2], 2));
			
			if (acceleration >= ST_THRESHOLD) {
				SMSProperties properties = SMSProperties.builder()
						.setAcceleration(acceleration)
						.setThreshold(ST_THRESHOLD)
						.build();
				SMSSession scanSession = new SMSSession(properties);
				scanSession.execute(properties);
				
				// ------------ LEGACY START ------------------------
				long timeStamp = event.timestamp;
				double accelSum = 0;

				while ((event.timestamp - timeStamp) < 180) {
					double accelDiff = acceleration - accelSum;
					accelSum += accelDiff;
				}

				// The Manual Switch Logic
				if (!isEnabled()) {
					setServiceState(true);
				} else {
					if (isEnabled()) {
						setServiceState(false);
					}
				}

				// Update global variables
				accel_prev = acceleration;
				// ------------ LEGACY END ------------------------
			}
		}
	}
	
	private class SMSSession extends AsyncTask<SMSProperties, Void, Boolean> implements SensorEventListener{

		private boolean isReceiverRegistered = false;
		
		@Override
		protected Boolean doInBackground(SMSProperties... params) {
			SMSProperties properties = params[0];
			registerReceiver(properties.getReceiver(), properties.getReceiverFilter());
			isReceiverRegistered = true;
			
			while (isReceiverRegistered) {
				
			}
			return null;
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
				
			}
			
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stubs
			
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
		isSensorRegistered = sensorManager.registerListener(this, sensor,
				SensorManager.SENSOR_DELAY_NORMAL);
		Log.i(DEBUG_SENSOR_MANAGER, "sensor registered");
		Log.i(DEBUG_SENSOR_MANAGER, "  *** sensor start ***  ");
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (receiverRegistered) {
			unregisterReceiver(smsIntentReceiver);
			receiverRegistered = false;
		}
		sensorManager.unregisterListener(this);
		isSensorRegistered = false;
		Log.i(DEBUG_SENSOR_MANAGER, "  *** sensor stop ***  ");
		Log.i(DEBUG_SENSOR_MANAGER, "state: onPause(), sensor unregistered");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (receiverRegistered) {
			unregisterReceiver(smsIntentReceiver);
		}
		sensorManager.unregisterListener(this);
		isSensorRegistered = false;
		Log.i(DEBUG_SENSOR_MANAGER, "  *** sensor end ***  ");
		Log.i(DEBUG_SENSOR_MANAGER, "state: onDestroy(),sensor unregistered");
	}
}
