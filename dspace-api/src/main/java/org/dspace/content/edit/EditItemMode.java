/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit;

import java.util.List;

import org.dspace.content.logic.Filter;
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
    private List<CrisSecurity> securities;
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
    private Filter additionalFilter;

    @Override
    public List<CrisSecurity> getSecurities() {
        return securities;
    }

    public void setSecurity(CrisSecurity security) {
        this.securities = List.of(security);
    }

    public void setSecurities(List<CrisSecurity> securities) {
        this.securities = securities;
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

    public void setAdditionalFilter(Filter additionalFilter) {
        this.additionalFilter = additionalFilter;
    }

    @Override
    public Filter getAdditionalFilter() {
        return additionalFilter;
    }

    @Override
    public List<String> getGroups() {
        return groups;
    }

    @Override
    public String toString() {
        return "EditItemMode [name=" + name + ", label=" + label + ", securities=" + securities
            + ", submissionDefinition=" + submissionDefinition + "]";
    }

}
