package sim_core;

import umontreal.iro.lecuyer.simevents.Event;
import umontreal.iro.lecuyer.simevents.Sim;

public class NetworkSim {
	
	final static double endtime = 480 * 60; // 120 minutes
	final static boolean debug = true;
	static int runNumber = 0;
	
	
	private static String networkType;
	
	public static int getRunNumber()
	{
		return runNumber;
	}
	
	public static void initSim(String type, double time)
	{
		
		networkType = type;
		Sim.init();
		
		new Snapshot().schedule( Configuration.getDoubleConfig("snapshotWaitTime") );
		if (time > -1)
			new EndOfSim().schedule(time);
	}

	public static void initSim(String type)
	{
		initSim(type, endtime);
	}


	public static void goTime() {
		
		
		// make the connections in all the nodes
		runNumber++;
		Node.makeLinks();
		
		System.out.println("Running " + Configuration.getConfig("configName"));
		
		// then run the sim
		Sim.start();
		
		
	}
	
	public static String getNetworkType(){
		return networkType;
	}
	
	public static String getNodeReports()
	{
		System.out.println();
		System.out.println();
		System.out.println();
		Message.summarizeData();
		return Node.nodeReports();
	}

	public static void makeCharts() {

		// Take the nodes
		// make a list of all the basestations
		
		
		
	}
	
	public static void reset()
	{
		Node.reset();
		Message.reset();
	}
	
	
	

}

class Snapshot extends Event
{
	private static int lastRoundDone = 0;
	public void actions()
	{
		int deno = Node.round - lastRoundDone;
		int enumer = Node.getAverageCH();
		int nodesAlive = 0;
		
		
		for (Node n : Node.nodes)
			if (n.isAlive())
				nodesAlive++;
		System.out.println("Average Number of CH: " + (enumer/(double)deno) + " and there are " + nodesAlive + " nodes alive. BaseStation at " + Basestation.getLastBS().x() + " " + Basestation.getLastBS().y()  );
		lastRoundDone = Node.round;
		Node.resetAverageCH();
		
		// schedule next event
		new Snapshot().schedule( Configuration.getDoubleConfig("snapshotWaitTime") );
		
	}
}


class EndOfSim extends Event
{
	public void actions()
	{				
		Sim.stop();
	}
}