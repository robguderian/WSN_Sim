package sim_core;
import CSMA.DataMessage;
import Util.Rand1;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;
import umontreal.iro.lecuyer.simevents.Event;
import umontreal.iro.lecuyer.simevents.Sim;


public abstract class SensorNode extends NetworkNode {
	public static final double DEFAULT_READING_FREQUENCY = 60; // one minute apart
	public final int DEFAULT_MESSAGE_LENGTH = 4;		// that's 4 bytes.
	protected double readingFrequency;
	
	protected RandomStream readings;
	protected int numberOfMessagesCreated = 0;
	
	public abstract Node clone();
	
	private SensorReading read;
	
	
	public SensorNode(int x, int y, String name)
	{
		this(x,y,name,DEFAULT_READING_FREQUENCY, Configuration.getIntConfig("range"));
	}
	
	
	public SensorNode(int x, int y, String name, double readingFrequency, double range) {
		this(x, y, name, range, Configuration.getDoubleConfig("readingFreq"), 
				Configuration.getDoubleConfig("onDraw"), Configuration.getDoubleConfig("offDraw"));
		
		
		
	}
	
	public SensorNode(int x, int y, String name, double readingFreqencyIn, double range, double onDraw, double offDraw)
	{
		this( x,  y,  name,  readingFreqencyIn,  range,  onDraw,  offDraw, Configuration.getIntConfig("battery"));
	}
	
	
	public SensorNode(int x, int y, String name, double readingFreqencyIn, double range, double onDraw, double offDraw, int battery)
	{
		this( x,  y,  name,  readingFreqencyIn,  range,  onDraw,  offDraw, battery, Configuration.getIntConfig("messageQueueSize"));
	}
	
	public SensorNode(int x, int y, String name, double readingFreqencyIn, double range, double onDraw, double offDraw, int battery, int queueSize) 
	{
		//(int x, int y, String name, double range, double onDrawIn, double offDrawIn)
		super(x, y, name, range, onDraw, offDraw, battery, queueSize);
		this.readingFrequency = readingFreqencyIn;
		
		// create the first reading event
		
		readings = new MRG32k3a("Random Stream for sensor reading events in a node");
		
		//if ( this.getDistance(Basestation.lastBS) > 500 ) // creates a dead spot
		
	}
	
	public void setFreq( double freq )
	{
		this.readingFrequency = freq;
		if (freq <= 0 && read != null)
			read.cancel();
	}
	
	public double getFreq()
	{
		return readingFrequency;
	}
	
	protected void resetNode()
	{
		super.resetNode();
		if (read != null)
			read.cancel();
		if (readingFrequency > 0)
			startReading();
	}
	
	protected void startReading()
	{
		if (read != null)
		{
			read.cancel();
		}
		if (this.readingFrequency > 0)
		{
			read = new SensorReading();
			read.schedule(0);
		}
	}

	
	private void readingCallback() {
		// add a message to the outgoing queue
		//if (debug > 2)
			//System.out.println("reading created. Making message");
		numberOfMessagesCreated++;
		addMessageToList( new DataMessage("Sensor reading from " + name + " at time" + Sim.time(), id, (int)Rand1.expon(readings, DEFAULT_MESSAGE_LENGTH)) );
		
		// create the next reading event
		/*double sch = Rand1.expon(readings, readingFrequency);
		new SensorReading().schedule(  sch  );*/
		read = new SensorReading();
		read.schedule(readingFrequency);

	}
	
	// **** SSJ Event Classes *****/
	
	private class SensorReading extends Event
	{
		public void actions()
		{
			if (isAlive())
			{
				readingCallback();
			}
		}
	}
}
