/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch.bte;

public class ImpRecordMetadata
{

    private String value;

    private String authority = null;

    private int confidence = -1;
    
    private int metadataOrder = -1;
    
    private int share = -1;
    
    private String language;
    
    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getAuthority()
    {
        return authority;
    }

    public void setAuthority(String authority)
    {
        this.authority = authority;
    }

    public int getConfidence()
    {
        return confidence;
    }

    public void setConfidence(int confidence)
    {
        this.confidence = confidence;
    }

    public int getMetadataOrder()
    {
        return metadataOrder;
    }

    public void setMetadataOrder(int metadataOrder)
    {
        this.metadataOrder = metadataOrder;
    }

    public int getShare()
    {
        return share;
    }

    public void setShare(int share)
    {
        this.share = share;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

  
}
