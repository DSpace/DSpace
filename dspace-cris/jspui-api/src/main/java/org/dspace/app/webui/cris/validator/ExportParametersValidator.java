/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.validator;


import org.dspace.app.cris.service.ApplicationService;
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
        
        //TODO validate query?
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
