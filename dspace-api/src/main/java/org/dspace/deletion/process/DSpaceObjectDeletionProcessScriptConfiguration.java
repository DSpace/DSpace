/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.deletion.process;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
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

    /**
     * Determines if the current user is allowed to execute the deletion script.
     *
     * This method implements a granular permission model that allows execution by:
     * <ul>
     *   <li>Repository administrators (full admin rights)</li>
     *   <li>Community or Collection administrators (can delete objects they administer)</li>
     *   <li>Item administrators (can delete items they administer)</li>
     * </ul>
     *
     * Note: This method checks if the user has ANY of these admin roles. The actual
     * authorization to delete a specific object is verified separately in the deletion
     * process based on the user's permissions for that particular object.
     *
     * @param context               the DSpace context containing the current user
     * @param commandLineParameters the command line parameters (not used in this implementation)
     * @return true if the current user is a repository admin, community/collection admin,
     * or item admin; false otherwise
     * @throws RuntimeException if a SQLException occurs during permission checking
     */
    @Override
    public boolean isAllowedToExecute(Context context, List<DSpaceCommandLineParameter> commandLineParameters) {
        try {
            return authorizeService.isAdmin(context) || authorizeService.isComColAdmin(context)
                || authorizeService.isItemAdmin(context);
        } catch (SQLException e) {
            throw new RuntimeException("SQLException occurred when checking if the current user is an admin", e);
        }
    }
}
