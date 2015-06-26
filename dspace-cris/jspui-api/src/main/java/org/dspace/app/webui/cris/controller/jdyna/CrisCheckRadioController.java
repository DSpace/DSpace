/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.jdyna;

import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.web.controller.SearchCheckRadioController;
import it.cilea.osd.jdyna.web.controller.SearchPointerController;
import it.cilea.osd.jdyna.web.tag.DisplayPointerTagLibrary;
import it.cilea.osd.jdyna.widget.WidgetCheckRadio;
import it.cilea.osd.jdyna.widget.WidgetPointer;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.widget.WidgetPointerDO;
import org.dspace.app.cris.util.Researcher;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public class CrisCheckRadioController extends
        SearchCheckRadioController<PropertiesDefinition, ACrisObject>
{

    private CrisSearchService searchService;

    @Override
    protected List<SelectableDTO> getResult(String query)
    {
        Context context = null;
        List<SelectableDTO> results = new ArrayList<SelectableDTO>();
        try
        {
            context = new Context();

            List<DSpaceObject> objects = getSearchService().search(context,
                    query, null, true, 0, Integer.MAX_VALUE, null);
            for (DSpaceObject obj : objects)
            {
                ACrisObject real = (ACrisObject) obj;
                SelectableDTO dto = new SelectableDTO(
                        real.getIdentifyingValue(), real.getDisplayValue());
                results.add(dto);
            }

        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }

        return results;
    }

    public void setSearchService(CrisSearchService searchService)
    {
        this.searchService = searchService;
    }

    public CrisSearchService getSearchService()
    {
        return searchService;
    }

}
