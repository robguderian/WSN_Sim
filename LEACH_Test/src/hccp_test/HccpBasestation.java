package hccp_test;


import sim_core.Basestation;
import sim_core.Configuration;
import sim_core.IClusterhead;
import sim_core.Message;
import sim_core.Node;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;
import umontreal.iro.lecuyer.simevents.Event;
import CSMA.BroadcastMessage;
import CSMA.CSMAMessageHandler;
import Util.Rand1;
import Util.Utility;

public class HccpBasestation extends Basestation implements IClusterhead {

	private boolean skipRound = false;
	HccpStage stage;
	RandomStream clusterheadRM;
	
	RandomStream sinkSleepRM;
	RandomStream roundtableBeaconRM;
	RandomStream roundtableBeaconDelayRM;

	
	
	double basestationBeacon;

	int cycleCount = 0;
	
	public HccpBasestation(int x, int y, String name) {
		this (x,y,name, Configuration.getDoubleConfig("range") );
	}
	
	public HccpBasestation(int x, int y, String name, double range) {
		this(x, y, name, range, Configuration.getDoubleConfig("onDraw"), Configuration.getDoubleConfig("offDraw"));
	}
	
	public HccpBasestation(int x, int y, String name, double range, double onDrawIn, double offDrawIn)
	{
		super(x, y, name, range);
		msgHandler = new CSMAMessageHandler(this);
		
		resetNode();
		
		basestationBeacon = Configuration.getDoubleConfig("hccp_basestation_beacon");
		
		clusterheadRM = new MRG32k3a();
		sinkSleepRM = new MRG32k3a();
		roundtableBeaconRM = new MRG32k3a();
		roundtableBeaconDelayRM = new MRG32k3a();
	}
	
	@Override
	public boolean isBaseStation()
	{
		return true;
	}
	
	public int getBeaconRank()
	{
		return 0;
	}

	@Override
	public void sendComplete(Message in) {
		numberOfMessagesSent++;
		
	}

