/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.dto;

import org.dspace.app.cris.model.ACrisObjectWithTypeSupport;

import it.cilea.osd.jdyna.dto.AnagraficaObjectWithTypeDTO;

public class CrisAnagraficaObjectWithTypeDTO extends AnagraficaObjectWithTypeDTO 
{
    
    private Boolean status;
    private String sourceID;
    private String sourceRef;
    private String uuid;
    
    public CrisAnagraficaObjectWithTypeDTO(ACrisObjectWithTypeSupport object)
    {
        super();
        setSourceID(object.getSourceID());
        setSourceRef(object.getSourceRef());
        setStatus(object.getStatus());
        setUuid(object.getUuid());
        setTipologiaId(object.getType());
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

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

}
