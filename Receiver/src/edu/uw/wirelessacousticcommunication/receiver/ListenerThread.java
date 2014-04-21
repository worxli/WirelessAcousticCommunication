package edu.uw.wirelessacousticcommunication.receiver;

import java.nio.ByteBuffer;
import java.util.BitSet;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class ListenerThread implements Runnable {
	
	private Handler handler;
	private Context context;
	private Intent serviceIntent;
	private boolean mBound = false;

	public ListenerThread(Handler handler, Context context) {
		
		this.handler = handler;
		this.context = context;

	}

	@Override
	public void run() {
		
		//buffer size
		int BufferElements2Rec = 1024;

		//audiorecorder initialization
		AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, BufferElements2Rec);
		
		//record from the mic
		//recorder.startRecording();
		
		//byte buffer to copy recorded bytes into
		ByteBuffer buffer = ByteBuffer.allocate(BufferElements2Rec);
		
		//read recorded bytes
		//recorder.read(buffer, BufferElements2Rec);
		
		Log.d("Debug recorded: ", buffer+"");		
		

	}
	
	//find the preamble in the data
		public static void findPreamble(ByteBuffer buf){
			
			String search = "";
			
			boolean found = false;
			boolean setup = false;
			boolean copied = false;
			int pos = 0;
			
			byte[] preamble = new byte[2];
			byte[] temp = new byte[2];
			
			//predefined preamble
			preamble[0] = (byte) 0xff;
			preamble[1] = (byte) 0xff;
			String preamb = byteToString(preamble);
			
			byte[] searchBytes = new byte[4];
			
			//fill up with four bytes at first
			while(!setup){
				if(buf.remaining()>=4){
					buf.get(searchBytes, 0, 4);
					setup = true;
				}
			}
			
			//search for preamble
			while(!found){
				
				//string to search in
				search = byteToString(searchBytes);
				
				//get position of first occurence of preamble
				pos = search.indexOf(preamb);
				
				System.out.println(pos);
				
				//if not found -> go ahead, otherwise break
				if(pos!=-1){
					found = true;
					break;
				}
				
				//copy second half to first half
				System.arraycopy(searchBytes, 2, searchBytes, 0, 2);
				
				//get bytes from buffer
				while(!copied){
					if(buf.remaining()>=2){
						buf.get(temp, 0, 2);
						copied = true;
					}
				}
				copied = false;
				
				//copy into searchbytes 
				System.arraycopy(temp,0,searchBytes,2,2);
				
			}
			
			//get header bytes
			int maxPackSize = 20;
			byte[] buffer = new byte[maxPackSize];
			
			int ind = 0;
			while(true){
				
				if(buf.remaining()>=1){
					buf.get(buffer, ind, 1);
					ind++;
				}
				
				if(ind==maxPackSize){
					break;
				}
			}

			//copy first 12 bytes into headerBytes
			byte[] headerBytes = new byte[12];
			
			int shift = pos%8;
			int high=0;
			if(pos==0){
				headerBytes[0] = searchBytes[2];
				headerBytes[1] = searchBytes[3];
				
				for (int i = 0; i < 10; i++) {
					headerBytes[i+2] = (byte) (buffer[i]);
					high = i+1;
				}
			} else if(pos==7){
				headerBytes[0] = searchBytes[3];
				headerBytes[1] = buffer[0];
				
				for (int i = 0; i < 10; i++) {
					headerBytes[i+2] = (byte) (buffer[i+1]);
				}
			} else if(pos==15){
				headerBytes[0] = buffer[0];
				headerBytes[1] = buffer[1];
				
				for (int i = 0; i < 10; i++) {
					headerBytes[i+2] = (byte) (buffer[i+2]);
				}
			} else if(pos<7){
				headerBytes[0] = (byte) ((byte) (searchBytes[2] << shift) | (searchBytes[3] >>> 8-shift));
				headerBytes[1] = (byte) ((byte) (searchBytes[3] << shift) | (buffer[0] >>> 8-shift));
				
				for (int i = 0; i < 10; i++) {
					//System.out.println("headerBytes: "+byteToString(headerBytes));
					//System.out.println("buffer: "+byteToString(buffer));
					//System.out.println("buffer: "+byteToString(new byte[]{(byte) (0b11110000 >>> 8-shift)}));
					//System.out.println("buffer shifted: "+byteToString(new byte[]{(byte) ((int)buffer[i+1] >>> (8-shift))}));
					//System.out.println("buffer +1: "+byteToString(new byte[]{buffer[i+1]}));
					//System.out.println((int)buffer[i+1]);
					//System.out.println((byte)buffer[i+1]>>>8);
					headerBytes[i+2] = (byte) ((byte) (buffer[i] << shift) | (byte)((int)buffer[i+1] >>> 8-shift));
				}
			} else {
				headerBytes[0] = (byte) ((byte) (searchBytes[2] << shift) | (buffer[0] >>> 8-shift));
				headerBytes[1] = (byte) ((byte) (buffer[0] << shift) | (buffer[1] >>> 8-shift));
				
				for (int i = 0; i < 10; i++) {
					headerBytes[i+2] = (byte) ((byte) (buffer[i+1] << shift) | (buffer[i+2] >>> 8-shift));
				}
			}
			
			//reconstruct header		
			WiAcHeader mHeader = new WiAcHeader(headerBytes);
			
			//create array for data bytes
			byte[] data = new byte[mHeader.getLength()];
			for (int i = 0; i < mHeader.getLength(); i++) {
				data[i] = (byte) ((byte) (buffer[i+high] << shift) | (buffer[i+high+1] >>> 8-shift));
			}
			
		}
	
		//convert byte array to bit string
		public static String byteToString(byte[] b){
			
			String s = "";
			
			for(int j=0; j<b.length; j++){
				
				s = s+String.format("%8s", Integer.toBinaryString(b[j] & 0xFF)).replace(' ', '0');
			}
			
			return s;
		}
	
	

}
