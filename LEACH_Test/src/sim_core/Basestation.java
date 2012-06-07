package sim_core;

import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;
import umontreal.iro.lecuyer.simevents.Event;
import Util.Rand1;


public abstract class Basestation extends Node {

	protected int numberOfMessagesReceived = 0;
	
	protected static Node lastBS; // useful if you only have one basestation
	
	protected int moveToX;
	protected int moveToY;
	protected double deltaX = 0;
	protected double deltaY = 0;
	protected double speed;  // this is in space units / second
	protected double updateDelay;
	
	protected boolean fastLinkUpdates = false;
	
	private int updateCounter = 0;
	
	
	
	private RandomStream moveToLocationRM;
	
	public Basestation(int x, int y, String name, double range, double onDraw, double offDraw)
	{
		super( x,  y,  name,  range,  onDraw,  offDraw);
		lastBS = this;
		
		if (Configuration.getBooleanConfig("moveBasestation"))
		{
			moveToLocationRM = new MRG32k3a();
			speed = Configuration.getDoubleConfig("basestationSpeed")/(double)Configuration.getIntConfig("basestationUpdateSpeed");
			
			moveToX = x; // start off, and make a new goal after first iteration.
			moveToY = y;
			updateDelay = 1/(double)Configuration.getIntConfig("basestationUpdateSpeed");
			
			fastLinkUpdates = Configuration.getBooleanConfig("fastLinkUpdates");
			
			new Move().schedule(updateDelay);
		}
		
	}
	
	
	public Basestation(int x, int y, String name, double range) {
		this(x,y,name,range,0,0);
		
	}
	public Basestation(int x, int y, String name) {
		this(x, y, name, Configuration.getIntConfig("range"));
		
	}

	public void assignMessage( Packet p)
	{
		// mark this message as complete!
		
	}
	
	
	private void updateDeltas()
	{
		double angle = Math.atan( Math.abs(moveToY-position.y)/Math.abs( moveToX - position.x) );
		if ( ( moveToX - position.x) >= 0 && (moveToY-position.y) >= 0 )
		{
			deltaY = speed * Math.sin(angle);
			deltaX = speed * Math.cos(angle);
		}
		else if (  ( moveToX - position.x) <= 0 && (moveToY-position.y) >= 0  )
		{
			//angle = Math.PI - angle;
			deltaY = speed * Math.sin(angle);
			deltaX =  -1 *speed * Math.cos(angle);
		}
		else if (  ( moveToX - position.x) <= 0 && (moveToY-position.y) <= 0  )
		{
			//angle = angle - Math.PI;
			deltaY = -1 * speed * Math.sin(angle);
			deltaX = -1 * speed * Math.cos(angle);
		}
		else
		{
			//angle = 2* Math.PI - angle ;
			deltaY = -1 *speed * Math.sin(angle);
			deltaX =  speed * Math.cos(angle);
		}
	}
	
	private void doMove()
	{
		// get new x/y coordinates
		
		
		//System.out.println("BS is at " + position.x + " " + position.y + " and is going to " + moveToX + " " + moveToY + " at " + deltaX + " " + deltaY);
		
		position.y += deltaY;
		position.x += deltaX;
		
		
		//System.out.println(position.x + " " + position.y);
		
		updateCounter++;
		if (updateCounter > 20)
		{
			updateDeltas();
			updateCounter = 0;
		}
		
		
		// if I'm within 1 step of the goal... we're done, choose a new goal.
		// I chose to say 1, assuming that delta is smaller. I could make it a tuneable.
		if ( Math.abs(position.x - moveToX) <= 1 && Math.abs(position.y - moveToY) <= 1 )
		{
			
			moveToX =(int)Rand1.uniform(moveToLocationRM, Configuration.getIntConfig("minX"), Configuration.getIntConfig("maxX"));
			moveToY =(int)Rand1.uniform(moveToLocationRM, Configuration.getIntConfig("minY"), Configuration.getIntConfig("maxY"));
			updateDeltas();
		}
		
		// if fast updates enabled
		if (fastLinkUpdates)
		{
			// remove the basestation from all the nodes the 
			// basestation knows about
			for (Node n : nodesInRange)
			{
				n.nodesInRange.remove(this);
			}
			
			// drop the basestation's links
			nodesInRange.clear();
			
			// recreate the basestation links. Add
			// basestation to all nodes in range.
			for (Node n : Node.nodes)
			{
				if ( inRange(n) && n != this)
				{
					n.nodesInRange.add(this);
					nodesInRange.add(n);
				}
			}
		}
		else
		{
			// call the make links that
			// was called in the sim creator
			// it's slow.... but... that's all we can do.
			makeLinks();
		}
	
		// schedule the next move
		new Move().schedule(updateDelay);
	}
	
	public static Node getLastBS()
	{
		return lastBS;
	}
	
	public class Move extends Event
	{
		public void actions()
		{
			//System.out.println("Move");
			doMove();
		}
	}
	
	
	protected void resetNode()
	{
		super.resetNode();
		numberOfMessagesReceived = 0;
	}
	
}
