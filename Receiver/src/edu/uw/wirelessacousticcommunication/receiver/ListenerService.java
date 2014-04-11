package edu.uw.wirelessacousticcommunication.receiver;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class ListenerService extends Service {
	
	private final IBinder mBinder = new LocalBinder();
	
	 /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        ListenerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ListenerService.this;
        }
    }

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}

}
