package com.modernmotion.safetext.util;

public class STConstants {
	public static final String DEBUG_STRING = "ST:";
	public static final String DEBUG_ACCELEROMETER = DEBUG_STRING + "ACCEL";
	public static final String DEBUG_SENSOR_MANAGER = DEBUG_STRING + "SENSORMANAGER";
	public static final String DEBUG_SENSOR = DEBUG_STRING + "SENSOR";
	public static final float THRESHOLD = 4.4704f; // 10 mph
	public static final double SCAN_WINDOW = 10.0;
	public static final double MS2S = 0.001;
	public static final float NS2S = 1.0f / 1000000000.0f;
	public static final int ONE_MINUTE = 60;
	public static final int THREE_MINUTES = 180;
	
	public static final String ST_START_CAPTURE = "com.modernmotion.safetext.action.START_CAPTURE";
	public static final String ST_STOP_CAPTURE = "com.modernmotion.safetext.action.STOP_CAPTURE";
	public static final String ST_SMS_MESSAGE_RECEIVED = "com.modernmotion.safetext.action.SMS_MESSAGE_RECEIVED";
	public static final String ST_PASSIVE_STATE = "com.modernmotion.safetext.monitor.state.ST_PASSIVE_STATE";
	public static final String ST_ACTIVE_STATE = "com.modernmotion.safetext.monitor.state.ST_ACTIVE_STATE";
	public static final String ST_DELAY_STATE = "com.modernmotion.safetext.monitor.state.ST_DELAY_STATE";
	public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
}
