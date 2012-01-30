package edu.mit.haoqili.sparse_bench;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.os.Handler;
import android.util.Log;

public class UDPslaveThread extends Thread {
	private static final String TAG = "*** UDPslaveThread";
	
	// UDP over IPv4 Networking
	private DatagramSocket udpSlaveSock;
	private boolean socketOK = true;
	
	Handler mainHandler;
	public void logm(String line) {
		Log.i(TAG, line);
		mainHandler.obtainMessage(Globals.MSG_LOG, TAG+": "+line).sendToTarget();
	}

	/** NetworkThread constructor */
	public UDPslaveThread(Handler ha) {
		restartSocket();
		mainHandler = ha;
		logm("in slave network thread");
	}

	/** Start of r */
	public void restartSocket() {
		if (udpSlaveSock != null && !udpSlaveSock.isClosed()) //doesn't get here
			udpSlaveSock.close();

		// Create UDP socket for receiving packets
		try {
			udpSlaveSock = new DatagramSocket(Globals.UDP_SLAVE_PORT);
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
			Log.e(TAG, "Cannot open socket :/ e: " + e.getMessage());
			socketOK = false;
			System.exit(5);
		}
	}

	/** If not socketOK, then receive loop thread will stop */
	boolean socketIsOK() {
		return socketOK;
	}

	/** Thread's process loop for UDP packets */
	@Override
	public void run() {
		logm("In slave's run()");
		byte[] receiveData = new byte[Globals.MAX_PACKET_SIZE];

		while (socketOK) {
			DatagramPacket dPacket = new DatagramPacket(receiveData,
					receiveData.length);
			logm("Slave waiting to receive packet from master ...");
			try {
				udpSlaveSock.receive(dPacket); // blocks
			} catch (IOException e) {
				logm("mySocket.receive broke :(");
				Log.e(TAG, "Exception on mySocket.receive: " + e.getMessage());
				socketOK = false;
				System.exit(5);
			}
			
			logm("!!!! Received UDP payload: " + dPacket.getLength());
			logm("Bytes: " + receiveData);

			// After this slave got its work (Sparse Runner)
			// calculate it and reply to the master its solution
			
			//analogous to diplomamatrix UserApp.java's 
			//    handleDSMRequest(): case WORK_AND_BARRIER
			logm("multiply worker / my portion of the matrix");
			// Deserialize SparseRunner
			long startTime = System.currentTimeMillis();
			SparseRunner sr = null;
			try {
				sr = srFromBytes(receiveData);
				logm("receive Data: ");
				//debugc(receiveData);
				sr.setHandler(mainHandler); // MUST be called before next line
				sr.run(); 
				
				// reply to Master
				byte[] reply_data = srToBytes(sr);
				logm("reply data: ");
				//debugc(reply_data);
				sendData(reply_data);
				long stopTime = System.currentTimeMillis();
				logm(String.format(":D:D Work finished in %d ms!", stopTime
						- startTime));
				break;
			} catch (Exception e) {
				logm("Exception deserializing SparseRunner! e: " + e);
				e.printStackTrace();
			}
			
		} // end while(socketOK)
		logm("closing socket ..");
		udpSlaveSock.close();
	} // end run()

	private void debugc(byte[] bytes){
		int debugc = 0;
		for (int j=0; j<bytes.length; j++) {
			if (j%800 == 0){
				if (bytes[j] == 0) {
					if (debugc > 5) break;
					debugc++;
				} else {
					debugc = 0;
				}
				logm(j + ": " + String.format("0x%02X", bytes[j]) + " debugc: " + debugc);
			}
		}
	}
	/** Send an UDP packet to the broadcast address */
	private void sendData(byte[] sendData) throws IOException {
		logm("send reply back to master Start");
		udpSlaveSock.send(new DatagramPacket(sendData, sendData.length,
				getMasterAddress(), Globals.UDP_MASTER_PORT));
		logm("send reply back to master Finished");
	}

	/** Calculate the broadcast IP we need to send the packet along. */
	private InetAddress getMasterAddress() throws IOException {
		return InetAddress.getByName(Globals.MASTER_IP_ADDRESS);
	}
	
    /**
     * Serialize a sparse runner into a byte array
     * to be sent in a Datagram UDP packet
     * 
     * @throws IOException
     */
    public byte[] srToBytes(SparseRunner a) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(a);
        out.close();
        byte[] bytes = bos.toByteArray();
        bos.close();
        return bytes;
    }
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
