package com.modernmotion.safetext.monitor;

import android.app.Activity;
import android.location.Location;
import com.modernmotion.safetext.MonitorState;
import com.modernmotion.safetext.STStatus;

import static com.modernmotion.safetext.util.STConstants.*;
import static com.modernmotion.safetext.util.STUtils.longToDecimal;
import static com.modernmotion.safetext.util.STUtils.speedToMPH;

public class DefaultSMSMonitor implements SMSMonitor {

	private STStatus mActivity;
    private boolean monitorActivitySet = false;
	private State passiveState;
	private State activeState;
	private State delayState;
	private State monitorState;

	private MonitorState mState;
	private MonitorListener monitorListener;

	public DefaultSMSMonitor() {
		try {
			activeState = new ActiveState(this);
			passiveState = new PassiveState(this);
			delayState = new DelayState(this);

			mState = MonitorState.PASSIVE;
			monitorState = passiveState;

		} catch (ClassCastException ex) {
			throw new ClassCastException(mActivity.getLocalClassName()
					+ "must implement ManualOverrideInterface.");
		}
	}

	public void setMonitorActivity(Activity context) {
		mActivity = (STStatus) context;
		monitorListener = mActivity;
        if (context != null) {
            monitorActivitySet = true;
        } else {
            monitorActivitySet = false;
        }
	}


	@Override
	public void transitionTo(MonitorState newState) {
		switch (newState.ordinal()) {
		case 0: // Passive
			mState = newState;
			monitorState = passiveState;
			break;
		case 1: // Active
			mState = newState;
			monitorState = activeState;
			break;
		case 2: // Delay
			mState = newState;
			monitorState = delayState;
			break;
		}

		if (isOverridden(newState)) {
			monitorState.onOverrideState();
		}
	}

	@Override
	public void setOverride(MonitorState stateToOverride,
			boolean override) {
		switch (stateToOverride.ordinal()) {
		case 0: // Passive
			passiveState.setOverride(override);
			break;
		case 1: // Active
			activeState.setOverride(override);
			break;
		case 2: // Delay
			delayState.setOverride(override);
			break;
		}
	}

	@Override
	public boolean isOverridden(MonitorState state) {
		boolean overridden = false;

		switch (state.ordinal()) {
		case 0: // Passive
			overridden = passiveState.isOverridden();
			break;
		case 1: // Active
			overridden = activeState.isOverridden();
			break;
		case 2: // Delay
			overridden = delayState.isOverridden();
			break;
		}

		return overridden;
	}

	@Override
	public void sense(Location location) {
		monitorState.run(location);
	}

	@Override
	public MonitorState currentState() {
		return mState;
	}

	abstract class State {

		protected SMSMonitor monitor;
		protected boolean override;
		protected double startTime;

		public State(SMSMonitor monitor) {
			this.monitor = monitor;
			override = false;
		}

		protected void setOverride(boolean newValue) {
			override = newValue;
		}

		protected boolean isOverridden() {
			return override;
		}

		protected double getDuration() {
			double currentTimeDouble = longToDecimal(System.currentTimeMillis());
			return (currentTimeDouble - startTime) * MS2S;
		}

		protected void run(final Location location) {
			if (location != null && location.hasSpeed()) {
				if (override) {
					override(location);
				} else {
					process(location);
				}
			}
		}

		protected void onOverrideState() {

		}

		protected void override(final Location location) {

		}

		protected abstract void process(final Location location);
	}

	private class PassiveState extends State {
		private int threshold = speedToMPH(THRESHOLD);
		private boolean _override = false;

		public PassiveState(DefaultSMSMonitor monitor) {
			super(monitor);
			setOverride(false);
		}

		@Override
		protected void override(final Location location) {
			int speed = speedToMPH(location);
			if (!_override) {
				if (speed < threshold) {
					return;
				}
				if (speed >= threshold) {
					_override = true;
				}
			} else {
				if (speed >= threshold) {
					return;
				}
				if (speed < threshold) {
					_override = false;
					monitor.setOverride(MonitorState.PASSIVE, false);
					mActivity.onPassiveModeOverrideChange();
				}
			}
		}

		@Override
		protected void process(final Location location) {
			int speed = speedToMPH(location);
			if (speed >= threshold) {
				// Speed threshold reached. Transition to Active state.
                if (monitorActivitySet) {
                    monitorListener.onCaptureStart();
                }
				SMSCaptureService.startSMSCapture(mActivity);
				startTime = longToDecimal(System.currentTimeMillis());

				monitor.transitionTo(MonitorState.ACTIVE);
			}
		}
	}

	private class ActiveState extends State {
		private int durationLimit = THREE_MINUTES;
		private double duration = 0;
		private int threshold = speedToMPH(THRESHOLD);

		public ActiveState(DefaultSMSMonitor monitor) {
			super(monitor);
		}

		@Override
		protected void onOverrideState() {

		}

		@Override
		protected void process(Location location) {
			duration = getDuration();
			if (duration >= durationLimit) {
				int speed = speedToMPH(location);
				if (speed >= threshold) {
					// Continue blocking SMS messages
					startTime = longToDecimal(System.currentTimeMillis());
				} else if (speed == 0) {
					startTime = longToDecimal(System.currentTimeMillis());
					monitor.transitionTo(MonitorState.DELAY);
				}
			}
		}
	}

	private class DelayState extends State {
		private int durationLimit = ONE_MINUTE;
		private double duration = 0;
		private int threshold = speedToMPH(THRESHOLD);

		public DelayState(DefaultSMSMonitor monitor) {
			super(monitor);
		}

		@Override
		protected void process(Location location) {
			duration = getDuration();
			if (duration >= durationLimit) {
				int speed = speedToMPH(location);

				if (speed >= threshold) {
					startTime = longToDecimal(System.currentTimeMillis());
					monitor.transitionTo(MonitorState.ACTIVE);
				} else if (speed == 0) {
					SMSCaptureService.stopSMSCapture(mActivity);
                    if(monitorActivitySet) {
                        monitorListener.onCaptureStop();
                    }
					monitor.transitionTo(MonitorState.PASSIVE);
				}
			}
		}
	}
}
