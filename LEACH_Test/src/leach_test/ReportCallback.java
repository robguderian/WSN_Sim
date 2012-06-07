package leach_test;

import sim_core.Configuration;
import sim_core.IReportCallback;
import umontreal.iro.lecuyer.stat.Tally;

public class ReportCallback implements IReportCallback {


	public String printReport(Tally averageDiedAt, double completed) {
		return Configuration.getDoubleConfig("clusterheadChance") + "," + 
						averageDiedAt.min() + ","+ averageDiedAt.max() +","+
	    				averageDiedAt.average() +"," + completed;
	}
	
	public String getImportantData()
	{
		return Configuration.getDoubleConfig("clusterheadChance") + ",";
	}
	
	
	public String getTitles()
	{
		return "ch,";
	}

}
