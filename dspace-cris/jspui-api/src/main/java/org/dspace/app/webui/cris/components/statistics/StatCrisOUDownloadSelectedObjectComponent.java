/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components.statistics;

import it.cilea.osd.jdyna.model.PropertiesDefinition;

import org.dspace.app.cris.model.jdyna.OUNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUPropertiesDefinition;

public class StatCrisOUDownloadSelectedObjectComponent extends
        StatCrisDownloadSelectedObjectComponent
{

    @Override
    protected PropertiesDefinition innerCall(Integer pkey)
    {

        PropertiesDefinition def = getApplicationService().get(
                OUPropertiesDefinition.class, pkey);
        if (def == null)
        {
            def = getApplicationService().get(OUNestedPropertiesDefinition.class,
                    pkey);
        }

        return def;
    }

}
