package com.modernmotion.safetext;

import android.app.Activity;
import android.content.*;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.modernmotion.safetext.monitor.PassiveModeOverrideDialog;
import com.modernmotion.safetext.monitor.PassiveModeOverrideDialog.PassiveModeOverrideInterface;
import com.modernmotion.safetext.monitor.SMSMonitor;
import com.modernmotion.safetext.monitor.SMSMonitor.MonitorListener;
import com.modernmotion.safetext.monitor.STMonitorService;
import com.modernmotion.safetext.monitor.STMonitorService.MonitorBinder;

import static com.modernmotion.safetext.util.STConstants.*;
import static com.modernmotion.safetext.util.STUtils.speedToMPH;

public class STStatus extends Activity implements PassiveModeOverrideInterface,
		LocationListener, MonitorListener {

	private final static String TAG = "DEBUG (Location): ";

	private boolean serviceEnabled;
	private ImageView serviceSwitch;
	private TextView activationIndicator;
	private TextView manualOverrideIndicator;
	private PassiveModeOverrideDialog passiveOverrideDialog;

	private TextView speedValue;
	private TextView lonValue;
	private TextView latValue;

	private SMSMonitor smsMonitor;
    private STMonitorService monitorService;
    private boolean isBound = false;

	private LocationManager locationManager;
	private String locationMode = LocationManager.GPS_PROVIDER;

	private static final String SERVICE_STATE = "serviceState";
	private static final String RECEIVER_STATE = "receiverState";

	private boolean receiverRegistered = false;
	private IntentFilter smsIntentFilter = new IntentFilter(
			"SMS_MESSAGE_RECEIVED");

	// Debug switch (true = diagnostic, false = production)
	private boolean debug = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (debug) {
			setContentView(R.layout.st_status_debug);
		} else {
			setContentView(R.layout.st_status);
		}

		serviceSwitch = (ImageView) findViewById(R.id.st_service_switch);
		serviceSwitch.setOnClickListener(passiveOverrideListener);

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
		//smsMonitor = new DefaultSMSMonitor();
		//smsMonitor.setMonitorActivity(this);
	}

    private ServiceConnection monitorConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MonitorBinder binder = (MonitorBinder) service;
            monitorService = binder.getService();
            monitorService.getMonitor().setMonitorActivity(STStatus.this);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            monitorService.getMonitor().setMonitorActivity(null);
            isBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent bindIntent = new Intent(this, STMonitorService.class);
        bindService(bindIntent, monitorConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
	protected void onResume() {
		super.onResume();
        if(isBound) {
            locationManager.requestLocationUpdates(locationMode, 0, 0, this);
            monitorService.getMonitor().sense(locationManager.getLastKnownLocation(locationMode));

            Log.d(DEBUG_SENSOR_MANAGER, "state: onResume()");
            Log.d(DEBUG_SENSOR_MANAGER, "  *** activity started ***  ");
        }
	}

	@Override
	protected void onPause() {
		super.onPause();
		locationManager.removeUpdates(this);
		Log.d(DEBUG_SENSOR_MANAGER, "  *** activity pause ***  ");
		Log.d(DEBUG_SENSOR_MANAGER, "state: onPause()");
	}

    @Override
    protected void onStop() {
        super.onStop();

        if(isBound) {
            unbindService(monitorConnection);
            isBound = false;
        }
    }

    @Override
	protected void onDestroy() {
		super.onDestroy();
		if (receiverRegistered) {
			unregisterReceiver(smsIntentReceiver);
		}
		locationManager.removeUpdates(this);
		Log.d(DEBUG_SENSOR_MANAGER, "  *** activity ended ***  ");
		Log.d(DEBUG_SENSOR_MANAGER, "state: onDestroy()");
	}

	/************************ Passive Mode Override Start *********************************/
    /*TODO: PassiveOverrideListener
     * Refactor onClick() to use STMonitorService's override method
    */
	private OnClickListener passiveOverrideListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
            if(isBound) {
                MonitorState currentState = monitorService.currentState();
                if(currentState == MonitorState.PASSIVE) {
                    Toast t = Toast.makeText(getApplicationContext(),
                            "Service State Check OK!",
                            Toast.LENGTH_SHORT);
                    t.show();

                    passiveOverrideDialog = new PassiveModeOverrideDialog();
                    passiveOverrideDialog.show(getFragmentManager(),
                            "manualOverride");
                }
            }
		}
	};

	@Override
	public void onAcquireMonitor(PassiveModeOverrideDialog dialog) {
		dialog.setMonitor(monitorService.getMonitor());

	}

	@Override
	public void onPassiveModeOverrideChange() {
        if(isBound) {
            boolean isOverridden = monitorService.getMonitor()
                    .isOverridden(MonitorState.PASSIVE);

            if (isOverridden) {
                manualOverrideIndicator = (TextView) findViewById(R.id.manual_override);
                manualOverrideIndicator.setVisibility(android.view.View.VISIBLE);
            } else {
                manualOverrideIndicator = (TextView) findViewById(R.id.manual_override);
                manualOverrideIndicator.setVisibility(android.view.View.INVISIBLE);
            }
        }
	}

	/******************* Passive Mode Override End ****************************************/

    /************************* Move to STMonitorService ********************************/
	/* GPS Monitoring Start */
	@Override
	public void onLocationChanged(Location location) {
		if (location != null && location.hasSpeed()) {
			int speed = speedToMPH(location);

			Log.i(TAG,
					"Current speed: " + speed + ", time: " + location.getTime());
			if (debug) {
				speedValue.setText(String.valueOf(speed));
				latValue.setText(String.valueOf(location.getLatitude()));
				lonValue.setText(String.valueOf(location.getLongitude()));
			}

			//smsMonitor.sense(location);
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	@Override
	public void onProviderEnabled(String provider) {
		// Active mode override off
		Toast gpsEnabled = Toast.makeText(this, "GPS Provider Enabled",
				Toast.LENGTH_SHORT);
		gpsEnabled.show();
	}

	@Override
	public void onProviderDisabled(String provider) {
		// Active mode override on
		Toast gpsDisabled = 
				Toast.makeText(this, "GPS Provider Disabled",Toast.LENGTH_SHORT);
		gpsDisabled.show();
	}
	/* GPS Monitoring End */
    /*********************** Move to STMonitorService **********************************/


    /**************************************** Keep This ********************************************************/
    private BroadcastReceiver smsIntentReceiver = new BroadcastReceiver() {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ST_SMS_MESSAGE_RECEIVED)) {
            Log.i("SMSTAG", "sms status captured!");
        } else if (action.equals(ST_PASSIVE_STATE)) {
            int messagesDumped = intent.getExtras().getInt("messagesDumped");
            Toast.makeText(context,
                    "Messages written to content provider: "
                            + messagesDumped, Toast.LENGTH_SHORT).show();
        }
    }
};

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
            serviceSwitch.setImageResource(R.drawable.st_active);
            activationIndicator.setText(R.string.st_service_status_enabled);
            if (!receiverRegistered) {
                registerReceiver(smsIntentReceiver, smsIntentFilter);
                receiverRegistered = true;
            }
        } else {
            serviceSwitch.setImageResource(R.drawable.st_passive);
            activationIndicator.setText(R.string.st_service_status_disabled);
        }
    }

	@Override
	public void onCaptureStart() {
		// Active Mode
		serviceSwitch.setImageResource(R.drawable.st_active);
		activationIndicator.setText(R.string.st_service_status_enabled);
		
		// Register intent receiver
		serviceEnabled = true;
		registerReceiver(smsIntentReceiver, smsIntentFilter);
		receiverRegistered = true;
	}

	@Override
	public void onCaptureStop() {
		// Passive Mode
		serviceSwitch.setImageResource(R.drawable.st_passive);
		activationIndicator.setText(R.string.st_service_status_disabled);
		
		// Unregister intent receiver
		serviceEnabled = false;
		unregisterReceiver(smsIntentReceiver);
		receiverRegistered = false;
	}

    @Override
    public void onStateChanged() {

    }
}
