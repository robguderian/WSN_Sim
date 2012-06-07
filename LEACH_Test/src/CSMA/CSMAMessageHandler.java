/*
 * Handles CSMA elegantly (well, I think so)
 * there are classes for each type of packet.
 * 
 * timeouts are set in the 'done sending' callback
 * The callback checks the packet to see if it requests a callback.
 * 
 * the message.reply returns the next message in the CSMA sequence
 */

package CSMA;

import sim_core.Configuration;
import sim_core.IClusterhead;
import sim_core.IMessageHandler;
import sim_core.Message;
import sim_core.Node;
import sim_core.Packet;
import umontreal.iro.lecuyer.randvar.ExponentialGen;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;
import umontreal.iro.lecuyer.simevents.Event;
import umontreal.iro.lecuyer.simevents.Sim;

public class CSMAMessageHandler implements IMessageHandler {

	//private double CSMA_TIMEOUT = 0.050;
	private double CSMA_BACKOFF = 0.100;
	private int CSMA_MAX_RETRIES = 5;
	private double A_JIFFY = 0.0000001;

	private int debugLevel = 4;

	private Message messageToSend; // The message that is being sent somewhere
	private CSMAMessage currentMessage; // the current message in CSMA handshake
	private Packet currentPacket;
	protected int isReceiving;

	private RadioReceive radio;
	private SendComplete sendRadio;
	protected Packet beingReceived;

	private Node owner;

	private boolean isOn = true;
	//private boolean allowSend = true;

	private CSMATimeout timeout = null;
	private CSMABackoff backoff = null;
	private int CSMAretries;

	//private double someoneSendingUntil;
	//protected RandomStream csmaTimeoutRand;
	RandomStream  csmaTimeoutRand;
	
	
	private boolean strictCSMA = true; // only listen to _my_ clusterhead or _my_ clustermotes



	public CSMAMessageHandler(Node owner) {
		
		if (!Configuration.verbose)
			debugLevel = -1;
		
		isReceiving = 0;
		CSMAretries = 0;
		this.owner = owner;

		//csmaTimeoutRand = new MRG32k3a(
		//		"Random Stream for timeout/backoff events in a node");
		csmaTimeoutRand = new MRG32k3a();
	}

	
	
	public void send (Message m, int to)
	{
		CSMAMessage message = (CSMAMessage)m;
		message.setTo(to);
		send (message);
	}
	
	/*
	 * Sends the message Assumes that everything is set to send. Passes the
	 * message to broadcast, which will do the CSMA work
	 */
	@Override
	public void send(Message m) {
		/*
		 * Send this message. Sends to all people in range. there is an address
		 * in the message. Really starts the CSMA sequence
		 */
		
		if (m == null)
		{
			if (Configuration.verbose)
				System.out.println("ur doin' it wrong. Message is null.");
		}
		
		
		
		if (isOn && !isSending()) {
			// tODO
			/*
			 * if (csma.needsAck()) { if (isReceiving == 0) { // not receiving.
			 * It could be receiving from any surrounding node // even if the
			 * message is not for this mote
			 * 
			 * 
			 * blastPacket( new Packet(owner, m, owner.getBaud())); }
			 * 
			 * timeout.schedule( CSMA_TIMEOUT ); }
			 */

			/*
			 * blast packet sets the timeout (in the send callback)
			 */
			messageToSend = m;
			CSMAretries = 0;
			CSMAMessage csma = (CSMAMessage) m;
			
			
			currentMessage = new RTSMessage(csma, this.owner.getId(), m.getDestination());
			currentPacket = new Packet(owner, currentMessage, owner.getBaud());
			blastPacket(currentPacket);
			
			if (Configuration.verbose)
				System.out.println(owner.getName() + " started sending " + currentPacket.getMessage() + " " + Sim.time() );
			if (Configuration.verbose && currentMessage == null )
				System.out.println(owner.getName() + " herps");

		}

	}

	
	public boolean isReceiving() {
		return ((isReceiving > 0) ? true : false);
	}

	public void broadcast(Message m)
	{
		broadcast(new Packet(owner, m, owner.getBaud()));
	}
	
	public void broadcast(Packet p) {
		if (currentPacket != null)
			if (Configuration.verbose)
				System.out.println("Broadcasting, but currentPacket != null");
		 
		currentPacket = p;
		currentMessage = (CSMAMessage)p.getMessage();
		if (debugLevel > 1)
			System.out.println(owner.getName() + " broadcasting >" + currentMessage.getInfo() +"<");
		blastPacket(p);
	}

