/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.deletion.process;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.deletion.process.strategies.DSpaceObjectDeletionStrategy;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.kernel.ServiceManager;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * Batch process for deleting DSpace objects (Item, Collection, Community).
 * This class orchestrates the deletion process, delegating the actual deletion logic
 * to a strategy registry that selects the appropriate strategy for each DSpaceObject type.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class DSpaceObjectDeletionProcess
        extends DSpaceRunnable<DSpaceObjectDeletionProcessScriptConfiguration<DSpaceObjectDeletionProcess>> {

    public static final String OBJECT_DELETION_SCRIPT = "object-deletion";

    private ItemService itemService;
    private HandleService handleService;
    private CommunityService communityService;
    private AuthorizeService authorizeService;
    private CollectionService collectionService;

    private String id;
    private Context context;
    private String[] copyVirtualMetadata;
    private List<DSpaceObjectDeletionStrategy> deletionStrategies = new ArrayList<>();

    @Override
    public void setup() throws ParseException {
        ServiceManager serviceManager = new DSpace().getServiceManager();
        itemService = ContentServiceFactory.getInstance().getItemService();
        handleService = HandleServiceFactory.getInstance().getHandleService();
        communityService = ContentServiceFactory.getInstance().getCommunityService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        deletionStrategies.addAll(serviceManager.getServicesByType(DSpaceObjectDeletionStrategy.class));

        parseCommandLineOptions();
    }

    /**
     * Parses and validates command line options.
     */
    private void parseCommandLineOptions() {
        this.id = commandLine.getOptionValue('i');
        this.copyVirtualMetadata = commandLine.hasOption('c') ? parseCopyVirtualMetadataOption() : new String[0];
    }

    private String[] parseCopyVirtualMetadataOption() {
        String value = commandLine.getOptionValue('c');
        if (value.contains(",")) {
            return value.split(",");
        }
        return new String[] { value };
    }

    @Override
    public void internalRun() throws Exception {
        assignCurrentUserInContext();
        Optional<DSpaceObject> dSpaceObjectOptional = resolveDSpaceObject(this.id);

        if (dSpaceObjectOptional.isEmpty()) {
            var error = String.format("DSpaceObject for provided identifier:%s doesn't exist!", this.id);
            throw new IllegalArgumentException(error);
        }

        DSpaceObject dso = dSpaceObjectOptional.get();

        if (!authorizeService.isAdmin(context, dso)) {
            throw new AuthorizeException("Current user is not eligible to execute script: " + OBJECT_DELETION_SCRIPT);
        }

        var info = "Performing deletion of DSpaceObject (and all child objects) for type=%s and uuid=%s";
        handler.logInfo(String.format(info, Constants.typeText[dso.getType()], dso.getID().toString()));
        getStrategy(dso).delete(this.context, dso, this.copyVirtualMetadata);
        handler.logInfo("Deletion completed!");
    }

    private DSpaceObjectDeletionStrategy getStrategy(DSpaceObject dso) {
        var error = "No strategy for type:" + dso.getType();
        return deletionStrategies.stream()
                                 .filter(s -> s.supports(dso))
                                 .findFirst()
                                 .orElseThrow(() -> new IllegalArgumentException(error));
    }

    private void assignCurrentUserInContext() throws SQLException {
        this.context = new Context();
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    /**
     * Resolves the identifier (Item, Collection, or Community).
     *
     * @param identifier   The UUID or handle of the DSpace object.
     * @return An Optional containing the DSpaceObject if found.
     * @throws SQLException If database error occurs.
     */
    private Optional<DSpaceObject> resolveDSpaceObject(String identifier) throws SQLException {
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

    @Override
    public DSpaceObjectDeletionProcessScriptConfiguration<DSpaceObjectDeletionProcess> getScriptConfiguration() {
        ServiceManager sm = new DSpace().getServiceManager();
        return sm.getServiceByName(OBJECT_DELETION_SCRIPT, DSpaceObjectDeletionProcessScriptConfiguration.class);
    }

}
