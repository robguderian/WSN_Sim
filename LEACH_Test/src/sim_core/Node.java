 package sim_core;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.FastScatterPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import umontreal.iro.lecuyer.simevents.Accumulate;
import umontreal.iro.lecuyer.simevents.Event;
import umontreal.iro.lecuyer.simevents.Sim;
import umontreal.iro.lecuyer.stat.Tally;
import CSMA.DataMessage;

/*
 * Class Node
 * All nodes in the simulation are inherited from this class
 */

public abstract class Node  {
	
	public static final int DISTANCE_TO_MESSAGE_X = 500;
	public static final int DISTANCE_TO_MESSAGE_Y = 500;
	
	
	//public static final double DEFAULT_RANGE = 100;
	//public static final double DEFAULT_BAUD = 54000; // 54kbps
	
	//public static final double DEFAULT_BATTERY_ON_DRAW = 2;
	//public static final double DEFAULT_BATTERY_OFF_DRAW = 0.5;
	public static double DEFAULT_BATTERY_UPDATE_TIME = 0.01; // in seconds
	
	private static float[] checkpoints = {0.10f,0.20f,0.30f,0.40f,0.50f,0.60f,0.70f,0.80f,0.90f, 1.0f};
	private static double[] checkpointTimes = new double [checkpoints.length];
	
	protected double onDraw;// = DEFAULT_BATTERY_ON_DRAW;
	protected double offDraw;// = DEFAULT_BATTERY_OFF_DRAW;

	protected static ArrayList<Node> nodes = new ArrayList<Node>();
	static int nextID = 0;

	protected IMessageHandler msgHandler;	
	
	protected ArrayList<Node> nodesInRange;
	
	protected int id;	
	protected Position position;	
	protected int readingArrivalRate; // -1 for no sensor readings. 	
	protected double baud;
	protected double range;

	protected double runtimeAsSensor;
	protected double runtimeAsClusterhead;
	protected int numberOfMessagesSent;
	protected int roundsUnclustered;
	
	protected int numberOfTimesAsClusterhead;
	protected int numberOfNodesInClusterTotal;
	
	protected static int round;
	protected static int averageCH;
	
	
	protected static int collisionCount;
	

	protected int beaconRank;
	protected int earliestBeaconReceived = Integer.MIN_VALUE;
	protected double diedAt = Integer.MIN_VALUE;
	protected int diedAtRound = Integer.MIN_VALUE;
	
	protected Battery battery;
	protected BatteryUpdate batteryUpdate;
	
	
	protected String name;
	protected boolean isClusterhead = false;
	
	protected Accumulate timeOn;
	
	protected Node following = null;
	protected ArrayList<Node> clustermembers = new ArrayList<Node>();
	
	protected static int numMessagesSent = 0;
	protected static int numMessagesReceived = 0;
	
	protected int messagesThrough = 0;
	
	protected boolean careIfAlive = true;

	
	
	/***** instance methods *****/
	
	
	
	
	
	public Node(int x, int y, String name)
	{
		this(x,y,name, Configuration.getIntConfig("range"));
	}
	public Node (int x, int y, String name, double range)
	{
		this(x,y,name, range, Configuration.getDoubleConfig("onDraw"), Configuration.getDoubleConfig("offDraw"));
	}
	public Node (int x, int y, String name, double range, double onDrawIn, double offDrawIn)
	{
		this( x, y, name, range,  onDrawIn,  offDrawIn, Configuration.getIntConfig("battery"));
		
	}
	public Node (int x, int y, String name, double range, double onDrawIn, double offDrawIn, int batteryPower)
	{
		// Initialize everything to nothing or default
		
		DEFAULT_BATTERY_UPDATE_TIME = Configuration.getDoubleConfig("batteryUpdateTime");
		
		baud = Configuration.getIntConfig("baud");
		
		position = new Position(x,y);		
		id = nextID;
		nextID++;
		
		this.range = range; // set the default 
		
		// keep tabs of all nodes
		nodes.add(this);		
		
		
		
		// initialize the list that holds the nodes that this node can send to
		nodesInRange = new ArrayList<Node>();
		
		// This node does not send messages (except ACK, and CH elections) so does not need 
		// to init any messages
		
		this.name = name;
		
		onDraw = onDrawIn;
		offDraw = offDrawIn;
		
		battery = new Battery(batteryPower);
		
		
		
	
	}
	public boolean keepGoing()
	{
		int aliveCount=0;
		int totalCount=0;
		boolean kg;
		
		
		for (int i = 0 ; i < nodes.size();i++)
		{
			if (!(nodes.get(i) instanceof Basestation))
			{
				totalCount++;
				if (nodes.get(i).careIfAlive())
					aliveCount++;
			}
		}
		
		// check for checkpoints
		// done somewhat inefficiently, but, it's not a bottleneck
		for (int i = 0; i < checkpoints.length; i++)
		{
			// if the percentage alive is greater than the careabout
			if ( 1 - (aliveCount/(double)totalCount) > checkpoints[i])
			{
				// log to file - append?
				// store for later, so time stamps match? Yeah.
				checkpointTimes[i] = Sim.time();
				checkpoints[i] = 101;
			}
		}
		
		
		if (Configuration.getDoubleConfig("runUntilPercDead") == 1 )
		{
			// if there's one alive... keep going.
			if (aliveCount > 0)
				kg = true;
			else
				kg = false;
			
		}
		else if (aliveCount/(double)totalCount > 1 - Configuration.getDoubleConfig("runUntilPercDead"))
			kg = true;
		else
		{
			kg = false;
			System.out.println("Last node died at " + Sim.time());
		}
		return kg;
	}
	/*
	 * start receiving a message
	 */
	public void receiveMessage(Packet p)
	{
		msgHandler.receiveMessage(p);
	}
	
