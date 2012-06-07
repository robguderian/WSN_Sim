package CSMA;

import sim_core.Message;

public abstract class CSMAMessage extends Message {

	public static final int  TO_BASE_STATION = -1;
	
	
	boolean needsAck = false;
	CSMAMessage actualPayload;
	
	protected int forId; // send to this id. Send to TO_BASE_STATION if it is to be routed to the sink
	
	
	public CSMAMessage(String info, int from, int length) {
		super(info, from, length);
		
		
		// by default, route to sink.
		forId = TO_BASE_STATION;
	}
	
	public CSMAMessage(String info, int from,  int length, int toId)
	{
		super(info, from,  length);
		forId = toId;
	}
	
	public int getDestination()
	{
		return forId;
	}
	
	/*
	 * Reply to this packet
	 * Broadcast packets and acks do not need a reply, and return null.
	 */
	/*public CSMAMessage reply(int from)
	{
		return null;
	}*/
	
	public CSMAMessage reply (int from, int to)
	{
		return null;
	}
	
	public boolean needsAck()
	{
		return needsAck;
	}
	
	/*
	 * Resets the to id. remember that Data packets have a 'finalDestination' that is the
	 * node the message is actually intended for.
	 */
	public void setTo(int to)
	{
		forId = to;
	}
	
	public void setFrom(int from)
	{
		fromId = from;
	}
	
	public CSMAMessage getActualPayload()
	{
		return actualPayload;
	}
	
	public String getStats()
	{
		String out;
		out = "Number of times reached Basestation: " + timesReachedBaseStation;
		return out;
	}
}
