package hccp_test;

public class HccpConstants {
	public static  double RECLUSTER_TIME = 0.300; // <-- this is the same amount of time for both candiate time, and ch time
	public static  double CHOOSE_CLUSTERHEAD_TIME = 0.300;
	public static  double WAIT_FOR_SCHEDULE_TIME = 0.300;
	public static  double TOTAL_RUN_TIME = 1 * 60; // n minutes times 60 seconds
	public static  double SLEEP_TIME = 0;
	public static  double SLEEP_NO_RECLUSTER_TIME = 0;
	
	public static  double ROUNDTABLE_TIME = 5; // keep this pretty short, all motes are on 
	public static  int    NUMBER_OF_SCHEDULED_RUNS = 5;
	
	public static double SENSOR_MISSION_WEIGHT = 0;
	public static double MESSAGE_QUEUE_WEIGHT = 0;
	public static double BATTERY_POWER_WEIGHT = 0;
	public static double RANDOM_WEIGHT = 0;
	public static double DUTY_CYCLE_WEIGHT = 0;
	public static final double  A_JIFFY = 0.0000001;
	public static boolean allowSuboptimalClusterheads = false;
	public static boolean allowSinkSleep = false;
	public static double sinkSleepPercentage = 0.1; // sleep this percentage of the time
	public static boolean verbose = false;
	
}
