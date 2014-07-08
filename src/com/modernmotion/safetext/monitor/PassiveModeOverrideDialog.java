package com.modernmotion.safetext.monitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import com.modernmotion.safetext.MonitorState;
import com.modernmotion.safetext.R;

public class PassiveModeOverrideDialog extends DialogFragment {
	
	public interface PassiveModeOverrideInterface {
		public void onPassiveModeOverrideChange();
		public void onAcquireMonitor(PassiveModeOverrideDialog dialog);
	}
	
	private PassiveModeOverrideInterface mListener;
	private SMSMonitor smsMonitor;
	
	public void setMonitor(SMSMonitor monitor) {
		smsMonitor = monitor;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		try {
			mListener = (PassiveModeOverrideInterface) activity;
			mListener.onAcquireMonitor(PassiveModeOverrideDialog.this);
		} catch (ClassCastException ex) {
			throw new ClassCastException(activity.toString()
					+ "must implement ManualOverrideInterface.");
		}
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		builder.setView(inflater.inflate(R.layout.st_override_pin, null));
		
		builder.setTitle(R.string.manual_override);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				smsMonitor.setOverride(MonitorState.PASSIVE, true);
				mListener.onPassiveModeOverrideChange();
			}
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				smsMonitor.setOverride(MonitorState.PASSIVE, false);
				mListener.onPassiveModeOverrideChange();
			}
		});

		return builder.create();
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
	}
}
