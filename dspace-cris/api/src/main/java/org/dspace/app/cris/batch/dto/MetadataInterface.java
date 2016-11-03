/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch.dto;

public interface MetadataInterface
{
    public String getImp_schema();

    public String getImp_element();

    public String getImp_qualifier();

    public String getImp_value();
    
    public String getImp_authority();
    
    public Integer getImp_confidence();
    
    public Integer getImp_share();

    public Integer getMetadata_order();

    public Integer getPkey();

}
