package sim_core;
import java.awt.Point;
import java.util.ArrayList;

import umontreal.iro.lecuyer.simevents.Sim;
import umontreal.iro.lecuyer.stat.Tally;

/*
 * Class Message
 * 
 * All messages in the simulator inherit from this class
 * Generic info is defined and initialized here
 */
public abstract class Message {
	
	static int addressLength = 2; // 2 or 8 bytes	
	static int nextID = 0;
	protected static ArrayList <Message> messages = new ArrayList<Message>();
	
	protected ArrayList<Node> messageIn = new ArrayList<Node>(); // TODO do this
	
	
	protected int timesReachedBaseStation;
	protected int id;
	protected int timesSent;
	private double timeCreated;
	protected int fromId;
	protected int originalCreator;
	// information about the message 
	protected int length;
	
	protected boolean forBS = true;
	
	protected String info;
	
	protected double closestIveBeenToSink;
	
	
	public double getClosestIveBeenToSink() {
		return closestIveBeenToSink;
	}

	public void setClosestIveBeenToSink(double newClosest) {
		if  (newClosest < closestIveBeenToSink)
			closestIveBeenToSink = newClosest;
	}

	public Message(String info, int from, int length)
	{
		// Initialize everything to nothing
		fromId = from;
		originalCreator = from;
		timesReachedBaseStation = 0;
		timesSent = 0;
		
		closestIveBeenToSink = Basestation.lastBS.getDistance( Node.getNodeFromId(from) );
		
		id = nextID;
		nextID++;
		
		
		
		timeCreated = Sim.time();
		this.info = info;
		
	}
	
	abstract public int getDestination();
	abstract public String getStats();
	public String getInfo()
	{
		return info;
	}
	public int getLength()
	{
		return length;
	}
	
	public int getFrom()
	{
		return fromId;
	}
	public String toString()
	{
		return info;
	}

	public void markComplete()
	{
		//timesReachedBaseStation++; is done in DataMessage
		
	}
	
	public double getTimeCreated()
	{
		return timeCreated;
	}
	
	public int reachedBSCount()
	{
		return timesReachedBaseStation;
	}
	
	public static String printAllMessageStats()
	{
		String out = "";
		for (Message m: messages){
			out += m.getStats() + "\n\n";
		}
		return out;
	}
	
	public static double messageStatsShort()
	{
		String out = "";
		int totalMessage = 0;
		int totalCompleted = 0;
		Tally quick = new Tally();
		for (Message m: messages){
			totalMessage++;
			if (m.timesReachedBaseStation > 0)
			{
				quick.add(m.timesReachedBaseStation);
				totalCompleted++;
			
			}
		}
		out = "Messages received to created " + totalCompleted + "/" + totalMessage + " = " + (totalCompleted/(double)totalMessage) + " with a max of " + quick.max() + " min " + quick.min() + "and avg "+quick.average() + " sd " + quick.standardDeviation();
		System.out.println(out);
		return (totalCompleted/(double)totalMessage);
	}
	
	public boolean isForBS()
	{
		return forBS;
	}
	
	
	/*** moved in from Message ***/
	public static String report()
	{
		String out = "";
		
		for (Message dm : messages)
		{
			out += dm.getStats()  + "\n";
		} 
		return out;
	}
	
	public static String reportForNode(int nodeId)
	{
		String out = "";
		int total = 0;
		int reachedBS = 0;
		
		for (Message dm : messages)
		{
			if (dm.originalCreator == nodeId)
			{
				total++;
				if (dm.timesReachedBaseStation > 0)
					reachedBS++;
				// check to see which nodes have this 
				// message. return the mote that is the closest
				// to the sink.
				//messageDistance.add( Basestation.getLastBS().getDistance( Node.getNodeFromId(dm.originalCreator)) - dm.getClosest() );
				
			}
		}
		double ratio = 0;
		if (total > 0)
			ratio = reachedBS/(double)total;
		out = "\nReached BS " + reachedBS + "/" + total + "=" + ratio + "\n";
		//out += "stats: average distance " + messageDistance.average() + " sd: " + messageDistance.standardDeviation() +"\n";
		return out;
	}
	
	protected static void summarizeData()
	{
		for (Message m : messages)
		{
			NetworkNode owner = (NetworkNode) Node.getNodeFromId(m.originalCreator);
			//owner.addToTally( m.getClosest(owner) );
			owner.addToTally( m.closestIveBeenToSink );
		}
	}
	
	public double getClosest(Node owner) {
		// get the closest the message was (if lost, consider it never left)
		Node bs = Basestation.getLastBS();
		NetworkNode temp;
		double dist = bs.getDistance( owner );
		
		if (this.timesReachedBaseStation > 0)
			dist = 0;
		else
		{
			for (Node n : Node.nodes)
			{
				if (n instanceof NetworkNode)
				{
					temp = (NetworkNode)n;
					if (temp.hasMessage(this)){
						if(dist > bs.getDistance(temp))
							dist = bs.getDistance(temp);
					}
				}
			}
		}
		
		return dist;
	}

	public static int getTotalNumberOfMessages(int nodeID)
	{
		int total = 0;
		for (Message dm : messages)
		{
			if (dm.originalCreator == nodeID)
			{
				total++;
				
			}
		}
		return total;
	}
	
	public static int getTotalNumberOfCompleted(int nodeID)
	{
		int reachedBS = 0;
		for (Message dm : messages)
		{
			if (dm.originalCreator == nodeID)
			{
				if (dm.timesReachedBaseStation > 0)
					reachedBS++;
			}
		}
		return reachedBS;
	}
	
	public static void reportAllNodes(ArrayList <Point> points)
	{
		for (Message dm : messages)
		{
			points.get(dm.originalCreator).x++;
			if (dm.timesReachedBaseStation > 0)
				points.get(dm.originalCreator).y++;
		}
	}

	public void addNodeHolder(Node n) {
		
		messageIn.add(n);
		
	}
	
	public static void reset()
	{
		messages.clear();
		nextID = 0;
		
	}
	
	
	
}
