Android Parallel Sparse Matrix Multiplication Benchmark
================

* Each thread runs on a different phone
* Choice of using TCP (more reliable) or UDP (a bit faster)
* Sparse Matrix code taken from [Java Grande Forum multi-threaded benchmarks][jgf]

Current Status
-----------
* Master runs 2 thread on itself and a slave. Hard-coded IP


How to Run
-----------

1. Start [an ad-hoc wifi][barn] on 2 phones
2. Install this app on 2 phones
3. Look at Globals.java to change parameters
4. Change the IP address of the master to 192.168.5.20
5. Change the IP address of the slave to 192.168.5.1
6. Based on whether you want to use UDP or TCP, press its appropriate buttons in the order displayed on the screen

        E.G. if you want to use UDP, you see: `UDP 1. Start Slaves` and `UDP 2. Start Master`. So you press the 1st button on the slave phone (we only have one slave as of now) and then press the 2nd button on the master phone.
7. Observe `******* end of benchmark *********`
8. Get ready for the next run by:

        Manage apps -> select this app -> Force stop

N.B.
---------
* See example results in results_logcats directory.

* If Globals.SPARSE_NUM_ITER is too big, SparseRunner will take a long time to run!

To Do for Me
----------

* <del>Dynamic IP ... sent by slaves at start</del> not useful since we're testing with 5 phones

Questions
----------

* Why do I have UDP packet losses between master and slave 3-10 % of the time?

[jgf]: http://www2.epcc.ed.ac.uk/computing/research_activities/java_grande/threads.html
[barn]: https://github.com/haoqili/barnacle

