package edu.mit.haoqili.sparse_bench;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class SparseMatrix_Benchmark extends Activity
{
	final static private String TAG = "##### Main Activity";
	Button button_slave;
	Button button_master;
	
    // UI - Log display
    ListView msgList;
	ArrayAdapter<String> receivedMessages;

    // logging stuff :
    // log messages handler
    private final Handler logHandler = new Handler() {
    	@Override
    	public void handleMessage(Message msg){
    		String text = (String) msg.obj;
    		receivedMessages.add(text);
    	}
    };
    public void logm(String line){
    	Log.i(TAG, line);
		logHandler.obtainMessage(0, TAG+": "+line).sendToTarget();
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        button_slave = (Button) findViewById(R.id.button_slave);
        button_slave.setOnClickListener(button_slave_listener);
        
        button_master = (Button) findViewById(R.id.button_master);
        button_master.setOnClickListener(button_master_listener);
    
        // logging (Borrowed from Jason's Sonar app)
        receivedMessages = new ArrayAdapter<String>(this, R.layout.message);
        msgList = (ListView) findViewById(R.id.logList);
        msgList.setAdapter(receivedMessages);

    }
  

    /** UI Callbacks for Buttons **/
    private OnClickListener button_slave_listener = new OnClickListener() {
    	public void onClick(View v) {
    		logm("########################");
    		logm("########################");
    		logm("clicked slave waiting to recieve button");
    		// start thread to receive loop for UDP packets
    		SlaveNetworkThread slave_th = new SlaveNetworkThread(logHandler);
    		slave_th.start(); // should loop to receive 
    	}
    };
    private OnClickListener button_master_listener = new OnClickListener() {
		public void onClick(View v) {
    		logm("########################");
    		logm("########################");
    		logm("clicked master benchmark button");
    		
    		// step 1. start its own slave network thread
    	    SlaveNetworkThread masters_slave_th = new SlaveNetworkThread(logHandler);
    		masters_slave_th.start();
    		
    		// step 2. starts its thread to receive UDP packet results from slaves
    		MasterNetworkThread master_th = new MasterNetworkThread(logHandler);
    		master_th.start();
    		// step 3. send work to slaves
    		//		ParaApp myParaApp = new ParaApp();
    		//		myParaApp.startBenchmark();
    		master_th.distributeWork();
		}
    };
}
