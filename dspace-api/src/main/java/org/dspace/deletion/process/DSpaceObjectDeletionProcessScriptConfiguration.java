/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.deletion.process;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Script configuration for the batch deletion process of DSpace objects (Item, Collection, Community).
 *
 * This class defines the command-line options and associates the runnable class
 * responsible for processing the deletion of DSpace objects, accepting as input
 * a UUID or handle of the target object.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class DSpaceObjectDeletionProcessScriptConfiguration<T extends DSpaceObjectDeletionProcess>
        extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();
            options.addOption("i", "identifier", true,"UUID or handle of Community, Collection or Item");
            options.getOption("i").setType(String.class);
            options.getOption("i").setRequired(true);

            options.addOption("c", "copyVirtualMetadata", true,"Optionaly, only for Item");
            options.getOption("c").setType(String.class);
            options.getOption("c").setRequired(false);

            super.options = options;
        }
        return options;
    }

    @Override
    public Class<T> getDspaceRunnableClass() {
        return this.dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

}
