package cz.cuni.mff.ufal.lindat.utilities.interfaces;

import java.util.List;

import cz.cuni.mff.ufal.lindat.utilities.hibernate.PiwikReport;

public interface IPiwikReport {

	public List<PiwikReport> getAllPiwikReports();
	
	public List<PiwikReport> getAllUsersSubscribed(int itemId);
	
	public boolean isSubscribe(int epersonId, int itemId);
	
	public boolean subscribe(int epersonId, int itemId);
	
	public boolean unSubscribe(int epersonId, int itemId);
}
