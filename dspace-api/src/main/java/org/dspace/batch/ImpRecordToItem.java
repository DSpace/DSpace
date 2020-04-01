/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch;

import java.util.UUID;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CacheConcurrencyStrategy;

/***
 * this table is populated by the framework to track the result of creation
 * action so that subsequent operation over the same origin record will result
 * in update instead of duplication of entries
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 *
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
@Table(name = "imp_record_to_item")
public class ImpRecordToItem {

    @Id
    @Column(name = "imp_record_id", length = 256)
    private String impRecordId;

    @Column(name = "imp_item_id", nullable = false)
    private UUID impItemId;

    @Column(name = "imp_sourceref", length = 256)
    private String impSourceref;

    public String getImpRecordId() {
        return impRecordId;
    }

    public void setImpRecordId(String impRecordId) {
        this.impRecordId = impRecordId;
    }

    public UUID getImpItemId() {
        return impItemId;
    }

    public void setImpItemId(UUID impItemId) {
        this.impItemId = impItemId;
    }

    public String getImpSourceref() {
        return impSourceref;
    }

    public void setImpSourceref(String impSourceref) {
        this.impSourceref = impSourceref;
    }
}
