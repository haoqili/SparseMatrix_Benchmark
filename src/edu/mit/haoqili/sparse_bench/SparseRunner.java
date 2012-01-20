package edu.mit.haoqili.sparse_bench;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.util.Log;

class SparseRunner implements Serializable, Runnable {
//class SparseRunner implements Runnable {

	private static final long serialVersionUID = 1L;

	public double yt[]; // my local, partial, work copy
	
	int id, nz, row[], col[], NUM_ITERATIONS;
	double val[], x[];
	int lowsum[];
	int highsum[];

	public SparseRunner(double yt[], int id, double val[], int row[], int col[], double x[],
			int NUM_ITERATIONS, int nz, int lowsum[], int highsum[]) {
		Log.i("SparseRunner", "SparseRunner initializing ................");
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

		Log.d("JGF", "SparseRunner created.");
	}

	public void run() {
		Log.d("JGF", "SparseRunner running...");

		for (int reps = 0; reps < NUM_ITERATIONS; reps++) {
			/*// test if SparseRunner is running
			 if ((reps % 1000) == 0) {
				Log.i(".", " " + reps);
			}*/
			// Entire array is given, and we pick a subset ranging from
			// lowsum[id] to highsum[id]
			for (int i = lowsum[id]; i < highsum[id]; i++) {
				yt[row[i]] += x[col[i]] * val[i];
			}
		}

		Log.d("JGF", "SparseRunner finished.");
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
	}
}
