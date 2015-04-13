package cz.cuni.mff.ufal.lindat.utilities.hibernate;


public class PiwikReport extends GenericEntity implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int reportId;
	private int epersonId;
	private int itemId;
	
	public PiwikReport() {
	}

	public PiwikReport(int reportId, int epersonId, int itemId) {
		this.reportId = reportId;
		this.epersonId = epersonId;
		this.itemId = itemId;
	}
	
	public int getReportId() {
		return this.reportId;
	}
	
	public void setReportId(int reportId) {
		this.reportId = reportId;
	}

	public int getEpersonId() {
		return this.epersonId;
	}

	public void setEpersonId(int epersonId) {
		this.epersonId = epersonId;
	}

	public int getItemId() {
		return this.itemId;
	}

	public void setItemId(int itemId) {
		this.itemId = itemId;
	}	

	@Override
	public int getID() {
		return reportId;
	}

}
