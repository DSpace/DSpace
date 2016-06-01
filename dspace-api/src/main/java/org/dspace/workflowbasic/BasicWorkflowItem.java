/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItem;

import javax.persistence.*;
import java.sql.SQLException;

/**
 * Class representing an item going through the workflow process in DSpace
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
@Entity
@Table(name = "workflowitem")
public class BasicWorkflowItem implements WorkflowItem, ReloadableEntity<Integer>
{

    @Id
    @Column(name = "workflow_id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="workflowitem_seq")
    @SequenceGenerator(name="workflowitem_seq", sequenceName="workflowitem_seq", allocationSize = 1)
    private Integer workflowitemId;


    /** The item this workflow object pertains to */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", unique = true)
    private Item item;

    /** The collection the item is being submitted to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private Collection collection;

    /** EPerson owning the current state */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner")
    private EPerson owner;

    @Column(name = "state")
    private int state;

    @Column(name = "multiple_titles")
    private boolean multipleTitles = false;

    @Column(name = "published_before")
    private boolean publishedBefore = false;

    @Column(name = "multiple_files")
    private boolean multipleFiles = false;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.workflowbasic.service.BasicWorkflowItemService#create(Context, Item, Collection)}
     *
     */
    protected BasicWorkflowItem()
    {

    }

    /**
     * Get the internal ID of this workflow item
     * 
     * @return the internal identifier
     */
    @Override
    public Integer getID()
    {
        return workflowitemId;
    }

    /**
     * get owner of WorkflowItem
     * 
     * @return EPerson owner
     */
    public EPerson getOwner()
    {
        return owner;
    }

    /**
     * set owner of WorkflowItem
     * 
     * @param ep
     *            owner
     */
    public void setOwner(EPerson ep)
    {
        this.owner = ep;
    }

    /**
     * Get state of WorkflowItem
     * 
     * @return state
     */
    public int getState()
    {
        return state;
    }

    /**
     * Set state of WorkflowItem
     * 
     * @param newstate
     *            new state (from <code>WorkflowManager</code>)
     */
    public void setState(int newstate)
    {
        this.state = newstate;
    }

    // InProgressSubmission methods
    @Override
    public Item getItem()
    {
        return item;
    }

    void setItem(Item item) {
        this.item = item;
    }

    @Override
    public Collection getCollection()
    {
        return collection;
    }

    void setCollection(Collection collection) {
        this.collection = collection;
    }

    @Override
    public EPerson getSubmitter() throws SQLException
    {
        return item.getSubmitter();
    }

    @Override
    public boolean hasMultipleFiles()
    {
        return multipleFiles;
    }

    @Override
    public void setMultipleFiles(boolean b)
    {
        this.multipleFiles = b;
    }

    @Override
    public boolean hasMultipleTitles()
    {
        return multipleTitles;
    }

    @Override
    public void setMultipleTitles(boolean b)
    {
        this.multipleTitles = b;
    }

    @Override
    public boolean isPublishedBefore()
    {
        return publishedBefore;
    }

    @Override
    public void setPublishedBefore(boolean b)
    {
        this.publishedBefore = b;
    }
}
