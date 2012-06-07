package sim_core;

public class Battery {


	private double power;
	private double startPower;
	
	public Battery()
	{
		this(Configuration.getIntConfig("battery"));
	}
	
	
	public Battery(double startPower)
	{
		this(startPower, Configuration.getIntConfig("batteryVariance"));
	}
	
	
	public Battery(double start, double variance)
	{
		
		this.startPower = start;
		power = startPower;
		
	}
	
	public void reset()
	{
		power = startPower;
	}
	
	
	public double getResidualPower()
	{
		return power;
	}
	
	public double getPercentLeft()
	{
		return power/startPower;
	}
	
	public void draw(double drawAmount)
	{
		power -= drawAmount;
	}
	
	public boolean isAlive()
	{
		if (power > 0)
			return true;
		return false;
	}
}
