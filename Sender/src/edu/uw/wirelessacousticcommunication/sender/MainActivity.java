package edu.uw.wirelessacousticcommunication.sender;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import edu.uw.wirelessacousticcommunication.sender.WorkerThread;

public class MainActivity extends Activity {

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
	
	public void send(View v){
		
		EditText et = (EditText)findViewById(R.id.message);
		String msg = et.getText().toString();
		et.setText("");
		
		//send msg to worker thread via handler
		new WorkerThread().execute(msg);
		
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
		
	}

}
