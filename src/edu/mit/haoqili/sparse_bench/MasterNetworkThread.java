package edu.mit.haoqili.sparse_bench;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import android.util.Log;

public class MasterNetworkThread extends Thread {
	private static final String TAG = "...... MasterNetworkThread";

	private ParaApp myParaApp = null;
	
	private boolean isEnd = false;
	
	// UDP over IPv4 Networking
	private static final int MAX_PACKET_SIZE = 110592; // bytes
	private DatagramSocket masterSocket;
	private boolean socketOK = true;
	
	public void logm(String line) {
		Log.i(TAG, line);
	}

	/** NetworkThread constructor */
	public MasterNetworkThread() {
		// step 2. starts its thread to receive UDP packet results from slaves
		restartSocket();
	}
	
	/** Send work to the slaves **/
	public void distributeWork(){
		myParaApp = new ParaApp();
		myParaApp.startBenchmark();		
	}
	

	/** Start of r */
	public void restartSocket() {
		if (masterSocket != null && !masterSocket.isClosed()) // doesn't gets here
			masterSocket.close();

		// Create UDP socket for receiving packets
		try {
			masterSocket = new DatagramSocket(Globals.MASTER_PORT);
			/* Are these things necessary?
			 * mySocket.setBroadcast(true);
			logm(String.format(
					"Initial socket buffer sizes: %d receive, %d send",
					mySocket.getReceiveBufferSize(),
					mySocket.getSendBufferSize()));
			//mySocket.setReceiveBufferSize(MAX_PACKET_SIZE);
			mySocket.setSendBufferSize(MAX_PACKET_SIZE);
			logm(String.format(
					"Set socket buffer sizes to: %d receive, %d send",
					mySocket.getReceiveBufferSize(),
					mySocket.getSendBufferSize()));
			*/
		} catch (Exception e) {
			logm("Cannot open socket :(  e: " + e.getMessage());
			socketOK = false;
			System.exit(5);
		}
	}

	/** If not socketOK, then receive loop thread will stop */
	boolean socketIsOK() {
		return socketOK;
	}

	/** Thread's receive loop for UDP packets */
	@Override
	public void run() {
		byte[] receiveData = new byte[MAX_PACKET_SIZE];

		while (socketOK) {
			DatagramPacket dPacket = new DatagramPacket(receiveData,
					receiveData.length);
			try {
				masterSocket.receive(dPacket);
			} catch (IOException e) {
				logm("mySocket.receive broke :(");
				Log.e(TAG, "Exception on mySocket.receive: " + e.getMessage());
				socketOK = false;
				System.exit(5);
			}
			
			logm("master received a reply of length: " + dPacket.getLength());

			//analogous to diplomamatrix UserApp.java's
			//    handleDSMReply() 
			long startTime = System.currentTimeMillis();
			SparseRunner sr = null;
			try {
				sr = srFromBytes(receiveData);
				// handle completed SparseRunner
				if (myParaApp == null) {
					logm("Whoa MasterNetworkThread received data even before ParaApp sent out data");
					throw new NullPointerException("no myParaApp");
				}
				isEnd = myParaApp.handleCompletedSparseRunner(sr);
				if (isEnd){
					break;
				}
			} catch (Exception e) {
				logm("Exception deserializing SparseRunner! e: " + e);
				e.printStackTrace();
			}
			
		} // end while(socketOK)
		logm("isEnd is here!!!!!! closing socket ...");
		masterSocket.close();
	} // end run()
	
	/**
	 * Deserialize an Serializable S.R. from a byte array
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws OptionalDataException
	 */
	public SparseRunner srFromBytes(byte[] d) throws OptionalDataException,
			ClassNotFoundException, IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(d);
		ObjectInputStream ois = new ObjectInputStream(bis);
		SparseRunner a = (SparseRunner) ois.readObject();
		ois.close();
		return a;
	}
	

}
