/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.solr.util;

import java.util.Date;

public class ExtraInfo
{
    public ExtraInfo(String remark, Date acquisitionTime, Date startTime, Date endTime)
    {
        super();
        this.remark = remark;
        this.acquisitionTime = acquisitionTime;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    public String remark;
    public Date acquisitionTime;
    public Date startTime;
    public Date endTime;
}
