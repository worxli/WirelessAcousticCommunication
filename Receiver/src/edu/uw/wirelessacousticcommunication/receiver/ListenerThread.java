package edu.uw.wirelessacousticcommunication.receiver;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

public class ListenerThread implements Runnable {
	
	private Handler handler;
	private Context context;
	private Intent serviceIntent;

	public ListenerThread(Handler handler, Context context) {
		
		this.handler = handler;
		this.context = context;
		
		// start service
		serviceIntent = new Intent(this.context, ListenerService.class);
		this.context.startService(serviceIntent); 
	}

	@Override
	public void run() {
		
		

	}

}
