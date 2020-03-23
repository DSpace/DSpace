/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.configuration;

import java.io.OutputStream;
import java.sql.SQLException;

import org.apache.commons.cli.Options;
import org.dspace.app.bulkedit.MetadataExport;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataExportScriptConfiguration extends ScriptConfiguration {

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public Class<? extends DSpaceRunnable> getDspaceRunnableClass() {
        return MetadataExport.class;
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

            options.addOption("i", "id", true, "ID or handle of thing to export (item, collection, or community)");
            options.getOption("i").setType(String.class);
            options.addOption("f", "file", true, "destination where you want file written");
            options.getOption("f").setType(OutputStream.class);
            options.getOption("f").setRequired(true);
            options.addOption("a", "all", false,
                              "include all metadata fields that are not normally changed (e.g. provenance)");
            options.getOption("a").setType(boolean.class);
            options.addOption("h", "help", false, "help");
            options.getOption("h").setType(boolean.class);


            super.options = options;
        }
        return options;
    }

}
