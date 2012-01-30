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
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;

import android.os.Handler;
import android.util.Log;

public class TCPslaveThread extends Thread {
	private static final String TAG = "*** TCPslaveThread";
	
	private Socket tcpSlaveSock;
	
	Handler mainHandler;
	public void logm(String line) {
		Log.i(TAG, line);
		mainHandler.obtainMessage(Globals.MSG_LOG, TAG+": "+line).sendToTarget();
	}

	/** NetworkThread constructor */
	public TCPslaveThread(Handler ha) {
		closeOldSocket();
		mainHandler = ha;
	}
	
	// like the first portion UDPslaveThread's restartSocket()
	public void closeOldSocket() {
		if (tcpSlaveSock != null && !tcpSlaveSock.isClosed()) {
			try {
				tcpSlaveSock.close();
			} catch (IOException e) {
				logm("wanted to close tcpSlaveSock, but failed e: " + e);
				e.printStackTrace();
			}
		}
	}

	/** process TCP packets */
	@Override
	public void run() {
		logm("inside TCPslaveThread run");
		try {
			logm("-- TCPslaveThread -- about to connect to master");
			tcpSlaveSock = new Socket(getMasterAddress(), getMyPort());
			logm("slave connecting to master, to get work ....");
			
			byte[] receiveData = new byte[Globals.MAX_PACKET_SIZE];
			InputStream is = tcpSlaveSock.getInputStream();
			
			int bytesRead = 0;
			int delta = 0;
			logm("slave connected to master, getting stream");
			while(delta != -1){
				// second read param is offset within receiveData
				delta = is.read(receiveData, bytesRead, Globals.MAX_PACKET_SIZE - bytesRead);
				if (delta == -1) {
					break;
				}
				bytesRead += delta;
			}
			
			logm("!!!! Received TCP payload from master of " + bytesRead + " bytes");
			/*logm("bytes: " + receiveData);
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
    		}*/
    		
			// After this slave got its work (Sparse Runner)
			// calculate it and reply to the master its solution
			
			//analogous to diplomamatrix UserApp.java's 
			//    handleDSMRequest(): case WORK_AND_BARRIER
			logm("multiply worker / my portion of the matrix: ");
			// Deserialize SparseRunner
			long startTime = System.currentTimeMillis();
			SparseRunner sr = null;
			try {
				logm("Bytes --> S.R.");
				sr = srFromBytes(receiveData);
				sr.setHandler(mainHandler); // MUST be called before next line
				logm("run my work/my portion of matrix!");
				sr.run(); 
				
				// reply to Master
				logm("reply to master: ");
				byte[] reply_data = srToBytes(sr);
				sendData(reply_data);
				long stopTime = System.currentTimeMillis();
				logm(String.format(":D:D Work finished in %d ms!", stopTime
						- startTime));
			} catch (Exception e) {
				logm("Exception deserializing SparseRunner! e: " + e);
				e.printStackTrace();
			}

			logm("end of slaveTCPThread!");
		} catch (Exception e) {
			Log.e(TAG, "Something broke in slave TCP :/ e: " + e.getMessage());
			e.printStackTrace();
			System.exit(5);
		}
	} // end run()

	/** Send an TCP packet to the broadcast address */
	private void sendData(byte[] sendData) throws IOException {
		// if master's connection remains open ...
		logm("slave sends reply back to master Start ...");

		OutputStream os = tcpSlaveSock.getOutputStream();
		
		logm(". send data");
		os.write(sendData);
		
		logm(". flush os");
		os.flush();
		
		// we don't do shutdownOutput here because
		// there is no further communication between slave and master
		logm(". slave closes socket");
		tcpSlaveSock.close(); 
		
		logm("... slave reply back to master Finished");
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
			e.printStackTrace();
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
     * to be sent in a TCP packet
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
		logm("srFromBytes: start ....");
		ByteArrayInputStream bis = new ByteArrayInputStream(d);
		ObjectInputStream ois = new ObjectInputStream(bis);
		SparseRunner a = (SparseRunner) ois.readObject();
		logm("srFromBytes: ... got s.r.!");
		ois.close();
		return a;
	}
}
