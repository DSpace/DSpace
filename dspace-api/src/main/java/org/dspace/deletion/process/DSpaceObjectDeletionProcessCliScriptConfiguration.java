/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.deletion.process;

import org.apache.commons.cli.Options;

/**
 * Extension of {@link DSpaceObjectDeletionProcessScriptConfiguration} for CLI execution.
 * Adds the -e (email) option to specify the user executing the deletion from command line.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 *
 */
public class DSpaceObjectDeletionProcessCliScriptConfiguration<T extends DSpaceObjectDeletionProcessCli>
    extends DSpaceObjectDeletionProcessScriptConfiguration<T> {

    @Override
    public Options getOptions() {
        Options options = super.getOptions();
        options.addOption("e", "email", true, "email address of user");
        options.getOption("e").setRequired(true);
        super.options = options;
        return options;
    }

}
