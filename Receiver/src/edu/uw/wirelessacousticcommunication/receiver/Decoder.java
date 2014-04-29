package edu.uw.wirelessacousticcommunication.receiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Decoder extends Thread {
	
    //FSK stuff
    private static double PEAK_AMPLITUDE_TRESHOLD = 12000; //12000
    private String TAG = "561 project";
    int AUDIO_SAMPLE_FREQ = 44100;
    double SAMPLING_TIME = 1.0/AUDIO_SAMPLE_FREQ;
	int AUDIO_BUFFER_SIZE = 22050;
	int BIT_HIGH_SYMBOL=2;
	int BIT_LOW_SYMBOL=1;
	int BIT_NONE_SYMBOL=0;
	int HIGH_BIT_N_PEAKS = 30;
	int LOW_BIT_N_PEAKS = 12; //12
	int SLOTS_PER_BIT = 4;
	int MINUMUM_NPEAKS = 50;
	int N_POINTS = 28;
	private static final int NUMBER_SAMPLES_PEAK = 2;
	//predefined preamble
    byte[] preamble = new byte[]{(byte) 0xaf}; 
    private String searchString = "";
    Handler handler;
	private boolean measure;
	private Context context;
	public boolean process = false;
	
	private Vector<byte[]> byteBuffer = new Vector<byte[]>();
	
	public Decoder(Handler hHandler, boolean measure, Context context){
		this.handler = hHandler;
		this.measure = measure;
		this.context = context;
	}
	
	public void run(){
		
		while(true){
			
			Log.i("processing","");
			processFSK(getSound());
			
			if(Thread.interrupted()){
				break;
			}
		}
		
		//Log.i("vector size", byteBuffer.size() + " byteBuffer element size:" + byteBuffer.get(2).length);
	}
	
	private int alignSignal(double[] data,int[] peaks){
		int alignedIndex=0;
		int maxPeakCount = 0;
		int maxPeakIndex = 0;
		int i = findNextNonZero(peaks,0);
		if(i>0) i=i-1;
		i = i*N_POINTS;
		int end = i+(N_POINTS*6);
		if(end>data.length) end = data.length;
		int startIndex = i;
		while(startIndex<end){
			int endIndex = startIndex + (N_POINTS*4);
			int n = countPeaks(data, startIndex, endIndex);
			if(n>maxPeakCount){
				maxPeakCount = n;
				maxPeakIndex = startIndex;
			}
			startIndex++;
		}
		if(maxPeakCount>=32)
			alignedIndex=maxPeakIndex;
		return alignedIndex;
	}
	
	public synchronized void addSound(byte[] sound, int nBytes){
		byte[] data = new byte[nBytes];
		for (int i = 0; i < nBytes; i++) {
			data[i] = sound[i];
		}
		byteBuffer.add(data);
	}
	
	private synchronized byte[] getSound(){
		// returns the first sound part and removes it from the buffer
		// or null if there is no sound in the buffer
		if (byteBuffer.size()>0)
			return (byte[])this.byteBuffer.remove(0);
		else
			return null;
	}
	
	private void processFSK(byte[] audioData){
		
		if (signalDetected(audioData)){
			double[] audioSamples = byte2double(audioData);
			//count peaks 
			int[] nPeaks = processSound(audioSamples,0);
			int sIndex = alignSignal(audioSamples,nPeaks);
			nPeaks = processSound(audioSamples,sIndex);
			
			for (int i = 0; i < nPeaks.length; i++) {
				//Log.i("peaks per double", nPeaks[i]+"");
			}
			
			//demodulate
			int[] bits = parseBits(nPeaks);
			
			for (int i = 0; i < bits.length; i++) {
				//Log.i("bits", bits[i]+"");
			}
			
			//int array to byte array
			/*
			byte[] packet = GetBytes(bits);
			
			Log.i("packet length:", packet.length+"");
			if(packet.length!=12){
				byte[] pack = new byte[12];
				for (int i = 0; i < packet.length; i++) {
					pack[i+12-pack.length] = packet[i];
				}
				handleData(pack);
			} else{
				handleData(packet);
			}
			*/
			
			String str = "";
			for (int i = 0; i < bits.length; i++) {
				str = str+(bits[i]-1==1?"1":"0");
			}
			
			handleData(str);
			
		}
	}
	
	public byte[] GetBytes(int[] bits)	{

		byte[] output = new byte[bits.length / 8];

	    for (int i = 0; i < output.length; i++) {

	        for (int b = 0; b <= 7; b++) {
	            output[i] |= (byte)((bits[i * 8 + b]>>1) << (7 - b));
	        }
	    }

	    return output;

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

	private int[] processSound(double[] sound,int sIndex){
		
		// split the sound array into slots of N_POINTS and calculate the number of peaks
		int nPoints = N_POINTS;
		int nParts = (sound.length-sIndex) / nPoints;
		int[] nPeaks = new int[nParts]; 
		int startIndex = sIndex;
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
			
		//Log.i(TAG, "signalDetected()=" + signalDetected);
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


	private void handleData(String buf){
		
		int error = bitErrors("");
		
		//get packet from buffer if there is one
		byte[] data = findPacket(buf);
		
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
			
			Log.i("reading", "start");
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

	private byte[] findPacket(String bitstring){
		
		/*
		Log.i("packet: ", byteToString(buffer));
		
		//preamble string
		String preamb = byteToString(preamble);
		int pos = 0;
		byte[] packet;
		
		if(init){
			
			System.arraycopy(buffer, 0, searchBuffer, bufferSizeBytes, bufferSizeBytes);
			Log.i("packet in searchBuffer: ", byteToString(searchBuffer));
			init = false;
		} else {
			
			System.arraycopy(searchBuffer, bufferSizeBytes, searchBuffer, 0, buffer.length);
			System.arraycopy(buffer, 0, searchBuffer, bufferSizeBytes, buffer.length);
			
			//string to search in
			String search = byteToString(searchBuffer);
				
			//get position of first occurence of preamble
			pos = search.indexOf(preamb);
			Log.i("Pos", pos+"");
			if(pos!=-1){
				packet = getPacket(search.substring(pos+preamb.length()));
				return packet;
			}
		}
		return null;
		*/
		
		searchString = searchString+""+bitstring;
		
		String preamb = byteToString(preamble);
		int pos = 0;
		byte[] packet;
		
		//get position of first occurence of preamble
		pos = searchString.indexOf(preamb);
		Log.i("Pos", pos+"");
		//Log.i("bit string received so far: ", searchString);
		if(pos!=-1){
			String packetString = searchString.substring(pos+preamb.length());
			if(packetString.length()>=12*8){
				packet = getPacket(packetString);
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
		
		Log.i("dest: ", mHeader.getDest());
		
		Log.i("src: ", mHeader.getSrc());
		
		Log.i("length: ", mHeader.getLength()+"");
		
		//create array for data bytes
		if(mHeader.getLength()>0&&length+12*8<str.length()){
			byte[] data = StringToBytes(str.substring(12*8,12*8+length));
			Log.i("length: ", new String(data));
			return data;
		}
		
		
		Log.i("searchStr length: ", str.length()+"");
		/*
		if(str.length()>8050){
			byte[] data = StringToBytes(str.substring(12*8,8016));
			return data;
		}
		*/
		
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
