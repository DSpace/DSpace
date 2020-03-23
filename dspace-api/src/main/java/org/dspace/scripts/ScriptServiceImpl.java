/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.core.Context;
import org.dspace.kernel.ServiceManager;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.service.ScriptService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The implementation for the {@link ScriptService}
 */
public class ScriptServiceImpl implements ScriptService {

    @Autowired
    private ServiceManager serviceManager;

    @Override
    public ScriptConfiguration getScriptForName(String name) {
        return serviceManager.getServiceByName(name, ScriptConfiguration.class);
    }

    @Override
    public List<ScriptConfiguration> getScriptConfigurations(Context context) {
        return serviceManager.getServicesByType(ScriptConfiguration.class).stream().filter(
            scriptConfiguration -> scriptConfiguration.isAllowedToExecute(context)).collect(Collectors.toList());
    }

    @Override
    public DSpaceRunnable getDSpaceRunnableForScriptConfiguration(ScriptConfiguration scriptToExecute)
        throws IllegalAccessException, InstantiationException {
        return scriptToExecute.getDspaceRunnableClass().newInstance();
    }
}
