/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;
import java.util.Date;

import org.dspace.app.rest.RestResourceController;

/**
 * The CrisMetrics REST Resource
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CrisMetricsRest extends BaseObjectRest<Integer> {

    private static final long serialVersionUID = 1L;
    public static final String NAME = "metric";
    public static final String CATEGORY = RestAddressableModel.CRIS;

    private String metricType;
    private Double metricCount;
    private Date acquisitionDate;
    private Date startDate;
    private Date endDate;
    private Boolean last;
    private String remark;

    private Integer deltaPeriod1;
    private Integer deltaPeriod2;
    private Integer rank;

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class<RestResourceController> getController() {
        return RestResourceController.class;
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

    public Date getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(Date acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
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

    public Boolean getLast() {
        return last;
    }

    public void setLast(Boolean last) {
        this.last = last;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getDeltaPeriod1() {
        return deltaPeriod1;
    }

    public void setDeltaPeriod1(Integer deltaPeriod1) {
        this.deltaPeriod1 = deltaPeriod1;
    }

    public Integer getDeltaPeriod2() {
        return deltaPeriod2;
    }

    public void setDeltaPeriod2(Integer deltaPeriod2) {
        this.deltaPeriod2 = deltaPeriod2;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

}