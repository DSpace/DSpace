/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import java.sql.SQLException;

import org.apache.commons.cli.Options;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The {@link ScriptConfiguration} for the {@link ItemImportCLITool} script
 * 
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemImportScriptConfiguration<T extends ItemImportCLITool> extends ScriptConfiguration<T> {
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
        options.addOption("a", "add", false, "add items to DSpace");
        options.addOption("b", "add-bte", false, "add items to DSpace via Biblio-Transformation-Engine (BTE)");
        options.addOption("r", "replace", false, "replace items in mapfile");
        options.addOption("d", "delete", false,
                "delete items listed in mapfile");
        options.addOption("i", "inputtype", true, "input type in case of BTE import");
        options.addOption("s", "source", true, "source of items (directory)");
        options.addOption("z", "zip", true, "name of zip file");
        options.addOption("c", "collection", true,
                "destination collection(s) Handle or database ID");
        options.addOption("m", "mapfile", true, "mapfile items in mapfile");
        options.addOption("e", "eperson", true,
                "email of eperson doing importing");
        options.addOption("w", "workflow", false,
                "send submission through collection's workflow");
        options.addOption("n", "notify", false,
                "if sending submissions through the workflow, send notification emails");
        options.addOption("t", "test", false,
                "test run - do not actually import items");
        options.addOption("p", "template", false, "apply template");
        options.addOption("R", "resume", false,
                "resume a failed import (add only)");
        options.addOption("q", "quiet", false, "don't display metadata");

        options.addOption("h", "help", false, "help");

        return options;
    }
}
