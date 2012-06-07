package hccp_test;


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

public class HccpSensorNode extends SensorNode implements IClusterhead {
	
	//final static double DEFAULT_PERCENTAGE_CH = 0.80;
	
	final static int DEFAULT_BEACON_RANK =100;
	double reliableTime;
	
	final static int CRITICAL_BATTERY_LEVEL = 100; // TODO: tune this.
	
	private double sensorMission = 0.50; // 50th percentile of nodes that want to be CH 
	
	private double chanceOfBeingCH;

	private int debug = 4 ;
	
	private double thisRoundSuboptimalRating;
	RandomStream clusterheadRM;
	RandomStream suboptimalClusterheadRM;
	RandomStream delayedMessageRM;
	RandomStream rebeaconRM;
	RandomStream goodnessRM;
	HccpStage stage;
	
	private boolean isCandidate;
	
	private double delay = 0;
	private double runtime = 0;
	private int cycleCount = 0;
	
	private Event chReplacement;
	private Event clusterheadCandidate;
	private Event clusterheadMessage;
	
	
		
	boolean allClusterheads = false;
	
	// beacon routing - keep track of when beacon was heard.
	
	int beaconTime = Integer.MAX_VALUE; // <-- actually seconds since last beacon.
	double beaconReceivedAt = 0;
	
	private boolean allowFirstOrderSuboptimal;
	private double firstOrderSuboptimalPercentage;
	private double firstOrderChPercentage;
	private double suboptimalClusterheadPercentage;
	
	private double chMarginPerc = 1;  // TODO - CONFIGURABLE.
	private double gotCHMessageAt;
	
	int lastRoundAsClusterhead = 0;

	
	public HccpSensorNode(int x, int y, String name, double readingFreqencyIn, double range)
	{
		this(x,y,name,readingFreqencyIn, range, Configuration.getDoubleConfig("clusterheadChance"));
	}
	
	public HccpSensorNode(int x, int y, String name, double readingFrequency) {
		this(x, y, name, readingFrequency, Configuration.getDoubleConfig("range"));
	}
	public HccpSensorNode(int x, int y, String name) {
		this(x,y,name, Configuration.getDoubleConfig("readingFreq"));
	}
	
	public HccpSensorNode(int x, int y, String name, double readingFreqencyIn, double range, double chPercentage) {
		this(x, y, name, readingFreqencyIn, range, chPercentage, Configuration.getDoubleConfig("onDraw"), Configuration.getDoubleConfig("offDraw"));
		
	}
	
	public HccpSensorNode(int x, int y, String name, double readingFreqencyIn, double range, double chPercentage, double onDraw, double offDraw) 
	{
		this(x, y, name, readingFreqencyIn, range, chPercentage,onDraw, offDraw, Configuration.getIntConfig("battery"));
	}
	
	public HccpSensorNode(int x, int y, String name, double readingFreqencyIn, double range, double chPercentage, double onDrawIn, double offDrawIn, int battery)
	{
		this( x,  y,  name,  readingFreqencyIn,  range,  chPercentage,  onDrawIn,  offDrawIn, battery, Configuration.getIntConfig("messageQueueSize"));
	}
	
	public HccpSensorNode(int x, int y, String name, double readingFreqencyIn, double range, double chPercentage, double onDraw, double offDraw, int battery, int queueSize) 
	{
		super(x, y, name, readingFreqencyIn, range, onDraw, offDraw, battery, queueSize);
		msgHandler = new CSMAMessageHandler(this);
		clusterheadRM = new MRG32k3a();
		suboptimalClusterheadRM = new MRG32k3a();
		delayedMessageRM = new MRG32k3a();
		rebeaconRM = new MRG32k3a();
		goodnessRM = new MRG32k3a();
		
		resetNode();
		
		
	}
	
