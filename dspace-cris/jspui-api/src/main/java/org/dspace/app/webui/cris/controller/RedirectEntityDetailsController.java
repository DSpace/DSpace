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

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

public class RedirectEntityDetailsController<T extends ACrisObject> extends
        ParameterizableViewController
{

    private ApplicationService applicationService;
    private Class<T> modelClass;
    
    /** log4j category */
    private static Logger log = Logger
            .getLogger(RedirectEntityDetailsController.class);

    
    public RedirectEntityDetailsController(Class<T> modelClazz)
    {
        this.modelClass = modelClazz;
    }
    
    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {        
        String id = request.getParameter("id");
        T entity = null;
        if (id == null || id.isEmpty())
        {

            String modeCode = request.getParameter("sourceid");
            String modeRef = request.getParameter("sourceref");
            if (modeCode != null && !modeCode.isEmpty())
            {
                entity = ((ApplicationService) applicationService)
                        .getEntityBySourceId(modeRef, modeCode, modelClass);
            }
            else
            {
                String path = request.getPathInfo().substring(1); // remove
                                                                  // first /
                String[] splitted = path.split("/");
                request.setAttribute("authority", splitted[1]);
                entity = ((ApplicationService) applicationService)
                        .get(modelClass,ResearcherPageUtils.getRealPersistentIdentifier(splitted[1], modelClass));
                 
            }
        }
        else
        {
            try
            {
                entity = applicationService.get(modelClass,
                        Integer.parseInt(id));
            }
            catch (NumberFormatException e)
            {
                log.error(e.getMessage(), e);
            }
        }

        return new ModelAndView("redirect:/cris/"+ entity.getPublicPath() + "/" + ResearcherPageUtils.getPersistentIdentifier(entity));
    }

 


    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public Class<T> getModelClass()
    {
        return modelClass;
    }

    public void setModelClass(Class<T> modelClass)
    {
        this.modelClass = modelClass;
    }
    
}
