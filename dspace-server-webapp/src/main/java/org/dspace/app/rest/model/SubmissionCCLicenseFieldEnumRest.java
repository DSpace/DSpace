/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 * This class is the REST representation of the CCLicenseFieldEnum model object and acts as a data sub object
 * for the SubmissionCCLicenseFieldRest class.
 * Refer to {@link org.dspace.license.CCLicenseFieldEnum} for explanation of the properties
 */
public class SubmissionCCLicenseFieldEnumRest {

    private String id;
    private String label;
    private String description;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
