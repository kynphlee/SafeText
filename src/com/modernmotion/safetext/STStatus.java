package com.modernmotion.safetext;

import static com.modernmotion.safetext.util.STConstants.*;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import java.util.Calendar;

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

public class STStatus extends Activity implements SensorEventListener {

	private boolean serviceEnabled;
	private ImageView serviceSwitch;
	private TextView activationIndicator, smsCount, senderValue, messageValue;

	private SensorManager sensorManager;
	private Sensor linearSensor;
	private static boolean sessionState = false;

	// TODO: Implement Acceleration-based switch variables
	private final String TAG = "STMotionSensor";
	private Calendar cal = Calendar.getInstance();
	private double acceleration;
	private SMSMonitor smsMonitor;
	private double previousTimestamp;
	private double durationTime;

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

		// Variables
		acceleration = 0.0;

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Log.i(DEBUG_SENSOR_MANAGER, "acquired sensor manager");

		linearSensor = sensorManager
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		Log.i(DEBUG_ACCELEROMETER, "sensor: " + linearSensor.getVendor()
				+ ", type: " + linearSensor.getName());

		sensorManager.registerListener(this, linearSensor,
				SensorManager.SENSOR_DELAY_NORMAL);
		Log.i(DEBUG_SENSOR_MANAGER, "registered sensor with normal delay");

		smsMonitor = new SMSMonitor();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	/*
	 * TODO:Accelerometer Sensor 1.) Create a 3 minute scan window (SMSSession).
	 * 1.a.) Compute the acceleration over time as the cumulative sum of
	 * differences. Example: accelCumulative += (acceleration -
	 * prevAcceleration)
	 * 
	 * 1.b.) After 3 minutes (180s), if the cumulative acceleration exceeds the
	 * threshold, continue SMSSession by instantiating a new SMSSession.
	 * 
	 * Update (7:20p @ 3/8/14): Design a state machine that operates with the
	 * following states: passive and active.
	 * 
	 * Update (1:57a @ 3/12/14): Define a state instance variable.
	 */

	private class SMSMonitor {

		private double acceleration;
		private float startTimeStamp;
		private SensorEvent sensorEvent;
		private State passiveState;
		private State activeState;
		private State monitorState;

		public SMSMonitor() {
			activeState = new ActiveState(this);
			passiveState = new PassiveState(this);
			monitorState = passiveState;
		}

		public void setState(State newState) {
			this.monitorState = newState;
		}

		public void sense(SensorEvent event) {
			sensorEvent = event;
			acceleration = sqrt(pow(sensorEvent.values[0], 2) + pow(sensorEvent.values[1], 2)
					+ pow(sensorEvent.values[2], 2));
			Log.i(TAG, "acceleration: " + acceleration);
			
			monitorState.run(sensorEvent);
		}

		public State getPassiveState() {
			return passiveState;
		}

		public State getActiveState() {
			return activeState;
		}

		abstract class State {
			protected SMSMonitor monitor;

			public State(SMSMonitor monitor) {
				this.monitor = monitor;
			}

			protected abstract void run(final SensorEvent sensorEvent);
		}

		private class PassiveState extends State {

			public PassiveState(SMSMonitor monitor) {
				super(monitor);
			}

			@Override
			protected void run(final SensorEvent sensorEvent) {
				/*
				 * Passive State: If the acceleration crosses the threshold,
				 * transition to the Active state.
				 */
				if (acceleration >= ST_THRESHOLD) {
					Log.d(TAG, "Threshold reached: " + acceleration);

					sessionState = true;
					startTimeStamp = sensorEvent.timestamp;
					setServiceState(sessionState);
					monitor.setState(getActiveState());
					Log.d(TAG, "Monitor started.");
				}
			}

		}

		private class ActiveState extends State {

			public ActiveState(SMSMonitor monitor) {
				super(monitor);
			}

			@Override
			protected void run(final SensorEvent sensorEvent) {
				/*
				 * Active State: Monitor the acceleration for a 3 minute
				 * duration
				 */
				double accelSum = acceleration;
				double accelDiff = 0.0;
				long currentTime = sensorEvent.timestamp;
				Log.d(TAG, "Current time (ms): " + currentTime);
				Log.d(TAG, "Difference: " + (currentTime - startTimeStamp));
				// monitor.setState(getPassiveState());
			}

		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float[] values = event.values.clone();
		acceleration = sqrt(pow(values[0], 2) + pow(values[1], 2)
				+ pow(values[2], 2));

		int mode = 0;
		if (mode == 0) {
			// The Goal.
			smsMonitor.sense(event);
		} else {
			Log.i(TAG, "acceleration: " + acceleration);
			if (acceleration >= ST_THRESHOLD) {
				Log.d(TAG, "Threshold reached: " + acceleration);
				if (!sessionState) {
					Log.d(TAG, "Monitor inactive. Starting...");

					previousTimestamp = event.timestamp;
					Log.d(TAG, "current threshold timestamp: "
							+ previousTimestamp);

					sessionState = true;
					setServiceState(sessionState);
					Log.d(TAG, "Monitor started.");
				}

				// TODO: Monitor the acceleration for a 3 minute duration
				double accelSum = acceleration;
				double accelDiff = 0.0;

				durationTime = (event.timestamp - previousTimestamp) * MS2S;
				Log.d(TAG, "Initial duration time: " + durationTime);

				if (durationTime < SCAN_WINDOW) {
					accelDiff = acceleration - accelDiff;
					accelSum += accelDiff;

					Log.d(TAG, "acceleration difference: " + accelDiff);
					Log.d(TAG, "cumulative acceleration: " + accelSum);

					if (durationTime >= SCAN_WINDOW) {
						if (accelSum >= ST_THRESHOLD) {
							durationTime = 0;
							previousTimestamp = event.timestamp;
						} else {
							sessionState = false;
							setServiceState(sessionState);
						}
					}
					durationTime = previousTimestamp;
					Log.d(TAG, "Current duration time: " + durationTime);
				}
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

			// SMSCaptureService.startSMSCapture(this);
			serviceEnabled = state;
			registerReceiver(smsIntentReceiver, smsIntentFilter);
			receiverRegistered = state;
		} else {
			// End service
			serviceSwitch.setImageResource(R.drawable.st_logo_grey);
			activationIndicator.setText(R.string.st_service_status_disabled);

			// SMSCaptureService.stopSMSCapture(this);
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
