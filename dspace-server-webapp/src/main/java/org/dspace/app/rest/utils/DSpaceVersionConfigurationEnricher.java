/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import org.dspace.app.util.Util;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Class that use the configuration service to add a property named
 * 'dspace.version' with the current DSpace application version.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component
public class DSpaceVersionConfigurationEnricher implements ApplicationRunner {

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        configurationService.addPropertyValue("dspace.version", Util.getSourceVersion());
    }

}
