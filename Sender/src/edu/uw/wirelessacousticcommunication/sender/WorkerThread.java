package edu.uw.wirelessacousticcommunication.sender;

import java.util.BitSet;
import java.util.Queue;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class WorkerThread extends AsyncTask<String, Void, Void> {
		
	/*
	 * convertData(String message) ** 
	 * slice message into chunks ** 
	 * convert to bit array ** 
	 * for each chunk: e.g. stuff bits, 
	 * create header and checksum / CRC, 
	 * add preamble
	 */
	private BitSet convertData(String msg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Void doInBackground(String... params) {
		
		//get message
		String msg = params[0];
			
		//convert message to data packet
		BitSet packets = convertData(msg);
		
		//modulate
		//modulate(bits, carrier signal, bitspersymbol)
	
		return null;
	}
}
