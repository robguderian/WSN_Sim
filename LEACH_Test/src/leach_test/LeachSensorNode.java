package leach_test;


import sim_core.Basestation;
import sim_core.Configuration;
import sim_core.IClusterhead;
import sim_core.Message;
import sim_core.Node;
import sim_core.SensorNode;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;
import umontreal.iro.lecuyer.simevents.Event;
import umontreal.iro.lecuyer.simevents.Sim;
import CSMA.BroadcastMessage;
import CSMA.CSMAMessage;
import CSMA.CSMAMessageHandler;
import CSMA.DataMessage;
import Util.Rand1;
import Util.Utility;

public class LeachSensorNode extends SensorNode implements IClusterhead {
	
	//final static double DEFAULT_PERCENTAGE_CH = 0.1;
	final static int DEFAULT_BEACON_RANK =100;
	
	private double chanceOfBeingCH;

	private boolean debug = false;
	private int lastRoundAsClusterhead;
	RandomStream clusterheadRM;
	
	LeachStage LStage;
	
	double delay = 0;
	double runtime = 0;
	
	
		
	// beacon routing - keep track of when beacon was heard.
	
	int beaconTime = Integer.MIN_VALUE; // <--- seconds since the beacon was heard.
	double beaconReceivedAt;
	double reliableTime;
	
	private double A_JIFFY = 0.0000001;

	
	public LeachSensorNode(int x, int y, String name, double readingFreqencyIn, double range)
	{
		this(x,y,name,readingFreqencyIn, range,  Configuration.getDoubleConfig("clusterheadChance"));
	}
	
	public LeachSensorNode(int x, int y, String name, double readingFrequency) {
		this(x, y, name, readingFrequency, Configuration.getIntConfig("range"));
	}
	public LeachSensorNode(int x, int y, String name) {
		this(x,y,name, Configuration.getIntConfig("sensorReadingFreq"));
	}
	
	public LeachSensorNode(int x, int y, String name, double readingFreqencyIn, double range, double chPercentage) {
		this(x,y,name,readingFreqencyIn, range,  Configuration.getDoubleConfig("clusterheadChance"),Configuration.getDoubleConfig("onDraw"),Configuration.getDoubleConfig("offDraw"));
	}
	
	public LeachSensorNode(int x, int y, String name, double readingFreqencyIn, double range, double chPercentage, double onDrawIn, double offDrawIn)
	{
		this( x,  y,  name,  readingFreqencyIn,  range,  chPercentage,  onDrawIn,  offDrawIn, Configuration.getIntConfig("battery"));
	}
	
	public LeachSensorNode(int x, int y, String name, double readingFreqencyIn, double range, double chPercentage, double onDrawIn, double offDrawIn, int battery)
	{
		this( x,  y,  name,  readingFreqencyIn,  range,  chPercentage,  onDrawIn,  offDrawIn, battery, Configuration.getIntConfig("messageQueueSize"));
	}
	
	public LeachSensorNode(int x, int y, String name, double readingFreqencyIn, double range, double chPercentage, double onDrawIn, double offDrawIn, int battery, int queueSize) 
	{
		super(x, y, name, readingFreqencyIn, range, onDrawIn, offDrawIn, battery, queueSize);
		msgHandler = new CSMAMessageHandler(this);
		clusterheadRM = new MRG32k3a();
		
		resetNode();
	}
	
	public Node clone()
	{
		return new LeachSensorNode((int)x(),(int)y(), name, this.readingFrequency, this.range, this.chanceOfBeingCH);
	}
	
	public boolean isBaseStation()
	{
		return false;
	}
	
