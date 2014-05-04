package com.modernmotion.safetext.util;

public class STConstants {
	public static final String DEBUG_STRING = "ST:";
	public static final String DEBUG_ACCELEROMETER = DEBUG_STRING + "ACCEL";
	public static final String DEBUG_SENSOR_MANAGER = DEBUG_STRING + "SENSORMANAGER";
	public static final String DEBUG_SENSOR = DEBUG_STRING + "SENSOR";
	public static final float UPPER_THRESHOLD = 6.7056f; // 15 mph
	public static final float LOWER_THRESHOLD = 2.2352f; // 5 mph
	public static final double SCAN_WINDOW = 10.0;
	public static final double MS2S = 0.001;
	public static final float NS2S = 1.0f / 1000000000.0f;
	public static final int ONE_MINUTE = 60;
	public static final int THREE_MINUTES = 180;
}