	/*
	 * The packet hits the radio. Send to everyone in range. This will cause
	 * collisions, and let the receiver decide whether or not to accept the
	 * packet.
	 * 
	 * Does not set timeout - this code is used by acks and broadcasts which do
	 * not require timeouts
	 */
	private void blastPacket(Packet p) {

		if (!isReceiving()) // CSMA check - will hit timeout without this.
		{
			// set timeout before sending
			// this makes the send complete on the sending node before it
			// completes on
			// the receiving nodes.
			sendRadio = new SendComplete();
			sendRadio.schedule(p.getTimeNeededToSend());

			if (Configuration.verbose)
				System.out.println(p.getMessage().getInfo());
			
			/*for (Node n : owner.getNodesInRange()) {
				if (debugLevel > 2)
					System.out.println(owner.getName() + " sending "
							+ p.getMessage() + " to " + n.getName());
				if (n.getId() != p.getMessage().getDestination() && n.getId() != owner.getId())
				{
					n.receiveMessage(p);			
				}
			}
			Node n = Node.getNodeFromId(p.getMessage().getDestination());
			
			if (n != null)
			{				
				
				n.receiveMessage(p);
			}*/ // why?  changed to below
			for (Node n : owner.getNodesInRange()) {
				if (debugLevel > 2)
					System.out.println(owner.getName() + " sending "
							+ p.getMessage() + " to " + n.getName());
				n.receiveMessage(p);			
				
			}

		} else {
			if (Configuration.verbose)
				System.out.println("Someone else is sending. " + owner.getName() +"  has to wait.");
			// set timeout - the send callback can't to it for us from here.
			if (backoff != null && debugLevel > 2)
				if (Configuration.verbose)
					System.out.println("backoff not null in blastpacket?");
			//currentPacket = p;
			//currentMessage = (CSMAMessage) p.getMessage();
			backoff = new CSMABackoff();
			//backoff.schedule(Rand1.expon(csmaTimeoutRand, CSMA_BACKOFF));
			backoff.schedule(ExponentialGen.nextDouble(csmaTimeoutRand, CSMA_BACKOFF));
			
		}

	}

	/*
	 * A new message is being received (started to send) Check to see if we are
	 * already receiving a packet - if so... the messages will both be garbled
	 * -set message being received to null otherwise, set the message being
	 * received to the message
	 * 
	 * set the receive timeout. The callback will take the value from
	 * beingReceived (which is null if there was a collision)
	 */
	public void receiveMessage(Packet p) {
		// getting this message, we like it or not!
		// if this is already receiving a packet... both are now screwed up.

		if (!isOn || !owner.isAlive())
			return;
		
		// check to see if it's the next message in the CSMA sequence we're
		// handling

		if (timeout != null) {
			// TODO is this the message we're waiting for?

			timeout.cancel();
			timeout = null;
		}

		// cancel the timeout if it is the message in question

		if (debugLevel > 2)
			System.out.println(owner.getName() + " got " + p.getMessage()
					+ "  message. Will take " + p.getTimeNeededToSend());
		if (isReceiving > 0 || radio != null) {
			// reschedule to the end of the time
			// set both packets to null.
			if (Configuration.verbose)
				System.out.println("Collision in " + owner.getName() + "!");
			beingReceived = null;

			// note that collisions can be double counted - one for each node
			// that didn't hear a message.
			Node.collisionHappened();
			
			//System.out.println(owner.getName() + " " + Sim.time());
			
			// take the maximum amount of time
			//if (radio != null)
			//	System.out.println("r" + radio.time() + " " +Sim.time());
			if ( radio == null || radio.time() < Sim.time()) //TODO - why is this needed?
			{
				if (radio != null)
					radio.cancel();
				radio = new RadioReceive();
				radio.schedule(p.getTimeNeededToSend());
			}
			else if (p !=null && radio.time() > Sim.time() + p.getTimeNeededToSend())
			{
				//System.out.println(owner.getName() + " " +Sim.time());
				//System.out.println("r" + radio.time() + " " +Sim.time());
				radio.reschedule(p.getTimeNeededToSend());
			}
		} else {
			// if it is not receiving already... hurray!
			radio = new RadioReceive();
			
			if (p.getMessage() instanceof AckMessage)
				radio.schedule(p.getTimeNeededToSend()+ A_JIFFY); // schedule the end of the
			else											// receive
				radio.schedule(p.getTimeNeededToSend());
			beingReceived = p;
		}

		isReceiving++;
	}

