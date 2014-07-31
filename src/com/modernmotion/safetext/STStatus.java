package com.modernmotion.safetext;

import android.app.Activity;
import android.content.*;
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
import com.modernmotion.safetext.monitor.SMSMonitor.MonitorListener;
import com.modernmotion.safetext.monitor.STMonitorService;
import com.modernmotion.safetext.monitor.STMonitorService.MonitorBinder;

import static com.modernmotion.safetext.util.STConstants.*;

public class STStatus extends Activity implements PassiveModeOverrideInterface,
		MonitorListener {

	private final static String TAG = STStatus.class.getSimpleName();

	private boolean serviceEnabled;
    private boolean receiverRegistered;
    private boolean heartbeatReceiverRegistered;

	private ImageView serviceSwitch;
	private TextView activationIndicator;
	private TextView manualOverrideIndicator;
	private PassiveModeOverrideDialog passiveOverrideDialog;

	private TextView speedValue;
	private TextView lonValue;
	private TextView latValue;

    private STMonitorService monitorService;
    private boolean isBound = false;

	private static final String SERVICE_STATE = "serviceState";
	private static final String RECEIVER_STATE = "receiverState";

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

        Intent initializer = new Intent(this, STMonitorService.class);
        startService(initializer);
        Log.d(TAG, "Initializing STMonitorService.");
	}

    @Override
    protected void onStart() {
        super.onStart();
        Intent bindIntent = new Intent(this, STMonitorService.class);
        bindService(bindIntent, monitorConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Sending bind intent to STMonitorService...");
    }

    @Override
	protected void onResume() {
		super.onResume();
        Log.d(TAG, "state: onResume()");
        Log.d(TAG, "  *** activity started ***  ");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "  *** activity pause ***  ");
		Log.d(TAG, "state: onPause()");
	}

    @Override
    protected void onStop() {
        super.onStop();

        if(isBound) {
            unbindService(monitorConnection);
            isBound = false;
        }

        if (receiverRegistered) {
            unregisterReceiver(smsIntentReceiver);
            receiverRegistered = false;
        }

        if(heartbeatReceiverRegistered) {
            unregisterReceiver(heartbeatReceiver);
            heartbeatReceiverRegistered = false;
        }
        Log.d(TAG, "  *** activity stopped ***  ");
        Log.d(TAG, "state: onStop()");
    }

    @Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "  *** activity ended ***  ");
		Log.d(TAG, "state: onDestroy()");
	}

    private ServiceConnection monitorConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MonitorBinder binder = (MonitorBinder) service;
            monitorService = binder.getService();
            monitorService.getMonitor().setMonitorActivity(STStatus.this);

            if (debug) {
                IntentFilter heartbeatFilter = new IntentFilter();
                heartbeatFilter.addAction(ST_GPS_HEARTBEAT);
                registerReceiver(heartbeatReceiver, heartbeatFilter);
                heartbeatReceiverRegistered = true;
            }
            isBound = true;
            if (monitorService.getMonitor().isOverridden(MonitorState.ACTIVE)) {
                onCaptureStart();
            }
            Log.d(STStatus.TAG, "Bound to STMonitorService.");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            monitorService.getMonitor().setMonitorActivity(null);
            isBound = false;
        }
    };

	/************************ Passive Mode Override Start *********************************/
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
                serviceSwitch.setImageResource(R.drawable.st_override);
                manualOverrideIndicator = (TextView) findViewById(R.id.manual_override);
                manualOverrideIndicator.setVisibility(android.view.View.VISIBLE);
            } else {
                serviceSwitch.setImageResource(R.drawable.st_passive);
                manualOverrideIndicator = (TextView) findViewById(R.id.manual_override);
                manualOverrideIndicator.setVisibility(android.view.View.INVISIBLE);
            }
        }
	}

	/******************* Passive Mode Override End ****************************************/

    private BroadcastReceiver smsIntentReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ST_SMS_MESSAGE_RECEIVED)) {
                Log.i("SMSTAG", "sms status captured!");
                Toast.makeText(context,
                        "Caught SMS! ", Toast.LENGTH_SHORT).show();
            } else if (action.equals(ST_PASSIVE_STATE)) {
                int messagesDumped = intent.getExtras().getInt("messagesDumped");
                Toast.makeText(context,
                        "Messages written to content provider: "
                                + messagesDumped, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private BroadcastReceiver heartbeatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isBound) {
                String action = intent.getAction();
                if (ST_GPS_HEARTBEAT.equals(action)) {
                    Bundle heartbeatBundle = intent.getExtras();
                    speedValue.setText(String.valueOf(heartbeatBundle.getFloat("speed")));
                    latValue.setText(String.valueOf(heartbeatBundle.getDouble("latitude")));
                    lonValue.setText(String.valueOf(heartbeatBundle.getDouble("longitude")));
                }
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
        serviceEnabled = true;
		
		// Register intent receiver
        IntentFilter smsIntentFilter = new IntentFilter(ST_SMS_MESSAGE_RECEIVED);
		registerReceiver(smsIntentReceiver, smsIntentFilter);
		receiverRegistered = true;
	}

	@Override
	public void onCaptureStop() {
		// Passive Mode
		serviceSwitch.setImageResource(R.drawable.st_passive);
		activationIndicator.setText(R.string.st_service_status_disabled);
        serviceEnabled = false;
		
		// Unregister intent receiver
        if (receiverRegistered) {
            unregisterReceiver(smsIntentReceiver);
            receiverRegistered = false;
        }
	}
}
