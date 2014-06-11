/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.admin;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.webui.cris.controller.BaseFormController;
import org.dspace.app.webui.cris.dto.OrganizationUnitDTO;
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
public class FormAdministrationAddOUController extends
        BaseFormController
{
    @Override
    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object command, BindException errors)
            throws Exception
    {
        OrganizationUnitDTO orgunitDTO = (OrganizationUnitDTO) command;
        String code = orgunitDTO.getSourceID();
        OrganizationUnit orgunit = null;
        if (code != null && !code.isEmpty())
        {
            orgunit = applicationService.getOrganizationUnitByCode(code);
            if (orgunit != null)
            {

                return new ModelAndView("redirect:/cris/ou/"
                        + "administrator/index.htm?error=true");
            }
        }
        else
        {

            orgunit = new OrganizationUnit();
            orgunit.setSourceID(code);
            orgunit.setStatus(false);
            orgunit.getDynamicField().setOrganizationUnit(orgunit);            
            applicationService.saveOrUpdate(OrganizationUnit.class, orgunit);

        }
        return new ModelAndView(getSuccessView() + orgunit.getId());
    }
}