	public Message getMessageToSend() {
		return messageToSend;
	}

	/*
	 * The message is done being sent to us - the message might be garbled
	 * (null), or it is appropriately received
	 * 
	 * If the message is null, we can't do anything, so don't. If it's not.
	 * respond if needed Otherwise, tell the node it has a new message.
	 */
	protected void messageReceiveCallback() 
	{
		if (beingReceived == null) 
		{
			// count collisions here
			// just bail.
			isReceiving = 0;
			
			// notify the owner it had a collision.
			owner.hadCollision();
			
			return;
		}
		
		if (strictCSMA) // don't listen if we don't know them
		{
			// if i'm not following you or you're not in my list... bail.
			if (!(owner.getFollowing() == beingReceived.getSender()|| 
					owner.hasClustermember(beingReceived.getSender()) || 
					beingReceived.getMessage() instanceof BroadcastMessage) )
				return;
		}

		isReceiving = 0;
		CSMAMessage done = (CSMAMessage) beingReceived.getMessage();
		

		if (timeout != null)
		{
			timeout.cancel();
			timeout = null;
		}

		// am i currently sending
		if (sendRadio != null)
			return;
		
		if (debugLevel > 2)
			System.out.println(owner.getName() + " finished receiving a "
					+ done + " message.");

		IClusterhead ic = (IClusterhead) owner;
		if (ic.isClusterhead() && (done.getDestination() == owner.getId() || done.getDestination() == -1 )){//|| done.getActualPayload() == messageToSend) {
			// and is a message is from a clustermember
			
			if (done.needsAck()) {

				// TODO is this message for us? Do we care?

				CSMAMessage out = done.reply(this.owner.getId(), done.getFrom() );
				currentMessage = out;

				if (out != null) {
					// blast packet (well, sendComplete) sets the timeout
					// for us, if the packet needs it.
					currentPacket = new Packet(owner, out, owner.getBaud());
					
					
					//blastPacket(currentPacket);
					new ScheduleReply().schedule(A_JIFFY);
					
					if (done instanceof DataMessage)
					{	
						owner.receiveComplete(done);
					}
				}
			
			} 
			else {
				/*
				 * We're done here. Clean up.
				 */
				if (done instanceof BroadcastMessage) {
					owner.receiveComplete(done);
				}

				if (done instanceof AckMessage) {
					
					owner.sendComplete(currentMessage);
				}

				currentMessage = null;
				messageToSend = null;

			}
		} // end isCH
		else if ( done.getDestination() == owner.getId() || done.getDestination() == -1 )
		{
			// not a clusterhead... pass it back to the node's receive to triage it there? TODO?
			if (done.needsAck()) {

				//  is this message for us? Do we care?

				
				CSMAMessage out = done.reply(this.owner.getId(), done.getFrom() );
				currentMessage = out;

				if (out != null) {
					// blast packet (well, sendComplete) sets the timeout
					// for us, if the packet needs it.
					currentPacket = new Packet(owner, out, owner.getBaud());
					
					//blastPacket(currentPacket);
					new ScheduleReply().schedule(A_JIFFY);

				}
				
			} 
			// this is either a broadcast or ack.
			if  (done instanceof BroadcastMessage)
			{
				owner.receiveComplete(done);
				
			}
			else if ( done instanceof AckMessage )
			{
				if (Configuration.verbose)
					System.out.println(this.currentMessage.getInfo());
				
				Message temp = currentMessage;
				currentMessage = null;
				currentPacket = null;
				messageToSend = null;
				
			
				
				owner.sendComplete(temp);
				//if (Configuration.verbose)
				//	System.out.println(Sim.eventList.viewFirst());
				
			}
			
		}
		beingReceived = null;

	

	}

	protected void sendCompleteCallback() {
		// start the timeout, if needed
		
		
		
		if (currentMessage == null)
		{
			if (Configuration.verbose)
				System.out.println(owner.getName() + " derps");
			currentMessage = (CSMAMessage) currentPacket.getMessage();
			if (currentMessage == null)
				if (Configuration.verbose)
					System.out.println("Critical hit!");

		}
		
		if (currentMessage.needsAck()) {
			if (timeout != null)
			{
				//System.out.println("Bad bad bad! Timeout was not null in "
				//		+ owner.getName() + " csma send");
				timeout.cancel();
				timeout = null;
			}
			timeout = new CSMATimeout();
			// timeout.schedule( CSMA_TIMEOUT );
			//timeout.schedule(currentPacket.getTimeNeededToSend() * 4); // TODO -
																		// un-hardcode
			timeout.schedule((currentMessage.getLength()+10) /owner.getBaud());

		} else {
			if (currentMessage instanceof BroadcastMessage) {
				// tell the owner that this message is done
				owner.sendComplete(messageToSend);

			}
			currentPacket = null;
			messageToSend = null;
			
		}
		sendRadio = null;
	}

