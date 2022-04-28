/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.harvest;

import java.sql.SQLException;

import org.apache.commons.cli.Options;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.springframework.beans.factory.annotation.Autowired;


public class HarvestScriptConfiguration<T extends Harvest> extends ScriptConfiguration<T> {
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

    public boolean isAllowedToExecute(final Context context) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            throw new RuntimeException("SQLException occurred when checking if the current user is an admin", e);
        }
    }

    public Options getOptions() {
        Options options = new Options();
        options.addOption("p", "purge", false, "delete all items in the collection");
        options.addOption("r", "run", false, "run the standard harvest procedure");
        options.addOption("g", "ping", false, "test the OAI server and set");
        options.addOption("s", "setup", false, "Set the collection up for harvesting");
        options.addOption("S", "start", false, "start the harvest loop");
        options.addOption("R", "reset", false, "reset harvest status on all collections");
        options.addOption("P", "purgeCollections", false, "purge all harvestable collections");
        options.addOption("o", "reimport", false, "reimport all items in the collection, " +
            "this is equivalent to -p -r, purging all items in a collection and reimporting them");
        options.addOption("c", "collection", true,
                          "harvesting collection (handle or id)");
        options.addOption("t", "type", true,
                          "type of harvesting (0 for none)");
        options.addOption("a", "address", true,
                          "address of the OAI-PMH server");
        options.addOption("i", "oai_set_id", true,
                          "id of the PMH set representing the harvested collection");
        options.addOption("m", "metadata_format", true,
                          "the name of the desired metadata format for harvesting, resolved to namespace and " +
                                  "crosswalk in dspace.cfg");

        options.addOption("h", "help", false, "help");

        return options;
    }
}