	private boolean chooseToBeClusterhead()
	{
		// the probability of a node becoming a clusterhead
		// is a double... from 0-1
		boolean clusterhead = false;
		double threshold;

		// the sink is always a clusterhead
		// check if this node is allowed to be a clusterhead (n in G)
		if ( chanceOfBeingCH > 0 && (1/chanceOfBeingCH ) - lastRoundAsClusterhead < 0 && chanceOfBeingCH > 0 && chanceOfBeingCH < 1) 
		{
			threshold = ( chanceOfBeingCH )/(double)( 1 - chanceOfBeingCH * ( lastRoundAsClusterhead%(int)(1/chanceOfBeingCH) ) );

			// now, choose a random number
			// if it's less than the threshold, become a clusterhead
			//if ( Rand1.uniform(clusterheadRM, 0.0, 1.0) < threshold )
			if (clusterheadRM.nextDouble() < threshold)
				clusterhead = true;
		}
		else if (chanceOfBeingCH >= 1)
			clusterhead = true;

		return clusterhead;

	}
	
	
	
	public double getRSSI(Node otherNode)
	{
		// TODO
		return 2;
	}
	
	public int getBeaconRank()
	{
		return beaconRank;
	}
	
	private int getBeaconRank(String message)
	{
		int theRank = DEFAULT_BEACON_RANK;
		
		String[] parts = message.split(";");
		if (parts.length > 1)
		{
			if ( Utility.isInteger(parts[1]) )
				theRank = Integer.parseInt(parts[1]);
				
		}
		
		return theRank;
	}
	
	private int getBeaconTime(String message)
	{
		int theTime = 0;
		
		String[] parts = message.split(";");
		if (parts.length > 1)
		{
			if ( Utility.isInteger(parts[2]) )
				theTime = Integer.parseInt(parts[2]);
				
		}
		
		return theTime;
	}

	
	public void sendMessage()
	{
		// send the message to this node's clusterhead
		//Packet toSend = new Packet( this, getMessage(), baud);
		
		//following.assignMessage(toSend);
		
		// send the message to all nodes in listening range
		//msgHandler.broadcast( toSend );
		
		if (debug)
			System.out.println (this.name + " sending a message");
		if( following != null )
		{
			Message m = getMessage();
			if (m == null)
				if (Configuration.verbose)
					System.out.println("Message is null in send?");
			if (!msgHandler.isSending())
				msgHandler.send(m, following.getId());
		}
		
	}
	
	
	@Override
	public void sendComplete(Message in) {
		
		messageQueue.remove(in);
		numMessagesSent++;

		queueSize.update(messageQueue.size());
		
		if (LStage ==LeachStage.RUN)
		{ 
			numberOfMessagesSent++;
			// blast next packet
			if (messageQueue.size() > 0 && following != null)
				msgHandler.send( this.getMessage(), following.getId() );
		}
		
		// else
		// shut up! We'll send again when possible
		
	}
	@Override
	public void receiveComplete(Message m) {
		
		if (Configuration.verbose)
			System.out.println("Sensor Node received a message!" + m);
		CSMAMessage c = (CSMAMessage) m; // we can assume this, since we're using leach
		// TODO - process it
		String message = m.getInfo();
		if(LStage == LeachStage.RECLUSTER)
		{
			int  from = c.getFrom();
			if (message.startsWith("CH") && !isClusterhead)
			{
				IClusterhead icIn = (IClusterhead) Node.getNodeFromId(from);
				if (beaconRank > icIn.getBeaconRank() && 
						Sim.time() - beaconReceivedAt +beaconTime <= getBeaconTime(message))
				{
					beaconRank = icIn.getBeaconRank() + 1;
					beaconTime = getBeaconTime(message);
					beaconReceivedAt = Sim.time();
					if (earliestBeaconReceived < 0)
						earliestBeaconReceived = round;
				}
				
				
				if (following == null && icIn.getBeaconRank() <= beaconRank)
					following = Node.getNodeFromId(from);
				else if (following != null)
				{
					IClusterhead icFollowing = (IClusterhead) following;
					//IClusterhead icNew = (IClusterhead)  Node.getNodeFromId(from);
					int chRank = getBeaconRank(message);
					
					if (chRank < icFollowing.getBeaconRank())
					{
						following = Node.getNodeFromId(from);
					}
					else if (getRSSI(following) < getRSSI( Node.getNodeFromId(from)) )
					{
						following = Node.getNodeFromId(from);
					}
							
					
				}
			}
			
		}
		else if (LStage == LeachStage.CHOOSE_CLUSTER){
			
			if (message.startsWith("FL") && isClusterhead){
				String[] parts = message.split("-");
				if ( parts.length > 2 && Utility.isInteger(parts[1])){
					int newId = Integer.parseInt(parts[1]);
					if (newId == id && Utility.isInteger(parts[2]))
					{
						clustermembers.add( Node.getNodeFromId(Integer.parseInt(parts[2])));
						this.numberOfNodesInClusterTotal++;
					}
				}
			}
		}
		else if (LStage == LeachStage.WAIT_FOR_SCHEDULE){
			// parse the input, if possible			
			if (!isClusterhead && !m.getInfo().startsWith("CH")  && !m.getInfo().startsWith("FL") && delay == 0)
			{
				double delayIn = parseSchedule(m.getInfo());
				
				if (m.getInfo().length() > 0 && delayIn >= 0 )
				{
					runtime = Double.parseDouble(m.getInfo().split(";")[0]) - A_JIFFY;
					delay = delayIn + A_JIFFY;
				}
			}
			
		}
		else if (isClusterhead && c.isForBS() && LStage == LeachStage.RUN)
		{
			if (m instanceof DataMessage)
			{
				this.addMessageToList(m); // pass it on
				numMessagesReceived++;

				m.setClosestIveBeenToSink( Basestation.getLastBS().getDistance(this));
				messagesThrough++;
				
				// don't accept messages if we can't.
				if (this.messageQueue.size() == this.maxQueueSize)
					msgHandler.off();
			}
		}
		else if (c.getDestination() == this.id || c instanceof BroadcastMessage)
			if (Configuration.verbose)
				System.out.println("Got a message for me... now what? Says " + message);
		/*
		 * else 
		 * 		eavesdrop?
		 */
		
		
		
	}
	
	
	private String createSchedule()
	{
		double thisdelay = 0;
		double thisruntime = 0;
		if (clustermembers.size() > 0)
			thisruntime = LeachConstants.TOTAL_RUN_TIME / clustermembers.size(); 
		String theString = thisruntime + ";";
		for(Node n : clustermembers){
			theString += n.getId() + "=" +thisdelay+";";
			thisdelay += thisruntime;
		}
		return theString;
	}
	
	
	
	
	
