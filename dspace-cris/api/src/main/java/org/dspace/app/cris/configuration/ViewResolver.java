/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.configuration;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.cris.model.dto.SimpleViewEntityDTO;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public class ViewResolver
{
    
    private List<String> metadata;
    
    private List<ISimpleViewResolver> plugins;

    public SimpleViewEntityDTO fillDTO(Context context, int itemID,
            int typeID) throws SQLException
    {
        
        DSpaceObject dso = DSpaceObject.find(context, typeID, itemID);
        
        SimpleViewEntityDTO dto = new SimpleViewEntityDTO(context, itemID, typeID);        
        dto.setEntityID(dso.getID());
        dto.setEntityTypeID(dso.getType());
        for(String mm : metadata) {
            dto.getDuplicateItem().put(mm, dso.getMetadataValue(mm));
        }
        
        for(ISimpleViewResolver plugin : plugins) {
            plugin.fillDTO(context, dto, dso);
        }
        return dto;
    }

    public List<String> getMetadata()
    {
        return metadata;
    }

    public void setMetadata(List<String> metadata)
    {
        this.metadata = metadata;
    }

    public List<ISimpleViewResolver> getPlugins()
    {
        return plugins;
    }

    public void setPlugins(List<ISimpleViewResolver> plugins)
    {
        this.plugins = plugins;
    }

}
