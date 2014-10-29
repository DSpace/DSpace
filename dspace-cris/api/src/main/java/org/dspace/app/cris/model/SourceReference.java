/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class SourceReference implements Serializable
{
    /** Cris internal unique identifier, must be null */
    @Column(nullable=true)
    private String sourceID;
    @Column(nullable=true)
    private String sourceRef;
    
    public String getSourceID()
    {
        return sourceID;
    }
    public void setSourceID(String sourceID)
    {
        this.sourceID = sourceID;
    }
    public String getSourceRef()
    {
        return sourceRef;
    }
    public void setSourceRef(String sourceRef)
    {
        this.sourceRef = sourceRef;
    }
    
}
