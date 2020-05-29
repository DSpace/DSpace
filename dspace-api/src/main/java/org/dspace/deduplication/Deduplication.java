/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.deduplication;

import java.util.Date;
import java.util.UUID;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Class representing a Deduplication data.
 *
 * @author fcadili (francesco.cadili at 4science.it)
 * @version $Revision$
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
@Table(name = "deduplication")
public class Deduplication {
    @Id
    @Column(name = "deduplication_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "deduplication_id_seq")
    @SequenceGenerator(name = "deduplication_id_seq", sequenceName = "deduplication_id_seq", allocationSize = 1)
    private Integer deduplicationId;

    @Column(name = "fake")
    private Boolean fake;

    @Column(name = "tofix")
    private Boolean tofix;

    @Column(name = "note", length = 256)
    private String note;

    @Column(name = "admin_time")
    @Temporal(TemporalType.TIMESTAMP)
    Date adminTime;

    @Column(name = "reader_time")
    @Temporal(TemporalType.TIMESTAMP)
    Date readerTime;

    @Column(name = "reader_note", length = 256)
    private String readerNote;

    @Column(name = "reject_time")
    @Temporal(TemporalType.TIMESTAMP)
    Date rejectTime;

    @Column(name = "submitter_decision", length = 256)
    private String submitterDecision;

    @Column(name = "workflow_decision", length = 256)
    private String workflowDecision;

    @Column(name = "admin_decision", length = 256)
    private String adminDecision;

    @Column(name = "eperson_id")
    UUID epersonId;

    @Column(name = "admin_id")
    UUID adminId;

    @Column(name = "reader_id")
    UUID readerId;

    @Column(name = "first_item_id")
    private UUID firstItemId;

    @Column(name = "second_item_id")
    private UUID secondItemId;

    public Integer getDeduplicationId() {
        return deduplicationId;
    }

    public void setDeduplicationId(Integer deduplicationId) {
        this.deduplicationId = deduplicationId;
    }

    public UUID getFirstItemId() {
        return firstItemId;
    }

    public void setFirstItemId(UUID firstItemId) {
        this.firstItemId = firstItemId;
    }

    public UUID getSecondItemId() {
        return secondItemId;
    }

    public void setSecondItemId(UUID secondItemId) {
        this.secondItemId = secondItemId;
    }

    public boolean isTofix() {
        return tofix;
    }

    public void setTofix(boolean tofix) {
        this.tofix = tofix;
    }

    public boolean isFake() {
        return fake;
    }

    public void setFake(boolean fake) {
        this.fake = fake;
    }

    public String getReaderNote() {
        return readerNote;
    }

    public void setReaderNote(String readerNote) {
        this.readerNote = readerNote;
    }

    public UUID getReaderId() {
        return readerId;
    }

    public void setReaderId(UUID readerId) {
        this.readerId = readerId;
    }

    public Date getReaderTime() {
        return readerTime;
    }

    public void setReaderTime(Date readerTime) {
        this.readerTime = readerTime;
    }

    public String getWorkflowDecision() {
        return workflowDecision;
    }

    public void setWorkflowDecision(String workflowDecision) {
        this.workflowDecision = workflowDecision;
    }

    public String getSubmitterDecision() {
        return submitterDecision;
    }

    public void setSubmitterDecision(String submitterDecision) {
        this.submitterDecision = submitterDecision;
    }

    public String getAdminDecision() {
        return adminDecision;
    }

    public void setAdminDecision(String adminDecision) {
        this.adminDecision = adminDecision;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public UUID getAdminId() {
        return adminId;
    }

    public void setAdminId(UUID adminId) {
        this.adminId = adminId;
    }

    public Date getAdminTime() {
        return adminTime;
    }

    public void setAdminTime(Date adminTime) {
        this.adminTime = adminTime;
    }

    public UUID getEpersonId() {
        return epersonId;
    }

    public void setEpersonId(UUID eperson_id) {
        this.epersonId = eperson_id;
    }

    public Date getRejectTime() {
        return rejectTime;
    }

    public void setRejectTime(Date rejectTime) {
        this.rejectTime = rejectTime;
    }
}