	/*
	 * the receive is complete, process the message
	 */
	abstract public void receiveComplete(Message m);
	
		
	
	public ArrayList<Node> getNodesInRange()
	{
		return nodesInRange;
		
	}
	
	public double getBaud()
	{
		return this.baud;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public int getId()
	{
		return id;
	}
	
	

	public double x()
	{
		return position.x;
	}
	
	public double y()
	{
		return position.y;
	}
	
	public void setX(double newX)
	{
		position.x = newX;
	}
	public void setY(double newY)
	{
		position.y = newY;
	}
	
	public String toString()
	{
		return name;
	}
	
	public static int getRound()
	{
		return round;
	}
	
	

	
	
	public boolean inRange(Node m) {
		if ( Math.sqrt( Math.pow( m.position.x - position.x, 2 ) + 
				Math.pow( m.position.y - position.y, 2 )  )  <= range)
			return true;
		return false;
	}
	
	public double getDistance(Node m){
		return ( Math.sqrt( Math.pow( m.position.x - position.x, 2 ) + 
				Math.pow( m.position.y - position.y, 2 )  ));
			
	}
	
	/*
	 * Called from the message handler when the message has been sent successfully.
	 * This allows this node to send anther message
	 */
	abstract public void sendComplete(Message m);
	
	/**** static methods ****/
	
	public static Node getNodeFromId(int id)
	{
		Node returnNode = null;
		// I realize I could do nodes.get(id), but a node could be deleted.
		for (int i = 0; i < nodes.size() && returnNode == null; i++)
		{
			if (nodes.get(i).id == id)
				returnNode = nodes.get(i);
				
		}
		return returnNode;
	}
	
	public static void collisionHappened()
	{
		collisionCount++;
	}
	
	public static void makeLinks()
	{
		/*
		 * Iterate through the list of nodes, and make links between them
		 * Links are 1 way
		 */
		
		
		System.out.println("Creating the links");
		for (Node n : nodes)
		{
			// drop all existing links
			n.nodesInRange.clear();
			
			
			for (Node m : nodes)
			{
				// don't link to yourself, and make sure it's in range
				if (n != m && n.inRange(m))
				{
					n.nodesInRange.add(m);
					if (Configuration.verbose)
						System.out.println("Node " + n + " connecting to " + m);
				}	
			}
		}
		System.out.println("Links created");
	}

	/**
	 * @return the runtimeAsSensor
	 */
	public double getRuntimeAsSensor() {
		return runtimeAsSensor;
	}

	
	/**
	 * @return the runtimeAsClusterhead
	 */
	public double getRuntimeAsClusterhead() {
		return runtimeAsClusterhead;
	}

	/**
	 * @return the total runtime
	 */
	public double getTotalRuntime(){
		return getRuntimeAsClusterhead() +  getRuntimeAsSensor();
	}
	
	public String report()
	{
		NetworkNode n = null;
		if (this instanceof NetworkNode)
			n = (NetworkNode)this;
		
		String out = "Stats for " + name + "\n";
		out += "id was : " + id + "\n";
		out += "Time as CH: " + runtimeAsClusterhead + "\n";
		out += "Sensor Timeslice Total: " + runtimeAsSensor+"\n";
		out += "Nodes in range:" + this.nodesInRange.size() + "\n";
		if (numberOfTimesAsClusterhead == 0)
			out += "Average cluster size: 0\n";
		else
			out += "Average cluster size: "+ (this.numberOfNodesInClusterTotal / this.numberOfTimesAsClusterhead) + "\n";
		out += "Number of rounds run "+ this.diedAtRound +"\n";
		out += "Beacon at end: " + this.beaconRank + "\n";
		out += "EarliestBeacon at " + this.earliestBeaconReceived + "\n";
		out += "Died at " + this.diedAt + "\n";
				
		if (!(this instanceof Basestation) && n.messageTally.numberObs() > 3)
		{
			out += "messages travelled " + n.messageTally.average() + " sd. " + n.messageTally.standardDeviation() + "\n";
			out += Message.reportForNode(id);
		}
		if (this instanceof SensorNode)
		{
			SensorNode sn = (SensorNode) n;
			out += "reading frequency: " + sn.readingFrequency + "\n";
			out += "queue size: " + sn.queueSize.report() + "\n";
		}
		return out;
	}
	
	
	protected double messageReport()
	{
		/*Tally messageDistance = new Tally();
		
		for (Message m : Messages)*/
		return 0.;
	}
	
	protected double messageSD()
	{
		return 0;
	}
	
	public int getNumberOfRoundsRun()
	{
		return this.diedAtRound;
	}
	
	public static int getAverageCH() {
		return averageCH;
	}
	
	
	public static String nodeReports()
	{
		
		return "";
	}
	
	public static void makeReportChartData(IReportCallback report)
	{
		
		
		/****** stolen from the above method*****/
		
		String strout = "";
		
		long averageTimealive =0 ;
		long averageRoundsAlive = 0;
		int count  = 0;
		int lost = 0;
		int totalQueueMessagesLost = 0;
		int averageUnClustered = 0;
		// prep the data
		for (Node n : nodes)
		{
			if (n instanceof NetworkNode)
			{
				NetworkNode nn = (NetworkNode)n;
				
				count++;
				if (nn.isAlive())
				{
					averageTimealive += Sim.time();
					averageRoundsAlive += Node.round;
				}
				else
				{
					averageTimealive += nn.diedAt;
					averageRoundsAlive += nn.diedAtRound;
				}
				averageUnClustered += nn.roundsUnclustered;
				
				totalQueueMessagesLost += nn.messagesLostDueToQueue;
				
				for (Message m : nn.messageQueue)
				{
					if ( m instanceof DataMessage)
					{
						//DataMessage dm = (DataMessage)m;
						//NetworkNode owner = (NetworkNode)Node.getNodeFromId( dm.getOriginalFrom() );
						//owner.messageTally.add( m.getClosestIveBeenToSink()  );
					
						m.addNodeHolder(n);
					}
				}
			}
			
		}
		
		for (Message m : Message.messages)
		{
			if (m.timesReachedBaseStation == 0 && m.messageIn.size() == 0)
				lost++;
			if ( m instanceof DataMessage)
			{
				DataMessage dm = (DataMessage)m;
				NetworkNode owner = (NetworkNode)Node.getNodeFromId( dm.getOriginalFrom() );
				owner.messageTally.add( m.getClosestIveBeenToSink()  );
				
				
			}
		}
		
		double timeAlive = averageTimealive/(double)count;
		double avgRounds = averageRoundsAlive/(double)count;
		double avgUnClust = (averageUnClustered/(double)count);
		System.out.println("Average time alive:" + timeAlive + " average rounds: " + 
				avgRounds + " avg times unclustered "+ avgUnClust  +"\n");
		System.out.println(lost + " messages lost, " +collisionCount + " collisions happened.");
		System.out.println("lost " + totalQueueMessagesLost + " because of queue sizes");
		System.out.println("Sent " + numMessagesSent  + " receieved " + numMessagesReceived);
		
		
		if (Configuration.getBooleanConfig("printNodeReports"))
			for (Node n : nodes)
			{
				strout += n.report();
			
				strout += "\n--------\n\n";
			}
		
		System.out.println(strout);
		
		/****** end stolen stuff ****************/
		
		
		
		
		
		ArrayList <Basestation> bases = new ArrayList <Basestation>();
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh.mm.SSS");
		
		String compName = "unknown";
		try
		{
		    InetAddress addr = InetAddress.getLocalHost();
		    compName = addr.getHostName();
            
        } 
        catch (UnknownHostException e) {
        }
        
        String strDate =  dateFormat.format(new Date());

        double averageAverage = 0;

        Tally averageTimeOn = new Tally();
        Tally averageDiedAt = new Tally();
        
        if (System.getProperty("os.name").indexOf("Mac") != -1)
        	Configuration.updateAllConfigs("outputFolder", "macOutput");
		
		for (Node n:Node.nodes)
		{
			if (n instanceof Basestation)
			{
				bases.add( (Basestation)n);
			}
		}
		
		// find the closest base station for each node
		// plot distance from BS vs # messages through
		ArrayList <Point> points = new ArrayList<Point>();
		
		// kludge!
		for (int i = 0; i < Node.nodes.size()+4; i++)
			points.add(new Point(0,0));
		DataMessage.reportAllNodes(points);
		
		// makes points into a 2d array.
		float [][] data = new float[2][Node.nodes.size() - bases.size()];
		
		int j = 0;
		try
		{
			System.out.println("Writing to "+ Configuration.getConfig("outputFolder") +"/"+ Configuration.getConfig("configName") + NetworkSim.getNetworkType() + "NodeData-"+compName+"-"+strDate+".csv");
			
			BufferedWriter out = null;
			
			if (Configuration.getBooleanConfig("writeNodeData"))
			{			
				out = new BufferedWriter(new FileWriter("./"+ Configuration.getConfig("outputFolder") +"/"+ Configuration.getConfig("configName") + NetworkSim.getNetworkType() + "NodeData-"+compName+"-"+strDate+".csv"));
				out.write(report.getTitles() + "Node,distance From sink,messages made,messages completed,rounds completed,finalRank,x,y,diedAt,nodes in Range,earliestBeacon,time as ch,time as sensor,message distance travelled,sd,stillAlive,average queue size,max queue size,sd,final queue size,numberOfTimesUnclustered,average Time on,messagesLostDueToQueue,hop messages,freq in\n");
			}
			for (Node n: Node.nodes)
			{
				if (n instanceof SensorNode)
				{
					SensorNode sn = (SensorNode)n;
					// find the closest base station.
					float distance = Float.MAX_VALUE;
					for (Node base : bases)
					{
						float newDistance = (float)n.getDistance(base);
						if ( newDistance < distance  )
							distance = newDistance;
					}
					data[0][j] = distance;
					data[1][j] = points.get(n.getId()).y/(float)points.get(n.getId()).x;// x is total, y is received
					if (Configuration.getBooleanConfig("writeNodeData"))
						out.write(report.getImportantData() + n.getName() + "," + distance + ","+points.get(n.getId()).x+"," +
								points.get(n.getId()).y + ", " + n.diedAtRound + "," + n.beaconRank +", " + n.x() + 
								", " + n.y() +"," + n.diedAt + ", " + n.nodesInRange.size() +"," + 
								n.earliestBeaconReceived+","+n.runtimeAsClusterhead+","+n.runtimeAsSensor+","+
								n.messageReport()+","+n.messageSD()+","+n.isAlive()+","+
								sn.queueSize.average()+"," + sn.queueSize.max() + ",\"donno\","+sn.messageQueue.size() +","+ sn.roundsUnclustered +","
								+ sn.timeOn.average() +","+ sn.messagesLostDueToQueue +","+ sn.messagesThrough +","+sn.getFreq()+"\n");
					
					averageAverage += sn.timeOn.average(); 
					averageTimeOn.add(sn.timeOn.average());
					if (n.getCareIfAlive() && !n.isAlive() && n.diedAt > 0)
						averageDiedAt.add(sn.diedAt);
					
					j++;
				}
			}
			if (Configuration.getBooleanConfig("writeNodeData"))
				out.close();
		}
		catch(Exception e)
		{
			System.out.println("Writing file crapped out");
			e.printStackTrace();
		}
		
		System.out.println("Average average time on: " + (averageAverage/nodes.size()) );
		
		
		// Print out stats to an output file.
		// Make it a csv with a well-known header... so we only need the data.		
		// columns
		// ch percentage, sub perc, 1st order sub, sink sleep %, first died time, last died time, average died time, %
		
		//Tally messageStats
		
		String shortOut =  	report.printReport(averageDiedAt, Message.messageStatsShort());
		
		shortOut += totalQueueMessagesLost+ ","+avgUnClust+","+ collisionCount + ","+ numMessagesSent  + "," + numMessagesReceived;
		
		// write this to a file
		try{
			BufferedWriter out = new BufferedWriter(new FileWriter("./"+ Configuration.getConfig("outputFolder") +"/"+ Configuration.getConfig("configName") + NetworkSim.getNetworkType() + "short-"+strDate+"."+compName+".csv"));
			out.append(shortOut + "\n");
			out.close();
		}
		catch(Exception e)
		{
			System.out.println("Writing short file crapped out");
			e.printStackTrace();
		}
		
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter("./"+ Configuration.getConfig("outputFolder") +"/"+ Configuration.getConfig("configName") + NetworkSim.getNetworkType() + "checkpoints-"+strDate+"."+compName+".csv"));
			
			
			out.write( report.getImportantData() );
			for (int i = 0 ; i < checkpoints.length; i++)
			{
				out.write(  checkpointTimes[i] + ",");
			}
			out.write("\n");
			
			
			out.close();
			
		}
		catch(Exception e)
		{
			System.out.println("writing checkpoints is teh err");
			e.printStackTrace();
		}
		// Mac lab machines fail hard on this.
		if ( System.getProperty("os.name").indexOf("Mac") == -1 && Configuration.getBooleanConfig("writeCharts"))
		{
			
			NumberAxis domainAxis = new NumberAxis("X");
	        domainAxis.setAutoRangeIncludesZero(true);
	        domainAxis.setTickUnit(new NumberTickUnit(50));
	        NumberAxis rangeAxis = new NumberAxis("Y");
	        rangeAxis.setRange(0,1);
	        DecimalFormat formatter = new DecimalFormat("0.##%");
	        rangeAxis.setTickUnit(new NumberTickUnit(0.05, formatter));
	
	        FastScatterPlot plot = new FastScatterPlot(data, domainAxis, rangeAxis);
	        
	        
	        JFreeChart chart = new JFreeChart("Fast Scatter Plot", plot);
	
	        
	        
	        String chartName = "ChartRun-";
	
	        if (Configuration.configExists("configName"))
	        	chartName = Configuration.getConfig("configName") + "-";
	        
	        //DateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmm");
	        
	        chartName = chartName + strDate; //dateFormat.format(new Date());
	
	        		
	        
	        try {
				ChartUtilities.saveChartAsJPEG(new File ("./"+ Configuration.getConfig("outputFolder") +"/DistanceToMessagesFastScatter-"+ NetworkSim.getNetworkType() + "-" + chartName + ".jpeg"), chart, DISTANCE_TO_MESSAGE_X, DISTANCE_TO_MESSAGE_Y);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			XYSeries dataset = new XYSeries("Node distance from BS to Number of message successfully sent");
			XYSeriesCollection d = new XYSeriesCollection();
			d.addSeries(dataset);
			
			for (int i = 0; i < data[0].length; i++)
				dataset.add(data[0][i], data[1][i]);
			
			JFreeChart jfc = ChartFactory.createScatterPlot("Distance Vs Completed Messages", "Distance from BS", "Messages Recieved at BS (percentage)",
					 d, PlotOrientation.VERTICAL, true, true, false);
			
			XYPlot plotv2 = (XYPlot) jfc.getPlot();
			NumberAxis y = (NumberAxis) plotv2.getRangeAxis();
			y.setTickUnit(new NumberTickUnit(0.05, formatter));
			y.setRange(0,1);
			
			//XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
	        //renderer.setSeriesLinesVisible(0, true);
	        //plot.setRenderer(renderer);
			
			try {
				ChartUtilities.saveChartAsJPEG(new File ("./"+ Configuration.getConfig("outputFolder") +"/DistanceToMessagesFastScatter2-"+ NetworkSim.getNetworkType() + "-"+chartName+".jpeg"), jfc, DISTANCE_TO_MESSAGE_X, DISTANCE_TO_MESSAGE_Y);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			NumberAxis x = (NumberAxis) plotv2.getDomainAxis();
			x.setAutoRangeIncludesZero(true);
			x.setTickUnit(new NumberTickUnit(50));
			try {
				ChartUtilities.saveChartAsJPEG(new File ("./"+ Configuration.getConfig("outputFolder") +"/DistanceToMessagesFastScatter3-"+ NetworkSim.getNetworkType() + "-"+chartName+".jpeg"), jfc, DISTANCE_TO_MESSAGE_X, DISTANCE_TO_MESSAGE_Y);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Done csv-ing on " + compName);
	}
	
	public boolean isAlive()
	{
		return battery.isAlive();
	}
	
	
	public boolean careIfAlive()
	{
		if (battery.isAlive() && careIfAlive)
		{
			return true;
		}
		return false;
	}
	
	public boolean getCareIfAlive()
	{
		return careIfAlive;
	}
	
	public boolean isClusterhead() {
		return isClusterhead;
	}
	
	public void hadCollision()
	{
		// ignore, allow to be overridden
	}
	

	public class BatteryUpdate extends Event
	{
		public void actions()
		{
			if (msgHandler.isOn())
			{
				battery.draw(onDraw);
				timeOn.update(1);
			}
			else
			{
				battery.draw(offDraw);
				timeOn.update(0);
			}
			if (battery.isAlive())
			{
				batteryUpdate.cancel();
				batteryUpdate = new BatteryUpdate();
				batteryUpdate.schedule(DEFAULT_BATTERY_UPDATE_TIME);
			}
			else
			{
				// stop accumulating stats
				timeOn.setCollecting(false);
			}
		}
	}

	public static void resetAverageCH() {
		averageCH = 0;
	}
	public Node getFollowing() {
		return following;
	}
	
	public boolean hasClustermember(Node toCheck) {
		for (Node n : clustermembers)
		{
			if(n == toCheck)
				return true;
		}
		return false;
	}
	public boolean hasClustermember(int toCheck) {
		return hasClustermember(Node.getNodeFromId(toCheck));
	}
	public void setCareIfAlive(boolean careIfAlive) {
		this.careIfAlive = careIfAlive;
	}
	
	
	public static void evenlySpace(ArrayList<Node> toPlace, double maxX, double maxY)
	{
		int rowCount = (int) Math.sqrt(toPlace.size());
		
		double deltaX = maxX/((double)rowCount + 1);
		double deltaY = maxY/((double)rowCount + 1);
		
		double currX = deltaX;
		double currY = deltaY;
		int currIndex = 0;
		
		for (int i = 0; i < rowCount; i++)
		{
			for (int j = 0; j < rowCount; j++)
			{
				toPlace.get(currIndex).setX(currX);
				toPlace.get(currIndex).setY(currY);
				
				currX += deltaX;
				currIndex++;
			}
			currX = deltaX;
			currY += deltaY;
		}
		
	}
	
	
	
	public static void reset()
	{
		nextID = 0;
		round = 0;
		collisionCount = 0;
		
		for (int i = 0 ; i < checkpoints.length ; i++)
		{
			checkpoints[i] = (i+1)/(float)10;
			checkpointTimes[i] = 0;
		}
		
		for (Node n : nodes)
		{
			n.resetNode();
		}
	}
	
	protected void resetNode()
	{
		
		
		runtimeAsSensor = 0;
		runtimeAsClusterhead = 0;
		numberOfMessagesSent =0;
		roundsUnclustered = 0;
		
		numberOfTimesAsClusterhead = 0;
		numberOfNodesInClusterTotal = 0;
		
		beaconRank = 0;
		earliestBeaconReceived = Integer.MIN_VALUE;
		diedAt = Integer.MIN_VALUE;
		diedAtRound = Integer.MIN_VALUE;
		
		isClusterhead = false;
		
		timeOn = new Accumulate();
		
		following = null;
		numMessagesSent = 0;
		numMessagesReceived = 0;
		messagesThrough = 0;
		messagesThrough = 0;
		
		clustermembers.clear();
		
	
		timeOn = new Accumulate();
			
		battery.reset();
		if ( !Configuration.getBooleanConfig("motesNeverDie") && onDraw > 0 && offDraw > 0   )
		{
			if (batteryUpdate != null)
				batteryUpdate.cancel();
			batteryUpdate = new BatteryUpdate();
			batteryUpdate.schedule(DEFAULT_BATTERY_UPDATE_TIME);
		}
		
	}
	
	public static ArrayList<Node> getNodes()
	{
		return nodes;
	}
	
	public static void clearNodes()
	{
		nodes.clear();
	}
	
	public void setRange(double newRange)
	{
		range = newRange;
	}
	
	
}
