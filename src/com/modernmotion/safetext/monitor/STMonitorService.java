package com.modernmotion.safetext.monitor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.*;
import android.os.Process;
import android.util.Log;
import com.modernmotion.safetext.MonitorState;

import static com.modernmotion.safetext.util.STConstants.DEBUG_STRING;
import static com.modernmotion.safetext.util.STConstants.ST_GPS_HEARTBEAT;

public class STMonitorService extends Service {
	private SMSMonitor monitor = null;
	private Looper serviceLooper;
    private MonitorServiceHandler serviceHandler;

	private LocationManager locationManager;
	private String locationMode = LocationManager.GPS_PROVIDER;

    private final MonitorBinder binder = new MonitorBinder();
    private final static String TAG = STMonitorService.class.getSimpleName();
    private boolean monitorServiceStarted = false;
    private boolean monitorActivityBound = false;

	@Override
	public void onCreate() {
		super.onCreate();
		
		HandlerThread stServiceThread = new HandlerThread(
				"STServiceThread", Process.THREAD_PRIORITY_BACKGROUND);
		stServiceThread.start();
		serviceLooper = stServiceThread.getLooper();
        serviceHandler = new MonitorServiceHandler(serviceLooper);
        Log.d(TAG, "STMonitorService has been created.");
	}

    public class MonitorBinder extends Binder {
        public STMonitorService getService() {
            return STMonitorService.this;
        }
    }

    private final class MonitorServiceHandler extends Handler implements LocationListener {
        public MonitorServiceHandler(Looper looper) {
            super(looper);

            // Monitor
            monitor = new DefaultSMSMonitor();
            monitor.setMonitorService(STMonitorService.this);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "MonitorServiceHandler called.");

            if (!monitorServiceStarted) {
                // GPS
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                locationManager.requestLocationUpdates(locationMode, 0, 0, this);
                monitorServiceStarted = true;
                Log.d(TAG, "STMonitorService has started");
            } else {
                Log.d(TAG, "STMonitorService was already running.");
            }
        }

        @Override
        public void onLocationChanged(Location location) {
            if (location != null && location.hasSpeed()) {
                Log.d(TAG, "Speed: " + location.getSpeed() +
                        " Location: " + location.getLatitude() + ", " + location.getLongitude());

                if (monitorActivityBound) {
                    Intent heartbeatIntent = new Intent();
                    heartbeatIntent.setAction(ST_GPS_HEARTBEAT);
                    heartbeatIntent.putExtra("longitude", location.getLongitude());
                    heartbeatIntent.putExtra("latitude", location.getLatitude());
                    heartbeatIntent.putExtra("speed", location.getSpeed());
                    sendBroadcast(heartbeatIntent);
                }
                monitor.sense(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        /****************************** GPS Override start *****************************************/
        @Override
        public void onProviderEnabled(String provider) {
            monitor.setOverride(MonitorState.ACTIVE, false);
        }

        @Override
        public void onProviderDisabled(String provider) {
            MonitorState state = monitor.currentState();
            if (state.equals(MonitorState.PASSIVE)
                    || state.equals(MonitorState.DELAY)) {
                monitor.setOverride(MonitorState.ACTIVE, true);
                monitor.transitionTo(MonitorState.ACTIVE);
            } else {
                monitor.setOverride(MonitorState.ACTIVE, true);
            }
        }
        /****************************** GPS Override end *****************************************/
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = serviceHandler.obtainMessage();
        serviceHandler.handleMessage(msg);

		return super.onStartCommand(intent, START_STICKY, startId);
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        monitorServiceStarted = false;
        Log.d(DEBUG_STRING, "Service Destroyed");
    }

    @Override
	public IBinder onBind(Intent intent) {
        monitorActivityBound = true;
		return binder;
	}

    @Override
    public boolean onUnbind(Intent intent) {
        monitorActivityBound = false;
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        monitorActivityBound = true;
    }

    /* STMonitorService Interface methods */

    public MonitorState currentState() {
        return monitor.currentState();
    }

    public SMSMonitor getMonitor() {
        return monitor;
    }
}
