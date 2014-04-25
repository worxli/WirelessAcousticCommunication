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
	private boolean init = true;
	
	public AudioRecord audioRecord; 
    public int mSamplesRead; //how many samples read 
    public int recordingState;
    public int bufferSizeBytes; 
    public int channelConfiguration = AudioFormat.CHANNEL_IN_MONO; 
    public int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; 
    public static short[] buffer; //+-32767 
    public static final int sampleRate = 44100; //samp per sec 8000, 11025, 22050 44100 or 48000
    
    //predefined preamble
    byte[] preamble = new byte[]{(byte) 0xff,(byte) 0xff};
    byte[] searchBuffer = new byte[2*bufferSizeBytes];

	public ListenerThread(Handler handler, Context context, boolean measure) {
		
		this.handler = handler;
		this.measure = measure;
		this.context = context;
		bufferSizeBytes = 4096;//AudioRecord.getMinBufferSize(sampleRate,channelConfiguration,audioEncoding); //4096 on ion
		bufferSizeBytes = AudioRecord.getMinBufferSize(44100,AudioFormat.CHANNEL_CONFIGURATION_MONO,AudioFormat.ENCODING_PCM_16BIT); //4096 on ion
        buffer = new short[bufferSizeBytes]; 
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
					byte[] byteBuffer = new byte[bufferSizeBytes]; 
					mSamplesRead = audioRecord.read(byteBuffer, 0, bufferSizeBytes);
					int amp;
					for(int i = 0; i < bufferSizeBytes - 1; i++){
						amp = (int)byteBuffer[i];
						
						//handler.handleMessage(handler.obtainMessage(amp));
						//publishProgress( amp );
						Log.v("WORKER","amp="+amp);
					}
					
					handleData(new byte[1]);
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
	private void handleData(byte[] buffer){
		
		int error = bitErrors("");
		
		//get packet from buffer if there is one
		byte[] data = findPacket(buffer);
		
		if(data!=null){
			if(!this.measure){
				//get message and post in view
				String ms = new String(data);
				
				passMsg(ms, 0);
			} else {
				//get bits and compare to sent bits
				String bits = "asdf";//byteToString(findPreamble(buffer));
				
				int errors = bitErrors(byteToString(data));
				
				passMsg(errors+"", 1);
			}
		}
	}
	
	//pass msg back to UI
	private void passMsg(String message,int type){

		Bundle bundle = new Bundle();
		Message msg = handler.obtainMessage();
		bundle.putString("msg", message);
        msg.setData(bundle);
        msg.what = type;
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

	private byte[] findPacket(byte[] buffer){
		
		Log.d("packet: ", byteToString(buffer));
		
		//preamble string
		String preamb = byteToString(preamble);
		int pos = 0;
		byte[] packet;
		
		if(init){
			
			System.arraycopy(buffer, 0, searchBuffer, bufferSizeBytes, bufferSizeBytes);
			init = false;
		} else {
			
			System.arraycopy(searchBuffer, bufferSizeBytes, searchBuffer, 0, buffer.length);
			System.arraycopy(buffer, 0, searchBuffer, bufferSizeBytes, buffer.length);
			
			//string to search in
			String search = byteToString(searchBuffer);
				
			//get position of first occurence of preamble
			pos = search.indexOf(preamb);
			Log.d("Pos", pos+"");
			if(pos!=-1){
				packet = getPacket(search.substring(pos+16));
				return packet;
			}
		}
		return null;
		
	}
	
	private byte[] getPacket(String str){
		
		byte[] header = StringToBytes(str.substring(0,12*8));
		
		//reconstruct header		
		WiAcHeader mHeader = new WiAcHeader(header);
		
		int length = mHeader.getLength()*8;
		
		Log.d("src: ", mHeader.getDest());
		
		Log.d("str: ", str.length()+" "+length);
		
		//create array for data bytes
		if(mHeader.getLength()>0&&length+12*8<str.length()){
			byte[] data = StringToBytes(str.substring(12*8,12*8+length));
			return data;
		}
		
		return null;
	}	
	
	public byte[] StringToBytes(String str){
		
		int len = str.length();
		int size = (len-len%8)/8;
		
		byte[] bytes = new byte[size];
		
		for (int i = 0; i < size; i++) {
			String st = str.substring(i*8,(i+1)*8);
			bytes[i] = (byte)Integer.parseInt(st,2);
		}
		
		return bytes;
	}
	
	public String byteToString(byte[] b){
		
		String s = "";
		for(int j=0; j<b.length; j++){
			s = s+String.format("%8s", Integer.toBinaryString(b[j] & 0xFF)).replace(' ', '0');
		}
		
		return s;
	}
}
