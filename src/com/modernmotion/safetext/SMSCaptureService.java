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
import android.util.Log;
import static com.modernmotion.safetext.util.STConstants.*;

public class SMSCaptureService extends Service {
	
	private Looper serviceLooper;
	private SMSCaptureHandler serviceHandler;
	private SMSBuffer smsBuffer;

	private int smsCount = 0;

	private boolean receiverRegistered = false;
	
	@Override
	public void onCreate() {
		HandlerThread stServiceThread = new HandlerThread(
				"SafeTextServiceThread", Process.THREAD_PRIORITY_BACKGROUND);
		stServiceThread.start();

		smsBuffer = new SMSBuffer(this);
		serviceLooper = stServiceThread.getLooper();
		serviceHandler = new SMSCaptureHandler(serviceLooper);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			String action = intent.getAction();
			if (action.equals(ST_START_CAPTURE)) {
				Message message = serviceHandler.obtainMessage();
				serviceHandler.sendMessage(message);
			} else if (action.equals(ST_STOP_CAPTURE)) {
				unregisterReceiver(smsReceiver);
				receiverRegistered = false;
				
				if (smsBuffer.count() > 0) {
					final int messagesDumped = smsBuffer.dumpSmsMessages();
					
					Intent smsReceivedIntent = new Intent();
					smsReceivedIntent.setAction(ST_PASSIVE_STATE);
					smsReceivedIntent.putExtra("messagesDumped", messagesDumped);
					sendBroadcast(smsReceivedIntent);
				}
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
			
			// Extract the SMS message from the PDU
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
			smsReceivedIntent.setAction(ST_SMS_MESSAGE_RECEIVED);
			smsReceivedIntent.putExtra("smsCount", smsCount);
			smsReceivedIntent.putExtra("sender", sender);
			smsReceivedIntent.putExtra("message", message);
			
			// Add message to the smsCache
			Object[] pdus = (Object[]) intent.getExtras().get("pdus");
			SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdus[0]);
			smsBuffer.add(sms);
			
			// Send SMS to STStatus for debug
			sendBroadcast(smsReceivedIntent);
			Log.i("SMSTAG", "sms captured!");
			
			// Send an auto-reply to the sender
			SmsManager smsManager = SmsManager.getDefault();
			//String autoReply = getResources().getString(R.string.st_service_active_message);
			//smsManager.sendTextMessage(messages[0].getOriginatingAddress(), null, autoReply, null, null);
			Log.i("SMSTAG", "sms auto-reply sent!");
		}
	};
	
	public static void startSMSCapture(Context context) {
		Intent intent = new Intent(context, SMSCaptureService.class);
		intent.setAction(ST_START_CAPTURE);
		context.startService(intent);
	}

	public static void stopSMSCapture(Context context) {
		Intent intent = new Intent(context, SMSCaptureService.class);
		intent.setAction(ST_STOP_CAPTURE);
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
