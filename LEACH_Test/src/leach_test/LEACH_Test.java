package leach_test;

import sim_core.Configuration;
import sim_core.NetworkSim;
import sim_core.Node;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;

//import sim_core.*;
public class LEACH_Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("Welcome to the LEACH test");

		/*NetworkSim.initSim();
		RandomStream placement = new MRG32k3a("Random Stream for randomly placing nodes");
		
		
		// set up a test network.
		
		//Networks.ticToc();
		//Networks.DaisyChain();
		//Networks.BiggerDaisyChain();
		Networks.createRandomNetwork( placement, 100, 500, 500);
		NetworkSim.goTime();
		
		System.out.println(NetworkSim.getNodeReports());
		
		System.out.println();
		//System.out.println(Message.printAllMessageStats());
		//System.out.println(DataMessage.report());
		
		
		//System.out.println(Node.nodes.get(0));
		System.out.println("\n\nDone Simulation!");*/
		
		long start = System.currentTimeMillis();
		
		//String filename = "Sparse500x500SuboptimalNoSleep.txt";
		String filename = "biggerDaisyChain.txt";
		
		if (args.length >= 1)
			filename = args[0];
		
		Configuration.configurationReader(filename);
		//Configuration.configurationReader("smallNetwork.txt");
		for (int i = 1; i < args.length; i++)
			Configuration.additionalConfig(args[i]);
		
		//Configuration.configurationReader("smallNetwork.txt");
		
		int simTime = 999999999;
		if (Configuration.configExists("simTime"))
			simTime = Configuration.getIntConfig("simTime");
		NetworkSim.initSim("leach", simTime);
		
		System.out.println("Running for " + simTime);
		
		RandomStream placement = new MRG32k3a("Random Stream for randomly placing nodes");
		
		// set up a test network.
		
		
		Networks.createFromConfig(placement);
		
		System.out.println("Created the network");
		
		
		
		if (Configuration.configExists("RECLUSTER_TIME"))
			LeachConstants.RECLUSTER_TIME = Configuration.getDoubleConfig("RECLUSTER_TIME");
		if (Configuration.configExists("CHOOSE_CLUSTERHEAD_TIME"))
			LeachConstants.CHOOSE_CLUSTERHEAD_TIME = Configuration.getDoubleConfig("CHOOSE_CLUSTERHEAD_TIME");
		if (Configuration.configExists("WAIT_FOR_SCHEDULE_TIME"))
			LeachConstants.WAIT_FOR_SCHEDULE_TIME = Configuration.getDoubleConfig("WAIT_FOR_SCHEDULE_TIME");
		if (Configuration.configExists("TOTAL_RUN_TIME"))
			LeachConstants.TOTAL_RUN_TIME = Configuration.getDoubleConfig("TOTAL_RUN_TIME");
		//if (Configuration.configExists("SLEEP_TIME"))
		//	LeachConstants.SLEEP = Configuration.getDoubleConfig("SLEEP_TIME");
		if (Configuration.configExists("leach_SLEEP"))
			LeachConstants.SLEEP = Configuration.getDoubleConfig("leach_SLEEP");
		
		
		
		
		//Networks.ticToc();
		//Networks.BiggerTicToc();
		//Networks.DaisyChain();
		//Networks.BiggerDaisyChain();
		//Networks.createRandomNetwork( placement,1000 , 800, 800);
		
		
		NetworkSim.goTime();
		
		System.out.println("Done running" +  Configuration.getConfig("configName"));
		
		System.out.println(NetworkSim.getNodeReports());
		System.out.println();
		System.out.println();
		//System.out.println(Message.messageStatsShort());
		
		System.out.println();
		
		if (Configuration.getBooleanConfig("makeCharts")) 
		{
			System.out.println("Creating pretty charts...");
		
			Node.makeReportChartData(new ReportCallback());
		}
		
		
		
		//System.out.println(Message.printAllMessageStats());
		//System.out.println(DataMessage.report());
		
		
		//System.out.println(Node.nodes.get(0));
		System.out.println("Completed in LEACH " + Configuration.getConfig("configName") + " in " +(System.currentTimeMillis() - start)/1000 + " seconds ("+((System.currentTimeMillis() - start)/60000)+" minutes)");
		System.out.println("\n\nDone Simulation!");

	}

}
