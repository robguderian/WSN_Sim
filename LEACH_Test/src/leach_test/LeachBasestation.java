package leach_test;


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


public class LeachBasestation extends Basestation implements IClusterhead {

	LeachStage LStage;
	RandomStream clusterheadRM;

	
	
	public LeachBasestation(int x, int y, String name) {
		this (x,y,name, Configuration.getIntConfig("range"));
	}
	
	public LeachBasestation(int x, int y, String name, double range) {
		super(x, y, name, range);
		msgHandler = new CSMAMessageHandler(this);
		
		resetNode();
		
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
		
		if (LStage == LeachStage.RUN)
		{
			numberOfMessagesReceived++;
			if (Configuration.verbose)
				System.out.println("Base Station received a message! " + m.getInfo());
			m.markComplete();
			m.setClosestIveBeenToSink(0);
			
			//TODO stats
		}
		else if (LStage == LeachStage.CHOOSE_CLUSTER){
			String message = m.getInfo();
			if (message.startsWith("FL")){
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
		else
		{
			if (Configuration.verbose)
				System.out.println("nuffin");
		}
	}

	private String createSchedule()
	{
		double delay = 0;
		double runtime = 0; 
		if (clustermembers.size() > 0)
			runtime = LeachConstants.TOTAL_RUN_TIME / clustermembers.size(); 
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
					LStage = LeachStage.RUN;
					startRun();
					break;
				case RUN :
					LStage = LeachStage.SLEEP;
					startSleep();
					break;
				case SLEEP:
					LStage = LeachStage.RECLUSTER;
					startRecluster();
					break;
				
				
			
			}
			
		}
	}

	private void startRecluster() {
		// Always choose to be a clusterhead
		// Announce.
		
		//System.out.println("recluster " + Sim.time());
		
		//System.out.print(".");
		round++;
		clustermembers.clear();
		msgHandler.on();
		//msgHandler.broadcast( new Packet(this, new BroadcastMessage("CH"+this.id, id, 4 ), this.baud ));
		
		//msgHandler.scheduleBroadcast(new BroadcastMessage("CH"+this.id, id, 4 ), Rand1.uniform(clusterheadRM, 0, LeachConstants.RECLUSTER_TIME/2 ) );
		msgHandler.scheduleBroadcast(new BroadcastMessage("CH"+this.id + ";0;0", id, 4 ), Rand1.uniform(clusterheadRM, 0, LeachConstants.RECLUSTER_TIME/(double)2 ) );
		
		new NextStage().schedule(LeachConstants.RECLUSTER_TIME);
		
	}

	private void startChooseCluster() {
		// Listen - add ids/nodes to the list of known clustermemebers
		msgHandler.on();
		new NextStage().schedule(LeachConstants.CHOOSE_CLUSTERHEAD_TIME);
		
	}
	
	private void startWaitForSchedule() {
		// create and send the schedule
		// announce schedule
		String schedule = createSchedule();
		// delay a broadcast, to try to space the annoucements.
		CSMAMessageHandler h = (CSMAMessageHandler) msgHandler; // since it's leach, we know this.
		h.scheduleBroadcast( new BroadcastMessage(schedule,this.id, schedule.length() ), Rand1.uniform(clusterheadRM, 0, LeachConstants.WAIT_FOR_SCHEDULE_TIME/2 ) );
		if (Configuration.verbose)
			System.out.println(name + "  broadcasting " + schedule);
		
		new NextStage().schedule(LeachConstants.WAIT_FOR_SCHEDULE_TIME);
	}

	

	
	private void startRun() {
		
		// just listen the entire time.
		runtimeAsClusterhead += LeachConstants.TOTAL_RUN_TIME;
		msgHandler.on();
		
		// do some math
		for (Node n : nodes)
		{
			if (n.isClusterhead() && n.isAlive())
				averageCH++;
		}
		
		new NextStage().schedule(LeachConstants.TOTAL_RUN_TIME);
		
	}

	private void startSleep()
	{
		new NextStage().schedule(LeachConstants.SLEEP);
	}
		
	
	protected void resetNode()
	{
		super.resetNode();
		// schedule the first event
		LStage = LeachStage.SLEEP;
		clusterheadRM = new MRG32k3a();		
		isClusterhead = true;
		new NextStage().schedule(0);
		
		
	}
}
