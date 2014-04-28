package edu.uw.wirelessacousticcommunication.receiver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ListenerThread extends Thread {
	
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
	private static final int NUMBER_SAMPLES_PEAK = 0;
    
    //predefined preamble
    byte[] preamble = new byte[]{(byte) 0xff,(byte) 0xff};
    byte[] searchBuffer = new byte[2*bufferSizeBytes];
    
    //FSK stuff
    private static double PEAK_AMPLITUDE_TRESHOLD = 10000;
    private String TAG = "561 project";
    int AUDIO_SAMPLE_FREQ = 44100;
    double SAMPLING_TIME = 1.0/AUDIO_SAMPLE_FREQ;
	int AUDIO_BUFFER_SIZE = 22050;
	int BIT_HIGH_SYMBOL=2;
	int BIT_LOW_SYMBOL=1;
	int BIT_NONE_SYMBOL=0;
	int HIGH_BIT_N_PEAKS = 12;
	int LOW_BIT_N_PEAKS = 7;
	int SLOTS_PER_BIT = 4;
	int MINUMUM_NPEAKS = 50;
	int N_POINTS = 28;

	public ListenerThread(Handler handler, Context context, boolean measure) {
		
		this.handler = handler;
		this.measure = measure;
		this.context = context;
		
		/*
		bufferSizeBytes = 4096;//AudioRecord.getMinBufferSize(sampleRate,channelConfiguration,audioEncoding); //4096 on ion
		bufferSizeBytes = AudioRecord.getMinBufferSize(44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT); //4096 on ion
        buffer = new short[bufferSizeBytes/2]; 
        //audioRecord = new AudioRecord(android.media.MediaRecorder.AudioSource.MIC,sampleRate,channelConfiguration,audioEncoding,bufferSizeBytes); //constructor
        audioRecord = new AudioRecord(android.media.MediaRecorder.AudioSource.MIC,44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,bufferSizeBytes); //constructor
        Log.v("WORKER","INIT");
        */
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
		
		if(audioRecord.getState()==audioRecord.STATE_INITIALIZED){
			audioRecord.startRecording();
			File root = Environment.getExternalStorageDirectory();
			File file = new File(root, "MIC_fahad.txt");
			FileWriter filewriter=null;
			BufferedWriter out=null;
			try{
				if (root.canWrite()) {
		            filewriter = new FileWriter(file);
		            out = new BufferedWriter(filewriter);
		        }
				while(!Thread.interrupted()) {
					mSamplesRead = audioRecord.read(buffer, 0, bufferSizeBytes/2);
					int amp;
					for(int i = 0; i < bufferSizeBytes/2; i++){
						amp = buffer[i];//Math.round(buffer[i]/32767);
						if(out!=null)
							out.write(amp+"\n");
						if(amp>0)
							Log.v("WORKER","amp="+amp);
					}
<<<<<<< HEAD

					//FSK
					processFSK(buffer, mSamplesRead);
					
					handleData(new byte[1]);
				} catch( Exception e ){
					e.printStackTrace();
=======
					//handleData(getDataBits(buffer));
>>>>>>> d53b3e7949f0604594bd49d0e16912d28780c09d
				}
				if(out!=null)
					out.close();
				audioRecord.stop();
	        }catch( Exception e ){
				e.printStackTrace();
			}
		}
		else{
			Log.e("WORKER", "Audio Recorder not init");
		}
		*/
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
				Log.d(TAG, "audio acq: length=" + nBytes);
				Log.d(TAG, "audio acq data: "+audioData[0]);
				// Log.v(TAG, "nBytes=" + nBytes);
				
				processFSK(audioData,nBytes);
				if (nBytes < 0) {
					Log.e(TAG, "audioRecordingRun() read error=" + nBytes);
				}
				
				if(Thread.interrupted()){
					break;
				}
			}
			
			aR.stop();
			aR.release();
		
		} else {
			Log.i(TAG, "not initialized");
		}
	}


	private void processFSK(byte[] audioData, int nBytes){
		
		if (signalDetected(audioData)){
			
			//count peaks 
			int[] nPeaks = processSound(byte2double(audioData));
			for (int i = 0; i < nPeaks.length; i++) {
				Log.d("peaks per double", nPeaks[i]+"");
			}
			
			//demodulate
			int[] bits = parseBits(nPeaks);
			
			for (int i = 0; i < bits.length; i++) {
				Log.d("bits", bits[i]+"");
			}
		}
	}
	
	private int[] parseBits(int[] peaks){
		// from the number of peaks array decode into an array of bits (2=bit-1, 1=bit-0, 0=no bit)
		// 
		int i =0;
		int lowCounter = 0;
		int highCounter = 0;
		int nBits = peaks.length /SLOTS_PER_BIT;
		int[] bits = new int[nBits];
		//i = findNextZero(peaks,i); // do not search for silence
		i = findNextNonZero(peaks,i);
		int nonZeroIndex = i;
		if (i+ SLOTS_PER_BIT >= peaks.length) //non-zero not found
			return bits;
		do {
			//int nPeaks = peaks[i]+peaks[i+1]+peaks[i+2]+peaks[i+3];
			int nPeaks = 0;
			for (int j = 0; j < SLOTS_PER_BIT; j++) {
				nPeaks+= peaks[i+j];
			}
			int position = i/SLOTS_PER_BIT;
			bits[position] = BIT_NONE_SYMBOL;
			
			if (nPeaks>= LOW_BIT_N_PEAKS) {
				//Log.w(TAG, "parseBits NPEAK=" + nPeaks);
				bits[position] = BIT_LOW_SYMBOL;
				lowCounter++;
			}
			if (nPeaks>=HIGH_BIT_N_PEAKS ) {
				bits[position] = BIT_HIGH_SYMBOL;
				highCounter++;
			}

			i=i+SLOTS_PER_BIT;
			
			
		} while (SLOTS_PER_BIT+i<peaks.length);
		lowCounter = lowCounter - highCounter;
		return bits;
	}
	
	private int findNextNonZero(int[] peaks, int startIndex){
		// returns the position of the next value != 0 starting form startIndex
		int index = startIndex;
		int value = 1;
		do {
			value = peaks[index];
			index++;
		} while (value==0 && index<peaks.length-1);
		return index-1;
	}

	private int[] processSound(double[] sound){
		
		// split the sound array into slots of N_POINTS and calculate the number of peaks
		int nPoints = N_POINTS;
		int nParts = sound.length / nPoints;
		int[] nPeaks = new int[nParts]; 
		int startIndex = 0;
		int i = 0;
		int peakCounter = 0;
		do {
			int endIndex = startIndex + nPoints;
			int n = countPeaks(sound, startIndex, endIndex);
			nPeaks[i] = n;
			peakCounter += n;
			i++;
			startIndex = endIndex;
		} while (i<nParts);

		if (peakCounter < MINUMUM_NPEAKS) {
			nPeaks = new int[0];
		}
		return nPeaks;
	}

	private boolean signalDetected(byte[] sound){
		
		boolean signalDetected = false;
		
		if ( sound!= null) {
			double data[] = byte2double(sound);
			signalDetected = signalAvailable(data);
			if (signalDetected) Log.w(TAG, "signalDetected() TRUE"); 
		}
			
		Log.i(TAG, "signalDetected()=" + signalDetected);
		return signalDetected;
	}
	
	public boolean signalAvailable(double[] sound){
		
		int nPoints = N_POINTS;
		int nParts = sound.length / nPoints;
		int nPeaks = 0;  
		int startIndex = 0;
		int i = 0;
		do {
			int endIndex = startIndex + nPoints;
			int n = countPeaks(sound, startIndex, endIndex);
			nPeaks += n;
			i++;
			startIndex = endIndex;
			if (nPeaks > MINUMUM_NPEAKS) return true;
		} while (i<nParts);
		if (nPeaks >3)
			Log.i(TAG,"signalAvailable() nPeaks=" + nPeaks);
		return false;
	}
	
	private double[] byte2double(byte[] data){
		double d[] = new double[data.length/2];
		ByteBuffer buf = ByteBuffer.wrap(data, 0, data.length);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		int counter = 0;
		while (buf.remaining() >= 2) {
			double s = buf.getShort();
			d[counter] = s;
			counter++;
		}
		return d;
	}

	private int countPeaks(double[] sound, int startIndex, int endIndex){
		// count the number of peaks in the selected interval
		// peak identification criteria: sign changed and several significant samples (>PEAK_AMPLITUDE_TRESHOLD) 
		
		int index = startIndex;
		int signChangeCounter = 0;
		int numberSamplesGreaterThresdhold = 0;
		int sign = 0; // initialized at the first significant value
		do {
			double value = sound[index];
			if (Math.abs(value)>PEAK_AMPLITUDE_TRESHOLD) 
				numberSamplesGreaterThresdhold++; //significant value
			// sign initialization: take the sign of the first significant value
			if (sign==0 & numberSamplesGreaterThresdhold>0) sign = (int) (value / Math.abs(value));
			boolean signChanged = false;
			if (sign <0 & value >0)	signChanged = true;
			if (sign >0 & value <0)	signChanged = true;
			
			if (signChanged & numberSamplesGreaterThresdhold>NUMBER_SAMPLES_PEAK){
				signChangeCounter++; // count peak
				sign=-1*sign; //change sign
			}
			index++;
		} while (index<endIndex);
		return signChangeCounter;
	}

	//ASK Method
	private byte[] getDataBits(short[] buffer){
		int startSignal=findSignalStart(buffer);
		if(startSignal==-1) return new byte[0];
		int bps=1;
		int samplesPerSymbol = 8;
		int minThreshold = 10000;
		int highSamples = (int)Math.floor(samplesPerSymbol/4);
		byte[] bitData=new byte[(int)Math.ceil((((buffer.length-startSignal)/samplesPerSymbol)*bps)/8)];
		String bitRep="";
		int count=0;
		for(int i=startSignal;i<buffer.length;i=i+samplesPerSymbol){
			for(int j=i;j<samplesPerSymbol;j++) 
				if(Math.abs(buffer[j])>minThreshold)
					count++;
			if(count>=highSamples)
				bitRep=bitRep+"1";
			else
				bitRep=bitRep+"0";
		}
		return bitData;
	}
	//ASK Method
	private int findSignalStart(short[] buffer){
		int bps=1;
		int samplesPerSymbol = 8;
		int minThreshold = 10000;
		int highSamples = (int)Math.floor(samplesPerSymbol/4);
		int count=0;
		int signalStart=0;
		int lastHighIndex=-1;
		boolean negSignal=false;
		for(int i=0;i<buffer.length;i++){
			if(Math.abs(buffer[i])>minThreshold){
				count++;
				lastHighIndex=i;
			}
			if(lastHighIndex<i-2)
				count=0;
			if(count>=highSamples)
				return signalStart;
			if(buffer[i]>=0 && negSignal){
				signalStart=i;
				negSignal=false;
			}else if(buffer[i]<0 && !negSignal){
				negSignal=true;
			}
		}
		return -1;
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
