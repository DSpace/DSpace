/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.metrics.common.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.google.gson.Gson;

import it.cilea.osd.common.core.HasTimeStampInfo;
import it.cilea.osd.common.core.TimeStampInfo;
import it.cilea.osd.common.model.Identifiable;

@Entity
@Table(name = "cris_metrics")
@NamedQueries({
        @NamedQuery(name = "CrisMetrics.findAll", query = "from CrisMetrics order by id"),
        @NamedQuery(name = "CrisMetrics.count", query = "select count(*) from CrisMetrics"),
        @NamedQuery(name = "CrisMetrics.uniqueLastMetricByResourceIdAndResourceTypeIdAndMetricsType", query = "select cit from CrisMetrics cit where resourceId = :par0 and resourceTypeId = :par1 and metrictype = :par2 and timeStampInfo.timestampCreated.timestamp in (select max(timeStampInfo.timestampCreated.timestamp) from CrisMetrics cit where resourceId = :par0 and resourceTypeId = :par1 and metrictype = :par2 and last = true)"),
        @NamedQuery(name = "CrisMetrics.findLastMetricByResourceIdAndResourceTypeIdAndMetricsTypes", query = "select cit from CrisMetrics cit where resourceId = :par0 and resourceTypeId = :par1 and metrictype in (:par2) and last = true")
})
public class CrisMetrics implements Identifiable, HasTimeStampInfo
{

    @Id
    @GeneratedValue(generator = "CRIS_METRICS_SEQ")
    @SequenceGenerator(name = "CRIS_METRICS_SEQ", sequenceName = "CRIS_METRICS_SEQ", allocationSize = 1)
    private Integer id;

    /** timestamp info for creation and last modify */
    @Embedded
    private TimeStampInfo timeStampInfo;

    private Date startDate;

    private Date endDate;

    private double metricCount;

    @Type(type = "org.hibernate.type.StringClobType")
    private String remark;

    @Transient
    private Map<String, String> tmpRemark = new HashMap<String, String>();

    private String metricType;

    private String uuid;

    private Integer resourceId;

    private Integer resourceTypeId;
    
    private boolean last;

    public TimeStampInfo getTimeStampInfo()
    {
        if (timeStampInfo == null)
        {
            timeStampInfo = new TimeStampInfo();
        }
        return timeStampInfo;
    }

    public void setTimeStampInfo(TimeStampInfo timeStampInfo)
    {
        this.timeStampInfo = timeStampInfo;
    }

    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public Integer getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(Integer objectId)
    {
        this.resourceId = objectId;
    }

    public Integer getResourceTypeId()
    {
        return resourceTypeId;
    }

    public void setResourceTypeId(Integer typeId)
    {
        this.resourceTypeId = typeId;
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    @Transient
    public String buildMetricsRemark()
    {
        Gson gson = new Gson();
        return gson.toJson(this.tmpRemark);
    }

    @Transient
    public String getIdentifier()
    {
        if (this.remark != null)
        {
            Gson gson = new Gson();
            tmpRemark = gson.fromJson(this.remark, Map.class);
        }
        return tmpRemark.get("identifier");
    }

    public String getMetricType()
    {
        return metricType;
    }

    public void setMetricType(String metricType)
    {
        this.metricType = metricType;
    }

    public double getMetricCount()
    {
        return metricCount;
    }

    public void setMetricCount(double metriccount)
    {
        this.metricCount = metriccount;
    }

    public Map<String, String> getTmpRemark()
    {
        return tmpRemark;
    }

    public void setTmpRemark(Map<String, String> tmpRemark)
    {
        this.tmpRemark = tmpRemark;
    }

    public boolean isLast() {
		return last;
	}
    
    public void setLast(boolean last) {
		this.last = last;
	}
}
