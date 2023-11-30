/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItem;
import org.hibernate.proxy.HibernateProxyHelper;

/**
 * Class representing an item in the process of being submitted by a user
 *
 * @author Robert Tansley
 * @version $Revision$
 */
@Entity
@Table(name = "workspaceitem")
public class WorkspaceItem
    implements InProgressSubmission, Serializable {

    @Id
    @Column(name = "workspace_item_id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workspaceitem_seq")
    @SequenceGenerator(name = "workspaceitem_seq", sequenceName = "workspaceitem_seq", allocationSize = 1)
    private Integer workspaceItemId;

    /**
     * The item this workspace object pertains to
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;


    /**
     * The collection the item is being submitted to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private Collection collection;

    @Column(name = "multiple_titles")
    private boolean multipleTitles = false;

    @Column(name = "published_before")
    private boolean publishedBefore = false;

    @Column(name = "multiple_files")
    private boolean multipleFiles = false;

    @Column(name = "stage_reached")
    private Integer stageReached = -1;

    @Column(name = "page_reached")
    private Integer pageReached = -1;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.WorkspaceItemService#create(Context, Collection, boolean)}
     * or
     * {@link org.dspace.content.service.WorkspaceItemService#create(Context, WorkflowItem)}
     */
    protected WorkspaceItem() {

    }

    /**
     * Get the internal ID of this workspace item
     *
     * @return the internal identifier
     */
    @Override
    public Integer getID() {
        return workspaceItemId;
    }

    /**
     * Get the value of the stage reached column
     *
     * @return the value of the stage reached column
     */
    public int getStageReached() {
        return stageReached;
    }

    /**
     * Set the value of the stage reached column
     *
     * @param v the value of the stage reached column
     */
    public void setStageReached(int v) {
        stageReached = v;
    }

    /**
     * Get the value of the page reached column (which represents the page
     * reached within a stage/step)
     *
     * @return the value of the page reached column
     */
    public int getPageReached() {
        return pageReached;
    }

    /**
     * Set the value of the page reached column (which represents the page
     * reached within a stage/step)
     *
     * @param v the value of the page reached column
     */
    public void setPageReached(int v) {
        pageReached = v;
    }

    /**
     * Decide if this WorkspaceItem is equal to another
     *
     * @param o The other workspace item to compare to
     * @return If they are equal or not
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(o);
        if (!getClass().equals(objClass)) {
            return false;
        }
        final WorkspaceItem that = (WorkspaceItem) o;
        if (!this.getID().equals(that.getID())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getID()).toHashCode();
    }

    // InProgressSubmission methods
    @Override
    public Item getItem() {
        return item;
    }

    void setItem(Item item) {
        this.item = item;
    }

    @Override
    public Collection getCollection() {
        return collection;
    }

    void setCollection(Collection collection) {
        this.collection = collection;
    }

    @Override
    public EPerson getSubmitter() {
        return item.getSubmitter();
    }

    @Override
    public boolean hasMultipleFiles() {
        return multipleFiles;
    }

    @Override
    public void setMultipleFiles(boolean b) {
        multipleFiles = b;
    }

    @Override
    public boolean hasMultipleTitles() {
        return multipleTitles;
    }

    @Override
    public void setMultipleTitles(boolean b) {
        multipleTitles = b;
    }

    @Override
    public boolean isPublishedBefore() {
        return publishedBefore;
    }

    @Override
    public void setPublishedBefore(boolean b) {
        publishedBefore = b;
    }

}
