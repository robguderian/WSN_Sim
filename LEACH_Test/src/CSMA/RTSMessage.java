package CSMA;


public class RTSMessage extends CSMAMessage {
	

	public RTSMessage(CSMAMessage actualPayloadIn, int from, int to) {
		//super(info, 0);
		
		super(actualPayloadIn.getInfo(), from, 0, to);
		needsAck = true;
		this.actualPayload = actualPayloadIn;
		
	}
	
	public RTSMessage(CSMAMessage actualPayloadIn, int from) {
		//super(info, 0);
		
		super(actualPayloadIn.getInfo(), from, 0);
		needsAck = true;
		this.actualPayload = actualPayloadIn;
		
	}
	
	public CSMAMessage reply(int from, int to)
	{
		return new CTSMessage(actualPayload, from, to);
	}
	
	public String toString()
	{
		return "rts";
	}

}
