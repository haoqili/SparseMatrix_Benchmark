package edu.mit.haoqili.sparse_bench;

import java.util.HashMap;
import java.util.Iterator;

import android.util.Log;

public class Globals {

	//TODO: change
	final static public String MASTER_IP_ADDRESS="192.168.5.20";
	static public String[] SLAVE_ADDRS = { 
										  "192.168.5.107", 
										  "192.168.5.106", 
										  "192.168.5.105",
										  "192.168.5.104",
										  "192.168.5.103",
										  "192.168.5.102",
										  "192.168.5.101",
										  MASTER_IP_ADDRESS};
	// DEBUGGING:
	//static public String[] SLAVE_ADDRS = {"192.168.5.1"};
	
	// static public String[] SLAVE_ADDRS = {MASTER_IP_ADDRESS};
	final static public int NTHREADS = SLAVE_ADDRS.length;

	final static public int SPARSE_NUM_ITER=1000;
	// old
	//final static public int SPARSE_NUM_ITER=100000;
	
	final static public int MAX_PACKET_SIZE = 110592; //bytes

	// handler stuff
	final static public int MSG_TCP_REPLY = 2;
	final static public int MSG_LOG = 3;

	// ports
	final static public int TCP_MASTER_PORT = 6998;
	final static public int TCP_SLAVE_PORTS = 7001;
	// HashMap for slaves to find their port number
	final static public HashMap<String, Integer> TCP_MY_PORT = mapTCPSlaves();
	// since Master has both its slave and master thread, use diff ports
	final static public int UDP_MASTER_PORT = 6664;
	final static public int UDP_SLAVE_PORT = 6665; 

	final static public int SlaveTCPSize = 48303; // TODO shouldn't hardcode this
	// constants
	final static public boolean CACHE_ENABLED_ON_START = false;
	final static public double BENCHMARK_READ_DISTRIBUTION_ON_START = 0.9f;
	final static public long BENCHMARK_START_DELAY = 1000L; // milliseconds
	final static public String CSM_SERVER_NAME="128.30.66.123:5212";
	final static public int MINIMUM_LATITUDE=128898;
	final static public int MINIMUM_LONGITUDE=10384948;
	final static public int MAX_X_REGIONS=10;
	final static public int MAX_Y_REGIONS=0;
	final static public int SAMPLING_DURATION=1000;
	final static public int SAMPLING_DISTANCE=1;
	final static public int REGION_WIDTH=180;
	final static public String BROADCAST_ADDRESS="192.168.5.255";

	public static final boolean DEBUG_SKIP_CLOUD = true;
	
	private static HashMap<String, Integer> mapTCPSlaves(){
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		for (int i = 0; i < NTHREADS; i++){
			map.put(SLAVE_ADDRS[i], i);
		}
		return map;
	}
	
	public static void printTCPPORTS(){
		Iterator iterator = TCP_MY_PORT.keySet().iterator();
		while(iterator.hasNext()){
			String key = iterator.next().toString();
			String value = TCP_MY_PORT.get(key).toString();
			Log.i("TCP_MY_PORT: ", key + " --> " + value );
		}
	}
	
}
