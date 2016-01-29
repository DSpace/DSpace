package org.dspace.solr.util;

import java.util.Date;

public class ExtraInfo
{
    public ExtraInfo(String remark, Date acquisitionTime)
    {
        super();
        this.remark = remark;
        this.acquisitionTime = acquisitionTime;
    }
    
    public String remark;
    public Date acquisitionTime;
}
