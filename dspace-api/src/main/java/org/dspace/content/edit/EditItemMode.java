/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit;

import java.util.List;

/**
 * This Class representing a modality of edit an item
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 */
public class EditItemMode {

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
    private EditItemModeSecurity security;
    /**
     * Defines the Submission Definition used from this edit configuration
     */
    private String submissionDefinition;
    /**
     * Contains the list of groups metadata for CUSTOM security
     */
    private List<String> groups;
    /**
     * Contains the list of users metadata for CUSTOM security
     */
    private List<String> users;

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
            switch (config[0]) {
                case "1":
                    this.setSecurity(EditItemModeSecurity.ADMIN);
                    break;
                case "2":
                    this.setSecurity(EditItemModeSecurity.OWNER);
                    break;
                case "3":
                    this.setSecurity(EditItemModeSecurity.ADMIN_OWNER);
                    break;
                case "4":
                    this.setSecurity(EditItemModeSecurity.CUSTOM);
                    break;
                default:
                    this.setSecurity(EditItemModeSecurity.NONE);
            }
            // set the submissionDefinition name
            this.setSubmissionDefinition(config[1]);
        }
    }

    public EditItemModeSecurity getSecurity() {
        return security;
    }
    public void setSecurity(EditItemModeSecurity security) {
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
    public List<String> getGroups() {
        return groups;
    }
    public void setGroups(List<String> groups) {
        this.groups = groups;
    }
    public List<String> getUsers() {
        return users;
    }
    public void setUsers(List<String> users) {
        this.users = users;
    }

    @Override
    public String toString() {
        return "EditItemMode [name=" + name + ", label=" + label + ", security=" + security + ", submissionDefinition="
                + submissionDefinition + "]";
    }

}
