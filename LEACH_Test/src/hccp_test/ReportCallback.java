package hccp_test;

import sim_core.Configuration;
import sim_core.IReportCallback;
import sim_core.NetworkSim;
import umontreal.iro.lecuyer.stat.Tally;

public class ReportCallback implements IReportCallback {
	
	
	public String printReport(Tally averageDiedAt, double completed) {
		/*return Configuration.getDoubleConfig("clusterheadChance") + "," +
				Configuration.getDoubleConfig("suboptimalClusterheadPercentage") + "," +
				Configuration.getDoubleConfig("hccp_firstorder_ch_perc") + "," +
				Configuration.getDoubleConfig("sinkSleepPercentage") + "," +
				Configuration.getDoubleConfig("hccp_firstorder_sub_perc") + "," +
				Configuration.getDoubleConfig("sinkSleepPercentage") + "," +
				HccpConstants.SENSOR_MISSION_WEIGHT + "," +
				HccpConstants.BATTERY_POWER_WEIGHT + "," +
				HccpConstants.DUTY_CYCLE_WEIGHT + "," +
				HccpConstants.RANDOM_WEIGHT + "," +
				HccpConstants.MESSAGE_QUEUE_WEIGHT + "," +
				averageDiedAt.min() + ","+ 
				averageDiedAt.max() +","+ 
        		averageDiedAt.average() + "," + 
        		completed     		;*/
		return getImportantData() +
			averageDiedAt.min() + ","+ 
			averageDiedAt.max() +","+ 
			averageDiedAt.average() + "," + 
			completed+","     		;
		
	}
	
	public String getImportantData()
	{
		return Configuration.getDoubleConfig("clusterheadChance") + "," +
				Configuration.getDoubleConfig("suboptimalClusterheadPercentage") + "," +
				Configuration.getDoubleConfig("hccp_firstorder_ch_perc") + "," +
				Configuration.getDoubleConfig("sinkSleepPercentage") + "," +
				Configuration.getDoubleConfig("hccp_firstorder_sub_perc") + "," +
				Configuration.getDoubleConfig("sinkSleepPercentage") + "," +
				HccpConstants.SENSOR_MISSION_WEIGHT + "," +
				HccpConstants.BATTERY_POWER_WEIGHT + "," +
				HccpConstants.DUTY_CYCLE_WEIGHT + "," +
				HccpConstants.RANDOM_WEIGHT + "," +
				HccpConstants.MESSAGE_QUEUE_WEIGHT + "," + 
				NetworkSim.getRunNumber() + ",";
	}
	
	public String getTitles()
	{
		return "CH,sub,fo ch, sink sleep, fo sub, sink sleep, sensor, battery,duty,random,message,run,";
	}
	
	
	

}
