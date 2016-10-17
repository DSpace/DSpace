/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.jdyna;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.jdyna.DynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.ProjectPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.service.ApplicationService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import flexjson.JSONSerializer;
import it.cilea.osd.jdyna.model.PropertiesDefinition;

//TODO
public class CrisPolicyController<TP extends PropertiesDefinition>
        extends ParameterizableViewController
{

    private ApplicationService applicationService;

    private CrisSearchService searchService;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {

        Map<String, Object> model = new HashMap<String, Object>();

        String method = request.getParameter("method");
        String related = request.getParameter("related");

        JSONSerializer serializer = new JSONSerializer();

        if (StringUtils.isNotEmpty(method))
        {

            switch (method)
            {
            case "getdefs":
                if (StringUtils.isNotBlank(related))
                {
                    Integer type = Integer.parseInt(related);
                    if (type > 1000)
                    {
                        applicationService
                                .getAllPropertiesDefinitionWithPolicySingle(
                                        DynamicPropertiesDefinition.class);
                    }
                    else
                    {
                        switch (type)
                        {
                        case 9:
                            applicationService
                                    .getAllPropertiesDefinitionWithPolicySingle(
                                            RPPropertiesDefinition.class);
                            break;
                        case 10:
                            applicationService
                                    .getAllPropertiesDefinitionWithPolicySingle(
                                            ProjectPropertiesDefinition.class);
                            break;
                        case 11:
                            applicationService
                                    .getAllPropertiesDefinitionWithPolicySingle(
                                            OUPropertiesDefinition.class);
                            break;
                        default:
                            break;
                        }
                    }
                }
                break;

            default:
                break;
            }

        }

        response.setContentType("application/json");
        return null;
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public CrisSearchService getSearchService()
    {
        return searchService;
    }

    public void setSearchService(CrisSearchService searchService)
    {
        this.searchService = searchService;
    }

}
