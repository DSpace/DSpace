/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.list.LazyList;
import org.dspace.app.cris.model.ws.Criteria;
import org.dspace.app.cris.model.ws.User;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

/**
 * This SpringMVC controller is responsible to handle request of export
 * 
 * @author cilea
 * 
 */
public class FormUserWSController extends BaseFormController
{
    private List<String> objectTypes;
    
    public void setObjectTypes(List<String> objectTypes)
    {
        this.objectTypes = objectTypes;
    }
    
    public List<String> getObjectTypes()
    {
        return objectTypes;
    }

    @Override
    protected Object formBackingObject(HttpServletRequest request)
            throws Exception
    {
        Context context = UIUtil.obtainContext(request);
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only system administrator can access to the functionality");
        }
        
        User userws = (User) super.formBackingObject(request);
        String id = request.getParameter("id");
        if(id!=null && !id.isEmpty()) {
            userws = applicationService.get(User.class, Integer.parseInt(id));
        }
        
        if (userws.getCriteria().isEmpty())
        {
            for (String criteria : objectTypes)
            {
                Criteria newCriteria = new Criteria();
                newCriteria.setCriteria(criteria);
                newCriteria.setFilter("");
                newCriteria.setEnabled(false);
                userws.getCriteria().add(newCriteria);
            }
        }        
        userws.setCriteria(LazyList.decorate(userws.getCriteria(),
                FactoryUtils.instantiateFactory(Criteria.class)));
        return userws;
    }

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object command, BindException errors)
            throws Exception
    {
        User object = (User) command;
        applicationService.saveOrUpdate(User.class, object);
        return new ModelAndView(getSuccessView());
    }

}
