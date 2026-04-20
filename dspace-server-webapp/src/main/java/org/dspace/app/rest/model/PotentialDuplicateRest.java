/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.UUID;

/**
 * REST Model defining a Potential Duplicate for serialisation to JSON
 * This is used in lists of potential duplicates for submission section data and item link / embeds.
 *
 * @author Kim Shepherd
 */
public class PotentialDuplicateRest extends RestAddressableModel {

    public static final String CATEGORY = RestModel.SUBMISSION;
    public static final String NAME = RestModel.DUPLICATES;

    /**
     * Type of REST resource
     */
    private static final String TYPE = "DUPLICATE";
    /**
     * Plural type of REST resource
     */
    private static final String TYPE_PLURAL = "DUPLICATES";
    /**
     * Title of duplicate object
     */
    private String title;
    /**
     * UUID of duplicate object
     */
    private UUID uuid;
    /**
     * Owning collection name (title) for duplicate item
     */
    private String owningCollectionName;
    /**
     * Workspace item ID, if the duplicate is a workspace item
     */
    private Integer workspaceItemId;
    /**
     * Workflow item ID, if the duplicate is a workflow item
     */
    private Integer workflowItemId;
    /**
     * List of configured metadata copied across from the duplicate item
     */
    private MetadataRest metadata;

    /**
     * Default constructor
     */
    public PotentialDuplicateRest() {
    }

    /**
     * Get UUID of duplicate item
     * @return UUID of duplicate item
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Set UUID of duplicate item
     * @param uuid UUID of duplicate item
     */
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Get title of duplicate item
     * @return title of duplicate item
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set title of duplicate item
     * @param title of duplicate item
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get owning collection name (title) of duplicate item
     * @return owning collection name (title) of duplicate item
     */
    public String getOwningCollectionName() {
        return owningCollectionName;
    }

    /**
     * Set owning collection name (title) of duplicate item
     * @param owningCollectionName owning collection name (title) of duplicate item
     */
    public void setOwningCollectionName(String owningCollectionName) {
        this.owningCollectionName = owningCollectionName;
    }

    /**
     * Get metadata (sorted, field->value list) for duplicate item
     * @return (sorted, field->value list) for duplicate item
     */
    public MetadataRest getMetadata() {
        return metadata;
    }

    /**
     * Set metadata (sorted, field->value list) for duplicate item
     * @param metadata MetadataRest list of values mapped to field keys
     */
    public void setMetadata(MetadataRest metadata) {
        this.metadata = metadata;
    }

    /**
     * Get workspace ID for duplicate item, if any
     * @return workspace item ID or null
     */
    public Integer getWorkspaceItemId() {
        return workspaceItemId;
    }

    /**
     * Set workspace ID for duplicate item
     * @param workspaceItemId workspace item ID
     */
    public void setWorkspaceItemId(Integer workspaceItemId) {
        this.workspaceItemId = workspaceItemId;
    }

    /**
     * Get workflow ID for duplicate item, if anh
     * @return workflow item ID or null
     */
    public Integer getWorkflowItemId() {
        return workflowItemId;
    }

    /**
     * Set workflow ID for duplicate item
     * @param workflowItemId workspace item ID
     */
    public void setWorkflowItemId(Integer workflowItemId) {
        this.workflowItemId = workflowItemId;
    }

    /**
     * Get REST resource type name
     * @return REST resource type (see static final string)
     */
    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Get REST resource type plural name
     * @return REST resource type plural name (see static final string)
     */
    @Override
    public String getTypePlural() {
        return TYPE_PLURAL;
    }

    /**
     * Get REST resource category.
     * Not implemented as this model is intended for use only as an ItemLink repository and submission section data,
     * it is actually a simple RestModel but has to 'implement' RestAddressableModel to serialize correctly
     *
     * @return null (not implemented)
     */
    @Override
    public String getCategory() {
        return null;
    }

    /**
     * Get REST controller for this model.
     * Not implemented as this model is intended for use only as an ItemLink repository and submission section data,
     * it is actually a simple RestModel but has to 'implement' RestAddressableModel to serialize correctly
     *
     * @return null (not implemented)
     */
    @Override
    public Class getController() {
        return null;
    }
}