	/*
	 * Leach stage methods
	 */

	private void startRecluster() {
		
		//System.out.println("recluster " + Sim.time());
		
		// Turn the radio on (probably waking from sleep)
		//round++;
		this.msgHandler.on();
		following = null;
		delay = 0;
		runtime = 0;
		isFailedClusterhead = false;
		clustermembers.clear();
		if (chooseToBeClusterhead())
		{
			// send a message
			isClusterhead = true;
			this.numberOfTimesAsClusterhead++;
			lastRoundAsClusterhead = 0;
			
			// send a 'isClusterhead' announcement
			// broadcast will happen 'all at once', but dealt with with backoffs.
			
			
			
			//msgHandler.broadcast( new Packet(this, new BroadcastMessage("CH"+this.id, id, 4 ), this.baud ));
			//msgHandler.scheduleBroadcast(new BroadcastMessage("CH"+this.id, id, 4 ), Rand1.uniform(clusterheadRM, 0, LeachConstants.RECLUSTER_TIME/2 ) );
			String message = "CH"+this.id;
			if (beaconRank < DEFAULT_BEACON_RANK)
			{
				int publishTime =(int)(Sim.time() - beaconReceivedAt + beaconTime); 
				message += ";"+beaconRank+";"+publishTime;
			}
			if (Configuration.verbose)
				System.out.println(name + "  chooses to be a clusterhead" + this.name + " " +message);
			msgHandler.scheduleBroadcast(new BroadcastMessage(message, id, 4 ), Rand1.uniform(clusterheadRM, 0, LeachConstants.RECLUSTER_TIME/(double)2 ) );
		}
		else
		{
			isClusterhead = false;
			
			// wait until next phase
			// listen for CH announcements
			
		}
		new NextStage().schedule(LeachConstants.RECLUSTER_TIME);
		
	}
	
	
	