	public Node clone()
	{
		return new HccpSensorNode((int)x(),(int)y(), name, this.readingFrequency, this.range, this.chanceOfBeingCH);
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
		
		double chChance = chanceOfBeingCH;
		//if (allowFirstOrderSuboptimal && beaconRank == 1)
		//	chChance = Configuration.getDoubleConfig("hccp_firstorder_ch_perc");
		if ( Configuration.getBooleanConfig("hccp_allow_firstorder_ch") && beaconRank == 1)
		{
			chChance = Configuration.getDoubleConfig("hccp_firstorder_ch_perc");
		}

		// the sink is always a clusterhead
		if (allClusterheads || chChance >= 1)
			clusterhead = true;
		else if (Configuration.getBooleanConfig("hccp_goodness"))
		{
			if (chChance >= 0)
			{
				/*
				 * leach-like. But, threshold is generated by an equation
				 */
				chChance = getClusterheadPercentage(chChance);
				threshold = ( chChance )/(double)( 1 - chChance * ( lastRoundAsClusterhead%(int)(1/chChance) ) );

				// now, choose a random number
				// if it's less than the threshold, become a clusterhead
				//System.out.println("Threshold" + threshold + " " + chanceOfBeingCH);
				if (clusterheadRM.nextDouble() < threshold)
					clusterhead = true;
				
				//System.out.println(name + " has ch chance of " + chChance);
			}
		}
		else if ( Configuration.getBooleanConfig("hccp_leach") && chChance > 0 && chChance < 1 && (1/chChance ) - lastRoundAsClusterhead < 0) // check if this node is allowed to be a clusterhead (n in G)
		{
			threshold = ( chChance )/( 1 - chChance * ( lastRoundAsClusterhead%(int)(1/chChance) ) );

			// now, choose a random number
			// if it's less than the threshold, become a clusterhead
			//System.out.println("Threshold" + threshold + " " + chanceOfBeingCH);
			if (clusterheadRM.nextDouble() < threshold)
				clusterhead = true;
		}
		else
			System.out.println("ur doin' ch election wrong");
		
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
		int theTime = Integer.MAX_VALUE;
		
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
		
		if (debug > 2)
			System.out.println (this.name + " sending a message");
		if (following !=null)
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
		
		
		
		if (stage == HccpStage.RUN)
		{ 
			numberOfMessagesSent++;
			if (following == Basestation.getLastBS() && this.roundsUnclustered > 100)
				if (Configuration.verbose)
					System.out.println("Herp");
			
			
			
			messageQueue.remove(in);
			numMessagesSent++;
			queueSize.update(messageQueue.size());
			// blast next packet
			if (messageQueue.size() > 0 && following != null)
				msgHandler.send( this.getMessage(), following.getId() );
			else
				msgHandler.off();
		}
		
		// else
		// shut up! We'll send again when possible
		
	}
	
	public void hadCollision()
	{
		// if was in announcecandidacy, care.
		if (stage == HccpStage.ANNOUNCE_CANDIDACY)
		{
			// lie to the system!
			handleCCMessage("CC000");
		}
	}
	
