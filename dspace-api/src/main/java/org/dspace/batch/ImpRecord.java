/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.CacheConcurrencyStrategy;

/***
 * contains information about the operations to perform. Each row represent a
 * specific operation on a single item
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
@Table(name = "imp_record")
public class ImpRecord {
    @Id
    @Column(name = "imp_id")
    private Integer impId;

    @Column(name = "imp_record_id", length = 256, nullable = false)
    private String impRecordId;

    @Column(name = "imp_eperson_uuid", nullable = false)
    private UUID impEpersonUuid;

    @Column(name = "imp_collection_uuid", nullable = false)
    private UUID impCollectionUuid;

    @Column(name = "status", length = 1)
    private String status;

    /***
     * The relation from ImpRecord and ImpWorkflowNState exists only is a workspace
     * operation is required.
     */
    @ManyToMany(mappedBy = "impRecords")
    private Set<ImpWorkflowNState> impWorkflowNStates = new HashSet<>();

    @Column(name = "operation", length = 64)
    private String operation;

    @Column(name = "last_modified")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;

    @Column(name = "handle", length = 64)
    private String handle;

    @Column(name = "imp_sourceref", length = 256)
    private String impSourceref;

    public Integer getImpId() {
        return impId;
    }

    public void setImpId(Integer impId) {
        this.impId = impId;
    }

    public String getImpRecordId() {
        return impRecordId;
    }

    public void setImpRecordId(String impRecordId) {
        this.impRecordId = impRecordId;
    }

    public UUID getImpEpersonUuid() {
        return impEpersonUuid;
    }

    void setImpEpersonUuid(UUID impEpersonUuid) {
        this.impEpersonUuid = impEpersonUuid;
    }

    public UUID getImpCollectionUuid() {
        return impCollectionUuid;
    }

    void setImpCollectionUuid(UUID impCollectionUuid) {
        this.impCollectionUuid = impCollectionUuid;
    }

    public String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    public String getOperation() {
        return operation;
    }

    void setOperation(String operation) {
        this.operation = operation;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getImpSourceref() {
        return impSourceref;
    }

    public void setImpSourceref(String impSourceref) {
        this.impSourceref = impSourceref;
    }

}
