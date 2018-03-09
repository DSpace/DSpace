/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license; 

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper class for representation of a license field declaration.
 * A license field is a single "question" which must be answered to
 * successfully generate a license.
 *
 */
public class CCLicenseField {

	private String id = "";
	private String label = "";
	private String description = "";
	private String type = "";

	private HashMap fieldEnum = null;

	/**
	 * Construct a new LicenseField class.  Note that after construction,
	 * at least the type should be set.
	 *
	 * @param id  The unique identifier for this field; this value will be used in constructing the answers XML.
	 * @param label The label to use when generating the user interface.
	 */
	public CCLicenseField(String id, String label) {
		super();

		this.fieldEnum = new HashMap();

		this.id = id;
		this.label = label;
	}

	/**
	 *
	 * @return Returns the identifier for this field.
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * @return Returns the description of the field.
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description The new description; this is often used as a tooltip when generating user interfaces.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return Returns the label.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label The label to set.
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * @return Returns the type.
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type The type to set.
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 *
	 * @return Returns an instance implementing the Map interface;
	 * 		the instance contains a mapping from identifiers to
	 * 		labels for the enumeration values.
	 *
	 * @see Map
	 */
	public Map<String, String> getEnum() {
		return this.fieldEnum;
	}


}


