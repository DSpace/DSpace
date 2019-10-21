/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.factory.impl;

import org.dspace.content.factory.ProcessServiceFactory;
import org.dspace.scripts.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The implementation for the {@link ProcessServiceFactory}
 */
public class ProcessServiceFactoryImpl extends ProcessServiceFactory {

    @Autowired(required = true)
    private ProcessService processService;

    @Override
    public ProcessService getProcessService() {
        return processService;
    }
}
