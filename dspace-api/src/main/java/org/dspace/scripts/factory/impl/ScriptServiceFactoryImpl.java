/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.factory.impl;

import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ProcessService;
import org.dspace.scripts.service.ScriptService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The implementation for the {@link ScriptServiceFactory}
 */
public class ScriptServiceFactoryImpl extends ScriptServiceFactory {

    @Autowired(required = true)
    private ScriptService scriptService;

    @Autowired(required = true)
    private ProcessService processService;

    @Override
    public ScriptService getScriptService() {
        return scriptService;
    }

    @Override
    public ProcessService getProcessService() {
        return processService;
    }
}
