package cz.cuni.mff.ufal.lindat.utilities.hibernate;

import java.util.HashSet;
import java.util.Set;

public class UserRegistration extends GenericEntity implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int epersonId;
	private String email;
	private String organization;
	private boolean confirmation;
	private Set<LicenseFileDownloadStatistic> licenseFileDownloadStatistics = new HashSet<LicenseFileDownloadStatistic>(0);
	private Set<LicenseResourceUserAllowance> licenseResourceUserAllowances = new HashSet<LicenseResourceUserAllowance>(0);
	private Set<LicenseDefinition> licenseDefinitions = new HashSet<LicenseDefinition>(0);	
	private Set<UserMetadata> userMetadata = new HashSet<UserMetadata>(0);
	
	public UserRegistration() {
	}

	public UserRegistration(int epersonId, String email, String organization,
			boolean confirmation) {
		this.epersonId = epersonId;
		this.email = email;
		this.organization = organization;
		this.confirmation = confirmation;
	}

	public UserRegistration(int epersonId, String email, String organization,
			boolean confirmation,
			Set<LicenseFileDownloadStatistic> licenseFileDownloadStatistics,
			Set<LicenseResourceUserAllowance> licenseResourceUserAllowances,
			Set<LicenseDefinition> licenseDefinitions) {
		this.epersonId = epersonId;
		this.email = email;
		this.organization = organization;
		this.confirmation = confirmation;
		this.licenseFileDownloadStatistics = licenseFileDownloadStatistics;
		this.licenseResourceUserAllowances = licenseResourceUserAllowances;
		this.licenseDefinitions = licenseDefinitions;
	}

	public int getEpersonId() {
		return this.epersonId;
	}

	public void setEpersonId(int epersonId) {
		this.epersonId = epersonId;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getOrganization() {
		return this.organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public boolean isConfirmation() {
		return this.confirmation;
	}

	public void setConfirmation(boolean confirmation) {
		this.confirmation = confirmation;
	}

	public Set<LicenseFileDownloadStatistic> getLicenseFileDownloadStatistics() {
		return this.licenseFileDownloadStatistics;
	}

	public void setLicenseFileDownloadStatistics(
			Set<LicenseFileDownloadStatistic> licenseFileDownloadStatistics) {
		this.licenseFileDownloadStatistics = licenseFileDownloadStatistics;
	}

	public Set<LicenseResourceUserAllowance> getLicenseResourceUserAllowances() {
		return this.licenseResourceUserAllowances;
	}

	public void setLicenseResourceUserAllowances(
			Set<LicenseResourceUserAllowance> licenseResourceUserAllowances) {
		this.licenseResourceUserAllowances = licenseResourceUserAllowances;
	}

	public Set<LicenseDefinition> getLicenseDefinitions() {
		return this.licenseDefinitions;
	}

	public void setLicenseDefinitions(Set<LicenseDefinition> licenseDefinitions) {
		this.licenseDefinitions = licenseDefinitions;
	}

	public Set<UserMetadata> getUserMetadata() {
		return userMetadata;
	}

	public void setUserMetadata(Set<UserMetadata> userMetadata) {
		this.userMetadata = userMetadata;
	}

	@Override
	public int getID() {
		return epersonId;
	}

}
