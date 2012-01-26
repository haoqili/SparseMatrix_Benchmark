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
	Button button_udp_slave;
	Button button_udp_master;
	Button button_tcp_master;
	Button button_tcp_slave;
	
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
        
        button_udp_slave = (Button) findViewById(R.id.button_udp_slave);
        button_udp_slave.setOnClickListener(udp_slave_listener);
        
        button_udp_master = (Button) findViewById(R.id.button_udp_master);
        button_udp_master.setOnClickListener(udp_master_listener);
        
        button_tcp_master = (Button) findViewById(R.id.button_tcp_master);
        button_tcp_master.setOnClickListener(tcp_master_listener);
        
        button_tcp_slave = (Button) findViewById(R.id.button_tcp_slave);
        button_tcp_slave.setOnClickListener(tcp_slave_listener);
    
        // logging (Borrowed from Jason's Sonar app)
        receivedMessages = new ArrayAdapter<String>(this, R.layout.message);
        msgList = (ListView) findViewById(R.id.logList);
        msgList.setAdapter(receivedMessages);

    }
  

    /** UI Callbacks for Buttons **/
    private OnClickListener udp_slave_listener = new OnClickListener() {
    	public void onClick(View v) {
    		logm("########################");
    		logm("########################");
    		logm("clicked UDP slave waiting to recieve button");
    		// start thread to receive loop for UDP packets (blocks)
    		SlaveNetworkThread slave_th = new SlaveNetworkThread(logHandler);
    		slave_th.start(); // should loop to receive 
    	}
    };
    private OnClickListener udp_master_listener = new OnClickListener() {
		public void onClick(View v) {
    		logm("########################");
    		logm("########################");
    		logm("clicked UDP master benchmark button");
    		
    		// step 1. start its own slave network thread (blocks to receive)
    	    SlaveNetworkThread masters_slave_th = new SlaveNetworkThread(logHandler);
    		masters_slave_th.start();
    		
    		// step 2. starts its thread to receive UDP packet results from slaves (blocks)
    		MasterNetworkThread master_th = new MasterNetworkThread(logHandler);
    		master_th.start();
    		// step 3. send work to slaves
    		//		ParaApp myParaApp = new ParaApp();
    		//		myParaApp.startBenchmark();
    		master_th.distributeWork();
		}
    };
    private OnClickListener tcp_master_listener = new OnClickListener() {
    	public void onClick(View v) {
    		logm("########################");
    		logm("########################");
    		logm("clicked TCP master benchmark button");
    		// step 0. check that TCP_MY_PORT is set up correctly
    		Globals.printTCPPORTS();
    		
    		// step 1. send work to slaves (blocks to send - wait for connections)
    		// MasterTCPThread --> ParaApp --> nthreads TCPSendThread Threads
    		MasterTCPThread master_tcp = new MasterTCPThread(logHandler);
    		master_tcp.distributeWork();
    		
    		// step 2. Slaves establish connection to Master to get packets and reply
    		// SlaveTCPThread
    		//SlaveTCPThread masters_slave_th = new SlaveTCPThread(logHandler);
    		//masters_slave_th.start(); 
    		
    		// step 3. gets data back from slaves
    		//master_tcp.start();

    	}
    };
    private OnClickListener tcp_slave_listener = new OnClickListener() {
    	public void onClick(View v) {
    		logm("########################");
    		logm("########################");
    		logm("clicked TCP slave benchmark button");
    		//Slaves establish connection to Master to get packets and reply
    		// SlaveTCPThread
    		SlaveTCPThread masters_slave_th = new SlaveTCPThread(logHandler);
    		masters_slave_th.start(); 

    	}
    };
}
