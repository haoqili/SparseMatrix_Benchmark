package edu.mit.haoqili.sparse_bench;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.os.Handler;
import android.util.Log;

class SparseRunner implements Serializable, Runnable {
	
	private static final String TAG = "... SparseRunner ";

	private static final long serialVersionUID = 1L;

	public double yt[]; // my local, partial, work copy
	
	int id, nz, row[], col[], NUM_ITERATIONS;
	double val[], x[];
	int lowsum[];
	int highsum[];
	
	// log
	transient Handler mainHandler = null; // transient so it doesn't get serialized
	public void logm(String line) {
		Log.i(TAG, line);
		mainHandler.obtainMessage(Globals.MSG_LOG, TAG+": "+line).sendToTarget();
	}

	public SparseRunner(double yt[], int id, double val[], int row[], int col[], double x[],
			int NUM_ITERATIONS, int nz, int lowsum[], int highsum[], Handler ha) {
		mainHandler = ha;

		logm("initializing ...");
		
		this.yt = yt;
		this.id = id;
		this.x = x;
		this.val = val;
		this.col = col;
		this.row = row;
		this.nz = nz;
		this.NUM_ITERATIONS = NUM_ITERATIONS;
		this.lowsum = lowsum;
		this.highsum = highsum;

		logm("created.");
	}

	public void run() {
		logm("running...");

		for (int reps = 0; reps < NUM_ITERATIONS; reps++) {
			/*// test if SparseRunner is running
			 if ((reps % 1000) == 0) {
				logm(", " + reps);
			}*/
			// Entire array is given, and we pick a subset ranging from
			// lowsum[id] to highsum[id]
			for (int i = lowsum[id]; i < highsum[id]; i++) {
				yt[row[i]] += x[col[i]] * val[i];
			}
		}

		logm("finished.");
	}
	public void setHandler(Handler ha){
		mainHandler = ha;
	}
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
	}
}
