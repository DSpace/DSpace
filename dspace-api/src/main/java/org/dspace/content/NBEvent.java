/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "nbevent")
public class NBEvent {

	@Id
	@Column(name = "nbevent_id")
	private String eventId;
	
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "nbevent_timestamp")
    private Date eventTimestamp;
    
    @Column(name = "eperson_uuid")
    private String eperson;

    @Column(name = "item_uuid")
    private String item;

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

	public String getEperson() {
		return eperson;
	}

	public void setEperson(String eperson) {
		this.eperson = eperson;
	}

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}
}
