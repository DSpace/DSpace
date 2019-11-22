/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.scripts.service.ScriptService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The implementation for the {@link ScriptService}
 */
public class ScriptServiceImpl implements ScriptService {

    @Autowired
    private List<DSpaceRunnable> dSpaceRunnables;

    @Override
    public DSpaceRunnable getScriptForName(String name) {
        return dSpaceRunnables.stream()
                              .filter(dSpaceRunnable -> StringUtils.equalsIgnoreCase(dSpaceRunnable.getName(), name))
                              .findFirst()
                              .orElse(null);
    }
}
