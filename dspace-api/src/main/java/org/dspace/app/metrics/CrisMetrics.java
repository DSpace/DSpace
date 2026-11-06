/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metrics;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ReloadableEntity;
import org.hibernate.Length;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Entity
@Table(name = "cris_metrics")
public class CrisMetrics  implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cris_metrics_seq")
    @SequenceGenerator(name = "cris_metrics_seq", sequenceName = "cris_metrics_seq", allocationSize = 1)
    private Integer id;

    private String metricType;

    private Double metricCount;

    private Date acquisitionDate;

    private Date startDate;

    private Date endDate;

    @ManyToOne
    @JoinColumn(name = "resource_id")
    protected DSpaceObject resource;

    private boolean last;

    @Column(name = "remark", length = Length.LONG32)
    private String remark;

    private Double deltaPeriod1;

    private Double deltaPeriod2;

    private Double rank;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public Double getMetricCount() {
        return metricCount;
    }

    public void setMetricCount(Double metricCount) {
        this.metricCount = metricCount;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public boolean getLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public Date getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(Date acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public DSpaceObject getResource() {
        return resource;
    }

    public void setResource(DSpaceObject dSpaceObject) {
        this.resource = dSpaceObject;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public Integer getID() {
        return this.getId();
    }

    public Double getDeltaPeriod1() {
        return deltaPeriod1;
    }

    public void setDeltaPeriod1(Double deltaPeriod1) {
        this.deltaPeriod1 = deltaPeriod1;
    }

    public Double getDeltaPeriod2() {
        return deltaPeriod2;
    }

    public void setDeltaPeriod2(Double deltaPeriod2) {
        this.deltaPeriod2 = deltaPeriod2;
    }

    public Double getRank() {
        return rank;
    }

    public void setRank(Double rank) {
        this.rank = rank;
    }

}