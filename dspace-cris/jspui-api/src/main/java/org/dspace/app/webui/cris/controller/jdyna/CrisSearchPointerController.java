/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.jdyna;

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
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import it.cilea.osd.jdyna.model.AWidget;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.utils.SelectableDTO;
import it.cilea.osd.jdyna.web.controller.SearchPointerController;
import it.cilea.osd.jdyna.web.tag.DisplayPointerTagLibrary;
import it.cilea.osd.jdyna.widget.WidgetPointer;

public class CrisSearchPointerController extends
        SearchPointerController<PropertiesDefinition, ACrisObject>
{

    private CrisSearchService searchService;

    @Override
    protected List<SelectableDTO> getResult(AWidget widget, String query, String expression, String... filtro)
    {
        Context context = null;
        List<SelectableDTO> results = new ArrayList<SelectableDTO>();
        try
        {
            context = new Context();

            List<DSpaceObject> objects = getSearchService().search(context,
                    query + "*", null, true, 0, Integer.MAX_VALUE, filtro);
            for (DSpaceObject obj : objects)
            {
                ACrisObject real = (ACrisObject) obj;
                String display = (String) DisplayPointerTagLibrary.evaluate(
                        obj, expression);
                SelectableDTO dto = new SelectableDTO(
                        real.getIdentifyingValue(), display);
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

    @Override
    protected String getDisplay(AWidget widget)
    {
        WidgetPointer widgetPointer = (WidgetPointer)widget;
        return widgetPointer.getDisplay();
    }
    
    @Override
    protected String[] getFilter(AWidget widget)
    {
        WidgetPointer widgetPointer = (WidgetPointer)widget;
        String filtro = widgetPointer.getFiltro();
        Class target = widgetPointer.getTargetValoreClass();

        String resourcetype = "search.resourcetype: [9 TO 11]";
        if (target.equals(ResearcherPage.class))
        {
            resourcetype = "search.resourcetype:" + CrisConstants.RP_TYPE_ID;
        }
        else if (target.equals(Project.class))
        {
            resourcetype = "search.resourcetype:"
                    + CrisConstants.PROJECT_TYPE_ID;
        }
        else if (target.equals(OrganizationUnit.class))
        {
            resourcetype = "search.resourcetype:" + CrisConstants.OU_TYPE_ID;
        }
        else if (target.equals(ResearchObject.class))
        {
            resourcetype = ((WidgetPointerDO) widgetPointer)
                    .getFilterExtended();
        }
        if(filtro==null || filtro.isEmpty()) {
            return new String[] { resourcetype };    
        }
        return new String[] { resourcetype, filtro };
    }

}
