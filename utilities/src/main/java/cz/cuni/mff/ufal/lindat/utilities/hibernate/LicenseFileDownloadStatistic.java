package cz.cuni.mff.ufal.lindat.utilities.hibernate;

import java.util.Date;

public class LicenseFileDownloadStatistic extends GenericEntity implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int transactionId;
	private UserRegistration userRegistration;
	private int bitstreamId;
	private Date createdOn;

	public LicenseFileDownloadStatistic() {
	}

	public LicenseFileDownloadStatistic(int transactionId,
			UserRegistration userRegistration, int bitstreamId, Date createdOn) {
		this.transactionId = transactionId;
		this.userRegistration = userRegistration;
		this.bitstreamId = bitstreamId;
		this.createdOn = createdOn;
	}

	public int getTransactionId() {
		return this.transactionId;
	}

	public void setTransactionId(int transactionId) {
		this.transactionId = transactionId;
	}

	public UserRegistration getUserRegistration() {
		return this.userRegistration;
	}

	public void setUserRegistration(UserRegistration userRegistration) {
		this.userRegistration = userRegistration;
	}

	public int getBitstreamId() {
		return this.bitstreamId;
	}

	public void setBitstreamId(int bitstreamId) {
		this.bitstreamId = bitstreamId;
	}

	public Date getCreatedOn() {
		return this.createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	@Override
	public int getID() {
		return transactionId;
	}

}
