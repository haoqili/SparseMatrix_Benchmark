Android Parallel Sparse Matrix Multiplication Benchmark
================

* Each thread runs on a different phone
* Sparse Matrix code taken from [Java Grande Forum multi-threaded benchmarks][jgf]
* NetworkThread code borrowed Jason's DiplomaMatrix

Current Status
-----------
Master runs 2 thread on itself and a slave. Hard-coded IP

How to Run
-----------

0. Start [an ad-hoc wifi][barn] on 2 phones
1. Install this app on 2 phones
2. Look at Glabals.java to change parameters
3. Change the IP address of the master to 192.168.5.20
4. Change the IP address of the slave to 192.168.5.1
5. On the slave, click on "Start Benchmark Slave ...."
6. Open logcat on the master
7. On the master, click on "Start Benchmark Master"
8. Observe `All slaves finished in xxxms`

You must start slaves first to start waiting for packets from the master.

N.B.
-------
* If the phones never reached "All slaves finished ..." or if you see a "Address already in use" error, "Force stop" the app:

    Manage apps -> select this app -> Force stop

* See example results in results_logcats directory.

* If Globals.SPARSE_NUM_ITER is too big, SparseRunner will take a long time to run!

To Do for Me
----------

* Change UDP to TCP to test if connection is better
* Dynamic IP ... sent by slaves at start

Questions
----------

* Why do I have UDP packet losses between master and slave sometimes

[jgf]: http://www2.epcc.ed.ac.uk/computing/research_activities/java_grande/threads.html
[barn]: https://github.com/haoqili/barnacle

