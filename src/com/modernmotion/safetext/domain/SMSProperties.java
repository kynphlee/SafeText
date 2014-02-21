package com.modernmotion.safetext.domain;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

public class SMSProperties {
	private final double acceleration;
	private final double threshold;
	private final BroadcastReceiver receiver;
	private final IntentFilter receiverFilter;
	
	private SMSProperties(Builder builder) {
		this.acceleration = builder.acceleration;
		this.threshold = builder.threshold;
		this.receiver = builder.receiver;
		this.receiverFilter = builder.receiverFilter;
	}

	public double getAcceleration() {
		return acceleration;
	}

	public double getThreshold() {
		return threshold;
	}

	public BroadcastReceiver getReceiver() {
		return receiver;
	}

	public IntentFilter getReceiverFilter() {
		return receiverFilter;
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		private double acceleration;
		private double threshold;
		private BroadcastReceiver receiver;
		private IntentFilter receiverFilter;
		
		public Builder setAcceleration(double acceleration) {
			this.acceleration = acceleration;
			return this;
		}
		
		public Builder setThreshold(double threshold) {
			this.threshold = threshold;
			return this;
		}
		
		public Builder setReceiver(BroadcastReceiver receiver) {
			this.receiver = receiver;
			return this;
		}
		
		public Builder setReceiverFilter(IntentFilter receiverFilter) {
			this.receiverFilter = receiverFilter;
			return this;
		}
		
		public SMSProperties build() {
			return new SMSProperties(this);
		}
	}
	
	
}
