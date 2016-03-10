/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.swagger;

import io.swagger.jaxrs.config.BeanConfig;
import org.dspace.app.util.Util;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * Bootstrap class to setup the configuration for swagger
 *
 * @author kevinvandevelde at atmire.com
 */
public class DSpaceSwaggerBootstrapServlet extends HttpServlet
{
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        BeanConfig beanConfig = new BeanConfig();
        //We set the version to the DSpace version
        beanConfig.setVersion(Util.getSourceVersion());
        beanConfig.setSchemes(configurationService.getArrayProperty("rest.schemes"));
        beanConfig.setHost(configurationService.getProperty("rest.host"));
        beanConfig.setBasePath(configurationService.getProperty("rest.setBasePath"));

        beanConfig.setResourcePackage(configurationService.getProperty("rest.swagger.scan-packages"));
        beanConfig.setScan(true);
    }
}
