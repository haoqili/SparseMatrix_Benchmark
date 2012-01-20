package edu.mit.haoqili.sparse_bench;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SparseMatrix_Benchmark extends Activity
{
	Button button_slave;
	Button button_master;

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
    
    }
    
    private void logm(String line){
    	Log.i("************ Main Activity: ", line);
    }
    /** UI Callbacks for Buttons **/
    private OnClickListener button_slave_listener = new OnClickListener() {
    	public void onClick(View v) {
    		logm("clicked slave waiting to recieve button");
    		// start thread to receive loop for UDP packets
    		SlaveNetworkThread slave_th = new SlaveNetworkThread();
    		slave_th.start(); // should loop to receive 
    	}
    };
    private OnClickListener button_master_listener = new OnClickListener() {
		public void onClick(View v) {
    		logm("clicked master benchmark button");
    		
    		// step 1. start its own slave network thread
    	    SlaveNetworkThread masters_slave_th = new SlaveNetworkThread();
    		masters_slave_th.start();
    		
    		// step 2. starts its thread to receive UDP packet results from slaves
    		MasterNetworkThread master_th = new MasterNetworkThread();
    		master_th.start();
    		// step 3. send work to slaves
    		//		ParaApp myParaApp = new ParaApp();
    		//		myParaApp.startBenchmark();
    		master_th.distributeWork();
		}
    };
}
