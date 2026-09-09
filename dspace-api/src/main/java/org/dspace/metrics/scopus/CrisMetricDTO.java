/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.scopus;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import jakarta.persistence.Lob;
import jakarta.persistence.Transient;

/**
 * 
 * @author mykhaylo boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CrisMetricDTO {

    private double metricCount;

    private String metricType;

    @Lob
    private String remark;

    @Transient
    private Map<String, String> tmpRemark = new HashMap<String, String>();

    public double getMetricCount() {
        return metricCount;
    }

    public void setMetricCount(double metricCount) {
        this.metricCount = metricCount;
    }

    @Transient
    public String buildMetricsRemark() {
        Gson gson = new Gson();
        return gson.toJson(this.tmpRemark);
    }

    @Transient
    public String getIdentifier() {
        if (this.remark != null) {
            Gson gson = new Gson();
            tmpRemark = gson.fromJson(this.remark, Map.class);
        }
        return tmpRemark.get("identifier");
    }

    public Map<String, String> getTmpRemark() {
        return tmpRemark;
    }

    public void setTmpRemark(Map<String, String> tmpRemark) {
        this.tmpRemark = tmpRemark;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }
}