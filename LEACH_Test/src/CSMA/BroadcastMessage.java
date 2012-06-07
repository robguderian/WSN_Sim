package CSMA;


public class BroadcastMessage extends CSMAMessage {

	public BroadcastMessage(String info, int from, int length) {
		super(info,from, length);
		
		needsAck = false;
		forId = -1;
	}
	
	public BroadcastMessage( String info, int from, int length, int to)
	{
		super (info, from, length, to);
		needsAck = false;
	}

	
	public String toString()
	{
		return "broadcast";
	}
}
