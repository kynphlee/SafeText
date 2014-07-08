package com.modernmotion.safetext.monitor;

import android.app.Activity;
import android.location.Location;
import com.modernmotion.safetext.MonitorState;

public interface SMSMonitor {

	public interface MonitorListener {
		public void onCaptureStart();
		public void onCaptureStop();
        public void onStateChanged();
	}

	public void setMonitorActivity(Activity context);
	
	public void transitionTo(MonitorState newState);

	public MonitorState currentState();

	public void setOverride(MonitorState stateToOverride, boolean override);
	
	public boolean isOverridden(MonitorState state);

	public void sense(Location location);
}