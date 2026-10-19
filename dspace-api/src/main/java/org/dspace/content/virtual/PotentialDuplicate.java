/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;

/**
 * Model of potential duplicate item. Provides as little data as possible, but enough to be useful
 * about the context / state of the duplicate, and metadata for preview purposes.
 * This class lives in the virtual package because it is not stored, addressable data, it's a stub / preview
 * based on an items' search result and metadata.
 *
 * @author Kim Shepherd
 */
public class PotentialDuplicate {
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
     * List of configured metadata values copied across from the duplicate item
     */
    private List<MetadataValue> metadataValueList;

    /**
     * Default constructor
     */
    public PotentialDuplicate() {
        this.metadataValueList = new LinkedList<>();
    }

    /**
     * Constructor that accepts an item and sets some values accordingly
     * @param item the potential duplicate item
     */
    public PotentialDuplicate(Item item) {
        // Throw error if item is null
        if (item == null) {
            throw new NullPointerException("Null item passed to potential duplicate constructor");
        }
        // Instantiate metadata value list
        this.metadataValueList = new LinkedList<>();
        // Set title
        this.title = item.getName();
        // Set UUID
        this.uuid = item.getID();
        // Set owning collection name
        if (item.getOwningCollection() != null) {
            this.owningCollectionName = item.getOwningCollection().getName();
        }
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
     * Get metadata (sorted, field->value list) for duplicate item
     * @return (sorted, field->value list) for duplicate item
     */
    public List<MetadataValue> getMetadataValueList() {
        return metadataValueList;
    }

    /**
     * Set metadata (sorted, field->value list) for duplicate item
     * @param metadataValueList MetadataRest list of values mapped to field keys
     */
    public void setMetadataValueList(List<MetadataValue> metadataValueList) {
        this.metadataValueList = metadataValueList;
    }

}
