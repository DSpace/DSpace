/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.json;

import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.BoxResearcherPage;
import org.dspace.app.cris.model.jdyna.TabResearcherPage;
import org.dspace.app.webui.cris.web.tag.ResearcherTagLibraryFunctions;

import it.cilea.osd.jdyna.web.controller.json.AjaxJSONNavigationController;


public class RPNavigationController
        extends
        AjaxJSONNavigationController<BoxResearcherPage, TabResearcherPage>
{

    public RPNavigationController()
    {
        super(TabResearcherPage.class);       
      
    }
      

    @Override
    public int countBoxPublicMetadata(Integer objectID,
            BoxResearcherPage box, boolean b)
    {        
        return ResearcherTagLibraryFunctions
        .countBoxPublicMetadata(getApplicationService().get(ResearcherPage.class, objectID), box, b);
    }

    @Override
    public boolean isBoxHidden(Integer objectID,
            BoxResearcherPage box)
    {
        return ResearcherTagLibraryFunctions.isBoxHidden(
                getApplicationService().get(ResearcherPage.class, objectID), box);
    }

    
}
