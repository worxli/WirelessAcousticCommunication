package edu.uw.wirelessacousticcommunication.sender;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class WorkerThread extends AsyncTask<String, Void, Void> {
	
	//we don't know destination so just broadcast
	private final String destIP = "255.255.255.255";
		
	private String message;// = new BitSet();
	private int duration = 1; // seconds
	private int bitRate = 300;
    private int sampleRate = 44100;
    private int numSamples;
    private double sample[];
    private double freqOfTone = 12001; // hz
    private int bitsPerSymbol;
    private byte generatedSnd[];
    private int sampleBitRep[];
    private int maxAmp=0;
    
    //temp header buffer
    private byte[] headerBuf = new byte[12]; 
    
    
    /*
	 * convertData(String message) ** 
	 * convert to byte array ** 
	 * create header and calculate CRC
	 * put all in a big byte array and then in a bitset
	 * add preamble
	 */
	
	@SuppressLint("NewApi")
	private byte[] convertData(String msg) {
		
		//get IP, if not available -> 0.0.0.0
		String srcIP = "1.1.1.1";//Utils.getIPAddress(true);
		
		//get bytes of message
		byte[] data = msg.getBytes();
		
		//src string to byte array
		String[] srcStr = srcIP.split("\\.");
		byte[] srcBytes = new byte[5];
		for (int i = 0; i < srcStr.length; i++) {
			srcBytes[i] = (byte)Integer.parseInt(srcStr[i]);
		}		
		
		//dest string to byte array
		String[] destStr = destIP.split("\\.");
		byte[] destBytes = new byte[5];
		for (int i = 0; i < srcStr.length; i++) {
			destBytes[i] = (byte)(int)Integer.parseInt(destStr[i]);
		}
		
		//Length of payload to byte array
		int len = data.length;
		byte[] length = ByteBuffer.allocate(4).putInt(len).array(); 
		
		//create 12 byte header
		byte[] header = new byte[12];
		
		for (int i = 0; i < srcBytes.length; i++) {
			header[i] = srcBytes[i];
		}
		for (int j = 0; j < destBytes.length; j++) {
			header[j+4] = destBytes[j];
		}
		for (int k = 0; k < length.length; k++) {
			header[k+8] = length[k];
		}
		
		String heaString = byteToString(length);
		System.out.println("len length: "+heaString.length()+" bytes:"+heaString.length()/8+" bits:"+heaString);
		

		//create payload: header + data + CRC
		//actually we just add the preamble in front of it
		//preamble 1111'1111 1111'1111 -> so in byte rep.: 255 255
		int prelen = 2;
		byte[] payload = new byte[prelen+header.length+data.length+8];
		
		payload[0] = (byte) 0xff;
		payload[1] = (byte) 0xff;
		
		//String preaString = byteToString(payload);
		//System.out.println("preamble length: "+preaString.length()+" bytes:"+preaString.length()/8+" bits:"+preaString);
		

		for (int i = 0; i < header.length; i++) {
			payload[i+prelen] = header[i];
		}
		for (int i = 0; i < data.length; i++) {
			payload[i+header.length+prelen] = data[i];
		}
		
		String headString = byteToString(header);
		System.out.println("header length: "+headString.length()+" bytes:"+headString.length()/8+" bits:"+headString);
		
		// checksum with the specified array of bytes
		Checksum checksum = new CRC32();
		checksum.update(payload, 0, payload.length-8);
		byte[] check = ByteBuffer.allocate(8).putLong(checksum.getValue()).array();
		
		for (int i = 0; i < check.length; i++) {
			payload[i+payload.length-8] = check[i];
		}
		
		String checkString = byteToString(check);
		System.out.println("check length: "+checkString.length()+" bytes:"+checkString.length()/8+" bits:"+checkString);
		
		checkString = byteToString(payload);
		System.out.println("payload length: "+checkString.length()+" bytes:"+checkString.length()/8+" bits:"+checkString);
		
		//this is our "bitarray" of the packet
		return payload;
	}

	public String byteToString(byte[] b){
		
		String s = "";
		
		for(int j=0; j<b.length; j++){
			
			s = s+String.format("%8s", Integer.toBinaryString(b[j] & 0xFF)).replace(' ', '0');
		}
		
		return s;
	}


	@SuppressLint("NewApi")
	@Override
	protected Void doInBackground(String... params) {
		
		//get message
		//String msg = "My name is Fahad Pervaiz. This is a test message. My name is Fahad Pervaiz. This is a test message. My name is Fahad Pervaiz. This is a test message. My name is Fahad Pervaiz. This is a test message. My name is Fahad Pervaiz. This is a test message. My name is Fahad Pervaiz. This is a test message. My name is Fahad Pervaiz. This is a test message. My name is Fahad Pervaiz. This is a test message. My name is Fahad Pervaiz. This is a test message. My name is Fahad Pervaiz. This is a test message. My name is Fahad Pervaiz. This is a test message.";//params[0];
		String msg = params[0];
		int freq = Integer.parseInt(params[1]);
		
		//debug
		freqOfTone = freq;
		
		//get bits per symbol
		int bps = Integer.parseInt(params[2]);
		Log.v("WORKER",params[2]+"=param - bps="+bps);
		bitsPerSymbol=bps;
		//convert message to data packet

		byte[] packet = convertData(msg);
		message=this.byteToString(packet);
		
		Log.v("WORKER","Working!!!!!!!!!!!!!!!");
		//sendData();
		sendDataFSK("0101");

		return null;
	}
	
	private void sendDataFSK(String message) {

		//freq low
		int lowfreq = 3150;
		int highfreq = 6300;
		int spb = 2; //slots per bit -> # periods of slowest wave per bit 
		double[] low = carrierWave(lowfreq,spb);
		double[] high = carrierWave(highfreq,spb);

		double[] msg = new double[0];
		for (int i = 0; i < message.length(); i++) {
			if(message.charAt(i)=='1'){
				//1 -> high freq
				msg = concatenateArrays(msg, high);
			} else {
				//0 -> low freq
				msg = concatenateArrays(msg, low);
			}
		}
		for (int i = 0; i < msg.length; i++) {
			//Log.d("samples", msg[i]+"");
		}
		
		short[] sound = new short[msg.length*2];
		int idx=0;
		for (final double mval : msg) {
            //final short val = (short) (mval*10000); //multiply with highest amplitude and creat short
            // in 16 bit wave PCM, first byte is the low order byte
            //sound[idx++] = (byte) (val & 0x00ff);
            //sound[idx++] = (byte) ((val & 0xff00) >>> 8);
			//Log.d("samples", "double:"+mval+" int: "+(int)(mval*31000)+" short: "+(short) ((short)(int)(mval*31000)));
			sound[idx++] = (short) (mval*31000);
        }
		for (int i = 0; i < sound.length; i++) {
			//Log.d("samples", sound[i]+"");
		}
		
		final AudioTrack aT = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, sound.length,
                AudioTrack.MODE_STATIC);
        aT.write(sound, 0, sound.length);
        aT.play();


	}
	
	private double[] concatenateArrays(double[] a1, double[] a2){
		double[] data = new double[a1.length+a2.length];
		for (int i = 0; i < a1.length; i++) {
			data[i] = a1[i];
		}
		for (int i = 0; i < a2.length; i++) {
			data[i + a1.length] = a2[i];
		}
		return data;
	}

	private double[] carrierWave(int freq, int sbp) {
		// TODO Auto-generated method stub
		int AUDIO_SAMPLE_FREQ = 44100;
	    int samplesOne = 28*sbp; //slowest freq=1.575 kHz -> 14peak = 28 samples per 
    	double[] wave = new double[samplesOne];
    	for (int i = 0; i < samplesOne; ++i) {
            wave[i] = Math.sin(2 * Math.PI * i/(AUDIO_SAMPLE_FREQ/freq));
        }

	    return wave;
	}
	
	private double[] carrierWaveTemp(int freq, int spb) {
		// TODO Auto-generated method stub
		int AUDIO_SAMPLE_FREQ = 44100;
	    int samplesOne = 28; //slowest freq=1.575 kHz -> 14peak = 28 samples per 
	    int samplesTwo = 64; //for 2 bps
	    double samplingTime=1.0/AUDIO_SAMPLE_FREQ;
	    double duration=68*samplingTime;
	    int numSamples=(int)(duration*AUDIO_SAMPLE_FREQ);
	    
    	double[] wave;// = new double[samplesOne*spb];
    	wave = new double[numSamples];
    	for (int i = 0; i < numSamples; ++i) {
            wave[i] = Math.sin(2 * Math.PI * i * freq * samplingTime);
            //Log.v("GEN",""+wave[i]);
    	}
	    return wave;
	}

	public void sendData(){
		//ASK MODULATION
		/*genCarrierSamples();
		ModulateASK(message);
		genWave();
		playSound();*/
	}
	//ASK Method
	public void genCarrierSamples(){
        // fill out the array
		//duration=(int)(Math.ceil(this.message.length()/this.bitsPerSymbol)/this.freqOfTone);
		numSamples=(int)(Math.ceil(this.message.length()/this.bitsPerSymbol) * Math.ceil(sampleRate/this.freqOfTone));
		sample = new double[numSamples];
		generatedSnd = new byte[2 * numSamples];
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i * (freqOfTone/sampleRate));
        }
    }
	//ASK Method
	public void genWave(){
		// convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
		int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767/maxAmp));
            Log.v("WORKEREE",sampleBitRep[idx/2]+" sin="+dVal+" val="+val);
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
	}
	//ASK Method
	public void ModulateASK(String bits){
		int samplesPerSymbol=(int) Math.ceil(sampleRate/freqOfTone);
		Log.v("WORKER","Start of Amp Cal Mod");
		Integer[] amp=calcAmp(bits,samplesPerSymbol,bitsPerSymbol);
		Log.v("WORKER","Start of Multi Mod");
		for (int i = 0; i < numSamples; i++) {
            sample[i] = sample[i]*amp[i];
        }
		Log.v("WORKER","END of Mod");
	}
	//ASK Method
	public Integer[] calcAmp(String bits, int sps, int bps){
		//String bits=this.byteToString(bitArray);
		Integer[] amp=new Integer[numSamples];
		sampleBitRep=new int[numSamples];
		int A=0;
		maxAmp=0;
		int counter=0;
		int tc=0;
		Log.v("WORKER","SPS="+sps);
		for(int i=0;i<bits.length();i=i+bps){
			String bitStr="";
			for(int j=0;j<bps;j++){
				bitStr=bitStr+""+bits.charAt(i+j);
			}
			A=Integer.parseInt(bitStr, 2)+1;
			maxAmp=Math.max(maxAmp,A);
			//Log.v("WORKER",A+"=A - bitStr"+bitStr);
			//Log.v("WORKER",counter+"=counter - sps="+sps);
			for(int j=0;j<sps;j++){
				amp[j+counter]=A;
				sampleBitRep[j+counter]=A;
			}
			counter=counter+sps;
			tc++;
		}
		//Log.v("WORKER","amp.size()="+counter);
		//Log.v("WORKER","tc="+tc);
		return amp;
	}
	//NOT IN USE
	public void genTone(){
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i * (freqOfTone/sampleRate));
        }
        double msgSample[] = new double[numSamples];
        
        for (int i=0; i < numSamples; ++i){
        	msgSample[i] = sampleRate/bitRate;
        }
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }
	
	public void playSound(){
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
    }

}
