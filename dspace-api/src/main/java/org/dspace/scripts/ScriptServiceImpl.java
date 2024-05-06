/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.kernel.ServiceManager;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.service.ScriptService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The implementation for the {@link ScriptService}
 */
public class ScriptServiceImpl implements ScriptService {
    private static final Logger log = LogManager.getLogger();

    @Autowired
    private ServiceManager serviceManager;

    @Override
    public ScriptConfiguration getScriptConfiguration(String name) {
        return serviceManager.getServiceByName(name, ScriptConfiguration.class);
    }

    @Override
    public List<ScriptConfiguration> getScriptConfigurations(Context context) {
        return serviceManager.getServicesByType(ScriptConfiguration.class).stream().filter(
            scriptConfiguration -> scriptConfiguration.isAllowedToExecute(context, null))
                             .sorted(Comparator.comparing(ScriptConfiguration::getName))
                             .collect(Collectors.toList());
    }

    @Override
    public DSpaceRunnable createDSpaceRunnableForScriptConfiguration(ScriptConfiguration scriptToExecute)
        throws IllegalAccessException, InstantiationException {
        try {
            return (DSpaceRunnable) scriptToExecute.getDspaceRunnableClass().getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException | NoSuchMethodException e) {
            log.error(e::getMessage, e);
            throw new RuntimeException(e);
        }
    }
}
