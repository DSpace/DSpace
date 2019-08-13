/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.factory.impl;

import org.dspace.content.factory.ProcessServiceFactory;
import org.dspace.content.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;

public class ProcessServiceFactoryImpl extends ProcessServiceFactory {

    @Autowired(required = true)
    private ProcessService processService;

    public ProcessService getProcessService() {
        return processService;
    }
}
