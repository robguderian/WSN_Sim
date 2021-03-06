package leach_test;


import java.util.ArrayList;

import sim_core.Configuration;
import sim_core.Node;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;
import Util.Rand1;

public class Networks {
	
	static void createRandomNetwork( RandomStream placement, int numberOfNodes, int maxX, int maxY )
	{
		/*
		 * create random nodes, placed randomly.
		 */
		
		// Clone the given mote the number of times given. Put within
		// the bounds of the 0 and maxX/maxY
		for (int i = 0 ; i < numberOfNodes; i++)
		{
			//(int x, int y, String name, double readingFreqencyIn, double range)
			new LeachSensorNode(placement.nextInt(0, maxX), placement.nextInt(0, maxY),"sensornode-" + i);
		}
		
	}
	
	static void ticToc()
	{
		//create two nodes to 'tic toc'.
		new LeachBasestation(100,200, "Base station");
		new LeachSensorNode(100,300,"sensor1");
		
	}
	
	static void BiggerTicToc()
	{
		new LeachBasestation(100,200, "Base station");
		new LeachSensorNode(100,300,"sensor1");
		new LeachSensorNode(100,400,"sensor2");
		new LeachSensorNode(200,100,"sensor3");
	}
	
	static void DaisyChain()
	{
		
		new LeachBasestation(0,0, "Base station", 100);
		new LeachSensorNode(0,75,"sensor1", 10, 100, 0.75);
		new LeachSensorNode(0,150,"sensor2", 10, 100, 0);
	}
	
	static void BiggerDaisyChain()
	{
		
		new LeachBasestation(0,0, "Base station", 100);
		new LeachSensorNode(0,75,"sensor1", 10, 100, 0.75);
		new LeachSensorNode(0,150,"sensor2", 10, 100, 0.75);
		new LeachSensorNode(0,225,"sensor3", 10, 100, 0.7);
		new LeachSensorNode(0,300,"sensor4", 10, 100, 0.5);
		new LeachSensorNode(0,375,"sensor5", 10, 100, 0);
	}
	
	public static void createFromConfig(RandomStream placement) 
	{
		RandomStream batteryRM = new MRG32k3a();
		
		// create the base station
		new LeachBasestation(
				Configuration.getIntConfig("baseStationX"), 
				Configuration.getIntConfig("baseStationY"),
				"BaseStation", 
				Configuration.getDoubleConfig("range") 
				);
		
		String[] nodeTypes = Configuration.getNodeConfigNames();
		ArrayList <Node> thisSet = new ArrayList<Node>();
		
		for (String node : nodeTypes)
		{
			int count = Configuration.getIntConfig(node, "count");
			for (int i = 0; i < count ; i++)
			{
				if (node.startsWith("bs"))
					new LeachBasestation(
							placement.nextInt(Configuration.getIntConfig(node, "minX"), Configuration.getIntConfig(node, "maxX")),
							placement.nextInt(Configuration.getIntConfig(node, "minY"), Configuration.getIntConfig(node, "maxY")),
							node+"-"+i, 
							Configuration.getIntConfig(node, "range")
							);
					
				else
				{
					int batteryGiven = Configuration.getIntConfig(node, "battery");
					if (Configuration.configExists(node, "batteryVariance"))
					{
						batteryGiven += Rand1.uniform(batteryRM, 
								-1 *Configuration.getIntConfig(node,"batteryVariance"), 
								Configuration.getIntConfig(node,"batteryVariance"));
					}
				
					LeachSensorNode n = new LeachSensorNode(
							placement.nextInt(Configuration.getIntConfig(node, "minX"), Configuration.getIntConfig(node, "maxX")),
							placement.nextInt(Configuration.getIntConfig(node, "minY"), Configuration.getIntConfig(node, "maxY")),
							node+"-"+i, 
							Configuration.getIntConfig(node, "sensorReadingFreq"), 
							Configuration.getIntConfig(node, "range"), 
							//Configuration.getDoubleConfig (node,"clusterheadChance"), // chopped intentionally.
							Configuration.getDoubleConfig ("clusterheadChance"),
							Configuration.getDoubleConfig(node, "onDraw"),
							Configuration.getDoubleConfig(node, "offDraw"),
							batteryGiven,
							Configuration.getIntConfig(node, "messageQueueSize"));
					n.setCareIfAlive(Configuration.getBooleanConfig(node, "care_if_alive"));
					
					thisSet.add(n);
					
				}
			}
			if ( Configuration.getBooleanConfig(node, "evenlySpace") )
				Node.evenlySpace(thisSet, Configuration.getDoubleConfig(node, "maxX"), Configuration.getDoubleConfig(node, "maxY"));
		}
		
		
		
		thisSet.clear();
		
		
	}
}
