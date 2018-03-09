/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic;

import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.EPerson;

import javax.persistence.*;

/**
 * Database entity representation of the TaskListItem table
 *
 * @author kevinvandevelde at atmire.com
 */
@Entity
@Table(name = "tasklistitem")
public class TaskListItem implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "tasklist_id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="tasklistitem_seq")
    @SequenceGenerator(name="tasklistitem_seq", sequenceName="tasklistitem_seq", allocationSize = 1)
    private int taskListItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eperson_id")
    private EPerson ePerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id")
    private BasicWorkflowItem workflowItem;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.workflowbasic.service.TaskListItemService#create(Context, BasicWorkflowItem, EPerson)}
     *
     */
    protected TaskListItem()
    {

    }

    public int getTaskListItemId() {
        return taskListItemId;
    }

    public EPerson getEPerson() {
        return ePerson;
    }

    public BasicWorkflowItem getWorkflowItem() {
        return workflowItem;
    }

    void setEPerson(EPerson ePerson) {
        this.ePerson = ePerson;
    }

    void setWorkflowItem(BasicWorkflowItem workflowItem) {
        this.workflowItem = workflowItem;
    }

    public Integer getID() {
        return taskListItemId;
    }
}
