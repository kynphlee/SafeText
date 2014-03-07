package com.modernmotion.safetext;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class SMSCaptureService extends Service {
	private static final String ACTION_START_CAPTURE = "com.modernmotion.safetext.action.START_CAPTURE";
	private static final String ACTION_STOP_CAPTURE = "com.modernmotion.safetext.action.STOP_CAPTURE";
	public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
	
	private Looper serviceLooper;
	private SMSCaptureHandler serviceHandler;

	private int smsCount = 0;

	private boolean receiverRegistered = false;
	
	@Override
	public void onCreate() {
		HandlerThread stServiceThread = new HandlerThread(
				"SafeTextServiceThread", Process.THREAD_PRIORITY_BACKGROUND);
		stServiceThread.start();

		serviceLooper = stServiceThread.getLooper();
		serviceHandler = new SMSCaptureHandler(serviceLooper);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			String action = intent.getAction();
			if (action.equals(ACTION_START_CAPTURE)) {
				Message message = serviceHandler.obtainMessage();
				serviceHandler.sendMessage(message);
			} else if (action.equals(ACTION_STOP_CAPTURE)) {
				unregisterReceiver(smsReceiver);
				receiverRegistered = false;
				stopSelf();
			}
		}
		return START_NOT_STICKY;
	}

	private final class SMSCaptureHandler extends Handler {
		public SMSCaptureHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			//	Start the SMS capture service here...
			IntentFilter smsFilter = new IntentFilter();
			smsFilter.addAction(SMS_RECEIVED_ACTION);
			smsFilter.setPriority(Integer.MAX_VALUE);
			
			registerReceiver(smsReceiver, smsFilter);
			receiverRegistered = true;
		}
	}
	
	private final BroadcastReceiver smsReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			String sender = null;
			String message = null;
			SmsMessage[] messages = null;
			
			if (action.equals(SMS_RECEIVED_ACTION)) {
				Bundle bundle = intent.getExtras();
				if (bundle != null) {
					Object[] pdus = (Object[]) bundle.get("pdus");
					messages = new SmsMessage[pdus.length];
					for (int i = 0; i < messages.length; i++) {
						messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
						sender = messages[i].getOriginatingAddress();
						message = messages[i].getMessageBody().toString();
						smsCount++;
					}
				}
			}
			
			// Block this sms message from propagating to other sms applications
			abortBroadcast();
			Intent smsReceivedIntent = new Intent();
			smsReceivedIntent.setAction("SMS_MESSAGE_RECEIVED");
			smsReceivedIntent.putExtra("smsCount", smsCount);
			smsReceivedIntent.putExtra("sender", sender);
			smsReceivedIntent.putExtra("message", message);
			sendBroadcast(smsReceivedIntent);
			
			// send an auto-reply to the sender
			SmsManager sms = SmsManager.getDefault();
			String autoReply = getResources().getString(R.string.st_service_active_message);
			sms.sendTextMessage(messages[0].getOriginatingAddress(), null, autoReply, null, null);
		}
	};

	public static void startSMSCapture(Context context) {
		Intent intent = new Intent(context, SMSCaptureService.class);
		intent.setAction(ACTION_START_CAPTURE);
		context.startService(intent);
	}

	public static void stopSMSCapture(Context context) {
		Intent intent = new Intent(context, SMSCaptureService.class);
		intent.setAction(ACTION_STOP_CAPTURE);
		context.startService(intent);
	}

	@Override
	public void onDestroy() {
		if (receiverRegistered) {
			unregisterReceiver(smsReceiver);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
