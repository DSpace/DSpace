/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.annotations.CacheConcurrencyStrategy;

/***
 * Contains all the information related to all operations to perform in the next
 * workflow transition.
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
@Table(name = "imp_workflow_nstate")
public class ImpWorkflowNState {
    @Id
    @Column(name = "imp_wnstate_op_id")
    private Integer impWNStateOpId;

    @ManyToMany(cascade = { CascadeType.ALL })
    @JoinTable(name = "imp_record_wstate", joinColumns = {
        @JoinColumn(name = "imp_wnstate_op_id") }, inverseJoinColumns = { @JoinColumn(name = "imp_id") })
    private Set<ImpRecord> impRecords = new HashSet<>();

    @Column(name = "imp_wnstate_desc", length = 64)
    private String impWNStateDesc;

    @Column(name = "imp_wnstate_op", length = 64, nullable = false)
    private String impWNStateOp;

    @Column(name = "imp_wnstate_op_par", length = 64)
    private String impWNStateOpPar;

    @Column(name = "imp_wnstate_order", nullable = false)
    private Integer impWNStateOrder;

    @Column(name = "imp_wnstate_eperson_uuid")
    private UUID impWNStateEpersonUuid;

    public Integer getImpWNStateOpId() {
        return impWNStateOpId;
    }

    public void setImpWNStateOpId(Integer impWNStateOpId) {
        this.impWNStateOpId = impWNStateOpId;
    }

    public Set<ImpRecord> getImpRecords() {
        return impRecords;
    }

    public void setImpRecords(Set<ImpRecord> impRecords) {
        this.impRecords = impRecords;
    }

    public String getImpWNStateDesc() {
        return impWNStateDesc;
    }

    public void setImpWNStateDesc(String impWNStateDesc) {
        this.impWNStateDesc = impWNStateDesc;
    }

    public String getImpWNStateOp() {
        return impWNStateOp;
    }

    public void setImpWNStateOp(String impWNStateOp) {
        this.impWNStateOp = impWNStateOp;
    }

    public Integer getImpWNStateOrder() {
        return impWNStateOrder;
    }

    public void setImpWNStateOrder(Integer impWNStateOrder) {
        this.impWNStateOrder = impWNStateOrder;
    }

    public UUID getImpWNStateEpersonUuid() {
        return impWNStateEpersonUuid;
    }

    public void setImpWNStateEpersonUuid(UUID impWNStateEpersonUuid) {
        this.impWNStateEpersonUuid = impWNStateEpersonUuid;
    }
}
