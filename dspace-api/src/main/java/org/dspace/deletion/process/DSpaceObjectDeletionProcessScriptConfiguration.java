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
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
    ItemService itemService;
    @Autowired
    HandleService handleService;
    @Autowired
    CommunityService communityService;
    @Autowired
    CollectionService collectionService;

    Logger log = LogManager.getLogger();

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

    /**
     * Match the authorization check implemented in AuthorizeServicePermissionEvaluatorPlugin
     * and REST usage: hasPermission('DELETE') for the DSO, with inherit = true
     * @param context DSpace context of the current user
     * @param commandLineParameters command line parameters, required to parse and resolve the DSO identifier
     * @return result of authorize delete check, default false
     * @throws IllegalArgumentException if the identifier cannot be resolved
     * @throws SQLException if the DAO operation for the authZ check fails
     */
    @Override
    public boolean isAllowedToExecute(Context context, List<DSpaceCommandLineParameter> commandLineParameters) {
        try {
            for (DSpaceCommandLineParameter parameter : commandLineParameters) {
                if ("-i".equals(parameter.getName())) {
                    DSpaceObject dso = resolveDSpaceObject(context, parameter.getValue())
                        .orElseThrow(() -> new IllegalArgumentException("Could not resolve %s to DSpace Object"
                                    .formatted(parameter.getValue())));
                    return authorizeService.authorizeActionBoolean(context, dso, Constants.DELETE, true);
                }
            }
        } catch (IllegalArgumentException | SQLException e) {
            log.error(e.getMessage());
        }
        return false;

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
     * Resolves the identifier (Item, Collection, or Community).
     *
     * @param identifier   The UUID or handle of the DSpace object.
     * @return An Optional containing the DSpaceObject if found.
     * @throws SQLException If database error occurs.
     */
    private Optional<DSpaceObject> resolveDSpaceObject(Context context, String identifier)
            throws SQLException {
        UUID uuid = null;
        try {
            uuid = UUID.fromString(identifier);
        } catch (Exception e) {
            // It's not a UUID, proceed to treat it as a handle.
        }

        if (uuid != null) {
            Item item = itemService.find(context, uuid);
            if (item != null) {
                return Optional.of(item);
            }
            Community community = communityService.find(context, uuid);
            if (community != null) {
                return Optional.of(community);
            }
            Collection collection = collectionService.find(context, uuid);
            if (collection != null) {
                return Optional.of(collection);
            }
        }
        DSpaceObject dso = handleService.resolveToObject(context, identifier);
        return dso != null ? Optional.of(dso) : Optional.empty();
    }
}
