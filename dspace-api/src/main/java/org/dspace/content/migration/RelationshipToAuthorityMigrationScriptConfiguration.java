/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.migration;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link RelationshipToAuthorityMigrationScript} script.
 *
 * @param <T> the script class type
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class RelationshipToAuthorityMigrationScriptConfiguration<T extends RelationshipToAuthorityMigrationScript>
    extends ScriptConfiguration<T> {

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
    public Options getOptions() {
        if (options == null) {
            Options newOptions = new Options();
            newOptions.addOption("t", "type-id", true,
                "The relationship type ID to migrate. If omitted, all relationship types that have a "
                + "configured migration definition are migrated");
            newOptions.addOption("d", "delete", false,
                "Delete relationships after successful migration");
            newOptions.addOption("n", "dry-run", false,
                "Dry run: log what would be done without committing changes");
            newOptions.addOption("b", "batch-size", true,
                "Number of relationships to process per batch (default 1000)");
            newOptions.addOption("h", "help", false,
                "Display help for this script");
            super.options = newOptions;
        }
        return options;
    }
}
