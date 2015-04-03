package cz.cuni.mff.ufal.lindat.utilities.hibernate;


import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class LicenseDefinition extends GenericEntity implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int licenseId;
	private UserRegistration userRegistration;
	private LicenseLabel licenseLabel;
	private String name;
	private String definition;
	private Date createdOn;
	private int confirmation;
    private String requiredInfo;
	private Set<LicenseResourceMapping> licenseResourceMappings = new HashSet<LicenseResourceMapping>(
			0);
	private Set<LicenseLabel> licenseLabelExtendedMappings = new HashSet<LicenseLabel>(
			0);


	public LicenseDefinition() {
	}

	public LicenseDefinition(int licenseId, UserRegistration userRegistration,
			LicenseLabel licenseLabel, String name, String definition,
			Date createdOn, int confirmation, String requiredInfo) {
		this.licenseId = licenseId;
		this.userRegistration = userRegistration;
		this.licenseLabel = licenseLabel;
		this.name = name;
		this.definition = definition;
		this.createdOn = createdOn;
		this.confirmation = confirmation;
		this.requiredInfo = requiredInfo;
	}

	public LicenseDefinition(int licenseId, UserRegistration userRegistration,
			LicenseLabel licenseLabel, String name, String definition,
			Date createdOn, int confirmation, String requiredInfo,
			Set<LicenseResourceMapping> licenseResourceMappings,
			Set<LicenseLabel> licenseLabelExtendedMappings) {
		this.licenseId = licenseId;
		this.userRegistration = userRegistration;
		this.licenseLabel = licenseLabel;
		this.name = name;
		this.definition = definition;
		this.createdOn = createdOn;
		this.confirmation = confirmation;
        this.requiredInfo = requiredInfo;
		this.licenseResourceMappings = licenseResourceMappings;
		this.licenseLabelExtendedMappings = licenseLabelExtendedMappings;
	}

	public int getLicenseId() {
		return this.licenseId;
	}

	public void setLicenseId(int licenseId) {
		this.licenseId = licenseId;
	}

	public UserRegistration getUserRegistration() {
		return this.userRegistration;
	}

	public void setUserRegistration(UserRegistration userRegistration) {
		this.userRegistration = userRegistration;
	}

	public LicenseLabel getLicenseLabel() {
		return this.licenseLabel;
	}

	public void setLicenseLabel(LicenseLabel licenseLabel) {
		this.licenseLabel = licenseLabel;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDefinition() {
		return this.definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	public Date getCreatedOn() {
		return this.createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

    public int getConfirmation() {
        return this.confirmation;
    }

    public void setConfirmation(int confirmation) {
        this.confirmation = confirmation;
    }

    public String getRequiredInfo() {
        return this.requiredInfo;
    }

    public void setRequiredInfo(String requiredInfo) {
        this.requiredInfo = requiredInfo;
    }

	public Set<LicenseResourceMapping> getLicenseResourceMappings() {
		return this.licenseResourceMappings;
	}

	public void setLicenseResourceMappings(
			Set<LicenseResourceMapping> licenseResourceMappings) {
		this.licenseResourceMappings = licenseResourceMappings;
	}

	public Set<LicenseLabel> getLicenseLabelExtendedMappings() {
		return this.licenseLabelExtendedMappings;
	}
	
	public Set<LicenseLabel> getSortedLicenseLabelExtendedMappings() {
		// to make the sorting correct based on LicenseLabel order
		return new TreeSet<LicenseLabel>(this.licenseLabelExtendedMappings);
	}

	public void setLicenseLabelExtendedMappings(
			Set<LicenseLabel> licenseLabelExtendedMappings) {
		this.licenseLabelExtendedMappings = licenseLabelExtendedMappings;
	}

	@Override
	public int getID() {
		return licenseId;
	}
			
}
