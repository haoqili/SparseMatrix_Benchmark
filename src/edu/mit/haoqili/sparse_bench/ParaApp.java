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
import java.util.Random;

import android.util.Log;

public class ParaApp {
	/* Network stuff */
	private DatagramSocket mySocket;

	/* Largely copied from DIPLOMAMatrix's UserApp */
	long kernelStartTime, kernelStopTime;
    long runStartTime, runStopTime;
    
	int barrier_count = 0; // barrier for each slave to finish


    // App-specific stuff
    public int nthreads = Globals.NTHREADS;
    public String hqteststr = "start ";

    public double ytotal = 0.0;
    public double global_yt[]; // this is very randomly coupled, this is the
    // shared output variable t split across threads

    private int size; // size of the sparse matrix, small medium and large
    private static final long RANDOM_SEED = 10101010;
    // generating entries in the random vector.

    private static final int datasizes_M[] = { 1000, 50000, 100000, 500000,
        1000000 };
    // number of rows in the matrix
    private static final int datasizes_N[] = { 1000, 50000, 100000, 500000,
        1000000 };
    // number of columns in the matrix
    private static final int datasizes_nz[] = { 2000, 250000, 500000, 2500000,
        5000000 };
    // number of non zero elements in the matrix

    // private static final int datasizes_M[] = { 50000, 100000, 500000, 1000000
    // };
    // number of rows in the matrix
    // private static final int datasizes_N[] = { 50000, 100000, 500000, 1000000
    // };
    // number of columns in the matrix
    // private static final int datasizes_nz[] = { 250000, 500000, 2500000,
    // 5000000 };
    // number of non zero elements in the matrix

    // number of iterations to compute the benchmark. I guess it's for
    // averaging. but it makes no sense to me.

    Random R = new Random(RANDOM_SEED);

    double[] x;
    double[] y;
    double[] val;
    int[] col;
    int[] row;
    int[] lowsum;
    int[] highsum; 

    private void logm(String line){
        Log.i("******** ParaApp: ", line);
    }

    /* PARAllel APP constructor */
    //public ParaApp(int nthreads){
    public ParaApp(){
    	logm("In Constructor");
    }

    public synchronized void startBenchmark() {
        logm("startBenchmark() ......");
        // 0 is the size, 0 1 or 2
        JGFrun(0);
    }


    /* =========== JGF STUFF =============== */
    
    public void JGFsetsize(int size) {
        this.size = size;

    }

    /*
     * JGFinitialise stores the matrix in Sparse Row format It isn't really
     * smart, it ends up storing it as Sparse Rows with a separate row and
     * column matrix.
     */
    public void JGFinitialise() {

        x = RandomVector(datasizes_N[size], R); // the vector , this makes
        // sense.
        y = new double[datasizes_M[size]]; // what is this ?

        val = new double[datasizes_nz[size]];
        col = new int[datasizes_nz[size]];
        row = new int[datasizes_nz[size]];

        int[] ilow = new int[nthreads];
        int[] iup = new int[nthreads];
        int[] sum = new int[nthreads + 1];
        lowsum = new int[nthreads + 1];
        highsum = new int[nthreads + 1];
        int[] rowt = new int[datasizes_nz[size]];
        int[] colt = new int[datasizes_nz[size]];
        double[] valt = new double[datasizes_nz[size]];
        int sect;

        // This portion below makes sense I guess now.
        for (int i = 0; i < datasizes_nz[size]; i++) {

            // generate random row index (0, M-1)
            row[i] = Math.abs(R.nextInt()) % datasizes_M[size];

            // generate random column index (0, N-1)
            col[i] = Math.abs(R.nextInt()) % datasizes_N[size];

            val[i] = R.nextDouble();

        }

        // reorder arrays for parallel decomposition

        // divide the number of rows by number of threads.
        sect = (datasizes_M[size] + nthreads - 1) / nthreads;

        for (int i = 0; i < nthreads; i++) {
            ilow[i] = i * sect;
            iup[i] = ((i + 1) * sect) - 1;
            if (iup[i] > datasizes_M[size])
                iup[i] = datasizes_M[size];
        }

        for (int i = 0; i < datasizes_nz[size]; i++) {
            for (int j = 0; j < nthreads; j++) {
                if ((row[i] >= ilow[j]) && (row[i] <= iup[j])) {
                    sum[j + 1]++;
                }
            }
        }

        // compute the number of elements in each section.

        for (int j = 0; j < nthreads; j++) {
            for (int i = 0; i <= j; i++) {
                lowsum[j] = lowsum[j] + sum[j - i]; // summing backwards I guess
                // ?
                highsum[j] = highsum[j] + sum[j - i];// summing backwards I
                // guess ?
            }
        }

        // compute the number of elements upto and including the current
        // section.

        for (int i = 0; i < datasizes_nz[size]; i++) {
            for (int j = 0; j < nthreads; j++) {
                if ((row[i] >= ilow[j]) && (row[i] <= iup[j])) {
                    rowt[highsum[j]] = row[i];
                    colt[highsum[j]] = col[i];
                    valt[highsum[j]] = val[i];
                    highsum[j]++;
                }
            }
        }

        // wonder what is going on here ?

        // The above code: run through all the non zero elements first.
        // Then, for each section in the matrix, check which section belongs to
        // .
        for (int i = 0; i < datasizes_nz[size]; i++) {
            row[i] = rowt[i];
            col[i] = colt[i];
            val[i] = valt[i];
        }

    }

