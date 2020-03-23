/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import java.io.InputStream;
import java.sql.SQLException;

import org.apache.commons.cli.Options;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.impl.MockDSpaceRunnableScript;
import org.springframework.beans.factory.annotation.Autowired;

public class MockDSpaceRunnableScriptConfiguration extends ScriptConfiguration {


    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public Class<? extends DSpaceRunnable> getDspaceRunnableClass() {
        return MockDSpaceRunnableScript.class;
    }

    @Override
    public boolean isAllowedToExecute(Context context) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("r", "remove", true, "description r");
            options.getOption("r").setType(String.class);
            options.addOption("i", "index", false, "description i");
            options.getOption("i").setType(boolean.class);
            options.getOption("i").setRequired(true);
            options.addOption("f", "file", true, "source file");
            options.getOption("f").setType(InputStream.class);
            options.getOption("f").setRequired(false);
            super.options = options;
        }
        return options;
    }
}
