package cz.cuni.mff.ufal.lindat.utilities.hibernate;

public class LicenseLabelExtendedMapping extends GenericEntity implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int mappingId;
	private LicenseLabel licenseLabel;
	private LicenseDefinition licenseDefinition;

	public LicenseLabelExtendedMapping() {
	}

	public LicenseLabelExtendedMapping(int mappingId,
			LicenseLabel licenseLabel, LicenseDefinition licenseDefinition) {
		this.mappingId = mappingId;
		this.licenseLabel = licenseLabel;
		this.licenseDefinition = licenseDefinition;
	}

	public int getMappingId() {
		return this.mappingId;
	}

	public void setMappingId(int mappingId) {
		this.mappingId = mappingId;
	}

	public LicenseLabel getLicenseLabel() {
		return this.licenseLabel;
	}

	public void setLicenseLabel(LicenseLabel licenseLabel) {
		this.licenseLabel = licenseLabel;
	}

	public LicenseDefinition getLicenseDefinition() {
		return this.licenseDefinition;
	}

	public void setLicenseDefinition(LicenseDefinition licenseDefinition) {
		this.licenseDefinition = licenseDefinition;
	}

	@Override
	public int getID() {
		return mappingId;
	}

}

