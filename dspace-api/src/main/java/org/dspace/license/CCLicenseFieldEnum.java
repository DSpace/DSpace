/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;

import org.apache.commons.lang3.StringUtils;

/**
 * Wrapper class for representation of a license field enum declaration.
 * A field enum is a single "answer" to the field question
 */
public class CCLicenseFieldEnum {

    private String id = "";
    private String label = "";
    private String description = "";

    public CCLicenseFieldEnum(String id, String label, String description) {
        if (StringUtils.isNotBlank(id)) {
            this.id = id;
        }
        if (StringUtils.isNotBlank(label)) {
            this.label = label;
        }
        if (StringUtils.isNotBlank(description)) {
            this.description = description;
        }

    }

    /**
     * Get the id of this enum
     * @return the id of this enum
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id of this enum
     * @param id
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Get the label of this enum
     * @return the label of this enum
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the label of this enum
     * @param label
     */
    public void setLabel(final String label) {
        this.label = label;
    }

    /**
     * Get the description of this enum
     * @return the description of this enum
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of this enum
     * @param description
     */
    public void setDescription(final String description) {
        this.description = description;
    }
}
