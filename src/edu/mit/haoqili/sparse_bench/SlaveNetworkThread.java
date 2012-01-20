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

import android.util.Log;

public class SlaveNetworkThread extends Thread {
	private static final String TAG = "...... SlaveNetworkThread";

	// UDP over IPv4 Networking
	private static final int MAX_PACKET_SIZE = 110592; // bytes
	private DatagramSocket slaveSocket;
	private boolean socketOK = true;
	
	public void logm(String line) {
		Log.i(TAG, line);
	}

	/** NetworkThread constructor */
	public SlaveNetworkThread() {
		restartSocket();
	}

	/** Start of r */
	public void restartSocket() {
		if (slaveSocket != null && !slaveSocket.isClosed()) //doesn't get here
			slaveSocket.close();

		// Create UDP socket for receiving packets
		try {
			slaveSocket = new DatagramSocket(Globals.SLAVE_PORT);
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
			Log.e(TAG, "Cannot open socket :/ e: " + e.getMessage());
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
				slaveSocket.receive(dPacket);
			} catch (IOException e) {
				logm("mySocket.receive broke :(");
				Log.e(TAG, "Exception on mySocket.receive: " + e.getMessage());
				socketOK = false;
				System.exit(5);
			}
			
			logm("!!!! Received UDP payload: " + dPacket.getLength());

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
				sr.run(); // more efficient than the 3 lines below
				/*
				Thread th = new Thread(sr);
				th.start();  //calculate this slave's sparse runner
				// th.join makes sure th is finished
				// see http://cnapagoda.blogspot.com/2010/01/thread-join-method.html
				th.join();*/
				
				// reply to Master
				byte[] reply_data = srToBytes(sr);
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
		slaveSocket.close();
	} // end run()

	/** Send an UDP packet to the broadcast address */
	private void sendData(byte[] sendData) throws IOException {
		logm("send reply back to master Start");
		slaveSocket.send(new DatagramPacket(sendData, sendData.length,
				getMasterAddress(), Globals.MASTER_PORT));
		logm("send reply back to master Finished");
	}

	/** Calculate the broadcast IP we need to send the packet along. */
	private InetAddress getMasterAddress() throws IOException {
		// TODO: Fill in Master's IP addr so this slave can reply
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
