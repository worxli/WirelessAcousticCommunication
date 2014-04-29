package edu.uw.wirelessacousticcommunication.receiver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ListenerThread extends Thread {
	
	private Handler handler;
	private boolean measure;
	private Context context;
	private boolean init = true;
	private String searchString = "";
	
	public AudioRecord audioRecord; 
    public int mSamplesRead; //how many samples read 
    public int recordingState;
    public int bufferSizeBytes; 
    public int channelConfiguration = AudioFormat.CHANNEL_IN_MONO; 
    public int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; 
    public static short[] buffer; //+-32767 
    public static final int sampleRate = 44100; //samp per sec 8000, 11025, 22050 44100 or 48000
	private static final int NUMBER_SAMPLES_PEAK = 2;
    
    //predefined preamble
    byte[] preamble = new byte[]{(byte) 0xaf};
    byte[] searchBuffer = new byte[2*bufferSizeBytes];
    
    //FSK stuff
    private static double PEAK_AMPLITUDE_TRESHOLD = 12000; //12000
    private String TAG = "561 project";
    int AUDIO_SAMPLE_FREQ = 44100;
    double SAMPLING_TIME = 1.0/AUDIO_SAMPLE_FREQ;
	int AUDIO_BUFFER_SIZE = 16000;
	int BIT_HIGH_SYMBOL=2;
	int BIT_LOW_SYMBOL=1;
	int BIT_NONE_SYMBOL=0;
	int HIGH_BIT_N_PEAKS = 30;
	int LOW_BIT_N_PEAKS = 12; //12
	int SLOTS_PER_BIT = 4;
	int MINUMUM_NPEAKS = 50;
	int N_POINTS = 28;
	
	private Decoder mDecoder;
	
	private Vector<byte[]> shortBuffer = new Vector<byte[]>();

	public ListenerThread(Handler handler, Context context, boolean measure) {
		
		this.handler = handler;
		this.measure = measure;
		this.context = context;
	
	}

	@Override
	public void run() {
		
		mDecoder = new Decoder(this.handler, this.measure, this.context);
		mDecoder.start();
		FSKrecording();

	}
	
	
	
	private void FSKrecording(){
		
		int minBufferSize = AudioTrack.getMinBufferSize(AUDIO_SAMPLE_FREQ, 2, AudioFormat.ENCODING_PCM_16BIT);
		if (AUDIO_BUFFER_SIZE < minBufferSize) AUDIO_BUFFER_SIZE = minBufferSize;

		Log.i(TAG, "buffer size:" + AUDIO_BUFFER_SIZE);
		byte[] audioData = new byte[AUDIO_BUFFER_SIZE];

		AudioRecord aR = new AudioRecord(MediaRecorder.AudioSource.MIC,
				AUDIO_SAMPLE_FREQ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
				AUDIO_BUFFER_SIZE);
		
		if(aR.getState()==aR.STATE_INITIALIZED){

			// audio recording
			aR.startRecording();
			int nBytes = 0;
			int index = 0;
			
			// continuous loop
			while (true) {
				nBytes = aR.read(audioData, index, AUDIO_BUFFER_SIZE);
				Log.i(TAG, "audio acq: length=" + nBytes);
				Log.i(TAG, "audio acq data: "+audioData[0]);
				// Log.v(TAG, "nBytes=" + nBytes);
				
				mDecoder.addSound(audioData, nBytes);
				
				
				if (nBytes < 0) {
					Log.e(TAG, "audioRecordingRun() read error=" + nBytes);
				}
				
				if(Thread.interrupted()){
					mDecoder.interrupt();
					break;
				}
			}
			
			aR.stop();
			aR.release();
			
			
			/*
			int len = audioData.length;
			
			byte[] audioDataHuge = new byte[shortBuffer.size()*len];
			for (int i = 0; i < shortBuffer.size(); i++) {
				byte[] tempData = shortBuffer.get(i);
				for (int j = 0; j < len; j++) {
					audioDataHuge[i*len+j] = tempData[j];
				}
			}
			processFSK(audioDataHuge,audioDataHuge.length);
			*/
		
		} else {
			Log.i(TAG, "not initialized");
		}
	}

}
