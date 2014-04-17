package edu.uw.wirelessacousticcommunication.sender;

import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Queue;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class WorkerThread extends AsyncTask<String, Void, Void> {
	
	private final String destIP = "1.1.1.1";
	private final String srcIP = "23.12.4.123";
		
	/*
	 * convertData(String message) ** 
	 * slice message into chunks ** 
	 * convert to bit array ** 
	 * for each chunk: e.g. stuff bits, 
	 * create header and calculate CRC
	 */
	private BitSet convertData(String msg) {
		
		//get bytes of message
		byte[] data = msg.getBytes();
		
		//src string to byte array
		String[] srcStr = srcIP.split("\\.");
		byte[] srcBytes = new byte[4];
		for (int i = 0; i < srcStr.length; i++) {
			srcBytes[i] = (byte)Integer.parseInt(srcStr[i]);
		}
		
		//dest string to byte array
		String[] destStr = destIP.split("\\.");
		byte[] destBytes = new byte[4];
		for (int i = 0; i < srcStr.length; i++) {
			destBytes[i] = (byte)Integer.parseInt(destStr[i]);
		}
		
		//Length of payload to byte array
		byte[] length = new byte[]{(byte) data.length};
		
		// checksum with the specified array of bytes
		Checksum checksum = new CRC32();
		checksum.update(data, 0, data.length);
		byte[] check = new byte[8];
		ByteBuffer buf = ByteBuffer.wrap(check);  
		buf.putLong(checksum.getValue());  
		
		byte[] header = new byte[17];
		
		for (int i = 0; i < srcBytes.length; i++) {
			header[i] = srcBytes[i];
		}
		for (int j = 0; j < destBytes.length; j++) {
			header[j+4] = destBytes[j];
		}
		for (int k = 0; k < length.length; k++) {
			header[k+8] = length[k];
		}
		for (int l = 0; l < check.length; l++) {
			header[l+9] = check[l];
		}
		
		byte[] payload = new byte[header.length+data.length];
		
		for (int i = 0; i < header.length; i++) {
			payload[i] = header[i];
		}
		for (int i = 0; i < data.length; i++) {
			payload[i+header.length] = data[i];
		}
		
		BitSet bitstring = new BitSet();
		
		for (int i=0; i<payload.length*8; i++) {
	        if ((payload[payload.length-i/8-1]&(1<<(i%8))) > 0) {
	            bitstring.set(i);
	        }
	    }		
		
		return bitstring;
	}
	
	public void sendAudio(){
		
		AudioTrack audio = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, 44100, AudioTrack.MODE_STREAM);

		String data = "some samlpe data dsf asdf asd f asd f asd  sf as df a sd f asdf  a sdf";
		byte[] audioData = data.getBytes();
		audio.write(audioData, 0, audioData.length);
		audio.play();
		
		Log.d("DEBUG", "audio played");
	}

	@Override
	protected Void doInBackground(String... params) {
		
		//get message
		String msg = params[0];
		
		Log.d("Debug", "message: "+msg);
			
		//convert message to data packet
		BitSet packets = convertData(msg);
		
		//modulate
		//modulate(bits, carrier signal, bitspersymbol)
		
		sendAudio();
		
		return null;
	}
}
