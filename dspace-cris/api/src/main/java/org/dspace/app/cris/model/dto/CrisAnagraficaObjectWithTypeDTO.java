/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.dto;

import it.cilea.osd.jdyna.dto.AnagraficaObjectWithTypeDTO;

import org.dspace.app.cris.model.ACrisObject;

public class CrisAnagraficaObjectWithTypeDTO extends AnagraficaObjectWithTypeDTO 
{

    private Boolean status;
    private String sourceID;
    
    public CrisAnagraficaObjectWithTypeDTO(ACrisObject object)
    {
        super();
        setSourceID(object.getSourceID());
        setStatus(object.getStatus());
    }

    public Boolean getStatus()
    {
        return status;
    }

    public void setStatus(Boolean status)
    {
        this.status = status;
    }

    public String getSourceID()
    {
        return sourceID;
    }

    public void setSourceID(String souceID)
    {
        this.sourceID = souceID;
    }
}
