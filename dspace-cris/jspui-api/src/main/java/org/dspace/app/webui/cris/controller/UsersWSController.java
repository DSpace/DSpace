/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import it.cilea.osd.common.controller.BaseAbstractController;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.model.ws.User;
import org.dspace.app.cris.service.ApplicationService;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller to manage show details, list and delete for a web services User
 * 
 * @author cilea
 * 
 */
public class UsersWSController extends BaseAbstractController
{
    /**
     * the applicationService for query the RP db, injected by Spring IoC
     */
    private ApplicationService applicationService;

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        ModelAndView retValue = null;
        if ("details".equals(method))
            retValue = handleDetails(request);
        else if ("delete".equals(method))
            retValue = handleDelete(request);
        else if ("list".equals(method))
            retValue = handleList(request);
        return retValue;
    }

        
    protected ModelAndView handleDetails(HttpServletRequest request) {
        
        Map<String, Object> model = new HashMap<String, Object>();
        String id = request.getParameter("id");        
        User user = applicationService.get(User.class, id);       
                        
        model.put("user", user);                
        return new ModelAndView(getDetailsView(), model);

    }
    

    protected ModelAndView handleDelete(HttpServletRequest request) {
        Map<String, Object> model = new HashMap<String, Object>();
        String id = request.getParameter("id");
        
        try {
            applicationService.delete(User.class, Integer.parseInt(id));    
            saveMessage(request, getText("action.ws.deleted"));
        }
        catch (Exception e) {
            saveMessage(request, getText("action.ws.deleted.noSuccess"));          
        }
        return new ModelAndView(getListView(), model);
    }


    protected ModelAndView handleList(HttpServletRequest arg0) throws Exception {
        Map<String, Object> model = new HashMap<String, Object>();               
        model.put("listUsers", applicationService.getList(User.class));
        return new ModelAndView(getListView(),model);
    }
}
