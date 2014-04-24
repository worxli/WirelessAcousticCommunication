package edu.uw.wirelessacousticcommunication.receiver;

import android.R.bool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	//listener thread
	private Thread listenerThread;
	private boolean mListening = false;
	private boolean mMeasuring = false;
	
	//handler for communication from listener thread back to UI thread
	Handler handler = new Handler(){
	    @Override
	    public void handleMessage(Message msg){
	        if(msg.what == 0){
	            setText(msg.getData().getString("msg"));
	        } else {
	        	setErrorRate(msg.getData().getInt("errors"));
	        }
	    }
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	//start listening to the surrounding -> start new thread
	@SuppressLint("NewApi")
	public void listen(View v){
		
		Button mButton = (Button) findViewById(R.id.listen);
		Button mButton2 = (Button) findViewById(R.id.measure);
		
		if(v.getId()==R.id.listen){
		
			if(mListening){
				
				mButton.setText("Start listening");
				mListening = false;
				mButton2.setEnabled(true);
				
				listenerThread.interrupt();
				
				
			} else {
				
				mButton.setText("Stop listening");
				mButton2.setEnabled(false);
				mListening = true;
				
				//check if thread exists
				if(listenerThread==null){
					listenerThread = new Thread(new ListenerThread(handler,this.getApplicationContext(),false));
					listenerThread.start();
				}
				
				Toast.makeText(getApplicationContext(), "started listening", Toast.LENGTH_SHORT).show();
			}
			
		} else {
		
			if(mMeasuring){
				
				mButton2.setText("Start measuring");
				mMeasuring = false;
				mButton.setEnabled(true);
				
				listenerThread.interrupt();
				
				
			} else {
				
				mButton2.setText("Stop measuring");
				mButton.setEnabled(false);
				mMeasuring = true;
				
				//check if thread exists
				if(listenerThread==null){
					listenerThread = new Thread(new ListenerThread(handler,this.getApplicationContext(),true));
					listenerThread.start();
				}
				
			}
		}
	}
	
	//display message on screen
	public void setText(String msg){
		TextView tv = (TextView) findViewById(R.id.message);
		tv.setText(msg);
	}
	
	//display message on screen
	public void setErrorRate(int errors){
		TextView tv = (TextView) findViewById(R.id.errors);
		tv.setText(errors+" bit errors, in 8016 bits payload");
	}	

}
