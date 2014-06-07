package com.modernmotion.safetext;

import static com.modernmotion.safetext.util.STConstants.*;

import com.modernmotion.safetext.ManualOverrideDialog.ManualOverrideInterface;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class STStatus extends Activity implements ManualOverrideInterface{

	private final static String TAG = "DEBUG (Location): ";

	private boolean serviceEnabled;
	private ImageView serviceSwitch;
	private TextView activationIndicator;
	private TextView manualOverrideIndicator;
	private ManualOverrideDialog overrideDialog;

	private TextView speedValue;
	private TextView lonValue;
	private TextView latValue;

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

	// Debug switch
	private boolean debug = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (debug) {
			setContentView(R.layout.st_status_debug);
		} else {
			setContentView(R.layout.st_status);
		}

		serviceSwitch = (ImageView) findViewById(R.id.st_service_switch);
		serviceSwitch.setOnClickListener(manualOverrideListener);

		activationIndicator = (TextView) findViewById(R.id.st_service_status_indicator);

		if (debug) {
			speedValue = (TextView) findViewById(R.id.st_speed);
			lonValue = (TextView) findViewById(R.id.lon_value);
			latValue = (TextView) findViewById(R.id.lat_value);

			lonValue.setText("0");
			latValue.setText("0");
		}

		// GPS
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// Monitor
		smsMonitor = new SMSMonitor();
	}

	private OnClickListener manualOverrideListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (smsMonitor.currentState() == MonitorStateTransition.PASSIVE) {
				overrideDialog = new ManualOverrideDialog();
				overrideDialog.show(getFragmentManager(), "manualOverride");
				
			}
		}
	};
	
	@Override
	public void onOverrideStateChage(DialogFragment dialog) {
		smsMonitor.override();
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
			if (location != null && location.hasSpeed()) {
				float speed = location.getSpeed() * 2.23694f;

				Log.i(TAG,
						"Current speed: " + speed + ", time: "
								+ location.getTime());
				if (debug) {
					speedValue.setText(String.valueOf(speed));
					latValue.setText(String.valueOf(location.getLatitude()));
					lonValue.setText(String.valueOf(location.getLongitude()));
				}

				smsMonitor.sense(location);
			}
		}
	};

	private enum MonitorStateTransition {

		PASSIVE("Passive"), ACTIVE("Active"), DELAY("Delay");

		private final String state;

		private MonitorStateTransition(final String newState) {
			state = newState;
		}

		public String getMonitorState() {
			return state;
		}
	}

	private class SMSMonitor {

		private State passiveState;
		private State activeState;
		private State delayState;
		private State monitorState;
		private MonitorStateTransition mState;
		protected double startTime;

		public SMSMonitor() {
			activeState = new ActiveState(this);
			passiveState = new PassiveState(this);
			delayState = new DelayState(this);
			setState(MonitorStateTransition.PASSIVE);
		}

		public void setState(MonitorStateTransition newState) {
			switch (newState.ordinal()) {
			case 0: // Passive
				mState = newState;
				monitorState = passiveState;
				break;
			case 1: // Active
				mState = newState;
				monitorState = activeState;
				break;
			case 2: // Delay
				mState = newState;
				monitorState = delayState;
				break;
			}
		}

		public MonitorStateTransition currentState() {
			return mState;
		}

		public void override() {
			if (mState == MonitorStateTransition.PASSIVE) {
				PassiveState state = (PassiveState) monitorState;
				if (!state.isOverridden()) {
					manualOverrideIndicator = (TextView) findViewById(R.id.manual_override);
					manualOverrideIndicator
							.setVisibility(android.view.View.VISIBLE);
					state.setOverride(true);
				} else {
					manualOverrideIndicator = (TextView) findViewById(R.id.manual_override);
					manualOverrideIndicator
							.setVisibility(android.view.View.INVISIBLE);
					state.setOverride(false);
				}
			}
		}

		public void setState(State newState) {
			monitorState = newState;
		}

		public void sense(Location location) {
			monitorState.run(location);
		}

		private float speedToMPH(Location location) {
			return (location.getSpeed() * 2.23694f);
		}

		private float speedToMPH(float speed) {
			return (speed * 2.23694f);
		}

		private double longToDecimal(long longVal) {
			return Long.valueOf(longVal).doubleValue();
		}

		abstract class State {

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

			protected State getDelayState() {
				return delayState;
			}

			protected double getDuration() {
				double currentTimeDouble = longToDecimal(System
						.currentTimeMillis());
				return (currentTimeDouble - startTime) * MS2S;
			}

			protected abstract void run(final Location location);
		}

		private class PassiveState extends State {
			private float threshold = speedToMPH(THRESHOLD);
			private boolean override;

			public PassiveState(SMSMonitor monitor) {
				super(monitor);
				override = false;
			}

			private void setOverride(boolean newValue) {
				override = newValue;
			}

			private boolean isOverridden() {
				return override;
			}

			@Override
			protected void run(final Location location) {
				if (location == null || !location.hasSpeed()) {
					return;
				}

				float speed = speedToMPH(location);
				if (speed >= threshold) {
					// Manual Override
					if (override) {
						if (speed < threshold || speed == 0.0f) {
							override = false;
						}
					} else {
						setSMSCaptureState(true);
						startTime = longToDecimal(System.currentTimeMillis());
						// monitor.setState(getActiveState());
						monitor.setState(MonitorStateTransition.ACTIVE);
					}
				}
			}
		}

		private class ActiveState extends State {
			private int durationLimit = THREE_MINUTES;
			private double duration = 0;
			private float threshold = speedToMPH(THRESHOLD);

			public ActiveState(SMSMonitor monitor) {
				super(monitor);
			}

			@Override
			protected void run(Location location) {
				duration = getDuration();
				if (duration >= durationLimit) {
					float speed = speedToMPH(location);
					if (speed >= threshold) {
						// Continue blocking SMS messages
						startTime = longToDecimal(System.currentTimeMillis());
					} else if (speed == 0) {
						startTime = longToDecimal(System.currentTimeMillis());
						monitor.setState(getDelayState());
					}
				}
			}
		}

		private class DelayState extends State {
			private int durationLimit = ONE_MINUTE;
			private double duration = 0;
			private float threshold = speedToMPH(THRESHOLD);

			public DelayState(SMSMonitor monitor) {
				super(monitor);
			}

			@Override
			protected void run(Location location) {
				duration = getDuration();
				if (duration >= durationLimit) {
					float speed = speedToMPH(location);

					if (speed >= threshold) {
						startTime = longToDecimal(System.currentTimeMillis());
						monitor.setState(getActiveState());
					} else if (speed == 0) {
						setSMSCaptureState(false);
						monitor.setState(getPassiveState());
					}
				}
			}
		}
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
			if (action.equals(ST_SMS_MESSAGE_RECEIVED)) {
				String message = intent.getExtras().getString("message");
				String sender = intent.getExtras().getString("sender");

				Log.i("SMSTAG", "sms status captured!");

				// Write to the sms content provider
				// ContentValues values = new ContentValues();
				// values.put("address", sender);
				// values.put("body", message);
				// getContentResolver().insert(Uri.parse("content://sms/sent"),
				// values);
				// Log.i("SMSTAG", "sms written to content provider!");
			} else if (action.equals(ST_PASSIVE_STATE)) {
				int messagesDumped = intent.getExtras()
						.getInt("messagesDumped");
				Toast.makeText(
						context,
						"Messages written to content provider: "
								+ messagesDumped, Toast.LENGTH_SHORT).show();
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
		locationManager.requestLocationUpdates(locationMode, 0, 0,
				locationListener);
		smsMonitor.sense(locationManager.getLastKnownLocation(locationMode));
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
		locationManager.removeUpdates(locationListener);
		Log.d(DEBUG_SENSOR_MANAGER, "  *** activity ended ***  ");
		Log.d(DEBUG_SENSOR_MANAGER, "state: onDestroy()");
	}
}
