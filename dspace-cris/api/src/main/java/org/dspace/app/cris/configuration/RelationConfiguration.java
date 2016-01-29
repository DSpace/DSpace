/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.configuration;

import org.dspace.content.DSpaceObject;
import org.springframework.beans.factory.annotation.Required;

public class RelationConfiguration
{
    private String type;
    
    private String relationName;

    private String query;

    private Class<? extends DSpaceObject> relationClass;
    
    public Class<? extends DSpaceObject> getRelationClass()
    {
        return relationClass;
    }

    @Required
    public void setRelationClass(
            Class<? extends DSpaceObject> targetObjectClass)
    {
        this.relationClass = targetObjectClass;
    }
     
    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }
    
    public String getRelationName()
    {
        return relationName;
    }

    public void setRelationName(String name)
    {
        this.relationName = name; 
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
 
}
