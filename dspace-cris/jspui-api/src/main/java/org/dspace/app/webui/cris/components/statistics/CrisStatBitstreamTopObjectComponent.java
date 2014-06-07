/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components.statistics;

import org.dspace.app.cris.util.ResearcherPageUtils;

public class CrisStatBitstreamTopObjectComponent extends StatBitstreamTopObjectComponent
{

    @Override
    protected String getObjectId(String id)
    {      
        return ResearcherPageUtils.getPersistentIdentifier(Integer.parseInt(id), getTargetObjectClass());
    }
    
 
    
}
