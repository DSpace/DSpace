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

import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.webui.cris.controller.BaseFormController;
import org.dspace.app.webui.cris.dto.ResearcherPageDTO;
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
public class FormAdministrationAddResearcherController extends
        BaseFormController
{

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object command, BindException errors)
            throws Exception
    {
        ResearcherPageDTO researcherDTO = (ResearcherPageDTO) command;
        String staffNo = researcherDTO.getSourceID();
        ResearcherPage researcher = null;
        if (staffNo != null && !staffNo.isEmpty())
        {
            researcher = applicationService
                    .getResearcherPageByStaffNo(researcherDTO.getSourceID());

            if (researcher != null)
            {

                return new ModelAndView("redirect:/cris/rp/"
                        + "administrator/index.htm?error=true");
            }

        }

        else
        {
            researcher = new ResearcherPage();
            researcher.setSourceID(staffNo);
            researcher.setStatus(false);
            researcher.getDynamicField().setResearcherPage(researcher);           
            applicationService.saveOrUpdate(ResearcherPage.class, researcher);

        }
        return new ModelAndView(getSuccessView() + researcher.getId());

    }
}
