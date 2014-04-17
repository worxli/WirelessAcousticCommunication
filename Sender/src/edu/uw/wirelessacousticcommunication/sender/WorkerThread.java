package edu.uw.wirelessacousticcommunication.sender;

import java.util.BitSet;
import java.util.Queue;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class WorkerThread extends AsyncTask<String, Void, Void> {
		
	/*
	 * convertData(String message) ** 
	 * slice message into chunks ** 
	 * convert to bit array ** 
	 * for each chunk: e.g. stuff bits, 
	 * create header and checksum / CRC, 
	 * add preamble
	 */
	private BitSet message;// = new BitSet();
	private final int duration = 5; // seconds
	private final int bitRate = 300;
    private final int sampleRate = 44100;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private double freqOfTone = 12001; // hz
    private final byte generatedSnd[] = new byte[2 * numSamples];
    
	private BitSet convertData(String msg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Void doInBackground(String... params) {
		
		//get message
		String msg = params[0];
		message= BitSet.valueOf(new long[]{Long.parseLong("1110010010110",2)});
		if(!msg.equals(""))
			freqOfTone=Double.parseDouble(msg);
		else
			freqOfTone=11999;
		//convert message to data packet
		BitSet packets = convertData(msg);
		
		//modulate
		//modulate(bits, carrier signal, bitspersymbol)
		Log.v("WORKER","Working!!!!!!!!!!!!!!!!!");
		genTone();
		playSound();
		return null;
	}
	
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
