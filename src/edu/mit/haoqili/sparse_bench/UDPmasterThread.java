package edu.mit.haoqili.sparse_bench;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import android.os.Handler;
import android.util.Log;

/** calls ParaApp to send UDP work packets to slaves
 *  and in here receives their finished UDP reply packets
 */
public class UDPmasterThread extends Thread {
	private static final String TAG = "*** UDPmasterThread";

	private ParaApp udpParaApp = null;
	
	private boolean isEnd = false;
	
	// UDP over IPv4 Networking
	private DatagramSocket udpMasterReceiveSock;
	
	private boolean socketOK = true;
	
	Handler mainHandler;
	public void logm(String line) {
		Log.i(TAG, line);
		mainHandler.obtainMessage(Globals.MSG_LOG, TAG+": "+line).sendToTarget();
	}

	/** NetworkThread constructor */
	public UDPmasterThread(Handler ha) {
		mainHandler = ha;
		
		// step 2. starts its thread to receive UDP packet results from slaves
		restartSocket();
	}
	
	/** Send work to the slaves **/
	public void distributeWork(){
		udpParaApp = new ParaApp(mainHandler);
		udpParaApp.startBenchmark(true);		
	}
	

	/** Start of r */
	public void restartSocket() {
		if (udpMasterReceiveSock != null && !udpMasterReceiveSock.isClosed()) // doesn't gets here
			udpMasterReceiveSock.close();

		// Create UDP socket for receiving packets
		try {
			udpMasterReceiveSock = new DatagramSocket(Globals.UDP_MASTER_PORT);
			/* Are these things necessary?
			 * mySocket.setBroadcast(true);
		logm(String.format(
				"Initial socket buffer sizes: %d receive, %d send",
				mySocket.getReceiveBufferSize(),
				mySocket.getSendBufferSize()));
		//mySocket.setReceiveBufferSize(Globals.MAX_PACKET_SIZE);
		mySocket.setSendBufferSize(Globals.MAX_PACKET_SIZE);
		logm(String.format(
				"Set socket buffer sizes to: %d receive, %d send",
				mySocket.getReceiveBufferSize(),
				mySocket.getSendBufferSize()));
			 */
		} catch (Exception e) {
			logm("Cannot open UDP socket :(  e: " + e.getMessage());
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
		byte[] receiveData = new byte[Globals.MAX_PACKET_SIZE];

		while (socketOK) {
			DatagramPacket dPacket = new DatagramPacket(receiveData,
					receiveData.length);
			try {
				udpMasterReceiveSock.receive(dPacket); // blocks
			} catch (IOException e) {
				logm("mySocket.receive broke :(");
				Log.e(TAG, "Exception on mySocket.receive: " + e.getMessage());
				socketOK = false;
				System.exit(5);
			}
			
			logm("master received a reply of length: " + dPacket.getLength());
			// handle the reply, put them together
			// analogous to diplomamatrix UserApp.java's
			//    handleDSMReply() 
			SparseRunner sr = null;
			try {
				sr = srFromBytes(receiveData);
				// handle completed SparseRunner
				if (udpParaApp == null) {
					logm("Whoa UDPmasterThread received data even before ParaApp sent out data");
					throw new NullPointerException("no udpParaApp");
				}
				isEnd = udpParaApp.handleCompletedSparseRunner(sr);
				if (isEnd){
					break;
				}
			} catch (Exception e) {
				logm("Exception deserializing SparseRunner! e: " + e);
				e.printStackTrace();
			}
			
		} // end while(socketOK)
		logm("isEnd is here!!!!!! closing socket ...");
		udpMasterReceiveSock.close();
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
