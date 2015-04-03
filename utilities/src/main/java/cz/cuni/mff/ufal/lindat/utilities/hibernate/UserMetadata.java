package cz.cuni.mff.ufal.lindat.utilities.hibernate;

public class UserMetadata extends GenericEntity implements java.io.Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int userMetadataId;
	private UserRegistration userRegistration;
	private LicenseResourceUserAllowance licenaseResourceUserAllowance;
	private String metadataKey;
	private String metadataValue;

	public UserMetadata() {
	}

	public UserMetadata(int userMetadataId, UserRegistration userRegistration,
			String metadataKey, String metadataValue) {
		this.userMetadataId = userMetadataId;
		this.userRegistration = userRegistration;
		this.metadataKey = metadataKey;
		this.metadataValue = metadataValue;
	}

	public UserMetadata(int userMetadataId, UserRegistration userRegistration,
			LicenseResourceUserAllowance licenaseResourceUserAllowance, String metadataKey,
			String metadataValue) {
		this.userMetadataId = userMetadataId;
		this.userRegistration = userRegistration;
		this.licenaseResourceUserAllowance = licenaseResourceUserAllowance;
		this.metadataKey = metadataKey;
		this.metadataValue = metadataValue;
	}

	public int getUserMetadataId() {
		return this.userMetadataId;
	}

	public void setUserMetadataId(int userMetadataId) {
		this.userMetadataId = userMetadataId;
	}

	public UserRegistration getUserRegistration() {
		return this.userRegistration;
	}

	public void setUserRegistration(UserRegistration userRegistration) {
		this.userRegistration = userRegistration;
	}

	public LicenseResourceUserAllowance getLicenseResourceUserAllowance() {
		return this.licenaseResourceUserAllowance;
	}

	public void setLicenseResourceUserAllowance(
			LicenseResourceUserAllowance licenaseResourceUserAllowance) {
		this.licenaseResourceUserAllowance = licenaseResourceUserAllowance;
	}

	public String getMetadataKey() {
		return this.metadataKey;
	}

	public void setMetadataKey(String metadataKey) {
		this.metadataKey = metadataKey;
	}

	public String getMetadataValue() {
		return this.metadataValue;
	}

	public void setMetadataValue(String metadataValue) {
		this.metadataValue = metadataValue;
	}

	@Override
	public int getID() {
		return userMetadataId;
	}
}
