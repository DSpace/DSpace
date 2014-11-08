package org.dspace.workflowbasic;

import org.dspace.eperson.EPerson;

import javax.persistence.*;

/**
 * Database entity representation of the TaskListItem table
 *
 * @author kevinvandevelde at atmire.com
 */
@Entity
@Table(name = "tasklistitem", schema = "public")
public class TaskListItem {

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
}
