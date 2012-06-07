package hccp_test;

import leach_test.ReportCallback;
import sim_core.Configuration;
import sim_core.NetworkSim;
import sim_core.Node;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;
import Util.Utility;

public class HCCP_Test {
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Welcome to the HCCP test");
		
		Utility.isDouble("123");

		
		long start = System.currentTimeMillis();
		
		//String filename = "Sparse500x500SuboptimalNoSleep.txt";
		//String filename = "testRealistic20-40-all.txt";
		String filename = "biggerDaisyChainSuboptimalNoSleep.txt";
		//String filename = "testRealistic20-90-all.txt";
		//String filename = "NeverDieRealistic20-50.txt";
		//String filename = "NeverDieRealistic20-50-all.txt";
		
		
		if (args.length >= 1)
			filename = args[0];
		
		
			
		
		Configuration.configurationReader(filename);
		//Configuration.configurationReader("smallNetwork.txt");
		for (int i = 1; i < args.length; i++)
			Configuration.additionalConfig(args[i]);
		
			
		
		if (Configuration.configExists("allowSinkSleep"))
			HccpConstants.allowSinkSleep = Configuration.getBooleanConfig("allowSinkSleep");
		HccpConstants.sinkSleepPercentage = Configuration.getDoubleConfig("sinkSleepPercentage");
		
		if (Configuration.configExists("allowSuboptimalClusterheads"))
			HccpConstants.allowSuboptimalClusterheads = Configuration.getBooleanConfig("allowSuboptimalClusterheads");
		
		
		//if (Configuration.configExists("RECLUSTER_TIME"))
			HccpConstants.RECLUSTER_TIME = Configuration.getDoubleConfig("RECLUSTER_TIME");
		//if (Configuration.configExists("CHOOSE_CLUSTERHEAD_TIME"))
			HccpConstants.RECLUSTER_TIME = Configuration.getDoubleConfig("CHOOSE_CLUSTERHEAD_TIME");
		//if (Configuration.configExists("WAIT_FOR_SCHEDULE_TIME"))
			HccpConstants.RECLUSTER_TIME = Configuration.getDoubleConfig("WAIT_FOR_SCHEDULE_TIME");
		//if (Configuration.configExists("TOTAL_RUN_TIME"))
			HccpConstants.TOTAL_RUN_TIME = Configuration.getDoubleConfig("TOTAL_RUN_TIME")/Configuration.getDoubleConfig("NUMBER_OF_SCHEDULED_RUNS");
		//if (Configuration.configExists("NUMBER_OF_SCHEDULED_RUNS"))
			HccpConstants.NUMBER_OF_SCHEDULED_RUNS = Configuration.getIntConfig("NUMBER_OF_SCHEDULED_RUNS");
		//if (Configuration.configExists("ROUNDTABLE_TIME"))
			HccpConstants.ROUNDTABLE_TIME = Configuration.getDoubleConfig("ROUNDTABLE_TIME");
		//if (Configuration.configExists("SLEEP_TIME"))
			HccpConstants.SLEEP_TIME = Configuration.getDoubleConfig("SLEEP_TIME");
		//if (Configuration.configExists("SLEEP_NO_RECLUSTER_TIME"))
			HccpConstants.SLEEP_NO_RECLUSTER_TIME = Configuration.getDoubleConfig("SLEEP_NO_RECLUSTER_TIME");
		
		HccpConstants.SENSOR_MISSION_WEIGHT = Configuration.getDoubleConfig("hccp_sensor_mission_weight");
		HccpConstants.MESSAGE_QUEUE_WEIGHT = Configuration.getDoubleConfig("hccp_message_queue_weight");
		HccpConstants.BATTERY_POWER_WEIGHT = Configuration.getDoubleConfig("hccp_battery_power_weight");
		HccpConstants.RANDOM_WEIGHT = Configuration.getDoubleConfig("hccp_random_weight");
		HccpConstants.DUTY_CYCLE_WEIGHT = Configuration.getDoubleConfig("hccp_duty_cycle_weight");
		int simTime = 999999999;
		if (Configuration.configExists("simTime"))
			simTime = Configuration.getIntConfig("simTime");
		NetworkSim.initSim("hccp", simTime);
		
		RandomStream placement = new MRG32k3a("Random Stream for randomly placing nodes");
		
		// set up a test network.
		
		
		Networks.createFromConfig(placement);
		
		System.out.println("Created the network");
		
		
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
		System.out.println("Completed HCCP " + Configuration.getConfig("configName") + " in " +(System.currentTimeMillis() - start)/1000 + " seconds ("+((System.currentTimeMillis() - start)/60000)+" minutes)");
		System.out.println("\n\nDone Simulation!");

	}

}
