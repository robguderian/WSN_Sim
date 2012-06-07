package CSMA;


public class CTSMessage extends CSMAMessage {

	public CTSMessage(String info, int from) {
		super(info, from,  0);
		needsAck = true;
	}
	
	public CTSMessage(CSMAMessage actualPayload, int from, int to) {
		//super(info, 0);
		super(actualPayload.getInfo(), from, 0, to);
		needsAck = true;
		this.actualPayload = actualPayload;
	}
	
	public CSMAMessage reply(int from, int to)
	{
		//return new DataMessage();
		//DataMessage d = (DataMessage) actualPayload;
		actualPayload.setTo(to);
		actualPayload.setFrom(from);
		
		return actualPayload;
		
	}
	
	public String toString()
	{
		return "cts";
	}

}
