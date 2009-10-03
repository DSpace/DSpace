/*
 * $Id: $
 * $URL: $
 * ExternalServiceManagerSystem.java - DSpace2 - Oct 6, 2008 2:22:36 AM - azeckoski
 **************************************************************************
 * Copyright (c) 2002-2009, The Duraspace Foundation.  All rights reserved.
 * Licensed under the Duraspace Foundation License.
 * 
 * A copy of the Duraspace License has been included in this
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
