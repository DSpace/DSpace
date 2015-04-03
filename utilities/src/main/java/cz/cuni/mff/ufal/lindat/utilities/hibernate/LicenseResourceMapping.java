package cz.cuni.mff.ufal.lindat.utilities.hibernate;

import java.util.HashSet;
import java.util.Set;

public class LicenseResourceMapping extends GenericEntity implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int mappingId;
	private LicenseDefinition licenseDefinition;
	private int bitstreamId;
	private Set<LicenseResourceUserAllowance> licenseResourceUserAllowances = new HashSet<LicenseResourceUserAllowance>(0);
	private boolean active = true;

	public LicenseResourceMapping() {
	}

	public LicenseResourceMapping(int mappingId,
			LicenseDefinition licenseDefinition, int bitstreamId) {
		this.mappingId = mappingId;
		this.licenseDefinition = licenseDefinition;
		this.bitstreamId = bitstreamId;
	}

	public LicenseResourceMapping(int mappingId,
			LicenseDefinition licenseDefinition, int bitstreamId,
			Set<LicenseResourceUserAllowance> licenseResourceUserAllowances) {
		this.mappingId = mappingId;
		this.licenseDefinition = licenseDefinition;
		this.bitstreamId = bitstreamId;
		this.licenseResourceUserAllowances = licenseResourceUserAllowances;
	}

	public int getMappingId() {
		return this.mappingId;
	}

	public void setMappingId(int mappingId) {
		this.mappingId = mappingId;
	}

	public LicenseDefinition getLicenseDefinition() {
		return this.licenseDefinition;
	}

	public void setLicenseDefinition(LicenseDefinition licenseDefinition) {
		this.licenseDefinition = licenseDefinition;
	}

	public int getBitstreamId() {
		return this.bitstreamId;
	}

	public void setBitstreamId(int bitstreamId) {
		this.bitstreamId = bitstreamId;
	}

	public Set<LicenseResourceUserAllowance> getLicenseResourceUserAllowances() {
		return this.licenseResourceUserAllowances;
	}

	public void setLicenseResourceUserAllowances(
			Set<LicenseResourceUserAllowance> licenseResourceUserAllowances) {
		this.licenseResourceUserAllowances = licenseResourceUserAllowances;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	@Override
	public int getID() {
		return mappingId;
	}

}
