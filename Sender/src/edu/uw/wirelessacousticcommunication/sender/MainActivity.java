package edu.uw.wirelessacousticcommunication.sender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		SeekBar sb = (SeekBar)findViewById(R.id.freqSlider);
	    sb.setMax(21);
	    sb.setProgress(0);
	    sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

	        @Override
	        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	        	
	            TextView text = (TextView)findViewById(R.id.freqText);
	            text.setText(1+progress+" kHz");
	        }

	        @Override
	        public void onStartTrackingTouch(SeekBar seekBar) {}

	        @Override
	        public void onStopTrackingTouch(SeekBar seekBar) {}

	    });
	    
	    SeekBar sb2 = (SeekBar)findViewById(R.id.symbolSlider);
	    sb2.setProgress(0); 
	    sb2.setMax(4);
	    sb2.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

	        @Override
	        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	        	
	            TextView text = (TextView)findViewById(R.id.symbolText);
	            text.setText((int)Math.pow(2, 1+progress)+" bits per symbol");
	            
	        }

	        @Override
	        public void onStartTrackingTouch(SeekBar seekBar) {}

	        @Override
	        public void onStopTrackingTouch(SeekBar seekBar) {}

	    });
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.main, menu);
		return false;
	}
	
	public void send(View v){
		
		EditText et = (EditText)findViewById(R.id.message);
		String msg = et.getText().toString();
		et.setText("");
		
		SeekBar sb = (SeekBar)findViewById(R.id.freqSlider);
		int frequency = (sb.getProgress()+1)*1000;
		
		SeekBar sb2 = (SeekBar)findViewById(R.id.symbolSlider);
		int bitspersymbol = (int)Math.pow(2,(sb2.getProgress()+1));
		
		//send msg to worker thread via handler
		new WorkerThread().execute(msg, frequency+"", ""+bitspersymbol);
		
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
		
	}
	
	public void sendFile(View v){
		BufferedReader reader = null;
		StringBuilder contents = new StringBuilder();
		
		SeekBar sb = (SeekBar)findViewById(R.id.freqSlider);
		int frequency = (sb.getProgress()+1)*1000;
		
		SeekBar sb2 = (SeekBar)findViewById(R.id.symbolSlider);
		int bitspersymbol = (int)Math.pow(2,(sb2.getProgress()+1));
		
		Log.d("reading", "start");
		try {
		    reader = new BufferedReader(
		        new InputStreamReader(getApplicationContext().getResources().openRawResource(R.raw.data))); 
		    
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
		
		//send msg to worker thread via handler
		new WorkerThread().execute(contents.toString(), frequency+"", ""+bitspersymbol);
	}

}
