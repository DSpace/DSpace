/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Objects;
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

import org.dspace.content.Item;
import org.dspace.core.ReloadableEntity;
import org.hibernate.annotations.Type;

/**
 * Entity that model a record on the ORCID synchronization queue. Each record in
 * this table is associated with an profile item and the entity to be
 * synchronized (which can be the profile itself, a publication or a
 * project/funding). If the entity is the profile itself then the metadata field
 * contains the signature of the information to be synchronized.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Entity
@Table(name = "orcid_queue")
public class OrcidQueue implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orcid_queue_id_seq")
    @SequenceGenerator(name = "orcid_queue_id_seq", sequenceName = "orcid_queue_id_seq", allocationSize = 1)
    private Integer id;

    /**
     * The profile item.
     */
    @ManyToOne
    @JoinColumn(name = "owner_id")
    protected Item profileItem;

    /**
     * The entity to be synchronized.
     */
    @ManyToOne
    @JoinColumn(name = "entity_id")
    private Item entity;

    /**
     * A description of the resource to be synchronized.
     */
    @Column(name = "description")
    private String description;

    /**
     * The identifier of the resource to be synchronized on ORCID side (in case of
     * update or deletion). For more details see
     * https://info.orcid.org/faq/what-is-a-put-code/
     */
    @Column(name = "put_code")
    private String putCode;

    /**
     * The record type. Could be publication, funding or a profile's section.
     */
    @Column(name = "record_type")
    private String recordType;

    /**
     * The signature of the metadata to be synchronized. This is used when the
     * entity is the owner itself.
     */
    @Lob
    @Column(name = "metadata")
    @Type(type = "org.dspace.storage.rdbms.hibernate.DatabaseAwareLobType")
    private String metadata;

    /**
     * The operation to be performed on ORCID.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation")
    private OrcidOperation operation;

    /**
     * Synchronization attempts already made for a particular record.
     */
    @Column(name = "attempts")
    private Integer attempts = 0;

    public boolean isInsertAction() {
        return entity != null && isEmpty(putCode);
    }

    public boolean isUpdateAction() {
        return entity != null && isNotEmpty(putCode);
    }

    public boolean isDeleteAction() {
        return entity == null && isNotEmpty(putCode);
    }

    public void setID(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getID() {
        return this.id;
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

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        OrcidQueue other = (OrcidQueue) obj;
        return Objects.equals(id, other.id);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    @Override
    public String toString() {
        return "OrcidQueue [id=" + id + ", profileItem=" + profileItem + ", entity=" + entity + ", description="
            + description
            + ", putCode=" + putCode + ", recordType=" + recordType + ", metadata=" + metadata + ", operation="
            + operation + "]";
    }

}
