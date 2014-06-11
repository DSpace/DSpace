/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.validator;


import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class ResearcherPageValidator implements Validator
{

    private Class clazz;

    private ApplicationService applicationService;

    public boolean supports(Class arg0)
    {
        return clazz.isAssignableFrom(arg0);
    }

    public void validate(Object arg0, Errors arg1)
    {
        ResearcherPage researcher = (ResearcherPage) arg0;

        ValidationUtils.rejectIfEmptyOrWhitespace(arg1, "staffNo",
                "error.staffNo.mandatory", "StaffNo is mandatory");
        ValidationUtils.rejectIfEmptyOrWhitespace(arg1, "fullName",
                "error.fullName.mandatory", "FullName is mandatory");

        
        String staffNo = researcher.getSourceID();
        if (staffNo!=null)
        {
            ResearcherPage temp = applicationService
                    .getResearcherPageByStaffNo(staffNo);
            if (temp != null)
            {
                if (!researcher.getId().equals(temp.getId()))
                {
                    arg1.reject("staffNo",
                            "Staff No is already in use by another researcher");
                }
            }
        }
    }

    public void setClazz(Class clazz)
    {
        this.clazz = clazz;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

}
