package edu.uw.wirelessacousticcommunication.receiver;

import edu.uw.wirelessacousticcommunication.receiver.ListenerService.LocalBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

public class ListenerThread implements Runnable {
	
	private Handler handler;
	private Context context;
	private Intent serviceIntent;
	private ListenerService mService;
	private boolean mBound = false;

	public ListenerThread(Handler handler, Context context) {
		
		this.handler = handler;
		this.context = context;

	}

	@Override
	public void run() {
		
		//start service
		this.context.bindService(
		        new Intent(this.context, ListenerService.class),
		        mConnection,
		        Context.BIND_AUTO_CREATE
		    );
		
		

	}
	
	/** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
	
	

}
