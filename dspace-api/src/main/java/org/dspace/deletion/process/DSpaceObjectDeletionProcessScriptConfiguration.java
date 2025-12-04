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
            options.addOption("i", "identifier", true,
                "UUID or handle of the DSpace object to delete (Item, Collection, or Community). " +
                "Example: -i 123e4567-e89b-12d3-a456-426614174000 or -i 123456789/123");
            options.getOption("i").setType(String.class);
            options.getOption("i").setRequired(true);

            options.addOption("c", "copyVirtualMetadata", true,
                "Optional parameter for Items with relationships. Controls whether virtual metadata " +
                "from relationships should be copied as physical metadata to related items before deletion. " +
                "Accepts three formats: " +
                "'all' - copies all virtual metadata from all relationships; " +
                "'configured' - copies metadata only for relationships with copyToLeft/copyToRight=true; " +
                "'<id1>,<id2>' - copies metadata only for specified RelationshipType IDs (comma-separated). " +
                "Example: -c all, -c configured, or -c 1,3,5");
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
