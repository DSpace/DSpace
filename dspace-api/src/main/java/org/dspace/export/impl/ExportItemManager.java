/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.export.impl;

import java.util.ArrayList;
import java.util.List;

import org.dspace.export.api.ExportItemProvider;
import org.dspace.export.api.ExportItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Export item service. Configuration based on setting from configuration file.
 *
 * @author João Melo <jmelo@lyncode.com>
 * 
 */
public class ExportItemManager implements ExportItemService 
{
    private List<ExportItemProvider> list = null;
    private final ConfigurationService CONFIGURATIONSERVICE
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Override
    public List<ExportItemProvider> getProviders() 
    {
        boolean isExportbarEnabled = CONFIGURATIONSERVICE.getBooleanProperty("export.bar.isEnable", false);

        if (isExportbarEnabled && list == null) 
        {
            list = new ArrayList<>();

            String[] exportList = CONFIGURATIONSERVICE.getArrayProperty("export.list");
            
            for (String id : exportList) 
            {
                if (id != null && !id.trim().equals("")) 
                {
                    id = id.trim();
                    DSpaceExportItemProvider dSpaceExportItemProvider = DSpaceExportItemProvider.getInstance(id);
                    
                    if (dSpaceExportItemProvider != null) 
                    {
                        list.add(dSpaceExportItemProvider);
                    }
                }
            }
        }
        return list;
    }

    @Override
    public ExportItemProvider getProvider(String id) 
    {
        boolean isExportbarEnabled = CONFIGURATIONSERVICE.getBooleanProperty("export.bar.isEnable", false);

        if (isExportbarEnabled) 
        {
            return DSpaceExportItemProvider.getInstance(id);
        }
        return null;
    }
}
