A Java networking simulator.

* Message passing
* Framework to allow stacks of protocols to be implemented and simulated
  * See src/agarnet/protocols/protocol.java
* Host that contains a protocol stack is a protocol itself,
  * See src/agarnet/protocols/host/host.java
* Links with bandwidth and latency simulated
  * See src/agarnet/link/link.java
* Ticked time simulator
  * Simulated protocols can run concurrently across CPUs, 
* Basic animation implemented in src/agarnet/anipanel.java
* Some framework CLI implementations present, though not the most elegant.