	private void startChooseCluster() {
		//System.out.println("choose " + Sim.time());
		// TODO Auto-generated method stub
		
		msgHandler.on();

		
		if (following != null)
			msgHandler.scheduleBroadcast( new BroadcastMessage("FL-" + following.getId()+"-"+id,this.id, 3, following.getId()), Rand1.uniform(clusterheadRM, 0, LeachConstants.CHOOSE_CLUSTERHEAD_TIME/(double)2 ) );
		new NextStage().schedule(LeachConstants.CHOOSE_CLUSTERHEAD_TIME);
		
	}
	private void startWaitForSchedule() {
		
		//System.out.println("wait for sch " + Sim.time());
		
		// restart the radio
		msgHandler.off();
		msgHandler.on();
		
		if (isClusterhead)
		{
			// announce schedule
			String schedule = createSchedule();
			// delay a broadcast, to try to space the annoucements.
			
			msgHandler.scheduleBroadcast( new BroadcastMessage(schedule,this.id, schedule.length() ), Rand1.uniform(clusterheadRM, 0, LeachConstants.WAIT_FOR_SCHEDULE_TIME/(double)2 ) );
			if (Configuration.verbose)
				System.out.println(name + "  broadcasting " + schedule);
			runtime = LeachConstants.TOTAL_RUN_TIME;
			delay = 0;
		}
		
		isFailedClusterhead = this.checkFailAsClusterhead();
		
		
		// listen for schedule. 
		
		new NextStage().schedule(LeachConstants.WAIT_FOR_SCHEDULE_TIME);
		
	}
	private void startWaitForTurn() {
		//System.out.println("wait for turn " + Sim.time());
		msgHandler.off();
		
		
		new NextStage().schedule(delay);
		
	}
	

	private void startRun() {
		
		if (isClusterhead)
		{
			runtimeAsClusterhead += runtime;
			
			//TODO make this configurable
			// don't accept messages if we can't.
			
			if (isFailedClusterhead)
				isClusterhead = false; 
			else if (this.messageQueue.size() == this.maxQueueSize)
				msgHandler.off();
			
		}
		else
		{
			runtimeAsSensor += runtime;
			if (following == null)
				roundsUnclustered++;
		}
		
		
		new NextStage().schedule(runtime);
		if (Configuration.verbose)
			System.out.println(name + " is going to run for " + runtime + " and is ch? " + isClusterhead());
		msgHandler.on();
		if (runtime > 0 && following != null && !isClusterhead && messageQueue.size() > 0 )
		{
			sendMessage();
		}
		
		
	}
	private void startWaitForRecluster() {
		//System.out.println("wait for re " + Sim.time());
		// print out the message queue
		lastRoundAsClusterhead++;
		if (Configuration.verbose)
			System.out.println("messageQueue has " + messageQueue.size()+ " messages" );
		msgHandler.off();
		double currDelay = LeachConstants.TOTAL_RUN_TIME -  runtime - delay;
		if (currDelay < 0)
			currDelay = 0;
		new NextStage().schedule( currDelay );
		
		/*Message m = (Message)this.messageQueue.view(List.FIRST);
		System.out.println("messageQueue has " + messageQueue.size()+ " messages" );
		while (m != null)
		{
			System.out.println(m.getInfo());
			m = (Message)messageQueue.view(List.NEXT);
		}*/
		
	}
	
	private void startSleep()
	{
		msgHandler.off();
		new NextStage().schedule(LeachConstants.SLEEP);
	}
	
	
	
	
	
