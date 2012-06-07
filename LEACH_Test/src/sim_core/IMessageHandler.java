package sim_core;

import CSMA.BroadcastMessage;

/*
 * When a message is returned it gets passed to the message handler.
 */

public interface IMessageHandler {
	
	public void send(Message m);
	public void send(Message m, int to);
	//public void broadcast( Message m);
	
	void scheduleBroadcast(BroadcastMessage in, double delay);
	
	
	public void broadcast (Packet p);
	public void broadcast (Message m);
	//public void reply(Packet m);
	public boolean isSending();
	public void receiveMessage(Packet p);
	
	public void cancelSending();
	public void cancelReceiving();
	public void cancelAll();
	
	public void on();		// Turn on this type of message handling
	public void off();		// turn off this type of message handling
	
	public boolean isOn();
}

