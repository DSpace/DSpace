/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.dspace.eperson.EPerson;

/**
 * This class represent the stored information about processed notification
 * broker events
 *
 */
@Entity
@Table(name = "qaevent_processed")
public class QAEventProcessed implements Serializable {

    private static final long serialVersionUID = 3427340199132007814L;

    @Id
    @Column(name = "qaevent_id")
    private String eventId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "qaevent_timestamp")
    private Date eventTimestamp;

    @JoinColumn(name = "eperson_uuid")
    @ManyToOne
    private EPerson eperson;

    @JoinColumn(name = "item_uuid")
    @ManyToOne
    private Item item;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Date getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Date eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public EPerson getEperson() {
        return eperson;
    }

    public void setEperson(EPerson eperson) {
        this.eperson = eperson;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

}
