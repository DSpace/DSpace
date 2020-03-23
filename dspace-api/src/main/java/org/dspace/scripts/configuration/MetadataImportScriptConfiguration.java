/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.configuration;

import java.io.InputStream;
import java.sql.SQLException;

import org.apache.commons.cli.Options;
import org.dspace.app.bulkedit.MetadataImport;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataImportScriptConfiguration extends ScriptConfiguration {

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public Class<? extends DSpaceRunnable> getDspaceRunnableClass() {
        return MetadataImport.class;
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

            options.addOption("f", "file", true, "source file");
            options.getOption("f").setType(InputStream.class);
            options.getOption("f").setRequired(true);
            options.addOption("e", "email", true, "email address or user id of user (required if adding new items)");
            options.getOption("e").setType(String.class);
            options.getOption("e").setRequired(true);
            options.addOption("s", "silent", false,
                              "silent operation - doesn't request confirmation of changes USE WITH CAUTION");
            options.getOption("s").setType(boolean.class);
            options.addOption("w", "workflow", false, "workflow - when adding new items, use collection workflow");
            options.getOption("w").setType(boolean.class);
            options.addOption("n", "notify", false,
                              "notify - when adding new items using a workflow, send notification emails");
            options.getOption("n").setType(boolean.class);
            options.addOption("v", "validate-only", false,
                              "validate - just validate the csv, don't run the import");
            options.getOption("v").setType(boolean.class);
            options.addOption("t", "template", false,
                              "template - when adding new items, use the collection template (if it exists)");
            options.getOption("t").setType(boolean.class);
            options.addOption("h", "help", false, "help");
            options.getOption("h").setType(boolean.class);

            super.options = options;
        }
        return options;
    }
}
