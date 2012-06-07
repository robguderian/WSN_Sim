package hccp_test;

import sim_core.Configuration;
import sim_core.NetworkSim;
import sim_core.Node;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;

public class HCCP_Multi_Goodness {
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Welcome to the HCCP Multi Goodness test");

		
		long start = System.currentTimeMillis();
		
		
		String filename = "biggerDaisyChainSuboptimalNoSleep.txt";
		
		
		if (args.length >= 1)
			filename = args[0];
		
		
			
		
		Configuration.configurationReader(filename);
		
		for (int i = 1; i < args.length; i++)
			Configuration.additionalConfig(args[i]);
		
			
		
		if (Configuration.configExists("allowSinkSleep"))
			HccpConstants.allowSinkSleep = Configuration.getBooleanConfig("allowSinkSleep");
		
		if (Configuration.configExists("allowSuboptimalClusterheads"))
			HccpConstants.allowSuboptimalClusterheads = Configuration.getBooleanConfig("allowSuboptimalClusterheads");
		
		HccpConstants.sinkSleepPercentage = Configuration.getDoubleConfig("sinkSleepPercentage");
		
		HccpConstants.RECLUSTER_TIME = Configuration.getDoubleConfig("RECLUSTER_TIME");
		HccpConstants.CHOOSE_CLUSTERHEAD_TIME = Configuration.getDoubleConfig("CHOOSE_CLUSTERHEAD_TIME");
		HccpConstants.WAIT_FOR_SCHEDULE_TIME = Configuration.getDoubleConfig("WAIT_FOR_SCHEDULE_TIME");
		HccpConstants.TOTAL_RUN_TIME = Configuration.getDoubleConfig("TOTAL_RUN_TIME")/Configuration.getDoubleConfig("NUMBER_OF_SCHEDULED_RUNS");
		HccpConstants.NUMBER_OF_SCHEDULED_RUNS = Configuration.getIntConfig("NUMBER_OF_SCHEDULED_RUNS");
		HccpConstants.ROUNDTABLE_TIME = Configuration.getDoubleConfig("ROUNDTABLE_TIME");
		HccpConstants.SLEEP_TIME = Configuration.getDoubleConfig("SLEEP_TIME");
		HccpConstants.SLEEP_NO_RECLUSTER_TIME = Configuration.getDoubleConfig("SLEEP_NO_RECLUSTER_TIME");
		
		HccpConstants.SENSOR_MISSION_WEIGHT = Configuration.getDoubleConfig("hccp_sensor_mission_weight");
		HccpConstants.MESSAGE_QUEUE_WEIGHT = Configuration.getDoubleConfig("hccp_message_queue_weight");
		HccpConstants.BATTERY_POWER_WEIGHT = Configuration.getDoubleConfig("hccp_battery_power_weight");
		HccpConstants.RANDOM_WEIGHT = Configuration.getDoubleConfig("hccp_random_weight");
		HccpConstants.DUTY_CYCLE_WEIGHT = Configuration.getDoubleConfig("hccp_duty_cycle_weight");
		int simTime = 999999999;
		if (Configuration.configExists("simTime"))
			simTime = Configuration.getIntConfig("simTime");
		
		MRG32k3a origRNG = new MRG32k3a("Random Stream for randomly placing nodes");
		
		RandomStream placement = origRNG.clone();
		
		// set up a test network.
		
		
		//placement.
		
		
		
		System.out.println("Created the network");
		
		for (double b = Configuration.getDoubleConfig("batteryPowerFrom"); 
				b < Configuration.getDoubleConfig("batteryPowerTo"); 
				b += Configuration.getDoubleConfig("batteryPowerIncrement") )
		{
			//for( double s = 0; s < 1 - b ; s += Configuration.getDoubleConfig("batteryPowerIncrement"))
			for( double s = Configuration.getDoubleConfig("sensorFrom"); s < 1 - b && s < Configuration.getDoubleConfig("sensorTo") ; s += Configuration.getDoubleConfig("sensorIncrement"))
				for (double q = 0; q < 1 - s - b; q += Configuration.getDoubleConfig("batteryPowerIncrement"))
					for (double r = 0; r < 1 - s - b - q; r+= Configuration.getDoubleConfig("batteryPowerIncrement") )
						for (double chChance = Configuration.getDoubleConfig("chChanceFrom");
								chChance <=Configuration.getDoubleConfig("chChanceTo");
								chChance += Configuration.getDoubleConfig("chChanceIncrement") )
						{
							// temp line of code to make my life easier
							if (true)// && q > 0.01 && r < 0.2 && s < 0.01 && 1 - s - q - b - r < 0.1)
							{
								// drop all existing nodes
								placement = origRNG.clone();
								Node.clearNodes();
								System.out.println("Number of nodes in system: "+ Node.getNodes().size());
								
								
								
								
								HccpConstants.SENSOR_MISSION_WEIGHT = s;
								HccpConstants.MESSAGE_QUEUE_WEIGHT = q;
								HccpConstants.BATTERY_POWER_WEIGHT = b;
								HccpConstants.RANDOM_WEIGHT = r;
								HccpConstants.DUTY_CYCLE_WEIGHT = 1 - s - q - b - r ;
								
								
								Configuration.updateAllConfigs("clusterheadChance", ""+chChance);
								
								NetworkSim.initSim("hccpGoodness", simTime);
								
								Networks.createFromConfig(placement);
								placement = origRNG.clone();
								NetworkSim.reset();
								
								
								
								System.out.println("hccp... ch: " + Configuration.getDoubleConfig("clusterheadChance") + " sub " + Configuration.getDoubleConfig("suboptimalClusterheadPercentage"));
								System.out.println("Sensor weight " +HccpConstants.SENSOR_MISSION_WEIGHT);
								System.out.println("Message Queue weight " + HccpConstants.MESSAGE_QUEUE_WEIGHT);
								System.out.println("Battery Weight " + HccpConstants.BATTERY_POWER_WEIGHT);
								System.out.println("Random Weight " +HccpConstants.RANDOM_WEIGHT);
								System.out.println("Duty cycle Weight "+HccpConstants.DUTY_CYCLE_WEIGHT);
												
								
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
							}
							
						}
		}
		System.out.println("\n\nDone Simulation!");

	}

}
