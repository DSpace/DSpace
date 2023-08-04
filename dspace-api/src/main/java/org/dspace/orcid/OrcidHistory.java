/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.dspace.content.Item;
import org.dspace.core.ReloadableEntity;
import org.hibernate.annotations.Type;

/**
 * The ORCID history entity that it contains information relating to an attempt
 * to synchronize the DSpace items and information on ORCID. While the entity
 * {@link OrcidQueue} contains the data to be synchronized with ORCID, this
 * entity instead contains the data synchronized with ORCID, with the result of
 * the synchronization. Each record in this table is associated with a profile
 * item and the entity synchronized (which can be the profile itself, a
 * publication or a project/funding). If the entity is the profile itself then
 * the metadata field contains the signature of the information synchronized.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Entity
@Table(name = "orcid_history")
public class OrcidHistory implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orcid_history_id_seq")
    @SequenceGenerator(name = "orcid_history_id_seq", sequenceName = "orcid_history_id_seq", allocationSize = 1)
    private Integer id;

    /**
     * The profile item.
     */
    @ManyToOne
    @JoinColumn(name = "owner_id")
    protected Item profileItem;

    /**
     * The synchronized item.
     */
    @ManyToOne
    @JoinColumn(name = "entity_id")
    private Item entity;

    /**
     * The identifier of the synchronized resource on ORCID side. For more details
     * see https://info.orcid.org/faq/what-is-a-put-code/
     */
    @Column(name = "put_code")
    private String putCode;

    /**
     * The record type. Could be publication, funding or a profile's section.
     */
    @Column(name = "record_type")
    private String recordType;

    /**
     * A description of the synchronized resource.
     */
    @Column(name = "description")
    private String description;

    /**
     * The signature of the synchronized metadata. This is used when the entity is
     * the owner itself.
     */
    @Lob
    @Type(type = "org.dspace.storage.rdbms.hibernate.DatabaseAwareLobType")
    @Column(name = "metadata")
    private String metadata;

    /**
     * The operation performed on ORCID.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation")
    private OrcidOperation operation;

    /**
     * The response message incoming from ORCID.
     */
    @Lob
    @Type(type = "org.dspace.storage.rdbms.hibernate.DatabaseAwareLobType")
    @Column(name = "response_message")
    private String responseMessage;

    /**
     * The timestamp of the synchronization attempt.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "timestamp_last_attempt")
    private Date timestamp = new Date();

    /**
     * The HTTP status incoming from ORCID.
     */
    @Column(name = "status")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getID() {
        return id;
    }

    public Item getProfileItem() {
        return profileItem;
    }

    public void setProfileItem(Item profileItem) {
        this.profileItem = profileItem;
    }

    public Item getEntity() {
        return entity;
    }

    public void setEntity(Item entity) {
        this.entity = entity;
    }

    public String getPutCode() {
        return putCode;
    }

    public void setPutCode(String putCode) {
        this.putCode = putCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public OrcidOperation getOperation() {
        return operation;
    }

    public void setOperation(OrcidOperation operation) {
        this.operation = operation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

}
