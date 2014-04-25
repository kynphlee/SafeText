package com.modernmotion.safetext;

import static com.modernmotion.safetext.util.STConstants.DEBUG_SENSOR_MANAGER;
import static com.modernmotion.safetext.util.STConstants.MS2S;
import static com.modernmotion.safetext.util.STConstants.ST_THRESHOLD;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class STStatus extends Activity implements SensorEventListener {
	
	private final static String TAG = "DEBUG (Location): ";

	private boolean serviceEnabled;
	private ImageView serviceSwitch;
	private TextView activationIndicator;
	private TextView speedLabel;
	private TextView speedValue;
	private TextView lonValue;
	private TextView latValue;

	private SensorManager sensorManager;
	private Sensor linearSensor;
	private SMSMonitor smsMonitor;

	private LocationManager locationManager;
	private static String NETWORK = LocationManager.NETWORK_PROVIDER;
	private static String GPS = LocationManager.GPS_PROVIDER;
	private String locationMode = GPS;

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
		activationIndicator = (TextView) findViewById(R.id.st_service_status_indicator);
		speedValue = (TextView)findViewById(R.id.st_speed);
		//lonValue = (TextView)findViewById(R.id.st_lon_value);
		//latValue = (TextView)findViewById(R.id.st_lat_value);
		
		//lonValue.setText("0");
		//latValue.setText("0");
		

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		linearSensor = sensorManager
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		sensorManager.registerListener(this, linearSensor,
				SensorManager.SENSOR_DELAY_NORMAL);
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		smsMonitor = new SMSMonitor();
	}

	private LocationListener locationListener = new LocationListener() {

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onLocationChanged(Location location) {
			//Log.i(TAG, "Current location: " + location);
			Log.i(TAG, "Current speed: " + location.getSpeed());
			speedValue.setText(String.valueOf(location.getSpeed()));
		}
	};

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	private class SMSMonitor {

		private double acceleration;
		private State passiveState;
		private State activeState;
		private State monitorState;
		protected double startTime;
		private static final long DURATION = 180;

		public SMSMonitor() {
			activeState = new ActiveState(this);
			passiveState = new PassiveState(this);
			monitorState = passiveState;
		}

		public void setState(State newState) {
			monitorState = newState;
		}

		public void sense(SensorEvent event) {
			acceleration = sqrt(pow(event.values[0], 2)
					+ pow(event.values[1], 2) + pow(event.values[2], 2));
			monitorState.run(event);
		}

		private double longToDecimal(long longVal) {
			return Long.valueOf(longVal).doubleValue();
		}

		abstract class State {

			double seconds = 0;

			protected SMSMonitor monitor;

			public State(SMSMonitor monitor) {
				this.monitor = monitor;
			}

			protected State getPassiveState() {
				return passiveState;
			}

			protected State getActiveState() {
				return activeState;
			}

			protected abstract void run(final SensorEvent event);
		}

		private class PassiveState extends State {

			public PassiveState(SMSMonitor monitor) {
				super(monitor);
			}

			@Override
			protected void run(final SensorEvent event) {
				if (acceleration >= ST_THRESHOLD) {
					setSMSCaptureState(true);
					startTime = longToDecimal(System.currentTimeMillis());
					monitor.setState(getActiveState());
				}
			}
		}

		private class ActiveState extends State {

			public ActiveState(SMSMonitor monitor) {
				super(monitor);
			}

			@Override
			protected void run(final SensorEvent event) {
				double currentTimeDouble = longToDecimal(System
						.currentTimeMillis());
				seconds = (currentTimeDouble - startTime) * MS2S;
				if (seconds >= DURATION) {
					setSMSCaptureState(false);
					monitor.setState(getPassiveState());
				}
			}
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		//smsMonitor.sense(event);
	}

	/*
	 * // ------------ LEGACY START ------------------------ // long timeStamp =
	 * event.timestamp; // double accelSum = 0; // // while ((event.timestamp -
	 * timeStamp) < 180) { // double accelDiff = acceleration - accelSum; //
	 * accelSum += accelDiff; // } // // // The Manual Switch Logic // if
	 * (!isEnabled()) { // setServiceState(true); // } else { // if
	 * (isEnabled()) { // setServiceState(false); // } // }
	 * 
	 * // Update global variables // accel_prev = acceleration; // ------------
	 * LEGACY END ------------------------
	 */

	private BroadcastReceiver smsIntentReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals("SMS_MESSAGE_RECEIVED")) {
				String message = intent.getExtras().getString("message");
				String sender = intent.getExtras().getString("sender");

				Log.i("SMSTAG", "sms status captured!");

				ContentValues values = new ContentValues();
				values.put("address", sender);
				values.put("body", message);
				getContentResolver().insert(Uri.parse("content://sms/sent"),
						values);
				Log.i("SMSTAG", "sms written to content provider!");
			}
		}
	};

	private void setSMSCaptureState(boolean state) {
		if (state) {
			// Start service
			serviceSwitch.setImageResource(R.drawable.st_logo_orange);
			activationIndicator.setText(R.string.st_service_status_enabled);

			SMSCaptureService.startSMSCapture(this);
			serviceEnabled = state;
			registerReceiver(smsIntentReceiver, smsIntentFilter);
			receiverRegistered = state;
		} else {
			// Stop service
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
		locationManager.requestLocationUpdates(locationMode, 0, 0, locationListener);
		Log.d(DEBUG_SENSOR_MANAGER, "state: onResume()");
		Log.d(DEBUG_SENSOR_MANAGER, "  *** activity started ***  ");
	}

	@Override
	protected void onPause() {
		super.onPause();
		locationManager.removeUpdates(locationListener);
		Log.d(DEBUG_SENSOR_MANAGER, "  *** activity pause ***  ");
		Log.d(DEBUG_SENSOR_MANAGER, "state: onPause()");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (receiverRegistered) {
			unregisterReceiver(smsIntentReceiver);
		}
		sensorManager.unregisterListener(this);
		locationManager.removeUpdates(locationListener);
		Log.d(DEBUG_SENSOR_MANAGER, "  *** activity ended ***  ");
		Log.d(DEBUG_SENSOR_MANAGER, "state: onDestroy()");
	}
}
