/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit;

import java.util.List;

import org.dspace.content.edit.service.EditItemModeValidator;
import org.dspace.content.logic.Filter;
import org.dspace.content.security.AccessItemMode;
import org.dspace.content.security.CrisSecurity;

/**
 * Defines a configurable edit mode for modifying already-published items in the repository.
 * <p>
 * Edit modes provide role-based, customizable editing workflows for archived items. Each mode specifies:
 * <ul>
 *   <li><strong>Security constraints</strong>: Which users or groups can use this edit mode</li>
 *   <li><strong>Submission definition</strong>: Which submission process to use (determines which metadata
 *       fields are editable and how they appear in the UI)</li>
 *   <li><strong>Additional filters</strong>: Optional fine-grained access control logic</li>
 * </ul>
 * <p>
 * <strong>How it works with {@link EditItem}:</strong>
 * <ol>
 *   <li>An {@link EditItem} wraps an already-published {@link org.dspace.content.Item}</li>
 *   <li>An {@code EditItemMode} is applied to the EditItem to control <em>how</em> it can be edited</li>
 *   <li>Different modes can provide different editing experiences for the same item</li>
 * </ol>
 * <p>
 * <strong>Example configuration from edititem-service.xml (person entity):</strong>
 * <pre>{@code
 * <entry key="person">
 *     <list>
 *         <!-- Mode 1: Full administrative access -->
 *         <bean class="org.dspace.content.edit.EditItemMode">
 *             <property name="name" value="FULL" />
 *             <property name="security">
 *                 <value type="org.dspace.content.security.CrisSecurity">
 *                     ITEM_ADMIN
 *                 </value>
 *             </property>
 *             <property name="submissionDefinition" value="admin-person-edit" />
 *         </bean>
 *         <!-- Mode 2: Self-service editing for profile owners -->
 *         <bean class="org.dspace.content.edit.EditItemMode">
 *             <property name="name" value="OWNER" />
 *             <property name="security">
 *                 <value type="org.dspace.content.security.CrisSecurity">
 *                     OWNER
 *                 </value>
 *             </property>
 *             <property name="submissionDefinition" value="person-edit" />
 *         </bean>
 *     </list>
 * </entry>
 * }</pre>
 * <p>
 * Result: Profile owners can edit their own profile using the "OWNER" mode with restricted fields
 * defined in the "person-edit" submission definition, while admins can edit any profile using the
 * "FULL" mode with complete access to all fields defined in "admin-person-edit".
 * <p>
 * Edit modes are configured per entity type (e.g., "Publication", "Person") in the
 * {@code edititem-service.xml} configuration file and validated at startup to ensure
 * no duplicate mode names exist within the same configuration key.
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @see EditItem
 * @see EditItemModeValidator
 * @see org.dspace.content.security.AccessItemMode
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

    /**
     * Retrieves the list of security configurations that define which users are enabled to use this edit mode.
     *
     * @return the list of CRIS security configurations
     */
    @Override
    public List<CrisSecurity> getSecurities() {
        return securities;
    }

    /**
     * Sets a single security configuration for this edit mode. This method wraps the provided
     * security configuration in a list containing only that element.
     *
     * @param security the CRIS security configuration to set
     */
    public void setSecurity(CrisSecurity security) {
        this.securities = List.of(security);
    }

    /**
     * Sets the list of security configurations that define which users are enabled to use this edit mode.
     *
     * @param securities the list of CRIS security configurations to set
     */
    public void setSecurities(List<CrisSecurity> securities) {
        this.securities = securities;
    }

    /**
     * Retrieves the name of the submission definition used by this edit configuration.
     *
     * @return the submission definition name
     */
    public String getSubmissionDefinition() {
        return submissionDefinition;
    }
    /**
     * Sets the name of the submission definition to be used by this edit configuration.
     *
     * @param submissionDefinition the submission definition name to set
     */
    public void setSubmissionDefinition(String submissionDefinition) {
        this.submissionDefinition = submissionDefinition;
    }
    /**
     * Retrieves the configuration name of this edit mode.
     *
     * @return the configuration name
     */
    public String getName() {
        return name;
    }
    /**
     * Sets the configuration name for this edit mode.
     *
     * @param name the configuration name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * Retrieves the label used in the user interface for internationalization (i18n) purposes.
     *
     * @return the UI label
     */
    public String getLabel() {
        return label;
    }
    /**
     * Sets the label to be used in the user interface for internationalization (i18n) purposes.
     *
     * @param label the UI label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }
    /**
     * Retrieves the list of group metadata fields for CUSTOM security or group names/UUIDs for GROUP security.
     *
     * @return the list of group metadata fields or group identifiers
     */
    public List<String> getGroupMetadataFields() {
        return groups;
    }
    /**
     * Sets the list of group metadata fields for CUSTOM security or group names/UUIDs for GROUP security.
     *
     * @param groups the list of group metadata fields or group identifiers to set
     */
    public void setGroups(List<String> groups) {
        this.groups = groups;
    }
    /**
     * Retrieves the list of user metadata fields for CUSTOM security configuration.
     *
     * @return the list of user metadata fields
     */
    public List<String> getUserMetadataFields() {
        return users;
    }
    /**
     * Sets the list of user metadata fields for CUSTOM security configuration.
     *
     * @param users the list of user metadata fields to set
     */
    public void setUsers(List<String> users) {
        this.users = users;
    }
    /**
     * Retrieves the list of item metadata fields for CUSTOM security configuration.
     *
     * @return the list of item metadata fields
     */
    public List<String> getItemMetadataFields() {
        return items;
    }
    /**
     * Sets the list of item metadata fields for CUSTOM security configuration.
     *
     * @param items the list of item metadata fields to set
     */
    public void setItems(List<String> items) {
        this.items = items;
    }

    /**
     * Sets an additional filter to be applied when determining access permissions for this edit mode.
     *
     * @param additionalFilter the additional filter to set
     */
    public void setAdditionalFilter(Filter additionalFilter) {
        this.additionalFilter = additionalFilter;
    }

    /**
     * Retrieves the additional filter applied when determining access permissions for this edit mode.
     *
     * @return the additional filter, or null if none is configured
     */
    @Override
    public Filter getAdditionalFilter() {
        return additionalFilter;
    }

    /**
     * Retrieves the list of groups associated with this edit mode. This method returns the same
     * value as {@link #getGroupMetadataFields()}.
     *
     * @return the list of group metadata fields or group identifiers
     */
    @Override
    public List<String> getGroups() {
        return groups;
    }

    /**
     * Returns a string representation of this EditItemMode instance, including the name, label,
     * securities, and submission definition.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "EditItemMode [name=" + name + ", label=" + label + ", securities=" + securities
            + ", submissionDefinition=" + submissionDefinition + "]";
    }

}