	@Override
	public boolean isSending() {
		// checks to see if it's in a CSMA state
		return (messageToSend == null && sendRadio == null && backoff == null ? false : true);
	}

	@Override
	public void cancelSending() {
		// this breaks CSMA - put the message back on the queue

		/*if (owner instanceof NetworkNode) // should always be true?
		{
			NetworkNode n = (NetworkNode) owner;
			if (messageToSend != null)
				n.addMessageToList(messageToSend);

		}*/ // removed. this now handled a different way. Message only deleted if completed.

		messageToSend = null;
		if (radio != null)
			radio.cancel();
		
		radio = null;
		if (sendRadio != null)
			sendRadio.cancel();
		sendRadio = null;
		
		if (timeout != null)
			timeout.cancel();
		timeout = null;

	}

	@Override
	public void cancelReceiving() {
		// this breaks CSMA - put the message back on the queue

		/*if (owner instanceof NetworkNode) // should always be true?
		{
			NetworkNode n = (NetworkNode) owner;
			n.addMessageToList(messageToSend);

		}*/

		messageToSend = null;
		if (radio != null)
			radio = null;
		if (sendRadio != null)
			sendRadio.cancel();
		sendRadio = null;
		if (timeout != null)
			timeout.cancel();
		timeout = null;
		isReceiving = 0;
	}

	@Override
	public void cancelAll() {
		// TODO Auto-generated method stub
		cancelSending();
		cancelReceiving();
	}

	@Override
	public void on() {
		// TODO Auto-generated method stub
		isOn = true;
	}

	@Override
	public void off() {

		isOn = false;
		cancelAll();
	}

	protected void timeoutCallback() {
		// check to see if the message should be retransmitted
		// or give up!

		if (debugLevel > 2) // if (SimConfiguration.verbose)
				System.out.println(owner.getName() + " timed out.");
		CSMAretries++;
		if (CSMAretries < CSMA_MAX_RETRIES) {
			if (debugLevel > 2)
				System.out.println("retransmitting");
			// blastPacket(new Packet(owner, currentMessage, owner.getBaud()));
			
			// currentPacket is null of I'm receiving a broadcast or RTS
			if (currentPacket != null)				
				blastPacket(currentPacket);
		} else {
			if (debugLevel > 2)
				System.out.println(owner.getName()
						+ "could not successfully send message");

			// restart sending this message from scratch

			cancelSending();
			CSMAretries = 0;
		}
	}
	
	

	public void scheduleBroadcast(BroadcastMessage in, double delay) {
		ScheduleBroadcast event =  new ScheduleBroadcast(in);
		event.schedule(delay);
	}
	
	public boolean isOn()
	{
		return isOn;
	}
	
	public boolean isStrictCSMA() {
		return strictCSMA;
	}



	public void setStrictCSMA(boolean strictCSMA) {
		this.strictCSMA = strictCSMA;
	}

	/***** SSJ inner classes - Events *****/

	class RadioReceive extends Event {

		@Override
		public void actions() {

			// clear the message receive stuff.
			// if the packet is null, it collided.
			isReceiving = 0;
			radio = null;
			messageReceiveCallback();

		}

	}

	class SendComplete extends Event {
		@Override
		public void actions() {

			// send message
			// block the radio device
			// Wait for an ACK.
			sendCompleteCallback();

		}
	}

	class CSMATimeout extends Event {
		public void actions() {
			timeout = null;
			timeoutCallback();
		}
	}

	class CSMABackoff extends Event {
		public void actions() {
			backoff = null;
			timeoutCallback();
		}
	}

	class ScheduleBroadcast extends Event {
		private BroadcastMessage message;

		public ScheduleBroadcast(BroadcastMessage m) {
			message = m;
		}

		public void actions() {
			broadcast(new Packet(owner, message, owner.getBaud()));
		}

	}
	
	class ScheduleReply extends Event {
		//private CSMAMessage m;
			
		public void actions()
		{
			if (currentPacket!= null)
				blastPacket(currentPacket);
		}
	}

}
