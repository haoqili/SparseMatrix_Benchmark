package edu.mit.haoqili.sparse_bench;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Handler;
import android.util.Log;

/** master sends TCP work packets to slaves
 *  and receive their finished TCP reply packets
 */
public class TCPmasterTranceiveThread extends Thread {
	private static final String TAG = "*** TCPmasterTranceiveThread";

	private Socket tcpMasterSock;
	private ServerSocket tcpMasterSendSock;
	private byte[] sendData;
	
	Handler mainHandler;
	public void logm(String line) {
		Log.i(TAG, line);
		mainHandler.obtainMessage(Globals.MSG_LOG, TAG+": "+line).sendToTarget();
	}

	/** NetworkThread constructor */
	public TCPmasterTranceiveThread(ServerSocket ss, byte[] data, Handler ha) {
		tcpMasterSendSock = ss;
		sendData = data;
		
		mainHandler = ha;
	}

	public void getReply(){
		try {
			byte[] receiveData = new byte[Globals.MAX_PACKET_SIZE];
			logm("master is getting reply...... ");
			InputStream is = tcpMasterSock.getInputStream();
			logm("master got input stream");
			int bytesRead = 0;
			int delta = 0;
			while (delta != -1) {
				// the second parameter of read() is the offset to write in receiveData!
				delta = is.read(receiveData, bytesRead, Globals.MAX_PACKET_SIZE - bytesRead);
				if (delta == -1){
					break;
				}
				bytesRead += delta;
			}
			logm("!!!! Received TCP payload from slave of " + bytesRead + " bytes");
			SparseRunner sr = srFromBytes(receiveData);
			logm(" >> master got bytes converted into S.R. id = " + sr.id);
			logm("send S.R. through handler");
			mainHandler.obtainMessage(Globals.MSG_TCP_REPLY, sr).sendToTarget();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	public void run() {
		try {
			logm("1. Waiting for slave to accept master's connection ...");
			// accept slave's connection
			// only accepts this onces to have a back and forth communication 
			// (just 1 round trip, because we shutdownOutput below) 
			tcpMasterSock = tcpMasterSendSock.accept(); // blocks
			
			logm("2. a Slave's Connection accepted :)");
			
			// send to the slave its portion of work
			OutputStream os = tcpMasterSock.getOutputStream();
			
			logm("3. send data");
			os.write(sendData);
			
			logm("4. flush os");
			os.flush();
			
			logm("5. shutdownOutput");
			// must have the line below, or slave hangs on receive
			// see http://stackoverflow.com/questions/4886293/socket-input-stream-hangs-on-final-read-best-way-to-handle-this
			// not tcpMasterSock.close() because still need to get slave's reply
			tcpMasterSock.shutdownOutput();
			
			logm("6. getting reply");
			getReply();
			
		} catch (IOException e) {
			logm("Master Send tcpMasterSendSock not accepting :( e:" + e.getMessage());
			System.exit(5);
		}
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
