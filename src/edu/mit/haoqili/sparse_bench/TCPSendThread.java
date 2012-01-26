package edu.mit.haoqili.sparse_bench;

import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Handler;
import android.util.Log;

public class TCPSendThread extends Thread {
	private static final String TAG = "*** TCPSendThread";

	private ServerSocket serverSocket;
	private byte[] sendData;
	
	Handler logHandler;
	public void logm(String line) {
		Log.i(TAG, line);
		logHandler.obtainMessage(0, TAG+": "+line).sendToTarget();
	}

	/** NetworkThread constructor */
	public TCPSendThread(ServerSocket ss, byte[] data, Handler ha) {
		serverSocket = ss;
		sendData = data;
		
		logHandler = ha;
	}


	@Override
	public void run() {
		try {
			logm("1. Waiting for slave to accept master's connection ...");
			// accept slave's connection
			Socket sock = serverSocket.accept();
			
			logm("2. Connection accepted :)");
			
			// send the slave's data work
			OutputStream os = sock.getOutputStream();
			logm("3. send data");
			os.write(sendData);
			logm("4. flush os");
			os.flush();
			//logm("5. close socket");
			//sock.close();
			
		} catch (IOException e) {
			logm("Master Send serverSocket not accepting :( e:" + e.getMessage());
			System.exit(5);
		}
	} // end run()
}
