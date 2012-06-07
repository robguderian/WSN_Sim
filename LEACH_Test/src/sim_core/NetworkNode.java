package sim_core;
import java.util.LinkedList;

import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;
import umontreal.iro.lecuyer.simevents.Accumulate;
import umontreal.iro.lecuyer.stat.Tally;
import CSMA.DataMessage;


/*
 * Superclass for nodes that exist in the network (non-basestation nodes)
 * Characterized by having a queue of messages to send
 */


public abstract class NetworkNode extends Node {

	//protected List messageQueue = new List();
	protected LinkedList <Message> messageQueue = new LinkedList<Message>();
	
	private boolean debug = true;
	
	protected Tally messageTally;
	protected Accumulate queueSize;
	
	
	protected int maxQueueSize;
	protected int messagesLostDueToQueue = 0;
	
	protected RandomStream failureRM;
	
	protected boolean isFailedClusterhead;
	
	public NetworkNode(int x, int y, String name, double range) {
		this(x, y, name, range, Configuration.getDoubleConfig("onDraw"), Configuration.getDoubleConfig("offDraw"));
			
	}
	
	public NetworkNode(int x, int y, String name, double range, double onDrawIn, double offDrawIn)
	{
		this ( x,  y,  name,  range,  onDrawIn,  offDrawIn, Configuration.getIntConfig("battery"));
	}
	
	public NetworkNode(int x, int y, String name, double range, double onDrawIn, double offDrawIn, int battery)
	{
		this( x, y, name, range, onDrawIn, offDrawIn, battery, Configuration.getIntConfig("messageQueueSize"));
	}
	
	public NetworkNode(int x, int y, String name, double range, double onDrawIn, double offDrawIn, int battery, int maxQueueSize)
	{
		super( x, y, name, range, onDrawIn, offDrawIn, battery);
		this.maxQueueSize = maxQueueSize;
		
		
		failureRM =  new MRG32k3a("Failure generator!"); 
	}
	
	
	public boolean checkFailAsClusterhead()
	{
		// fail with some frequency
		
		if ( failureRM.nextDouble() < Configuration.getDoubleConfig("failureRate"))
		{
			return true;
		}
		return false;
	}
	
	public void assignMessage( Packet p)
	{
		if (!false)
			addMessageToList(p.getMessage());
	}
	
	
	/*
	 * sends the next message in the message queue
	 */
	public void sendMessage()
	{
		// send the message to this node's clusterhead
		//Packet toSend = new Packet( this, getMessage(), baud);
		
		//following.assignMessage(toSend);
		
		// send the message to all nodes in listening range
		//msgHandler.broadcast( toSend );
		
		if (debug)
			System.out.println (this.name + " sending a message");
		
		Message m = getMessage();
		if (m == null)
			System.out.println("Message is null in send?");
		if (!msgHandler.isSending())
			msgHandler.send(m);
		
		
	}
	
	public void addMessageToList( Message m)
	{
		if (m != null && m instanceof DataMessage && messageQueue.size() < maxQueueSize)
		{
			//messageQueue.insert(m,List.FIRST);
			messageQueue.addFirst(m);
			
		
			// get ready to send the message 
			//messageReadyToSend();
		}
		else if(messageQueue.size() == maxQueueSize )
			messagesLostDueToQueue++;
		queueSize.update(messageQueue.size());
		
	}
	
	protected Message getMessage()
	{
		// we know the list is all messages, so a straight cast will suffice
		//return (Message)messageQueue.remove(List.LAST);
		//queueSize.update(messageQueue.size()-1);
		
		return messageQueue.getLast();
	}
	
	public boolean hasMessage(Message m)
	{	
		for (Message n : messageQueue)
		{
			if (m.getInfo().equals(n.getInfo()))
			{
				return true;
			}
		}
		return false;
	}
	
	public void addToTally(double in)
	{
		messageTally.add(in);
	}
	
	
	protected double messageReport()
	{
	
		if (messageTally.numberObs() > 3)
			return messageTally.average();
		return 0;
	}
	
	protected double messageSD()
	{
		if (messageTally.numberObs() > 3)
			return messageTally.standardDeviation();
		return 0;
	}
	
	abstract protected  void messageReadyToSend();
	
	
	protected void resetNode()
	{
		super.resetNode();
		messageTally= new Tally();
		queueSize = new Accumulate();
		queueSize.update(0);
		
		messageQueue.clear();
		messagesLostDueToQueue = 0;
		
		isFailedClusterhead = false;
		
	}

}
