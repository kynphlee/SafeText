package com.modernmotion.safetext.util;

import android.app.Activity;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.modernmotion.safetext.R;

public class GPSListener implements LocationListener {
	private final static String TAG = "DEBUG (Location): ";
	private Activity context;
	
	private TextView speedValue;
	private TextView lonValue;
	private TextView latValue;
	
	class StatusRunnable implements Runnable {

		private String _message;
		
		public StatusRunnable(String message) {
			_message = message;
		}
		@Override
		public void run() {
			Toast t = Toast.makeText(context, _message, Toast.LENGTH_SHORT);
			t.show();
		}
	}
	
	public GPSListener(Activity context) {
		this.context = context;
		
		speedValue = (TextView) context.findViewById(R.id.st_speed);
		lonValue = (TextView) context.findViewById(R.id.lon_value);
		latValue = (TextView) context.findViewById(R.id.lat_value);
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

			/*smsMonitor.sense(location);*/
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		StatusRunnable providerStatus = null;
		
		switch(status) {
		case LocationProvider.AVAILABLE:
			providerStatus = new StatusRunnable("Provider Available.");
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			providerStatus = new StatusRunnable("Provider Temporarily Unavailable.");
			break;
		case LocationProvider.OUT_OF_SERVICE:
			providerStatus = new StatusRunnable("Provider Out Of Service.");
			break;
		}
		
		context.runOnUiThread(providerStatus);
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

}
