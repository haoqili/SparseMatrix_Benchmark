package edu.mit.haoqili.sparse_bench;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;

import android.os.Handler;
import android.util.Log;

public class SlaveTCPThread extends Thread {
	private static final String TAG = "*** SlaveTCPThread";
	
	
	// UDP over IPv4 Networking
	private static final int MAX_PACKET_SIZE = 110592; // bytes
	private Socket slaveTCPSoc;
	private boolean socketOK = true;
	
	Handler logHandler;
	public void logm(String line) {
		Log.i(TAG, line);
		logHandler.obtainMessage(0, TAG+": "+line).sendToTarget();
	}

	/** NetworkThread constructor */
	public SlaveTCPThread(Handler ha) {
		closeOldSocket();
		logHandler = ha;
	}

	public void closeOldSocket() {
		if (slaveTCPSoc != null && !slaveTCPSoc.isClosed()) {
			try {
				slaveTCPSoc.close();
			} catch (IOException e) {
				logm("wanted to close slaveTCPSoc, but failed e: " + e);
				e.printStackTrace();
			}
		}
	}

	/** If not socketOK, then receive loop thread will stop */
	boolean socketIsOK() {
		return socketOK;
	}

	/** Thread's process TCP packets */
	@Override
	public void run() {
		// Create UDP socket for receiving packets
		logm("inside slaveTCPThread run");
		try {
			logm("-- SlaveTCPThread -- about to connect to master");
			slaveTCPSoc = new Socket(getMasterAddress(), getMyPort());
			logm("slave connecting to master, to get work ....");
			
			byte[] receiveData = new byte[MAX_PACKET_SIZE];
			// I have tried DataOutput/InputStream, but they didn't work so I gave up
			// see: http://stackoverflow.com/questions/8760109/tcp-how-to-send-receive-real-time-large-packets-in-java
			InputStream is = slaveTCPSoc.getInputStream();
			//int bytesRead = is.read(receiveData,0,MAX_PACKET_SIZE); //receiveData.length = MAX_PACKET_SIZE
			
			// fix!
			int bytesRead = 0;
			logm("here, getting stream");
			// If put < MAX_PACKET_SIZE, hangs, see
			//   http://stackoverflow.com/questions/4886293/socket-input-stream-hangs-on-final-read-best-way-to-handle-this
			
			// TODO: not hard-coded this number
			// 48303 comes from the consistent UDP packet size for this app
			// TCP had 48295 instead
			while(bytesRead < Globals.SlaveTCPSize){
				logm("while " + bytesRead + " != " + MAX_PACKET_SIZE + ", " + is.available());
				//bytesRead += is.read(receiveData,0,MAX_PACKET_SIZE);
				int delta = is.read(receiveData,0,MAX_PACKET_SIZE);
				bytesRead += delta;
				//logm("delta = " + delta + ", receiveData]bytesRead] = " + String.format("0x%02X", receiveData[bytesRead-400]));
				int debugc = 0;
				for (int j=bytesRead-delta; j<bytesRead; j++) {

	    			if (j%800 == 0){
	    				if (receiveData[j] == 0) {
	    					if (debugc > 5) break;
	    					debugc++;
	    				} else {
	    					debugc = 0;
	    				}
	    				logm(j + ": " + String.format("0x%02X", receiveData[j]) + " debugc: " + debugc);
	    			}
	    		}
			}
			
			logm("!!!! Received TCP payload read " + bytesRead + " bytes");
			logm("bytes: " + receiveData);
			int debugc = 0;
    		for (int j=0; j<Globals.SlaveTCPSize; j++) {
    			if (j%800 == 0){
    				if (receiveData[j] == 0) {
    					if (debugc > 5) break;
    					debugc++;
    				} else {
    					debugc = 0;
    				}
    				logm(j + ": " + String.format("0x%02X", receiveData[j]) + " debugc: " + debugc);
    			}
    		}
    		
			// After this slave got its work (Sparse Runner)
			// calculate it and reply to the master its solution
			
			//analogous to diplomamatrix UserApp.java's 
			//    handleDSMRequest(): case WORK_AND_BARRIER
			logm("multiply worker / my portion of the matrix");
			// Deserialize SparseRunner
			long startTime = System.currentTimeMillis();
			SparseRunner sr = null;
			try {
				logm("tmp 1");
				sr = srFromBytes(receiveData);
				logm("tmp 2");
				sr.setHandler(logHandler); // MUST be called before next line
				logm("tmp 3");
				sr.run(); 
				
				// reply to Master
				logm("tmp 4");
				byte[] reply_data = srToBytes(sr);
				sendData(reply_data);
				long stopTime = System.currentTimeMillis();
				logm(String.format(":D:D Work finished in %d ms!", stopTime
						- startTime));
			} catch (Exception e) {
				logm("Exception deserializing SparseRunner! e: " + e);
				e.printStackTrace();
			}

			logm("closing TCP socket ..");
			slaveTCPSoc.close();
		} catch (Exception e) {
			Log.e(TAG, "Something broke in slave TCP :/ e: " + e.getMessage());
			socketOK = false;
			logm("WAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
			logm("WAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
			//System.exit(5);
		}
	} // end run()

	/** Send an TCP packet to the broadcast address */
	private void sendData(byte[] sendData) throws IOException {
		// TODO: Try!!
		// create a new socket connection ... 
		// like in case when master closed connection after send out work
		/*masterSendTCPSocs[thd_i] = new ServerSocket(Globals.TCP_SLAVE_PORTS + thd_i);
		Thread send_thread = new TCPSendThread(masterSendTCPSocs[thd_i], sendData, logHandler);
		send_thread.start();
		*/
		// if master's connection remains open ...
		logm("send reply back to master Start");

		OutputStream os = slaveTCPSoc.getOutputStream();
		os.write(sendData);
		logm(". flush os");
		os.flush();
		logm(". close socket");
		slaveTCPSoc.close();
		
		//slaveTCPSoc.send(new DatagramPacket(sendData, sendData.length,
		//		getMasterAddress(), Globals.UDP_MASTER_PORT));
		logm("send reply back to master Finished");
	}

	/** Get slave's own IP Address */
	private int getMyPort() {
		String myIP = getMyIP();
		logm("MyIP = " + myIP + " myPort = " + (Globals.TCP_SLAVE_PORTS+Globals.TCP_MY_PORT.get(myIP)));
		return Globals.TCP_SLAVE_PORTS+Globals.TCP_MY_PORT.get(myIP);
	}
	
	private String getMyIP() {
		try {
			NetworkInterface intf = NetworkInterface.getByName("eth0");
			for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
					.hasMoreElements();) {
				InetAddress inetAddress = enumIpAddr.nextElement();
				if (!inetAddress.isLoopbackAddress()) {
					return inetAddress.getHostAddress();
				}
			}
			logm(":( no IP address found for this slave");
			System.exit(5);
		} catch (Exception e) {
			Log.e(TAG, "can't determine local IP address: " + e.toString());
			System.exit(5);
		}	
		return "";
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
		logm("here 1");
		ByteArrayInputStream bis = new ByteArrayInputStream(d);
		//InputStream is = slaveTCPSoc.getInputStream();
		logm("here 2");
		ObjectInputStream ois = new ObjectInputStream(bis);
		logm("here 3");
		SparseRunner a = (SparseRunner) ois.readObject();
		logm("here 4");
		ois.close();
		return a;
	}
}
