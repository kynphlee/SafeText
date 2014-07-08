package com.modernmotion.safetext.util;

import android.location.Location;

public class STUtils {
	public static int speedToMPH(Location location) {
		return Math.round((location.getSpeed() * 2.23694f));
	}

	public static int speedToMPH(float speed) {
		return Math.round((speed * 2.23694f));
	}

	public static double longToDecimal(long longVal) {
		return Long.valueOf(longVal).doubleValue();
	}
}
