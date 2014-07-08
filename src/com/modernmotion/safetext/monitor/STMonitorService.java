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

public class STMonitorService extends Service implements LocationListener {
	private SMSMonitor monitor = null;
	private Looper serviceLooper;

	private LocationManager locationManager;
	private String locationMode = LocationManager.GPS_PROVIDER;

    private final MonitorBinder binder = new MonitorBinder();

	@Override
	public void onCreate() {
		super.onCreate();
		
		HandlerThread stServiceThread = new HandlerThread(
				"STServiceThread", Process.THREAD_PRIORITY_BACKGROUND);
		stServiceThread.start();
		serviceLooper = stServiceThread.getLooper();
		
		// GPS
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(locationMode, 0, 0, this);

		// Monitor
		monitor = new DefaultSMSMonitor();
	}

    public class MonitorBinder extends Binder {
        public STMonitorService getService() {
            return STMonitorService.this;
        }
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(DEBUG_STRING, "Service Started");
		return super.onStartCommand(intent, START_REDELIVER_INTENT, startId);
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(DEBUG_STRING, "Service Destroyed");
    }

    @Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
	public void onLocationChanged(Location location) {
		if (location != null && location.hasSpeed()) {
			monitor.sense(location);
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	@Override
	public void onProviderEnabled(String provider) {

	}

	@Override
	public void onProviderDisabled(String provider) {

	}

    /* STMonitorService Interface methods */

    public MonitorState currentState() {
        return monitor.currentState();
    }

    public SMSMonitor getMonitor() {
        return monitor;
    }
}
