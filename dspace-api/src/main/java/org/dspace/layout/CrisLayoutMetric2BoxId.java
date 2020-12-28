/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * This class defines the Id for the relation beetwen the objects CrisLayoutBox and a metric type
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
@Embeddable
public class CrisLayoutMetric2BoxId implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -2649635188820470846L;

    @Column(name = "cris_layout_box_id")
    private Integer crisLayoutBoxId;
    @Column(name = "metric_type")
    private String crisLayoutMetricId;

    public CrisLayoutMetric2BoxId() {}

    public CrisLayoutMetric2BoxId(Integer boxId, String metricId) {
        this.crisLayoutMetricId = metricId;
        this.crisLayoutBoxId = boxId;
    }

    public Integer getCrisLayoutBoxId() {
        return crisLayoutBoxId;
    }

    public void setCrisLayoutBoxId(Integer boxId) {
        this.crisLayoutBoxId = boxId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((crisLayoutBoxId == null) ? 0 : crisLayoutBoxId.hashCode());
        result = prime * result + ((getCrisLayoutMetricId() == null) ? 0 : getCrisLayoutMetricId().hashCode());
        return result;
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
        CrisLayoutMetric2BoxId other = (CrisLayoutMetric2BoxId) obj;
        if (crisLayoutBoxId == null) {
            if (other.crisLayoutBoxId != null) {
                return false;
            }
        } else if (!crisLayoutBoxId.equals(other.crisLayoutBoxId)) {
            return false;
        }
        if (getCrisLayoutMetricId() == null) {
            if (other.getCrisLayoutMetricId() != null) {
                return false;
            }
        } else if (!getCrisLayoutMetricId().equals(other.getCrisLayoutMetricId())) {
            return false;
        }
        return true;
    }

    public String getCrisLayoutMetricId() {
        return crisLayoutMetricId;
    }

    public void setCrisLayoutMetricId(String crisLayoutMetricId) {
        this.crisLayoutMetricId = crisLayoutMetricId;
    }


}
