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
		
		//send msg to worker thread via handler
		new WorkerThread().execute(msg);
		
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
		
	}
	
	public void sendFile(View v){
		BufferedReader reader = null;
		StringBuilder contents = new StringBuilder();
		
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
		
		Toast.makeText(getApplicationContext(), "reading file finished", Toast.LENGTH_SHORT).show();
		
		//send msg to worker thread via handler
		new WorkerThread().execute(contents.toString());
	}

}
