/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit;

import java.util.List;

import org.dspace.content.security.AccessItemMode;
import org.dspace.content.security.CrisSecurity;

/**
 * This Class representing a modality of edit an item
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 */
public class EditItemMode implements AccessItemMode {

    public static final String NONE = "none";
    /**
     * Configuration name
     */
    private String name;
    /**
     * Label used in UI for i18n
     */
    private String label;
    /**
     * Defines the users enabled to use this edit configuration
     */
    private CrisSecurity security;
    /**
     * Defines the Submission Definition used from this edit configuration
     */
    private String submissionDefinition;
    /**
     * Contains the list of groups metadata for CUSTOM security or groups name/uuid
     * for GROUP security
     */
    private List<String> groups;
    /**
     * Contains the list of users metadata for CUSTOM security
     */
    private List<String> users;
    /**
     * Contains the list of items metadata for CUSTOM security
     */
    private List<String> items;

    public EditItemMode() {}

    /**
     * builds an EditItemMode Object from configuration values.
     * The configuration values must be enclosed in an array with this order:
     * - [X, SUBMISSION_CONFIG]
     * where:
     * - X is the security configuration (1 = Admin, 2 = Owner, 3 = Admin+Owner, 4 = Custom)
     * - SUBMISSION_CONFIG is the submission configuration to use
     * @param name string that represent the name of edit configuration
     * @param config string array that contains the edit configuration parameters
     */
    public EditItemMode(String name, String[] config) {
        this.setName(name);
        if (config != null && config.length == 2) {
            // set the security mode
            this.setSecurity(CrisSecurity.getByValue(config[0]));
            // set the submissionDefinition name
            this.setSubmissionDefinition(config[1]);
        }
    }

    public CrisSecurity getSecurity() {
        return security;
    }

    public void setSecurity(CrisSecurity security) {
        this.security = security;
    }
    public String getSubmissionDefinition() {
        return submissionDefinition;
    }
    public void setSubmissionDefinition(String submissionDefinition) {
        this.submissionDefinition = submissionDefinition;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public List<String> getGroupMetadataFields() {
        return groups;
    }
    public void setGroups(List<String> groups) {
        this.groups = groups;
    }
    public List<String> getUserMetadataFields() {
        return users;
    }
    public void setUsers(List<String> users) {
        this.users = users;
    }
    public List<String> getItemMetadataFields() {
        return items;
    }
    public void setItems(List<String> items) {
        this.items = items;
    }

    @Override
    public List<String> getGroups() {
        return groups;
    }

    @Override
    public String toString() {
        return "EditItemMode [name=" + name + ", label=" + label + ", security=" + security + ", submissionDefinition="
                + submissionDefinition + "]";
    }

}
