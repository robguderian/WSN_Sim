package CSMA;

import sim_core.Configuration;
public class DataMessage extends CSMAMessage {

	
	
	// stats about the message
	//private int finalDestination = -1;
	private int originalFrom;
	
	//private static ArrayList<DataMessage> dataMessages = new ArrayList<DataMessage>();
	
	public DataMessage( String info, int from,  int length )
	{
		super(info, from,  length); 
		originalFrom = from;
		needsAck = true;
		//dataMessages.add(this);
		messages.add(this);
	}
	
	public void  markComplete()
	{
		super.markComplete();

		/*if (timesReachedBaseStation == 0)
		{
			NetworkNode n = (NetworkNode)Node.getNodeFromId(originalFrom);
			//n.addToTally(0);
		}*/ // replaced with a different idea

		
		timesReachedBaseStation++;
		
				
		// TODO tally the first time?
	}
	
	public CSMAMessage reply(int from, int to)
	{
		if (Configuration.verbose)
			System.out.println("Data message received");
		return new AckMessage(this, from, to);
	}
	
	public String toString()
	{
		return "data";
	}
	public CSMAMessage getActualPayload()
	{
		return this;
	}
	
	public String getStats()
	{
		String out;
		out = "Number of times reached Basestation: " + timesReachedBaseStation + " from  " + originalFrom + " " + fromId + " " + info;
		return out;
	}

	public int getOriginalFrom() {
		return originalFrom;
	}
	
	
}