	@Override
	public void receiveComplete(Message m) {
		
		if (stage == HccpStage.RUN)
		{
			numberOfMessagesReceived++;
			if (Configuration.verbose)
				System.out.println("Base Station received a message! " + m.getInfo());
			m.markComplete();
			m.setClosestIveBeenToSink( 0 );
		}
		else if (stage == HccpStage.CHOOSE_CLUSTER){
			String message = m.getInfo();
			
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
				// the rest of the message should be the id of 'who' this node is going to be following
				if (message.length() >1)
				{	
					message = message.substring(2);
					if ( Utility.isInteger(message))
					{
						int newId = Integer.parseInt(message);
						if (newId == this.id)
							clustermembers.add( Node.getNodeFromId(m.getFrom()));
					}
				}
		}
		else if (stage == HccpStage.ROUNDTABLE_DISCUSSION)
		{
			if (m.getInfo().equals("g")) // get beacon
			{
				
				if (Configuration.verbose)
					System.out.println("Sending rank " + beaconRank);
				
				msgHandler.scheduleBroadcast( new BroadcastMessage("b0:0" ,this.id, 3), Rand1.uniform(clusterheadRM, 0,  HccpConstants.A_JIFFY  ) );
				
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
	
	
	public String report()
	{
		String out = super.report();
		out += "\nNumber of Messages received: "+ numberOfMessagesReceived;
		return out;
	}
	
	
	
	class NextStage extends Event
	{
		@Override
		public void actions()
		{
			// stages RECLUSTER, CHOOSE_CLUSTER,  WAIT_FOR_SCHEDULE, WAIT_FOR_TURN, RUN, WAIT_FOR_RECLUSTER,
			// move to the next stage
			switch (stage){
				case ANNOUNCE_CANDIDACY:
					stage = HccpStage.ANNOUNCE_CLUSTERHEAD;
					startAnnounceClusterhead();
					break;
				case ANNOUNCE_CLUSTERHEAD :
					stage = HccpStage.CHOOSE_CLUSTER;
					startChooseCluster();
					break;
				case CHOOSE_CLUSTER:
					stage = HccpStage.WAIT_FOR_SCHEDULE;
					startWaitForSchedule();
					break;
				case WAIT_FOR_SCHEDULE:
					stage = HccpStage.RUN;
					startRun();
					break;
				case RUN :
					stage = HccpStage.ROUNDTABLE_DISCUSSION;
					startRoundtable();
					break;
				
				case ROUNDTABLE_DISCUSSION :
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
					startRecluster();
					break;
				case SLEEP_NO_RECLUSTER:
					stage = HccpStage.RUN;
					startRun();
					break;
				case STARTUP:
					stage = HccpStage.ANNOUNCE_CANDIDACY;
					startRecluster();
				
			
			}
			
		}
	}

	private void startRecluster() {
		// Always choose to be a clusterhead
		// Announce.
				
		round++;
		clustermembers.clear();
		msgHandler.on();
		//msgHandler.broadcast( new Packet(this, new BroadcastMessage("CH"+this.id, id, 4 ), this.baud ));
		
		//msgHandler.scheduleBroadcast(new BroadcastMessage("CH"+this.id, id, 4 ), Rand1.uniform(clusterheadRM, 0, LeachConstants.RECLUSTER_TIME/2 ) );
		isClusterhead = true;
		skipRound = false;
		if (HccpConstants.allowSinkSleep)
		{
			if (Utility.randU01(sinkSleepRM) <= HccpConstants.sinkSleepPercentage)
			{
				isClusterhead = false;
				skipRound = true;
			}
		}
		
		if (!skipRound)
			msgHandler.scheduleBroadcast(new BroadcastMessage("CC"+this.id + ";0;0", id, 4 ), Rand1.uniform(clusterheadRM, 0, HccpConstants.RECLUSTER_TIME/2 ) );
		
		if (Configuration.getBooleanConfig("hccp_skip_candidacy"))
			new NextStage().schedule(0);
		else
			new NextStage().schedule(HccpConstants.RECLUSTER_TIME);
		
	}
	
	private void startAnnounceClusterhead()
	{
		// actually announce the clusterhead, if this is one (which it will always be)
		
		//System.out.println(name + " starting announce at " + Sim.time());
		
		if ( !skipRound )
			msgHandler.scheduleBroadcast(new BroadcastMessage("CH"+this.id + ";0;0", id, 4 ), Rand1.uniform(clusterheadRM, 0, HccpConstants.RECLUSTER_TIME/2 ) );
		new NextStage().schedule(HccpConstants.RECLUSTER_TIME);
	}

	private void startChooseCluster() {
		// Listen - add ids/nodes to the list of known clustermemebers
		msgHandler.on();
		if (HccpConstants.allowSinkSleep && skipRound)
			msgHandler.off();
		new NextStage().schedule(HccpConstants.CHOOSE_CLUSTERHEAD_TIME);
		
	}
	
	private void startWaitForSchedule() {
		// create and send the schedule
		// announce schedule
		String schedule = createSchedule();
		// delay a broadcast, to try to space the annoucements.
		CSMAMessageHandler h = (CSMAMessageHandler) msgHandler; // since it's leach, we know this.
		if (!skipRound)
			h.scheduleBroadcast( new BroadcastMessage(schedule,this.id, schedule.length() ), Rand1.uniform(clusterheadRM, 0, HccpConstants.WAIT_FOR_SCHEDULE_TIME/2 ) );
		if (Configuration.verbose)
			System.out.println(name + "  broadcasting " + schedule);
		
		new NextStage().schedule(HccpConstants.WAIT_FOR_SCHEDULE_TIME);
	}

	

	
	private void startRun() {
		
		// just listen the entire time.
		runtimeAsClusterhead += HccpConstants.TOTAL_RUN_TIME;
		if (!skipRound)
			msgHandler.on();
		
		// count the number of CHs.
		if (cycleCount == 0)
		{	for (Node n : nodes)
			{
				if (n.isClusterhead() && n.isAlive())
					averageCH++;
			}
		}
		
		new NextStage().schedule(HccpConstants.TOTAL_RUN_TIME);
		
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
		msgHandler.on();
		
		// send a beacon every 5 ish times? Configurable
		if (HccpConstants.ROUNDTABLE_TIME > 0)
		{
			if (basestationBeacon < roundtableBeaconRM.nextDouble())
			{
				//System.out.println("sending beacon update");
				if ( Configuration.getBooleanConfig("hccp_beacon") )
					msgHandler.scheduleBroadcast(new BroadcastMessage("b0:0",this.id, 3) , Rand1.uniform(roundtableBeaconDelayRM, 0, HccpConstants.ROUNDTABLE_TIME/(double)5) );
			}
		}
		
		new NextStage().schedule( HccpConstants.ROUNDTABLE_TIME );
	}
	
	private void startSleep()
	{
		new NextStage().schedule( HccpConstants.SLEEP_TIME );
	}
	
	private void startNoReclusterSleep()
	{
		new NextStage().schedule(HccpConstants.SLEEP_NO_RECLUSTER_TIME);
	}
	
	protected void resetNode()
	{
		super.resetNode();
		cycleCount = 0;
		// schedule the first event
		stage = HccpStage.STARTUP;
		new NextStage().schedule(0);
	}
	

}
