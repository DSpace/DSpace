/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.servicemanager;

import org.dspace.servicemanager.config.DSpaceConfigurationService;

import java.util.List;

/**
 * Interface for modular service manager systems.
 * Provides a generic initialization routine, in leiu of hardcoded constructors
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ExternalServiceManagerSystem extends ServiceManagerSystem {
    /**
     * Initialize the service manager's configuration
     *
     * @param parent
     * @param configurationService
     * @param testMode
     * @param developmentMode
     * @param serviceManagers
     */
    void init(ServiceManagerSystem parent, DSpaceConfigurationService configurationService,
            boolean testMode, boolean developmentMode, List<ServiceManagerSystem> serviceManagers);

}
