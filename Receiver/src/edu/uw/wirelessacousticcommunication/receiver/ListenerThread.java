package edu.uw.wirelessacousticcommunication.receiver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ListenerThread implements Runnable {
	
	private Handler handler;
	private boolean measure;
	private Context context;

	private Intent serviceIntent;
	private boolean mBound = false;
	
	public AudioRecord audioRecord; 
    public int mSamplesRead; //how many samples read 
    public int recordingState;
    public int bufferSizeBytes; 
    public int channelConfiguration = AudioFormat.CHANNEL_IN_MONO; 
    public int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; 
    public static short[] buffer; //+-32767 
    public static final int sampleRate = 44100; //samp per sec 8000, 11025, 22050 44100 or 48000



	public ListenerThread(Handler handler, Context context, boolean measure) {
		
		this.handler = handler;
		this.measure = measure;
		this.context = context;
		bufferSizeBytes = 4096;//AudioRecord.getMinBufferSize(sampleRate,channelConfiguration,audioEncoding); //4096 on ion
		bufferSizeBytes = AudioRecord.getMinBufferSize(44100,AudioFormat.CHANNEL_CONFIGURATION_MONO,AudioFormat.ENCODING_PCM_16BIT); //4096 on ion
        buffer = new short[bufferSizeBytes/2]; 
        //audioRecord = new AudioRecord(android.media.MediaRecorder.AudioSource.MIC,sampleRate,channelConfiguration,audioEncoding,bufferSizeBytes); //constructor
        audioRecord = new AudioRecord(android.media.MediaRecorder.AudioSource.MIC,44100,AudioFormat.CHANNEL_CONFIGURATION_MONO,AudioFormat.ENCODING_PCM_16BIT,bufferSizeBytes); //constructor
        Log.v("WORKER","INIT");
	}

	@Override
	public void run() {
		/*
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
		*/
		if(audioRecord.getState()==audioRecord.STATE_INITIALIZED){
			audioRecord.startRecording();
			while(!Thread.interrupted()) {
				try{
					//short[] byteBuffer = new short[bufferSizeBytes/2]; 
					mSamplesRead = audioRecord.read(buffer, 0, bufferSizeBytes/2);
					int amp;
					for(int i = 0; i < bufferSizeBytes/2; i++){
						//amp = (int)buffer[i];
						amp = buffer[i];//Math.round(buffer[i]/32767);
						
						//handler.handleMessage(handler.obtainMessage(amp));
						//publishProgress( amp );
						//if(amp>0)
							Log.v("WORKER","amp="+amp);
					}
				} catch( Exception e ){
					e.printStackTrace();
				}
	        }
			audioRecord.stop();
		}
		else{
			Log.e("WORKER", "Audio Recorder not init");
		}
		


	}
	private void handleData(ByteBuffer buffer){
		Message msg = null;	
		Bundle bundle = new Bundle();
		
		int error = bitErrors("");
		
		if(!this.measure){
			//get message and post in view
			String ms = new String(findPreamble(buffer));
			
			msg = handler.obtainMessage();
			bundle.putString("msg", ms);
	        msg.setData(bundle);
	        msg.what = 0;
		} else {
			//get bits and compare to sent bits
			String bits = "asdf";//byteToString(findPreamble(buffer));
			
			int errors = bitErrors(bits);
			msg = handler.obtainMessage();
			bundle.putInt("errors", errors);
	        msg.setData(bundle);
	        msg.what = 1;
		}
		
        handler.sendMessage(msg);
	}
	//compare received bits and 1Kb file
	private int bitErrors(String bits) {
		
			BufferedReader reader = null;
			StringBuilder contents = new StringBuilder();
			
			Log.d("reading", "start");
			try {
			    reader = new BufferedReader(
			        new InputStreamReader(context.getResources().openRawResource(R.raw.data))); 
			    
			    // do reading, usually loop until end of file reading 
			    String line = null; 
			    while (( line = reader.readLine()) != null){
			    	contents.append(line);
			    }
			} catch (IOException e) {
			    Log.e("exc", e.getMessage());
			} finally {
			    if (reader != null) {
			        try {
						reader.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			    }
			}

			String actualbits = byteToString(contents.toString().getBytes()); 
			
			if(actualbits.length()!=bits.length()){
				return -1;
			} else {
				int err = 0;
				
				for (int i = 0; i < actualbits.length(); i++) {
					if(actualbits.charAt(i)!=bits.charAt(i)){
						err++;
					}
				}
				return err;
			}
		}

		//find the preamble in the data
		@SuppressLint("NewApi")
		public byte[] findPreamble(ByteBuffer buf){
			
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
			byte[] buffer = new byte[12];
			
			int ind = 0;
			while(true){
				
				if(buf.remaining()>=1){
					buf.get(buffer, ind, 1);
					ind++;
				}
				
				if(ind==11){
					break;
				}
			}
			
			byte[] header = new byte[13];
			int k;
			if(pos<8){
				header[0] = searchBytes[2];
				header[1] = searchBytes[3];
				k=2;
			} else if(pos<16) {
				header[0] = searchBytes[3];
				k=1;
			} else {
				k=0;
			}
			
			for (int i = 0; i < buffer.length-k; i++) {
				header[i+k] = buffer[i];
			}
			
			BitSet headerBits = BitSet.valueOf(header);
			BitSet headerBitsShift = new BitSet(96);
			
			int shift = pos%8;
			for (int i = shift; i < 96+shift; i++) {
				if(headerBits.get(i)){
					headerBitsShift.set(i-shift);
				}
			}
			
			byte[] headerBytes = new byte[12];
			byte[] headerBytesShifted = headerBitsShift.toByteArray();
			
			for (int i = 0; i < headerBytesShifted.length; i++) {
				headerBytes[i] = headerBytesShifted[i];
			}
			
			//reconstruct header		
			WiAcHeader mHeader = new WiAcHeader(headerBytes);
			
			//create array for data bytes
			if(mHeader.getLength()>0){
				byte[] data = new byte[mHeader.getLength()+1];
				
				byte[] dataBuffer = new byte[mHeader.getLength()];
				
				ind = 0;
				while(true){
					
					if(buf.remaining()>=1){
						buf.get(dataBuffer, ind, 1);
						ind++;
					}
					
					if(ind==mHeader.getLength()-1){
						break;
					}
				}
				
				if (k==2) {
					data[0] = buffer[10];
					k=1;
				} else if (k==1){
					data[0] = buffer[11];
				}
				
				for (int i = 0; i < dataBuffer.length-k; i++) {
					data[i+k] = dataBuffer[i];
				}
				
				BitSet dataBits = BitSet.valueOf(data);
				BitSet dataBitsShift = new BitSet();
				
				for (int i = shift; i < dataBits.length(); i++) {
					if(dataBits.get(i)){
						dataBitsShift.set(i-shift);
					}
				}
				
				byte[] dataBytes = new byte[mHeader.getLength()];
				byte[] dataBytesShifted = dataBitsShift.toByteArray();
				
				for (int i = 0; i < dataBytesShifted.length; i++) {
					dataBytes[i] = dataBytesShifted[i];
				}
				return dataBytes;
			}
			
			return null;
			
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
