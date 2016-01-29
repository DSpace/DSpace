/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.RPNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPNestedProperty;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.model.jdyna.RPTypeNestedObject;

public class CrisRPRefDisplayStrategy extends ACrisRefDisplayStrategy<RPProperty, RPPropertiesDefinition, RPNestedProperty, RPNestedPropertiesDefinition, RPNestedObject, RPTypeNestedObject>
{

    @Override
    public Class<ResearcherPage> getClassName()
    {
        return ResearcherPage.class;
    }

    @Override
    public String getPublicPath()
    {
        return "rp";
    }

    
}
