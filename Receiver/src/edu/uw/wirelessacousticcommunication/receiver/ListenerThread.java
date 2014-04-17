package edu.uw.wirelessacousticcommunication.receiver;

import java.nio.ByteBuffer;
import java.util.BitSet;

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
		
		WiAcHeader sampleHeader = getHeader(BitSet.valueOf("adfasdfasdfasdfasdfasdfadsfadsfadf".getBytes()));

	}
	
	public WiAcHeader getHeader(BitSet bitstring){
		
		//reconstruction
		byte[] bytes = new byte[bitstring.length()/8+1];
	    for (int i=0; i<bitstring.length(); i++) {
	        if (bitstring.get(i)) {
	            bytes[bytes.length-i/8-1] |= 1<<(i%8);
	        }
	    }
	    
	    //define variables
	    byte[] src = new byte[4];
	    byte[] dest = new byte[4];
	    int length;
	    long CRC;
	    byte[] CRCbytes = new byte[8];
	    
	    //get src
	    for (int i = 0; i < 4; i++) {
	    	src[i] = bytes[i];
		}
	    
	    //get dest
	    for (int i = 0; i < 4; i++) {
	    	dest[i] = bytes[i+4];
		}
	    
	    length = bytes[8];
	    
	    for (int i = 0; i < 8; i++) {
	    	CRCbytes[i] = bytes[i+17];
		}

	    ByteBuffer buf2 = ByteBuffer.wrap(CRCbytes);  
	    CRC = buf2.getLong();
	    
	    WiAcHeader mHeader = new WiAcHeader(src, dest, length, CRC);
		
	    return mHeader;
		
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
