/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;

import org.apache.commons.cli.Options;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The {@link ScriptConfiguration} for the {@link IndexClient} script
 */
public class IndexDiscoveryScriptConfiguration extends ScriptConfiguration {

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public Class<? extends DSpaceRunnable> getDspaceRunnableClass() {
        return IndexClient.class;
    }

    @Override
    public boolean isAllowedToExecute(Context context) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            throw new RuntimeException("SQLException occured", e);
        }
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            super.options = IndexClientOptions.constructOptions();
        }
        return options;
    }
}
