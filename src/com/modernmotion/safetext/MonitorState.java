package com.modernmotion.safetext;

public enum MonitorState {

	PASSIVE("Passive"), ACTIVE("Active"), DELAY("Delay");

	private final String state;

	private MonitorState(final String newState) {
		state = newState;
	}

	public String getMonitorState() {
		return state;
	}
}