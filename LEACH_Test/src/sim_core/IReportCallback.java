package sim_core;

import umontreal.iro.lecuyer.stat.Tally;

public interface IReportCallback {

	public String printReport(Tally averageDiedAt, double completed);
	public String getImportantData();
	public String getTitles();
}

