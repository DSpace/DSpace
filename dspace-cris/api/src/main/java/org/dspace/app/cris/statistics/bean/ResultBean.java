/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.bean;

import java.util.Map;

public class ResultBean
{
    private Object dataBeans;

    private Map parameters;

    public ResultBean()
    {
    }

    public ResultBean(Object dataBeans, Map parameters)
    {
        this.dataBeans = dataBeans;
        this.parameters = parameters;
    }

    public Object getDataBeans()
    {
        return dataBeans;
    }

    public void setDataBean(Object dataBeans)
    {
        this.dataBeans = dataBeans;
    }

    public Map getParameters()
    {
        return parameters;
    }

    public void setParameters(Map parameters)
    {
        this.parameters = parameters;
    }

}
