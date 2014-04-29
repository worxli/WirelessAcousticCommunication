package edu.uw.wirelessacousticcommunication.sender;

import java.nio.ByteBuffer;
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;

public class WorkerThread extends AsyncTask<String, Void, Void> {
	
	//we don't know destination so just broadcast
	private final String destIP = "255.255.255.255";
    private int sampleRate = 44100;
    
    /*
	 * convertData(String message) ** 
	 * convert to byte array ** 
	 * create header and calculate CRC
	 * put all in a big byte array and then in a bitset
	 * add preamble
	 */
	
	@SuppressLint("NewApi")
	private byte[] convertData(String msg, int start, int end) {
		
		
		msg = msg.substring(start, end);
		/*
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
		
		//create payload: header + data + CRC
		//actually we just add the preamble in front of it
		//preamble 1010'1111
		int prelen = 1;
		byte[] payload = new byte[prelen+header.length+data.length];
		
		payload[0] = (byte) 0xaf;
		
		for (int i = 0; i < header.length; i++) {
			payload[i+prelen] = header[i];
		}
		for (int i = 0; i < data.length; i++) {
			payload[i+header.length+prelen] = data[i];
		}
		
		byte[] packet = new byte[payload.length*8];
		
		int idx = 0;
		for (int i = 0; i < payload.length; i++) {
			for (int j = 7; j >= 0 ; j--) {
				packet[idx++] = (byte) (isBitSet(payload[i],j)?1:0);
			}
		}
		*/
		
		//new just put preamble and 12 byte data
		//get bytes of message
		byte[] data = msg.getBytes();
		int prelen = 1;
		byte[] payload = new byte[prelen+1+data.length];
		
		payload[0] = (byte) 0xaf;
		payload[1] = (byte) data.length;
		
		for (int i = 0; i < data.length; i++) {
			payload[i+prelen+1] = data[i];
			//Log.i("data:", (char)data[i]+"");
		}
		
		byte[] packet = new byte[payload.length*8];
		
		int idx = 0;
		for (int i = 0; i < payload.length; i++) {
			for (int j = 7; j >= 0 ; j--) {
				packet[idx++] = (byte) (isBitSet(payload[i],j)?1:0);
			}
		}
		
		//this is our "bitarray" of the packet
		return packet;
	}
	
	private static Boolean isBitSet(byte b, int bit)
	{
	    return (b & (1 << bit)) != 0;
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
		String msg = params[0];
		
		msg = "aaaa";
		
		int le = msg.length();
		int start = 0;
		int it = 4;
		int end = (it>le)?le:it;
		for (int i = 0; i < le; i=i+it) {
			
			//new byte[]{(byte)1, (byte)0}; //
			byte[] packet = convertData(msg, start, end);
			sendDataFSK(packet, 0, packet.length);
			Log.i("message", packet.length+"");
			start = start+it;
			end = (end+it>le)?le:end+it;
			
		}

		return null;
	}
	
	private void sendDataFSK(byte[] message, int start, int end) {
		

		//freq low
		int lowfreq = 3150;
		int highfreq = 6300;
		int spb = 4; //slots per bit -> # periods of slowest wave per bit 
		double[] low = carrierWave(lowfreq,spb);
		double[] high = carrierWave(highfreq,spb);

		/*
		double[] msg = new double[0];
		for (int i = start; i < end; i++) {
			if(message[i]==1){
				//1 -> high freq
				msg = concatenateArrays(msg, high);
			} else {
				//0 -> low freq
				msg = concatenateArrays(msg, low);
			}
		}
		*/
		
		int lowlen = low.length;
		double[] msg = new double[message.length*lowlen];
		for (int i = start; i < message.length; i++) {
			if(message[i]==1){
				//1 -> high freq
				for (int j = 0; j < lowlen; j++) {
					msg[i*lowlen+j] = high[j];
					//Log.d("message", msg[i*lowlen+j]+"");
				}
			} else {
				//0 -> low freq
				for (int j = 0; j < lowlen; j++) {
					msg[i*lowlen+j] = low[j];
					//Log.d("message", msg[i*lowlen+j]+"");
				}
			}
		}
		
		for (int i = 0; i < msg.length; i++) {
			//Log.d("message", msg[i]+"");
		}
		
		
		short[] sound = new short[msg.length*2];
		int idx=0;
		for (final double mval : msg) {
			sound[idx++] = (short) (mval*31000); //apply highest amplitude
        }
		
		final AudioTrack aT = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, sound.length,
                AudioTrack.MODE_STATIC);
        aT.write(sound, 0, sound.length);
        aT.play();
        
        try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


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
	

}
