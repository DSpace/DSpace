/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.jdyna;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;
import it.cilea.osd.jdyna.web.controller.FileServiceController;
import it.cilea.osd.jdyna.widget.WidgetFile;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.model.jdyna.widget.AWidgetFileCris;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.statistics.util.StatsConfig;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;
import org.springframework.web.servlet.ModelAndView;

public class CrisFileServiceController<T extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>, P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>>
        extends FileServiceController
{

    private ApplicationService applicationService;

    private String filePath;
    
    private Class<T> targetObject;

    private Class<TP> targetPropertyDefinition;

    @Override
    protected String getPath()
    {
        return ConfigurationManager.getProperty(CrisConstants.CFG_MODULE,
                getFilePath());
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public String getFilePath()
    {
        return filePath;
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        try
        {
            return super.handleRequest(request, response);
        }
        catch(RuntimeException ex) {
            JSPManager.showJSP(request, response, "/error/404.jsp");
            return null;
        }
        finally
        {

            String idString = request.getPathInfo();
            String[] pathInfo = idString.split("/", 4);
            String folder = pathInfo[3];
            
            int indexOf = folder.indexOf("/");
            String id = folder.substring(0, indexOf);
            String idTP = folder.substring((indexOf+1), folder.length() - 1);
            TP tp = applicationService.get(getTargetPropertyDefinition(),
                    Integer.parseInt(idTP));
            if (tp.getRendering() instanceof AWidgetFileCris)
            {
                AWidgetFileCris widget = (AWidgetFileCris) tp.getRendering();
                // Fire usage event.
                if (widget.isUseInStatistics())
                {
                    request.setAttribute("sectionid", tp.getId());
                    new DSpace().getEventService().fireEvent(
                            new UsageEvent(UsageEvent.Action.VIEW, request,
                                    UIUtil.obtainContext(request),
                                    applicationService.getEntityByCrisId(id,
                                            getTargetObject())));
                }
            }

        }

    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public Class<T> getTargetObject()
    {
        return targetObject;
    }

    public void setTargetObject(Class<T> targetObject)
    {
        this.targetObject = targetObject;
    }

    public Class<TP> getTargetPropertyDefinition()
    {
        return targetPropertyDefinition;
    }

    public void setTargetPropertyDefinition(Class<TP> targetPropertyDefinition)
    {
        this.targetPropertyDefinition = targetPropertyDefinition;
    }
  
}
