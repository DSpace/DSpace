package cz.cuni.mff.ufal.lindat.utilities.hibernate;


import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class LicenseLabel extends GenericEntity implements java.io.Serializable, Comparable<LicenseLabel> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int labelId;
	private String label;
	private String title;
	private Boolean isExtended;
	private Set<LicenseDefinition> licenseDefinitions = new HashSet<LicenseDefinition>(
			0);

	public LicenseLabel() {
	}

	public LicenseLabel(int labelId, String label) {
		this.labelId = labelId;
		this.label = label;
	}

	public LicenseLabel(int labelId, String label, String title,
			Boolean isExtended, Set<LicenseDefinition> licenseDefinitions) {
		this.labelId = labelId;
		this.label = label;
		this.title = title;
		this.isExtended = isExtended;
		this.licenseDefinitions = licenseDefinitions;
	}

	public int getLabelId() {
		return this.labelId;
	}

	public void setLabelId(int labelId) {
		this.labelId = labelId;
	}

	public String getLabel() {
		return this.label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Boolean getIsExtended() {
		return this.isExtended;
	}

	public void setIsExtended(Boolean isExtended) {
		this.isExtended = isExtended;
	}

	public Set<LicenseDefinition> getLicenseDefinitions() {
		return this.licenseDefinitions;
	}

	public void setLicenseDefinitions(Set<LicenseDefinition> licenseDefinitions) {
		this.licenseDefinitions = licenseDefinitions;
	}

	@Override
	public int getID() {
		return labelId;
	}
	
	/* hack for ordering the labels */
	
	static Hashtable<String, Integer> order = new Hashtable<String, Integer>();
	static {
		order.put("CC", 1);
		order.put("ZERO", 2);		
		order.put("BY", 2);
		order.put("NC", 3);
		order.put("SA", 4);
		order.put("ND", 5);
	}

	public int getOrder() {
		if(order.containsKey(this.label))
			return order.get(this.label);
		else
			return this.isExtended?0:-1;
	}

	@Override
	public int compareTo(LicenseLabel lbl) {
		if(this.getOrder()>lbl.getOrder())
			return 1;
		else
		if(this.getOrder()<lbl.getOrder())
			return -1;
		else
		return 0;
	}
		
}

