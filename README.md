Android Parallel Sparse Matrix Multiplication Benchmark
================

* Each thread runs on a different phone
* Choice of using TCP (more reliable) or UDP (a bit faster)
* Sparse Matrix code taken from [Java Grande Forum multi-threaded benchmarks][jgf]

Current Status
-----------
* Master runs n threads on itself and n-1 slaves.
* Hard code IP


How to Run
-----------

1. Start [an ad-hoc wifi][barn] on n phones
2. Install this app on n phones
3. Look at Globals.java to change parameters, especially the MASTER_IP_ADDRESS and SLAVE_ADDRS
    
        In shell:
        `su`
        `ifconfig eth0 192.168.5.xxx`
        `netcfg` and you should see the changed IP
4. Based on whether you want to use UDP or TCP, press its appropriate buttons in the order displayed on the screen

        E.G. if you want to use UDP, you see: `UDP 1. Start Slaves` and `UDP 2. Start Master`. So you press the 1st button on the slave phone (we only have one slave as of now) and then press the 2nd button on the master phone.
5. Observe `******* end of benchmark *********` note the "ms" times right before that line
6. Get ready for the next run by:

        Manage apps -> select this app -> Force stop

N.B.
---------
* See example results in results directory.

* If Globals.SPARSE_NUM_ITER is too big, SparseRunner will take a long time to run!

* Using UDP might result in packet losses, so TCP is strongly recommended (a few hundred millisecs of overhead)

Troubleshooting
----------

* Most errors can be resolved by force stopping the app on all phones

* Error something like "Connection refused ... java.net.ConnectException ... PlainSocketImpl", that means the ports are in use. To fix the problem change in Globals.java TCP_MASTER_PORT and TCP_SLAVE_PORTS to some other numbers

[jgf]: http://www2.epcc.ed.ac.uk/computing/research_activities/java_grande/threads.html
[barn]: https://github.com/haoqili/barnacle

