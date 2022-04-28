/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.EPerson;

/**
 * Claimed task representing the database representation of an action claimed by an eperson
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
@Entity
@Table(name = "cwf_in_progress_user")
public class InProgressUser implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "in_progress_user_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cwf_in_progress_user_seq")
    @SequenceGenerator(name = "cwf_in_progress_user_seq", sequenceName = "cwf_in_progress_user_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private EPerson ePerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflowitem_id")
    private XmlWorkflowItem workflowItem;

    @Column(name = "finished")
    private boolean finished = false;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.xmlworkflow.storedcomponents.service.InProgressUserService#create(Context)}
     */
    protected InProgressUser() {

    }

    @Override
    public Integer getID() {
        return id;
    }

    public void setUser(EPerson user) {
        this.ePerson = user;
    }

    public EPerson getUser() {
        return this.ePerson;
    }

    public void setWorkflowItem(XmlWorkflowItem workflowItem) {
        this.workflowItem = workflowItem;
    }

    public XmlWorkflowItem getWorkflowItem() {
        return this.workflowItem;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
