/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemexport;

import java.sql.SQLException;

import org.apache.commons.cli.Options;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The {@link ScriptConfiguration} for the {@link ItemExportCLITool} script
 * 
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemExportScriptConfiguration<T extends ItemExportCLITool> extends ScriptConfiguration<T> {
    @Autowired
    private AuthorizeService authorizeService;

    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public boolean isAllowedToExecute(final Context context) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            throw new RuntimeException("SQLException occurred when checking if the current user is an admin", e);
        }
    }

    @Override
    public Options getOptions() {
        Options options = new Options();

        options.addOption("t", "type", true, "type: COLLECTION or ITEM");
        options.addOption("i", "id", true, "ID or handle of thing to export");
        options.addOption("d", "dest", true,
                          "destination where you want items to go");
        options.addOption("m", "migrate", false,
                          "export for migration (remove handle and metadata that will be re-created in new system)");
        options.addOption("n", "number", true,
                          "sequence number to begin exporting items with");
        options.addOption("z", "zip", true, "export as zip file (specify filename e.g. export.zip)");
        options.addOption("h", "help", false, "help");

        // as pointed out by Peter Dietz this provides similar functionality to export metadata
        // but it is needed since it directly exports to Simple Archive Format (SAF)
        options.addOption("x", "exclude-bitstreams", false, "do not export bitstreams");

        options.addOption("h", "help", false, "help");

        return options;
    }
}
