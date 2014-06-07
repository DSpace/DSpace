/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.CrisSubscribeService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.util.UIUtil;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

/**
 * This SpringMVC controller allows a dspace user to subscribe alert notification
 * of updated or newly added item to a RP
 * 
 * @author cilea
 * 
 */
public class CrisSubscribeController<T extends ACrisObject> extends MultiActionController
{

    /**
     * the applicationService for query the RP db, injected by Spring IoC
     */  
    private ApplicationService applicationService;
    
    private CrisSubscribeService rpSubService;

    private Class<T> className;
    
    private String viewName;
    
    public void setViewName(String viewName)
    {
        this.viewName = viewName;
    }
    
    public String getViewName()
    {
        return viewName;
    }
    
    public ModelAndView subscribe(HttpServletRequest arg0,
            HttpServletResponse arg1) throws Exception
    {                
        String uuid = arg0.getParameter("uuid");        
        if(uuid==null || uuid.isEmpty()) {
            return null;
        }        
        rpSubService.subscribe(UIUtil.obtainContext(arg0).getCurrentUser(), uuid);
        ACrisObject acrisobject = applicationService.getEntityByUUID(uuid);
        return new ModelAndView(getViewName()+ "/" + acrisobject.getPublicPath() +"/"+ acrisobject.getCrisID() +"?subscribe=true");
    }

    public ModelAndView unsubscribe(HttpServletRequest arg0,
            HttpServletResponse arg1) throws Exception
    {                
        String uuid = arg0.getParameter("uuid");        
        if(uuid==null || uuid.isEmpty()) {
            return null;
        }        
        rpSubService.unsubscribe(UIUtil.obtainContext(arg0).getCurrentUser(), uuid);
        ACrisObject acrisobject = applicationService.getEntityByUUID(uuid);
        return new ModelAndView(getViewName()+ "/" + acrisobject.getPublicPath() +"/"+ acrisobject.getCrisID() +"?subscribe=false");
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }
    
    public void setRpSubService(CrisSubscribeService rpSubService)
    {
        this.rpSubService = rpSubService;
    }
    
    public CrisSubscribeService getRpSubService()
    {
        return rpSubService;
    }

    public Class<T> getClassName()
    {
        return className;
    }

    public void setClassName(Class<T> className)
    {
        this.className = className;
    }
}
