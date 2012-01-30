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
	
	private ParaApp tcpParaApp;
	
    // UI - Log display
    ListView msgList;
	ArrayAdapter<String> receivedMessages;

    private final Handler mainHandler = new Handler() {
    	@Override
    	public void handleMessage(Message msg){
    		switch (msg.what){
    		case Globals.MSG_LOG: // logging stuff
    			String text = (String) msg.obj;
    			receivedMessages.add(text);
    			break;
    		case Globals.MSG_TCP_REPLY:
    			SparseRunner sr = (SparseRunner) msg.obj;
    			logm(" << in main activity, sr's id: " + sr.id);
    			tcpParaApp.handleCompletedSparseRunner(sr);
    			break;
    		}
    	}
    };
    public void logm(String line){
    	Log.i(TAG, line);
		mainHandler.obtainMessage(Globals.MSG_LOG, TAG+": "+line).sendToTarget();
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
    
        // logging
        receivedMessages = new ArrayAdapter<String>(this, R.layout.message);
        msgList = (ListView) findViewById(R.id.logList);
        msgList.setAdapter(receivedMessages);

    }
  

    /** UDP blocks on receive, 
     * so we have to start the slaves first to get ready to receive stuff 
     * we also have to set up master's receive of slave's replies
     * before finally we start handing out work to the slaves **/
    private OnClickListener udp_slave_listener = new OnClickListener() {
    	public void onClick(View v) {
    		logm("########################");
    		logm("########################");
    		logm("clicked UDP slave waiting to recieve button");
    		// start thread to receive loop for UDP packets (blocks)
    		UDPslaveThread udp_slave_th = new UDPslaveThread(mainHandler);
    		udp_slave_th.start(); // should loop to receive 
    	}
    };
    private OnClickListener udp_master_listener = new OnClickListener() {
		public void onClick(View v) {
    		logm("########################");
    		logm("########################");
    		logm("clicked UDP master benchmark button");
    		
    		// step 1. start master's own slave thread (blocks to receive)
    	    UDPslaveThread udp_masters_slave_th = new UDPslaveThread(mainHandler);
    		udp_masters_slave_th.start();
    		
    		// step 2. starts its thread to receive UDP packet results from slaves (blocks)
    		// UDPmasterThread, contains UDP_ParaApp --> ParaApp, sends UDP work packets to slaves
    		UDPmasterThread master_th = new UDPmasterThread(mainHandler);
    		master_th.start();
    		// step 3. send work to slaves, by calling UDP_ParaApp.startBenchmark();
    		master_th.distributeWork();
    		// UDPmasterThread waits for slaves' finished replies, 
    		// and assembles the final product through its private udpParaApp
		}
    };
    
    /** TCP blocks on the server waiting for clients to accept connections (to the server),
     * so we have to set up the master to wait for slaves 
     * to accept their connections to the master **/
    private OnClickListener tcp_master_listener = new OnClickListener() {
    	public void onClick(View v) {
    		logm("########################");
    		logm("########################");
    		logm("clicked TCP master benchmark button");
    		// step 0. check that TCP_MY_PORT is set up correctly
    		Globals.printTCPPORTS();
    		
    		// step 1. send work to slaves (blocks to send - wait for connections)
    		// ParaApp --> send nthreads of TCPmasterTranceiveThread(s)
    		// btw, No need for TCPmasterThread because TCP doesn't block on receive
    		//      If there were a "TCPmasterThread", it would only have the 2 lines below
    		tcpParaApp = new ParaApp(mainHandler); 
    		tcpParaApp.startBenchmark(false);
    		// TCPmasterTranceive UDPmasterThread waits for slaves' finished replies
    		// and sends a handler message to this main activity to let tcpParaApp handle it

    		
    		// step 2. Slaves establish connection to Master to get packets and reply
    		// SlaveTCPThread
    		TCPslaveThread tcp_masters_slave_th = new TCPslaveThread(mainHandler);
    		tcp_masters_slave_th.start(); 
    		
    	}
    };
    private OnClickListener tcp_slave_listener = new OnClickListener() {
    	public void onClick(View v) {
    		logm("########################");
    		logm("########################");
    		logm("clicked TCP slave benchmark button");
    		//Slaves establish connection to Master to get packets and reply
    		// SlaveTCPThread
    		TCPslaveThread tcp_slave_th = new TCPslaveThread(mainHandler);
    		tcp_slave_th.start(); 

    	}
    };
}
