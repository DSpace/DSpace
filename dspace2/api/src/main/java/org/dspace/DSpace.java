/**
 * $Id$
 * $URL$
 * DSpace.java - Dspace - Sep 1, 2008 4:43:11 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace;

import org.dspace.services.ConfigurationService;
import org.dspace.services.LicenseService;
import org.dspace.services.manager.ServiceManager;


/**
 * This is the dSpace static cover services access object,
 * it allows access to all core dSpace services for those who cannot or will not use the injection service to get services<br/>
 * Note that this may not include every core service but should include all the services that are useful to UI developers at least
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpace {

    private static ServiceManager internalServiceManager;
    public static ServiceManager getServiceManager() {
        if (internalServiceManager == null) {
            throw new IllegalStateException("the dSpace static cover is not currently initialized and therefore cannot be used yet, " +
            		"you should not attempt to use the static cover in services as it will not be funcitional until the entire " +
            		"set of dSpace core services are initialized");
        }
        return internalServiceManager;
    }
    public static void initialize(ServiceManager serviceManager) {
        internalServiceManager = serviceManager;
    }

    // place static methods to retrieve services below here -AZ

    public static ConfigurationService getConfigurationService() {
        return DSpace.getServiceManager().getServiceByType(ConfigurationService.class);
    }

    public static LicenseService getLicenseService() {
        return DSpace.getServiceManager().getServiceByType(LicenseService.class);
    }

}
