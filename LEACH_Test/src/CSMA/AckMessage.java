package CSMA;


public class AckMessage extends CSMAMessage {

	public AckMessage(String info, int from) {
		super(info, from,  0);
		needsAck = false;
		
	}
	
	public AckMessage(CSMAMessage actualPayload, int from) {
		//super(info, 0);
		super(actualPayload.getInfo(), from, 0);
		needsAck = false;
		this.actualPayload = actualPayload;
	}
	public AckMessage(CSMAMessage actualPayload, int from, int to) {
		//super(info, 0);
		super(actualPayload.getInfo(), from, 0, to);
		needsAck = false;
		this.actualPayload = actualPayload;
	}
	
	public CSMAMessage reply()
	{
		// TODO mark message sent successfully?
		
		System.out.println("Done sending message!");
		return null;
	}
	
	public String toString()
	{
		return "ack";
	}

}
