package hccp_test;

import sim_core.Configuration;
import sim_core.NetworkSim;
import sim_core.Node;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;

public class HCCP_Multi_One_Config {
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Welcome to the HCCP Multi test");

		
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
		
		
		RandomStream placement = new MRG32k3a("Random Stream for randomly placing nodes");
		
		// set up a test network.
		
		
				
		System.out.println("Created the network");
		
		for (int i = 0; i < Configuration.getIntConfig("reps") ; i++)
		{
			for(int networkSize = Configuration.getIntConfig("networkSizeStart"); 
					networkSize < Configuration.getIntConfig("networkSizeTo");
					networkSize += Configuration.getIntConfig("networkSizeIncrement"))
			{
				
				// blow away all the existing motes
				Node.clearNodes();
				
				// create the new network.
				Networks.createFromConfig(placement);  // ~~~~~~~!!!!!!!!!!!!!!!!!!! NOT ACTUALLY CHANGING NETWORK SIZE
				
				for(double radioRange = Configuration.getDoubleConfig("radioRangeStart");
						radioRange < Configuration.getDoubleConfig("radioRangeTo");
						radioRange += Configuration.getDoubleConfig("radioRangeIncrement"))
				{
					
	
					NetworkSim.initSim("hccp", simTime);
					
					
					//Configuration.updateAllConfigs("clusterheadChance", ""+chChance);
					//Configuration.updateAllConfigs("suboptimalClusterheadPercentage", ""+subOptimal);
					
					NetworkSim.reset();
					
					// some redundant work. Whatever.
					for (Node n : Node.getNodes())
					{
						
						// change the range
						n.setRange(radioRange);
						
						
						
						if (n instanceof HccpSensorNode)
						{
							HccpSensorNode sn = (HccpSensorNode) n;
							//sn.setChanceOfBeingCH(chChance);
							//sn.setSuboptimalClusterheadPercentage(subOptimal);
							
							
							
							sn.setMission( Configuration.getDoubleConfig("sensorMission" ));
							sn.setChMarginPerc(  Configuration.getDoubleConfig( "hccp_ch_margin"));
							sn.setFirstOrderSuboptimalPercentage(Configuration.getDoubleConfig( "hccp_firstorder_ch_perc"));
							sn.setSuboptimalClusterheadPercentage(Configuration.getDoubleConfig( "suboptimalClusterheadPercentage"));
							sn.setFirstOrderChPercentage(Configuration.getDoubleConfig( "hccp_firstorder_ch_perc"));
						}
					}
				}
				
				
				
				
				System.out.println("hccp... ch: " + Configuration.getDoubleConfig("clusterheadChance") + " sub " + Configuration.getDoubleConfig("suboptimalClusterheadPercentage"));
				System.out.println("Sensor weight " +HccpConstants.SENSOR_MISSION_WEIGHT);
				System.out.println("Message Queue weight " + HccpConstants.MESSAGE_QUEUE_WEIGHT);
				System.out.println("Battery Weight " + HccpConstants.BATTERY_POWER_WEIGHT);
				System.out.println("Random Weight " +HccpConstants.RANDOM_WEIGHT);
				System.out.println("Duty cycle Weight "+HccpConstants.DUTY_CYCLE_WEIGHT);
								
				
				// gotime makes the new links, with the new radio range.
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
		System.out.println("\n\nDone Simulation!");

	}

}
