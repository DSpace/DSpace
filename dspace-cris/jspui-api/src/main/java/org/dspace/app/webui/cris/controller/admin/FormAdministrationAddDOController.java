/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.admin;

import it.cilea.osd.jdyna.web.Utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.webui.cris.controller.BaseFormController;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

/**
 * This SpringMVC controller is responsible to handle the creation of a new
 * ResearcherPage. The initialization of the DTO is done by the
 * {@link RPAdminController}
 * 
 * @author cilea
 * 
 */
public class FormAdministrationAddDOController extends BaseFormController
{

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object command, BindException errors)
            throws Exception
    {
        ResearchObject object = (ResearchObject) command;
        String path = Utils.getAdminSpecificPath(request, null);
        object = new ResearchObject();
        object.setStatus(false);
        object.getDynamicField().setDynamicObject(object);
        object.setTypo(applicationService.findTypoByShortName(
                DynamicObjectType.class, path));
        applicationService.saveOrUpdate(ResearchObject.class, object);
        return new ModelAndView(getSuccessView() + object.getId() + "&path="
                + path);

    }
}