	@Override
	public void receiveComplete(Message m) {
		
		//System.out.println("Sensor Node received a message!" );
		CSMAMessage c = (CSMAMessage) m; // we can assume this, since we're using leach
		String message = m.getInfo();
		
		if (isCandidate && stage == HccpStage.ANNOUNCE_CANDIDACY)
		{
			handleCCMessage(message);
		}
		else if(stage == HccpStage.ANNOUNCE_CLUSTERHEAD)
		{
			int  from = c.getFrom();
			if (message.startsWith("CH") && !isClusterhead)
			{
				IClusterhead icIn = (IClusterhead) Node.getNodeFromId(from);
				
				// if it's better than I know, or I don't know better
				// new: if it's newer it's better.
				//if (beaconRank > icIn.getBeaconRank() || beaconTime + reliableTime < getBeaconTime(message)  )
				
				//System.out.println(  Sim.time()  - beaconReceivedAt + beaconTime + " " + getBeaconTime(message));
				//if ( getBeaconTime(message) != Integer.MAX_VALUE && Math.floor(Sim.time()  - beaconReceivedAt + beaconTime) > getBeaconTime(message) + reliableTime)
				if ( getBeaconTime(message) != Integer.MAX_VALUE && Math.floor(Sim.time()  - beaconReceivedAt + beaconTime) > getBeaconTime(message) )
				{
					beaconRank = icIn.getBeaconRank() + 1;
					if (earliestBeaconReceived < 0)
						earliestBeaconReceived = round;
					beaconTime = getBeaconTime(message); // mark it a little older for good measure.
					beaconReceivedAt = Sim.time();
				}
				
				
				if (following == null && icIn.getBeaconRank() <= beaconRank)
				{
					following = Node.getNodeFromId(from);
					gotCHMessageAt = Sim.time();
				}
				else if (following != null)
				{
					IClusterhead icFollowing = (IClusterhead) following;
					//IClusterhead icNew = (IClusterhead)  Node.getNodeFromId(from);
					int chRank = getBeaconRank(message);
					
					
					if (chRank < icFollowing.getBeaconRank() &&
							Sim.time() - gotCHMessageAt < HccpConstants.RECLUSTER_TIME * chMarginPerc)
					{
						following = Node.getNodeFromId(from);
						gotCHMessageAt = Sim.time();
					}
					/*else if (getRSSI(following) < getRSSI( Node.getNodeFromId(from)) )
					{
						following = Node.getNodeFromId(from);
					}*/
							
					
				}
			}
			else
			{
				handleCHLikeCCMessage(message);
				// if no longer a clusterhead, follow this mofo
				if (!isClusterhead)
				{
					following = Node.getNodeFromId(from);
					gotCHMessageAt = Sim.time();
				}
				
			}
			
		}
		else if (stage == HccpStage.CHOOSE_CLUSTER){
			
			if (message.startsWith("FL-") && isClusterhead){
				// the rest of the message should be the id of 'who' this node is going to be following
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
		else if (stage == HccpStage.WAIT_FOR_SCHEDULE){
			// parse the input, if possible
			
			if (Configuration.verbose)
				System.out.println("Looking for " + id+ " in + " +m.getInfo());
			if (!isClusterhead)
			{
				delay = parseSchedule(m.getInfo());
				
				if (m.getInfo().length() > 0 && delay >= 0 && Utility.isDouble(m.getInfo().split(";")[0]) )
					runtime = Double.parseDouble(m.getInfo().split(";")[0]);
			}
			
		}
		
		else if (stage == HccpStage.ROUNDTABLE_DISCUSSION)
		{
			// see if i'd make a good clusterhead.
			// if I think I'd be a good clusterhead
			if (m.getInfo().startsWith("OO") && (following == null || m.getFrom() == following.getId()))
			{
				following = null;
			
				chReplacement = new ClusterheadReplacement();
				if (chooseToBeClusterhead())
				{
					chReplacement.schedule( getGoodnessDelay() );
				}
			}
			if (following == null && m.getInfo().startsWith("CH"))
			{
				following =  Node.getNodeFromId(m.getFrom());
				if (chReplacement != null)
					chReplacement.cancel();
			}
			if (m.getInfo().equals("g")) // get beacon
			{
				// schedule a reply with beacon
				if (beaconRank < DEFAULT_BEACON_RANK)
				{
					// reply
					if (Configuration.verbose)
						System.out.println("Sending rank " + beaconRank);
					if ( beaconRank < DEFAULT_BEACON_RANK && Configuration.getDoubleConfig("hccp_rebeaconPercentage") >  rebeaconRM.nextDouble()  )
					{
						String messageOut = "b" + beaconRank;
						
						int publishTime =(int)Math.ceil(Sim.time() - beaconReceivedAt + beaconTime) + 1;
						messageOut += ":"+publishTime;
						
						msgHandler.scheduleBroadcast( new BroadcastMessage(messageOut,this.id, 3, -1), Rand1.uniform(delayedMessageRM, 0,  HccpConstants.ROUNDTABLE_TIME/(double)2  ) );
					}
				}
			}
			if (m.getInfo().startsWith("b"))
			{
				int firstColon = m.getInfo().indexOf(":");
			
				int newRank = Integer.parseInt( m.getInfo().substring(1,firstColon));
				int beaconTimeIn = Integer.parseInt(m.getInfo().substring(firstColon+1));
 				//if (newRank < beaconRank || ((Sim.time() - beaconReceivedAt + beaconTime > reliableTime ) && beaconIn < beaconTime))
				//if ((Sim.time() - beaconReceivedAt + beaconTime > reliableTime ) && beaconTimeIn < beaconTime)
				
				// if it's newer it's better.
				// 
				double myTime = Sim.time() - beaconReceivedAt + beaconTime;
				//if (Math.floor(myTime) > beaconTimeIn + reliableTime)
				if (Math.floor(myTime) > beaconTimeIn)
				{
					beaconRank = newRank + 1;
					beaconReceivedAt = Sim.time();
					beaconTime = beaconTimeIn;
					if (earliestBeaconReceived < 0)
						earliestBeaconReceived = round;
					if (Configuration.getBooleanConfig("hccp_autorebeacon") && Configuration.getDoubleConfig("hccp_rebeaconPercentage") >  rebeaconRM.nextDouble()  )
							msgHandler.scheduleBroadcast( new BroadcastMessage("b" + beaconRank+":"+(beaconTime+1),this.id, 3, -1), Rand1.uniform(delayedMessageRM, 0, getTimeToSend(m.getLength()) * 2  ) );
						
				}
			}
		}
		
		//else if (isClusterhead && c.isForBS() && stage == HccpStage.RUN && m instanceof DataMessage)
		else if (isClusterhead && stage == HccpStage.RUN && m instanceof DataMessage)
		{
			
			if (Configuration.verbose)
				System.out.println(name + " got message: " + m);
			this.addMessageToList(m); // pass it on
			numMessagesReceived++;

			
			m.setClosestIveBeenToSink( Basestation.getLastBS().getDistance(this));
			messagesThrough++;
			
			
			// TODO - make this configurable
			// Turn off if there is no space left.
			if (this.messageQueue.size() == this.maxQueueSize)
				msgHandler.off();
			
		}
		
		else if (c.getDestination() == this.id || c instanceof BroadcastMessage)
			if (Configuration.verbose)
				System.out.println("Got a message for me ("+name+")... now what? Says " + message);
		/*
		 * else 
		 * 		eavesdrop?
		 */
		
		
		
	}
	
	private void handleCHLikeCCMessage(String message)
	{

		// Listen to other messages. If it's better than us, don't be a CH
		// if it's better than us, and we haven't announced yet, don't.
		if (message.startsWith("CH") && isClusterhead)
		//if (isClusterhead)
		{
			// TODO -is this better than me
			// for now, if it announced first, don't announce
			
			// TODO- check rssi maybe?
			
			
			if (!HccpConstants.allowSuboptimalClusterheads)
			{
				/*
				 * Not allowing suboptimal clusterheads. just bail.
				 */
				if (clusterheadMessage != null) // I haven't sent yet. I must be worse				
				{
					clusterheadMessage.cancel();
					clusterheadMessage = null;
					isClusterhead = false;
				}
				else
				{
					// consider the values sent by the other candidate
					// TODO
				}
			}
			else if (allowFirstOrderSuboptimal && this.beaconRank == 1 && 
					this.thisRoundSuboptimalRating < clusterheadRM.nextDouble())
			{
				if (clusterheadMessage != null) // I haven't sent yet. I must be worse				
				{
					clusterheadMessage.cancel();
					clusterheadMessage = null;
					isClusterhead = false;
				}
			}
			else if (HccpConstants.allowSuboptimalClusterheads && 
					this.thisRoundSuboptimalRating < clusterheadRM.nextDouble())
			{
				// keep being a clusterhead
				if (clusterheadMessage != null) // I haven't sent yet. I must be worse				
				{
					clusterheadMessage.cancel();
					clusterheadMessage = null;
					isClusterhead = false;
				}
			}
			else
			{
				if (Configuration.verbose)
					System.out.println(name + " is a suboptimal CH");
			}
		}
	}
	
	
	private void handleCCMessage(String message)
	{

		// Listen to other messages. If it's better than us, don't be a CH
		// if it's better than us, and we haven't announced yet, don't.
		if (message.startsWith("CC") && isClusterhead)
		//if (isClusterhead)
		{
			// TODO -is this better than me
			// for now, if it announced first, don't announce
			
			// TODO- check rssi maybe?
			
			
			if (!HccpConstants.allowSuboptimalClusterheads)
			{
				/*
				 * Not allowing suboptimal clusterheads. just bail.
				 */
				if (clusterheadCandidate != null) // I haven't sent yet. I must be worse				
				{
					clusterheadCandidate.cancel();
					clusterheadCandidate = null;
					isClusterhead = false;
				}
				else
				{
					// consider the values sent by the other candidate
					// TODO
				}
			}
			else if (allowFirstOrderSuboptimal && this.beaconRank == 1 && 
					this.thisRoundSuboptimalRating < clusterheadRM.nextDouble())
			{
				if (clusterheadCandidate != null) // I haven't sent yet. I must be worse				
				{
					clusterheadCandidate.cancel();
					clusterheadCandidate = null;
					isClusterhead = false;
				}
			}
			else if (HccpConstants.allowSuboptimalClusterheads && 
					this.thisRoundSuboptimalRating < clusterheadRM.nextDouble())
			{
				// keep being a clusterhead
				if (clusterheadCandidate != null) // I haven't sent yet. I must be worse				
				{
					clusterheadCandidate.cancel();
					clusterheadCandidate = null;
					isClusterhead = false;
				}
			}
			else
			{
				if (Configuration.verbose)
					System.out.println(name + " is a suboptimal CH");
			}
		}
	}
	
	
	private String createSchedule()
	{
		double delay = 0;
		double runtime = 0;
		if (clustermembers.size() > 0)
			runtime = HccpConstants.TOTAL_RUN_TIME / clustermembers.size(); 
		String theString = runtime + ";";
		for(Node n : clustermembers){
			theString += n.getId() + "=" +delay+";";
			delay += runtime;
		}
		return theString;
	}
	
	
	
	
	
	/*
	 * Leach stage methods
	 */

	private void startRecluster() {
		//round++;
		this.msgHandler.off();
		following = null;
		delay = 0;
		runtime = 0;
		clustermembers.clear();
		gotCHMessageAt = 0;
		
		isFailedClusterhead = false;
		
		
		clusterheadCandidate = null;
		
		
		if (chooseToBeClusterhead() && !dealBreaker())
		{
			
			
			if (HccpConstants.allowSuboptimalClusterheads)
			{
				//this.thisRoundSuboptimalRating = Utility.randU01(suboptimalClusterheadRM);
				//this.thisRoundSuboptimalRating = suboptimalClusterheadRM.nextDouble();
				
				// run leach-like
				if (suboptimalClusterheadPercentage == 1)
					thisRoundSuboptimalRating = 1;
				else if (beaconRank == 1)
					thisRoundSuboptimalRating = Math.min(1, ( firstOrderSuboptimalPercentage )/( 1 - firstOrderSuboptimalPercentage * ( lastRoundAsClusterhead%(int)(1/firstOrderSuboptimalPercentage) ) ) );
				else
					thisRoundSuboptimalRating = Math.min(1, ( suboptimalClusterheadPercentage )/( 1 - suboptimalClusterheadPercentage * ( lastRoundAsClusterhead%(int)(1/suboptimalClusterheadPercentage) ) ) );
				
				//System.out.println(thisRoundSuboptimalRating);
			}
			
			this.msgHandler.on();
			// send a message
			isClusterhead = true;
			isCandidate = true;
			// send a 'isClusterhead' announcement
			// broadcast will happen 'all at once', but dealt with with backoffs.
			
			//if (debug > 2)
			if (Configuration.verbose)
				System.out.println(name + "  chooses to be a clusterhead candidate");
			
			//msgHandler.broadcast( new Packet(this, new BroadcastMessage("CH"+this.id, id, 4 ), this.baud ));
			//msgHandler.scheduleBroadcast(new BroadcastMessage("CH"+this.id, id, 4 ), Rand1.uniform(clusterheadRM, 0, LeachConstants.RECLUSTER_TIME/2 ) );
			
			/*String message = "CC"+this.id;
			if (beaconRank < DEFAULT_BEACON_RANK)
				message += ";"+beaconRank;*/ // dealt with in clusterhead canadidate message
			//msgHandler.scheduleBroadcast(new BroadcastMessage("CC"+this.id, id, 4 ), Rand1.uniform(clusterheadRM, 0, HccpConstants.RECLUSTER_TIME/2 ) );
			
			if (!Configuration.getBooleanConfig("hccp_skip_candidacy"))
			{
				clusterheadCandidate = new ClusterheadCandidateMessage();
				clusterheadCandidate.schedule( getGoodnessDelay());
			}
			
		}
		else
		{
			isCandidate = false;
			isClusterhead = false;
			msgHandler.off();
			// wait until next phase
			// listen for CH announcements
			
		}
		if (Configuration.getBooleanConfig("hccp_skip_candidacy"))
			new NextStage().schedule(0);
		else
			new NextStage().schedule(HccpConstants.RECLUSTER_TIME);
		
	}
	
	private void startAnnounceClusterhead()
	{
		// actually announce the clusterhead, if this is one.
		
		// Turn the radio on (probably waking from sleep)
		
		//System.out.println(name + " starting announce at " + Sim.time());
		
		this.msgHandler.on();
		following = null;
		delay = 0;
		runtime = 0;
		clustermembers.clear();
		if (isClusterhead)
		{
			// send a message
			
			// send a 'isClusterhead' announcement
			// broadcast will happen 'all at once', but dealt with with backoffs.
			
			
			//msgHandler.broadcast( new Packet(this, new BroadcastMessage("CH"+this.id, id, 4 ), this.baud ));
			//msgHandler.scheduleBroadcast(new BroadcastMessage("CH"+this.id, id, 4 ), Rand1.uniform(clusterheadRM, 0, LeachConstants.RECLUSTER_TIME/2 ) );
			String message = "CH"+this.id;
			
			numberOfTimesAsClusterhead++;
			lastRoundAsClusterhead = 0;

			if (beaconRank < DEFAULT_BEACON_RANK)
			{
				int publishTime =(int)Math.ceil(Sim.time() - beaconReceivedAt + beaconTime) + 1;
				message += ";"+beaconRank + ";" +publishTime;
			}
			if (Configuration.verbose)
				System.out.println(name + "  chooses to be a clusterhead " + message);
			
			//msgHandler.scheduleBroadcast(new BroadcastMessage(message, id, 4 ), getGoodnessDelay() );
			
			clusterheadMessage = new ClusterheadMessage();
			clusterheadMessage.schedule( getGoodnessDelay());
			
		}
		else
		{
			// listen
			
		}
		
		new NextStage().schedule(HccpConstants.RECLUSTER_TIME);
	}
	
	private void startChooseCluster() {
		
		msgHandler.on();

		
		if (following != null)
			msgHandler.scheduleBroadcast( new BroadcastMessage("FL-" + following.getId()+"-"+id,this.id, 3, following.getId()), Rand1.uniform(clusterheadRM, 0, HccpConstants.CHOOSE_CLUSTERHEAD_TIME/(double)2 ) );
		
		
		// check for failure
		isFailedClusterhead = checkFailAsClusterhead();
		new NextStage().schedule(HccpConstants.CHOOSE_CLUSTERHEAD_TIME);
		
	}
	private void startWaitForSchedule() {
		
		// restart the radio
		msgHandler.off();
		msgHandler.on();
		
		if (isClusterhead)
		{
			// announce schedule
			String schedule = createSchedule();
			// delay a broadcast, to try to space the annoucements.
			
			msgHandler.scheduleBroadcast( new BroadcastMessage(schedule,this.id, schedule.length() ), Rand1.uniform(clusterheadRM, 0, HccpConstants.WAIT_FOR_SCHEDULE_TIME/(double)2 ) );
			if (Configuration.verbose)
				System.out.println(name + "  broadcasting " + schedule);
			runtime = HccpConstants.TOTAL_RUN_TIME;
		}
		
		// listen for schedule. 
		
		new NextStage().schedule(HccpConstants.WAIT_FOR_SCHEDULE_TIME);
		
	}
	private void startWaitForTurn() {
		msgHandler.off();
		new NextStage().schedule(delay);
		
	}
	

	private void startRun() {
		
		msgHandler.on();
		if (isClusterhead)
			runtimeAsClusterhead += runtime;
		else
		{
			runtimeAsSensor += runtime;
			if (following == null)
				roundsUnclustered++;
		}
		
		
		new NextStage().schedule(runtime);
		
		if (Configuration.verbose)
		//if (runtime > 0)
			System.out.println(name + " is going to run for " + runtime + " message queue is " + messageQueue.size());
		msgHandler.on();
		if (runtime > 0 && following != null && !isClusterhead)
		{
			if (messageQueue.size() > 0)
				sendMessage();
		}
		else if (!isClusterhead)
			msgHandler.off();
		
		
		if (isClusterhead && isFailedClusterhead)
		{
			// msgHandler stays on (kills the battery)
			// 
			isClusterhead = false;
		}
		
	}
	private void startWaitForRoundtable()
	{
		// turn radio off
		double currDelay = HccpConstants.TOTAL_RUN_TIME -  runtime - delay;
		msgHandler.off();
		if (currDelay < 0)
		{
			//System.out.println("Zero problem... total:" +HccpConstants.TOTAL_RUN_TIME + " runtime:" + runtime + " delay:" + delay);
			currDelay = 0;
			
		}
		
		new NextStage().schedule( currDelay );
	}
	private void startRoundtable()
	{
		// queue any messages that should be sent
		// TODO
		
		/*
		 * if (i'm dying)
		 * 	say something
		 * if (i need something)
		 * 	say something
		 * 
		 */
		
		
		chReplacement = null;
		//I'm dying or the queue is full
		msgHandler.off();
		if (HccpConstants.ROUNDTABLE_TIME > 0)
		{
			
			
			
			if (battery.getResidualPower() < CRITICAL_BATTERY_LEVEL || messageQueue.size()/(double)this.maxQueueSize > 0.9 || isFailedClusterhead)
			{
				msgHandler.on();
				
				isFailedClusterhead = false;
				if (Configuration.verbose)
					System.out.println(name + " opting out of clusterhead");
				msgHandler.scheduleBroadcast( new BroadcastMessage("OO"+this.id, id, 4), 0); // no delay, all motes should be switching over at the same time, this queues it to the end.
			}
			/*
			 * Don't always run. Only run sometimes... maybe.
			 */
			else if ( Configuration.getDoubleConfig("hccp_roundtable_perc") > delayedMessageRM.nextDouble() )
			{
				//else if (beaconRank == DEFAULT_BEACON_RANK || Sim.time() - beaconReceivedAt + beaconTime > reliableTime * 2)
				msgHandler.on();
				if (beaconRank == DEFAULT_BEACON_RANK || Sim.time() - beaconReceivedAt + beaconTime > 60) // TODO configurable
				{
					
					//System.out.println("g up!");
					if ( Configuration.getBooleanConfig("hccp_beacon") )
						msgHandler.scheduleBroadcast( new BroadcastMessage("g", id, 4),Rand1.uniform(delayedMessageRM, 0,  HccpConstants.ROUNDTABLE_TIME/(double)2  ) ); // no delay, all motes should be switching over at the same time, this queues it to the end.
				}
			}
		}
		
		new NextStage().schedule( HccpConstants.ROUNDTABLE_TIME );
	}
	
	private void startSleep()
	{
		msgHandler.off();
		new NextStage().schedule( HccpConstants.SLEEP_TIME );
	}
	
	private void startNoReclusterSleep()
	{
		msgHandler.off();
		new NextStage().schedule(HccpConstants.SLEEP_NO_RECLUSTER_TIME);
	}
	

	
	
	private double parseSchedule(String schedule)
	{
		// return the delay for this node
		double thisDelay = 0;
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
				if (Configuration.verbose)
					System.out.println(name + ": Found my junk: " + thisDelay);
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
		//if (debug > 2)
		//	System.out.println("Sending a message");
		if (!msgHandler.isSending() && stage == HccpStage.RUN && !isClusterhead)			
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
	
	
	/*
	 * Delays more the worse you are
	 */
	private double getGoodnessDelay()
	{
		double availableTime = 0.001;
		double delay = 0;
		
		//String name = this.toString();
		
		if (stage ==  HccpStage.ROUNDTABLE_DISCUSSION)
			availableTime = HccpConstants.ROUNDTABLE_TIME;
		else if (stage == HccpStage.ANNOUNCE_CANDIDACY)
			availableTime = HccpConstants.RECLUSTER_TIME;
		else if (stage == HccpStage.ANNOUNCE_CLUSTERHEAD)
			availableTime = HccpConstants.RECLUSTER_TIME;
		
		// add delay times for stuff.
		
		// add up to n% of available time based on battery
		delay =  (availableTime * HccpConstants.BATTERY_POWER_WEIGHT  * (1 - battery.getPercentLeft() ) );
		
		// n% of available time based on mission
		// delay gets longer if this node wants to be a clustermote
		delay = delay + (availableTime * HccpConstants.SENSOR_MISSION_WEIGHT * sensorMission );
		
		// n% on messagequeue size
		delay = delay + (availableTime * HccpConstants.MESSAGE_QUEUE_WEIGHT * ( messageQueue.size()/(double)maxQueueSize ) );
		
		// n% of random
		delay = delay + (availableTime * HccpConstants.RANDOM_WEIGHT * (goodnessRM.nextDouble()));
		
		// n% of duty cycling based on last round as CH.
		// more delay if I was just a clusterhead.
		delay = delay + (availableTime * HccpConstants.DUTY_CYCLE_WEIGHT *  ( 1- Math.min(1, this.lastRoundAsClusterhead * chanceOfBeingCH)));
		
		//System.out.println("Goodness delay is" + delay + " out of " + availableTime);
		
		
		// in case it overflows due to bad config.
		delay = Math.min(delay, availableTime);
		
		
		return delay;
	}
	
	
	/*
	 * get the goodness threshold : read: ch percentage
	 */
	private double getClusterheadPercentage(double maxChChance)
	{
		
		double percentage = 0;
		percentage =  (maxChChance * HccpConstants.BATTERY_POWER_WEIGHT  * (battery.getPercentLeft() ) );
		
		// n% of available time based on mission
		// CH changce gets larger if this node wants to be a clusterhead
		percentage = percentage + (maxChChance * HccpConstants.SENSOR_MISSION_WEIGHT * (1-sensorMission) );
		
		// n% on messagequeue size
		percentage = percentage + (maxChChance * HccpConstants.MESSAGE_QUEUE_WEIGHT * ( 1- messageQueue.size()/(double)maxQueueSize ) );
		
		// n% of random
		percentage = percentage + (maxChChance * HccpConstants.RANDOM_WEIGHT * (goodnessRM.nextDouble())); // TODO change RM
		
		// n% of duty cycling based on last round as CH.
		// less chance of being CH if I was just a clusterhead.
		// getting duty cycled somewhere else?
		percentage = percentage + (maxChChance * HccpConstants.DUTY_CYCLE_WEIGHT *  (1 -  Math.min(1, this.lastRoundAsClusterhead * maxChChance)));
		
		//System.out.println("Goodness delay is" + delay + " out of " + availableTime);
		
		
		// in case it overflows due to bad config.
		percentage = Math.min(percentage, maxChChance);
		return percentage;
	}
	
	/*
	 * Check to see if there's something that would make this mote a
	 * super crappy CH.
	 */
	private boolean dealBreaker()
	{
		if (messageQueue.size()/(double)this.maxQueueSize > 0.95)
			return true;
		// Battery power? Probably a bad idea... what if they're all dead? Network would fail sooner!
		return false;
	}
	
	public void setMission(double newMission)
	{
		sensorMission = newMission;
	}
	
	private double getTimeToSend(int in)
	{
		return in/baud;
	}
	
	public double getChMarginPerc() {
		return chMarginPerc;
	}

	public void setChMarginPerc(double chMarginPerc) {
		this.chMarginPerc = chMarginPerc;
	}

	class ClusterheadReplacement extends Event
	{
		public void actions()
		{
			msgHandler.broadcast(new BroadcastMessage("CH"+id, id, 4 ));
		}
	}
	class ClusterheadCandidateMessage extends Event
	{
		public void actions()
		{
			msgHandler.broadcast(new BroadcastMessage("CC"+id, id, 4 ));
			clusterheadCandidate = null;
		}
	}
	
	class ClusterheadMessage extends Event
	{
		public void actions()
		{
			msgHandler.broadcast(new BroadcastMessage("CH"+id, id, 4 ));
			clusterheadMessage = null;
		}
	}

	class NextStage extends Event
	{
		public void actions()
		{
			// stages RECLUSTER, CHOOSE_CLUSTER,  WAIT_FOR_SCHEDULE, WAIT_FOR_TURN, RUN, WAIT_FOR_RECLUSTER,
			// move to the next stage
			if (battery.isAlive())
			{
				
				switch (stage){
					case ANNOUNCE_CANDIDACY:
						if (debug > 3)
							System.out.println(name + " Announce clusterhead");
						stage = HccpStage.ANNOUNCE_CLUSTERHEAD;
						startAnnounceClusterhead();
						break;
					case ANNOUNCE_CLUSTERHEAD:
						if (debug > 3)
							System.out.println(name + " choose cluster");
						stage = HccpStage.CHOOSE_CLUSTER;
						startChooseCluster();
						break;
					case CHOOSE_CLUSTER:
						if (debug > 3)
							System.out.println(name + " wait for schedule");
						stage = HccpStage.WAIT_FOR_SCHEDULE;
						startWaitForSchedule();
						break;
					case WAIT_FOR_SCHEDULE:
						if (debug > 3)
							System.out.println(name + " wait for turn");
						stage = HccpStage.WAIT_FOR_TURN;
						startWaitForTurn();
						break;
					case WAIT_FOR_TURN :
						if (debug > 3)
							System.out.println(name + " run");
						stage = HccpStage.RUN;
						startRun();
						break;
					case RUN :
						stage = HccpStage.WAIT_FOR_ROUNDTABLE;
						if (debug > 3)
							System.out.println(name + " Wait for roundtable");
						startWaitForRoundtable();
						break;
					case WAIT_FOR_ROUNDTABLE:
						stage = HccpStage.ROUNDTABLE_DISCUSSION;
						if (debug > 3)
							System.out.println(name + " roundtable");
						startRoundtable();
						break;
					case ROUNDTABLE_DISCUSSION :
						if (debug > 3)
							System.out.println(name + " sleep");
						cycleCount++;
						if (cycleCount < HccpConstants.NUMBER_OF_SCHEDULED_RUNS)
						{
							stage = HccpStage.SLEEP_NO_RECLUSTER;
							startNoReclusterSleep();
						}
						else
						{
							stage = HccpStage.SLEEP;
							startSleep();
							cycleCount = 0;
						}
						break;
					case SLEEP:
						stage = HccpStage.ANNOUNCE_CANDIDACY;
						if (debug > 3)
							System.out.println(name + " recluster");
						lastRoundAsClusterhead++;
						startRecluster();
						break;
					case SLEEP_NO_RECLUSTER:
						stage = HccpStage.WAIT_FOR_TURN;
						if (debug > 3)
							System.out.println(name + " back to run - wait for turn");
						startWaitForTurn();
						break;
					case STARTUP:
						stage = HccpStage.ANNOUNCE_CANDIDACY;
						startRecluster();
				}
			}
			else
			{
				msgHandler.off();
				stage = HccpStage.DEAD;
				if (Configuration.verbose)
					System.out.println(name + " is now dead");
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
		
	public double getFirstOrderSuboptimalPercentage() {
		return firstOrderSuboptimalPercentage;
	}

	public void setFirstOrderSuboptimalPercentage(
			double firstOrderSuboptimalPercentage) {
		this.firstOrderSuboptimalPercentage = firstOrderSuboptimalPercentage;
	}

	public double getSuboptimalClusterheadPercentage() {
		return suboptimalClusterheadPercentage;
	}

	public void setSuboptimalClusterheadPercentage(
			double suboptimalClusterheadPercentage) {
		this.suboptimalClusterheadPercentage = suboptimalClusterheadPercentage;
	}

	public double getFirstOrderChPercentage() {
		return firstOrderChPercentage;
	}

	public void setFirstOrderChPercentage(double firstOrderChPercentage) {
		this.firstOrderChPercentage = firstOrderChPercentage;
	}

	public double getChanceOfBeingCH() {
		return chanceOfBeingCH;
	}

	public void setChanceOfBeingCH(double chanceOfBeingCH) {
		this.chanceOfBeingCH = chanceOfBeingCH;
	}
	
	protected void resetNode()
	{
		super.resetNode();
		numberOfMessagesCreated = 0;
		
				
		beaconTime = Integer.MAX_VALUE; // <-- actually seconds since last beacon.
		beaconReceivedAt = 0;
		
		cycleCount = 0;
		
		gotCHMessageAt = 0;
		
		lastRoundAsClusterhead = 0;
		
		// schedule the first event, reclustering
		stage = HccpStage.STARTUP;
		
		chanceOfBeingCH = Configuration.getDoubleConfig("clusterheadChance");
		
		if (!Configuration.verbose)
			debug = -1;
		
		beaconRank = DEFAULT_BEACON_RANK;
		reliableTime = Configuration.getDoubleConfig("reliableTime");
		
		allClusterheads = Configuration.getBooleanConfig("hccp_all_clusterheads");
		
		firstOrderSuboptimalPercentage = Configuration.getDoubleConfig("hccp_firstorder_sub_perc");
		allowFirstOrderSuboptimal = Configuration.getBooleanConfig("hccp_firstorder_suboptimal"); 
		firstOrderChPercentage = Configuration.getDoubleConfig("hccp_firstorder_ch_perc");
		
		
		
		if (Configuration.getBooleanConfig("hccp_cheat_routing"))
		{
			// fake it.
			// assume that all motes have the same range.
			this.beaconRank = (int) (this.getDistance(Basestation.getLastBS())/ this.range + 2);
			beaconTime = 0;
			earliestBeaconReceived = 0;
		}
		new NextStage().schedule(0);
		
	}
	
	public boolean careIfAlive()
	{
		if (careIfAlive && stage != HccpStage.DEAD)
		{
			return true;
		}
		return false;
	}



}
