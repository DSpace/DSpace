/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.dto;

import org.apache.commons.lang.StringUtils;

public class ExportParametersDTO
{
    
    private String filter;
    
    private String query;
    
    public String getFilter()
    {
        return filter;
    }

    public void setFilter(String filter)
    {
        this.filter = filter;
    }

    public String getQuery()
    {
        if(StringUtils.isBlank(query)) {
            query = "*:*";
        }
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }
    
    
}
