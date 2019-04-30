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
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

public class ViewResolver
{
    
    private List<String> metadata;
    
    private List<ISimpleViewResolver> plugins;

    public SimpleViewEntityDTO fillDTO(Context context, int itemID,
            int typeID) throws SQLException
    {
        SimpleViewEntityDTO dto = new SimpleViewEntityDTO(context, itemID, typeID);        
        DSpaceObject dso = null;
        if (typeID == Constants.ITEM)
        {
            dso = DSpaceObject.find(context, typeID, itemID);
        }
        else
        {
            ApplicationService applicationService = new DSpace().getServiceManager()
                    .getServiceByName("applicationService", ApplicationService.class);
            dso = applicationService.getEntityById(itemID, typeID);
        }

        dto.setEntityID(dso.getID());
        dto.setEntityTypeID(dso.getType());
        for(String mm : metadata) {
            dto.getDuplicateItem().put(mm, dso.getMetadataValue(mm));
        }

        if (plugins != null && !plugins.isEmpty())
        {
            for(ISimpleViewResolver plugin : plugins)
            {
                plugin.fillDTO(context, dto, dso);
            }
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
