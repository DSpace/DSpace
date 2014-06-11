/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;


import java.util.Map;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.content.AItemEnhancer;

/**
 * This class is used to add dynamically to the item information from linked
 * CRIS entities as it was metadata of the item
 * 
 * @author bollini
 * 
 */
public class CrisItemEnhancer extends AItemEnhancer
{

    // the Class of the linked CRIS entity
    private Class<? extends ACrisObject> clazz;
    // the type of the linked CRIS dynamic entity
    private String type;

    // the path in the CRIS entity property to map to a specific virtual "qualifier"
    // crisitem.author.dept = dept 
    private Map<String, String> qualifiers2path;

    public Class<? extends ACrisObject> getClazz()
    {
        return clazz;
    }

    public void setClazz(Class<? extends ACrisObject> clazz)
    {
        this.clazz = clazz;
    }

    public Map<String, String> getQualifiers2path()
    {
        return qualifiers2path;
    }


    public void setQualifiers2path(Map<String, String> qualifiers2path)
    {
        this.qualifiers2path = qualifiers2path;
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