	private double parseSchedule(String schedule)
	{
		// return the delay for this node
		double thisDelay = -1;
		String[] pairs = schedule.split(";");
		String idAsString = ""+id;
		boolean foundDelay = false;
		for (int i = 0 ; i < pairs.length && !foundDelay; i++)
		{
			String[] nameValue = pairs[i].split("=");
			if ( nameValue.length == 2 && nameValue[0].equals(idAsString))
			{
				thisDelay = Double.parseDouble(nameValue[1]);
				foundDelay = true;
			}
		}
		
		return thisDelay;
		
	}

		
	
	@Override
	/*
	 * There is a message in the send queue that is ready to be sent.
	 * Handle appropriately (send at the right time)
	 */
	protected void messageReadyToSend() {
		
		// Just send the message
		//if (debug)
		//	System.out.println("Sending a message");
		if (!msgHandler.isSending() && LStage == LeachStage.RUN && !isClusterhead)			
			this.sendMessage();
	}

	
	
	
	
	public String report()
	{
		String out = super.report();
		out += "\nNumber of messages created: "+ numberOfMessagesCreated;
		out += "\nNumber of messages sent: "+ numberOfMessagesSent;
		out += "\nNumber of hop messages received " + messagesThrough;
		out += "\nNumber of messages in queue: " +this.messageQueue.size();
		out += "\nBattery power left over: " + this.battery.getResidualPower();
		return out;
	}
	
	
	class NextStage extends Event
	{
		public void actions()
		{
			// stages RECLUSTER, CHOOSE_CLUSTER,  WAIT_FOR_SCHEDULE, WAIT_FOR_TURN, RUN, WAIT_FOR_RECLUSTER,
			// move to the next stage
			if (battery.isAlive())
			{
				switch (LStage){
					case RECLUSTER:
						LStage = LeachStage.CHOOSE_CLUSTER;
						startChooseCluster();
						break;
					case CHOOSE_CLUSTER:
						LStage = LeachStage.WAIT_FOR_SCHEDULE;
						startWaitForSchedule();
						break;
					case WAIT_FOR_SCHEDULE:
						LStage = LeachStage.WAIT_FOR_TURN;
						startWaitForTurn();
						break;
					case WAIT_FOR_TURN :
						LStage = LeachStage.RUN;
						startRun();
						break;
					case RUN :
						LStage = LeachStage.WAIT_FOR_RECLUSTER;
						startWaitForRecluster();
						break;
					case WAIT_FOR_RECLUSTER:
						LStage = LeachStage.SLEEP;
						startSleep();
						break;
					case SLEEP:
						LStage = LeachStage.RECLUSTER;
						startRecluster();
						break;
					
				
				}
			}
			else
			{
				msgHandler.off();
				LStage = LeachStage.DEAD;
				if (Configuration.verbose)
					System.out.println(name + " is now Dead");
				diedAt = Sim.time();
				queueSize.setCollecting(false);
				diedAtRound = round;
				if (!keepGoing())
				{
					Sim.stop();
				}
			}
		}

		
	}
	
	public boolean careIfAlive()
	{
		if (careIfAlive && LStage != LeachStage.DEAD)
		{
			return true;
		}
		return false;
	}

	
	protected void resetNode()
	{
		super.resetNode();

		// schedule the first event, reclustering
		LStage = LeachStage.SLEEP;
		numberOfMessagesCreated = 0;
		
		chanceOfBeingCH = Configuration.getDoubleConfig("clusterheadChance"); // borks by-node chchance
		
		debug = Configuration.verbose;
		
		beaconRank = DEFAULT_BEACON_RANK;
		reliableTime = Configuration.getDoubleConfig("reliableTime");

		if (Configuration.getBooleanConfig("leach_cheat_routing"))
		{
			// fake it.
			// assume that all motes have the same range.
			this.beaconRank = (int) (this.getDistance(Basestation.getLastBS())/ this.range + 2);
			earliestBeaconReceived = 0;
		}
		
		//set a round. use the custerhead RM. Fake last round as clusterhead.
		lastRoundAsClusterhead = (int) ( clusterheadRM.nextDouble() * (1/Configuration.getDoubleConfig("clusterheadChance")) + 1);
		
		new NextStage().schedule(0);
	}

}
