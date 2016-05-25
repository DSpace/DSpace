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
