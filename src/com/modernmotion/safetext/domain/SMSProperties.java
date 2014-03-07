package com.modernmotion.safetext.domain;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;

public class SMSProperties {
	private final double acceleration;
	private final SensorManager sensorManager;
	private final Sensor linearSensor;
	private final Context context;
	private final BroadcastReceiver receiver;
	private final IntentFilter receiverFilter;
	
	private SMSProperties(Builder builder) {
		this.acceleration = builder.acceleration;
		this.sensorManager = builder.sensorManager;
		this.linearSensor = builder.linearSensor;
		this.context = builder.context;
		this.receiver = builder.receiver;
		this.receiverFilter = builder.receiverFilter;
	}

	public SensorManager getSensorManager() {
		return sensorManager;
	}

	public Sensor getLinearSensor() {
		return linearSensor;
	}

	public double getAcceleration() {
		return acceleration;
	}

	public BroadcastReceiver getReceiver() {
		return receiver;
	}

	public IntentFilter getReceiverFilter() {
		return receiverFilter;
	}
	
	public Context getContext() {
		return context;
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		private double acceleration;
		private SensorManager sensorManager;
		private Sensor linearSensor;
		private Context context;
		private BroadcastReceiver receiver;
		private IntentFilter receiverFilter;
		
		public Builder setAcceleration(double acceleration) {
			this.acceleration = acceleration;
			return this;
		}
		
		public Builder setReceiver(BroadcastReceiver receiver) {
			this.receiver = receiver;
			return this;
		}

		public Builder setSensorManager(SensorManager sensorManager) {
			this.sensorManager = sensorManager;
			return this;
		}
		
		public Builder setSensor(Sensor linearSensor) {
			this.linearSensor = linearSensor;
			return this;
		}
		
		public Builder setReceiverFilter(IntentFilter receiverFilter) {
			this.receiverFilter = receiverFilter;
			return this;
		}
		
		public Builder setContext(Context context) {
			this.context = context;
			return this;
		}
		
		public SMSProperties build() {
			return new SMSProperties(this);
		}
	}
	
	
}
