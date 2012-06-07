package sim_core;

/*
 * Class Packet
 * 
 * Holds the contents of a 802.15.4 packet
 * Sender information, RSSI values (calculated by sender)
 * the message and any other values that would be valuable 
 * to send in a packet
 */

public class Packet {

	Node sender;
	Message m;
	
	double timeNeededToSend;
	int overheadLength = 3; // TODO - 
	
	public Packet(Node sender, Message m, double radioSpeed)
	{
		this.sender = sender;
		this.m = m;
		
		this.timeNeededToSend = (m.getLength()+overheadLength) /radioSpeed ;
	}
	
	public Message getMessage()
	{
		return m;
	}
	
	public double getTimeNeededToSend()
	{
		return timeNeededToSend;
	}
	
	public Node getSender()
	{
		return sender;
	}
	
	
	
}
