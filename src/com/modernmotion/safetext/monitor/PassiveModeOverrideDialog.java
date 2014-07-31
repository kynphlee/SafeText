package com.modernmotion.safetext.monitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;
import com.modernmotion.safetext.MonitorState;
import com.modernmotion.safetext.R;

import java.io.*;

public class PassiveModeOverrideDialog extends DialogFragment {
	
	public interface PassiveModeOverrideInterface {
		public void onPassiveModeOverrideChange();
		public void onAcquireMonitor(PassiveModeOverrideDialog dialog);
	}
	
	private PassiveModeOverrideInterface mListener;
	private SMSMonitor smsMonitor;

    private static boolean isEnabled;
    private int buttonText;
	
	public void setMonitor(SMSMonitor monitor) {
		smsMonitor = monitor;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		try {
            if (!isEnabled) {
                buttonText = R.string.overrideEnable;
            } else {
                buttonText = R.string.overrideDisable;
            }

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
		builder.setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
                // Looks up the pin stored in internal storage
                String filename = getString(R.string.filename);
                EditText userIn = (EditText) getDialog().findViewById(R.id.pin);
                String userPin = userIn.getText().toString();
                String storedPin = null;
                StringBuilder sBuilder = new StringBuilder();

                try {
                    FileInputStream fin = getActivity().openFileInput(filename);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fin));
                    sBuilder.append(br.readLine());
                    storedPin = sBuilder.substring(5, sBuilder.length());
                    fin.close();

                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                if (userPin.equals(storedPin)) {
                    if (!isEnabled) {
                        smsMonitor.setOverride(MonitorState.PASSIVE, true);
                        mListener.onPassiveModeOverrideChange();
                        isEnabled = true;
                    } else {
                        smsMonitor.setOverride(MonitorState.PASSIVE, false);
                        mListener.onPassiveModeOverrideChange();
                        isEnabled = false;
                    }
                } else {
                    Toast.makeText(getActivity().getApplicationContext(),
                            R.string.invalid_pin,
                            Toast.LENGTH_SHORT).show();
                }
			}
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		return builder.create();
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
	}
}
