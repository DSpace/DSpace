package cz.cuni.mff.ufal.lindat.utilities.hibernate;

import java.util.Date;

public class LicenseResourceUserAllowance extends GenericEntity implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int transactionId;
	private UserRegistration userRegistration;
	private LicenseResourceMapping licenseResourceMapping;
	private Date createdOn;
	private String token;

	public LicenseResourceUserAllowance() {
	}

	public LicenseResourceUserAllowance(int transactionId,
			UserRegistration userRegistration,
			LicenseResourceMapping licenseResourceMapping, Date createdOn, String token) {
		this.transactionId = transactionId;
		this.userRegistration = userRegistration;
		this.licenseResourceMapping = licenseResourceMapping;
		this.createdOn = createdOn;
		this.token = token;
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

	public LicenseResourceMapping getLicenseResourceMapping() {
		return this.licenseResourceMapping;
	}

	public void setLicenseResourceMapping(
			LicenseResourceMapping licenseResourceMapping) {
		this.licenseResourceMapping = licenseResourceMapping;
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
	
	public String getToken() {
		return this.token;
	}

	public void setToken(String token) {
		this.token = token;
	}

}