    public void JGFkernel() {
        int nz = val.length;
        global_yt = y;

        System.gc();

        logm("JGFkernel Starting benchmark, nthreads = " + nthreads);

        kernelStartTime = System.currentTimeMillis();

        // Send pieces of work to each slave as a Serializable SparseRunner
        for (int i = nthreads - 1; i >= 0; i--) {
        	logm("..");
        	logm("making piece of nthread = " + i);
            SparseRunner sr = new SparseRunner(global_yt, i, val, row, col, x,
                    Globals.SPARSE_NUM_ITER, nz, lowsum, highsum);
            byte[] b = null;
            try {
                logm(String.format(
                            "Serializing SparseRunner into Serializable for region %d,0...", i));
                b = srToBytes(sr);
                
                logm("JGFkernel Sending Serializable with payload bytes: " + b.length);
                logm("sendToSlave for nthread = " + i);
                sendToSlave(b, i);
            } catch (Exception e) {
            	logm("JGFkerner exception :( e: " + e);
                e.printStackTrace();
            }            
            
            b = null;
            sr = null;
        }
    }

    public void JGFrun(int size) {
        logm("JGFrun() start ......;");
        runStartTime = System.currentTimeMillis();

        JGFsetsize(size);
        JGFinitialise(); // matrix balancing
        JGFkernel(); // calls test() in master.java; spawn slave tasks,
        // distribute to slaves, collect/join/barrier at end
        logm("JGFrun() finished ^^^^^^^^^^^^^");
    }
    
	private static double[] RandomVector(int N, java.util.Random R) {
		double A[] = new double[N];

		for (int i = 0; i < N; i++)
			A[i] = R.nextDouble() * 1e-6;

		return A;
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
    /** Send an UDP packet to the broadcast address */
    private void sendToSlave(byte[] sendData, int thd_i) throws IOException {
    	mySocket = new DatagramSocket();
    	mySocket.send(new DatagramPacket(sendData, sendData.length,
    			getSlaveAddress(thd_i),
    			Globals.SLAVE_PORT));
    	mySocket.close();
	}
	private InetAddress getSlaveAddress(int i) throws IOException {
		logm("      Slave addr: " + Globals.SLAVE_ADDRS[i]);
		return InetAddress.getByName(Globals.SLAVE_ADDRS[i]);

	}
    /**
     * Deserialize a Serializable from a byte array
     * which we got from a UDP packet
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws OptionalDataException
     */
    public SparseRunner srFromBytes(byte[] d) throws OptionalDataException, ClassNotFoundException, IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(d);
        ObjectInputStream ois = new ObjectInputStream(bis);
        SparseRunner a = (SparseRunner) ois.readObject();
        ois.close();
        return a;
    }
    
	public synchronized boolean handleCompletedSparseRunner(SparseRunner sr) {
		// combine worker's results into main result matrix
		logm("starting to handle completed sparserunner");
		for (int i = lowsum[sr.id]; i < highsum[sr.id]; i++) {
			global_yt[row[i]] = sr.yt[row[i]];
		}

		// Barrier here
		barrier_count++;
		logm("updated barrier_count = " + barrier_count + " nthreads = " + nthreads);
		if (barrier_count == nthreads) {
			logm("Yay barrier count is equal to nthreads!");
			barrier_count = 0;
			int nz = val.length;

			// Sum results for fast validation
			for (int i = 0; i < nz; i++) {
				ytotal += global_yt[row[i]];
			}

			kernelStopTime = System.currentTimeMillis();

			logm("###################################################");
			logm("############ DONE!!!!!!!! :D:D:D:D:D:D ############");
			logm("###################################################");
			
			runStopTime = System.currentTimeMillis();
			logm(String.format("JGFrun finished in %dms", runStopTime
							- runStartTime));

			logm(String.format("All slaves finished in %dms", kernelStopTime
							- kernelStartTime));
			return true;
		}
		return false;
	}
}
