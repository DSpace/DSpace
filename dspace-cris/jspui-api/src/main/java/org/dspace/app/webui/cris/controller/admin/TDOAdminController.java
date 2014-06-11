/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.admin;

import it.cilea.osd.common.controller.BaseAbstractController;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.service.ApplicationService;
import org.springframework.web.servlet.ModelAndView;

/**
 * Concrete SpringMVC controller is used to list admin OU page
 * 
 * @author pascarelli
 * 
 */
public class TDOAdminController extends BaseAbstractController
{
    protected Log log = LogFactory.getLog(getClass());

    private ApplicationService applicationService;
    
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        ModelAndView retValue = null;
        if ("list".equals(method))
            retValue = showAdminView(request, response);
        else if ("delete".equals(method))
            retValue = handleDelete(request);
        return retValue;
    }

    private ModelAndView handleDelete(HttpServletRequest request)
    {
        Map<String, Object> model = new HashMap<String, Object>();
        String id = request.getParameter("id");
        try {
            applicationService.delete(DynamicObjectType.class, Integer.parseInt(id));    
            saveMessage(request, getText("action.deleted"));
        }
        catch (Exception e) {
            saveMessage(request, getText("action.deleted.noSuccess"));          
        }
        return new ModelAndView(getListView(), model);
    }
    
    private ModelAndView showAdminView(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("researchobjects",
                applicationService.getList(DynamicObjectType.class));
        return new ModelAndView(getListView(),model);
    }


    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

}
