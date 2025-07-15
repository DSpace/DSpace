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

import org.apache.commons.cli.ParseException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.deletion.process.strategies.CollectionDeletionStrategy;
import org.dspace.deletion.process.strategies.CommunityDeletionStrategy;
import org.dspace.deletion.process.strategies.DSpaceObjectDeletionStrategy;
import org.dspace.deletion.process.strategies.ItemDeletionStrategy;
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

    private ItemService itemService;
    private HandleService handleService;
    private CommunityService communityService;
    private CollectionService collectionService;

    private String id;
    private Context context;
    private List<DSpaceObjectDeletionStrategy> deletionStrategies;

    @Override
    public void setup() throws ParseException {
        ServiceManager sm = new DSpace().getServiceManager();
        itemService = ContentServiceFactory.getInstance().getItemService();
        handleService = HandleServiceFactory.getInstance().getHandleService();
        communityService = ContentServiceFactory.getInstance().getCommunityService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();

        deletionStrategies.add(sm.getServiceByName("itemDeletionStrategy", ItemDeletionStrategy.class));
        deletionStrategies.add(sm.getServiceByName("communityDeletionStrategy", CommunityDeletionStrategy.class));
        deletionStrategies.add(sm.getServiceByName("collectionDeletionStrategy", CollectionDeletionStrategy.class));

        parseCommandLineOptions();
    }

    /**
     * Parses and validates command line options.
     */
    private void parseCommandLineOptions() {
        this.id = commandLine.getOptionValue('i');
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
        getStrategy(dso).delete(context, dso);
    }

    private DSpaceObjectDeletionStrategy getStrategy(DSpaceObject dso) {
        return deletionStrategies.stream()
                                 .filter(s -> s.supports(dso))
                                 .findFirst()
                                 .orElseThrow(() ->
                                                new IllegalArgumentException("No strategy for type: " + dso.getType()));
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
        return sm.getServiceByName("dspace-object-deletion", DSpaceObjectDeletionProcessScriptConfiguration.class);
    }

}
