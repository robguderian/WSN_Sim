package sim_core;

import java.awt.geom.Point2D;

public class PointPair {
	
	private Point2D start;
	private Point2D end;
	
	public PointPair( Point2D start, Point2D end)
	{
		this.start = start;
		this.end = end;
		
	}
	
	public Point2D getStart()
	{
		return start;
	}
	public Point2D getEnd()
	{
		return end;
	}
	

}
