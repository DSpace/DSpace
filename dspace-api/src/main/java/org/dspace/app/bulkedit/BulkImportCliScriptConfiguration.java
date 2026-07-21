/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import org.apache.commons.cli.Options;

/**
 * Extension of {@link BulkImportScriptConfiguration} for CLI.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class BulkImportCliScriptConfiguration<T extends BulkImportCli> extends BulkImportScriptConfiguration<T> {

    @Override
    public Options getOptions() {
        Options options = super.getOptions();
        options.addOption("e", "email", true, "email address of user");
        options.getOption("e").setRequired(true);
        super.options = options;
        return options;
    }

}
