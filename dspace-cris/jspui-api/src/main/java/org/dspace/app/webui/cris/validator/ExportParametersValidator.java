/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.validator;


import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.cris.dto.ExportParametersDTO;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class ExportParametersValidator implements Validator
{

    private Class clazz;

    private ApplicationService applicationService;

    public boolean supports(Class arg0)
    {
        return clazz.isAssignableFrom(arg0);
    }

    public void validate(Object arg0, Errors arg1)
    {
        ExportParametersDTO param = (ExportParametersDTO) arg0;

        if (param.getRpIdStart() != null)
        {
            if (!param.getRpIdStart().toLowerCase().startsWith("rp")
                    && !StringUtils.isNumeric(param.getRpIdStart()))
            {
                // errore formato non valido
                arg1.rejectValue("rpIdStart", "jsp.layout.hku.export.validation.notvalid.rpIdStart");
            }
            else
            {
                // check sulla lunghezza e fix a rp00000
                if (param.getRpIdStart().toLowerCase().startsWith("rp"))
                {
                    param.setRpIdStart(ResearcherPageUtils.getPersistentIdentifier(Integer.parseInt(param.getRpIdStart().substring(2)),ResearcherPage.class));
                }
                else
                {
                    param.setRpIdStart(ResearcherPageUtils.getPersistentIdentifier(Integer.parseInt(param.getRpIdStart()),ResearcherPage.class));
                }
            }
        }

        if (param.getRpIdEnd() != null)
        {
            if (!param.getRpIdEnd().toLowerCase().startsWith("rp")
                    && !StringUtils.isNumeric(param.getRpIdEnd()))
            {
                // errore formato non valido
                arg1.rejectValue("rpIdStart", "jsp.layout.hku.export.validation.notvalid.rpIdEnd");
            }
            else
            {
                // check sulla lunghezza e fix a rp00000
                if (param.getRpIdEnd().toLowerCase().startsWith("rp"))
                {
                    param.setRpIdEnd(ResearcherPageUtils.getPersistentIdentifier(Integer.parseInt(param.getRpIdEnd().substring(2)),ResearcherPage.class));
                }
                else
                {
                    param.setRpIdEnd(ResearcherPageUtils.getPersistentIdentifier(Integer.parseInt(param.getRpIdEnd()),ResearcherPage.class));
                }
            }
        }
        
        if (param.getCreationStart() != null && param.getCreationEnd() != null && param.getCreationStart().after(param.getCreationEnd()))
        {
            arg1.rejectValue("creationStart", "jsp.layout.hku.export.validation.notvalid.creationStart");
        }
        
        // if only a rp id is entered use it for both limit
        if (param.getRpIdStart() != null && param.getRpIdEnd() == null)
        {
            param.setRpIdEnd(param.getRpIdStart());
        }
        
        if (param.getRpIdEnd() != null && param.getRpIdStart() == null)
        {
            param.setRpIdStart(param.getRpIdEnd());
        }
        
        // if only a staff no is entered use it for both limit        
        if (param.getStaffNoStart() != null && param.getStaffNoEnd() == null)
        {
            param.setStaffNoEnd(param.getStaffNoStart());
        }
        
        if (param.getStaffNoEnd() != null && param.getStaffNoStart() == null)
        {
            param.setStaffNoStart(param.getStaffNoEnd());
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
