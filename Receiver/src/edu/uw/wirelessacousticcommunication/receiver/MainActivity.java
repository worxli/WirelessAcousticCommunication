package edu.uw.wirelessacousticcommunication.receiver;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
	
	
	//handler for communication from listener thread back to UI thread
	Handler handler = new Handler(){
	    @Override
	    public void handleMessage(Message msg){
	        if(msg.what == 0){
	            setText(msg.getData().getString("message"));
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
	public void listen(View v){
		
		Button mButton = (Button) findViewById(R.id.listen);
		
		if(mListening){
			
			mButton.setText("Start listening");
			mListening = false;
			
			listenerThread.interrupt();
			
			
		} else {
			
			mButton.setText("Stop listening");
			mListening = true;
			
			//check if thread exists
			if(listenerThread==null){
				listenerThread = new Thread(new ListenerThread(handler,this.getApplicationContext()));
				listenerThread.start();
			}
			
			Toast.makeText(getApplicationContext(), "started listening", Toast.LENGTH_SHORT).show();
		}
		
	}
	
	//display message on screen
	public void setText(String msg){
		TextView tv = (TextView) findViewById(R.id.message);
		tv.setText(msg);
	}

}
