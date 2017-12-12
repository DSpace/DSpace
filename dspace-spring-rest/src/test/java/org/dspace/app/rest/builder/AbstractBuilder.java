/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.RegistrationDataService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.InProgressUserService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;

import java.util.LinkedList;
import java.util.List;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

/**
 * Abstract builder class that holds references to all available services
 *
 * @author Jonas Van Goolen - (jonas@atmire.com)
 */
public abstract class AbstractBuilder<T, S> {

    static CommunityService communityService;
    static CollectionService collectionService;
    static ItemService itemService;
    static InstallItemService installItemService;
    static WorkspaceItemService workspaceItemService;
    static EPersonService ePersonService;
    static GroupService groupService;
    static BundleService bundleService;
    static BitstreamService bitstreamService;
    static BitstreamFormatService bitstreamFormatService;
    static AuthorizeService authorizeService;
    static ResourcePolicyService resourcePolicyService;
    static IndexingService indexingService;
    static RegistrationDataService registrationDataService;
    static VersionHistoryService versionHistoryService;
    static ClaimedTaskService claimedTaskService;
    static InProgressUserService inProgressUserService;
    static PoolTaskService poolTaskService;
    static WorkflowItemRoleService workflowItemRoleService;
    static MetadataFieldService metadataFieldService;
    static MetadataSchemaService metadataSchemaService;
    static SiteService siteService;

    protected Context context;

    private static List<AbstractBuilder> builders = new LinkedList<>();
    /** log4j category */
    private static final Logger log = Logger.getLogger(AbstractDSpaceObjectBuilder.class);

    protected AbstractBuilder(Context context){
        this.context = context;
        builders.add(this);
    }

    public static void init() {
        communityService = ContentServiceFactory.getInstance().getCommunityService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        installItemService = ContentServiceFactory.getInstance().getInstallItemService();
        workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        groupService = EPersonServiceFactory.getInstance().getGroupService();
        bundleService = ContentServiceFactory.getInstance().getBundleService();
        bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
        authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
        indexingService = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(IndexingService.class.getName(),IndexingService.class);
        registrationDataService = EPersonServiceFactory.getInstance().getRegistrationDataService();
        versionHistoryService = VersionServiceFactory.getInstance().getVersionHistoryService();
        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
        siteService = ContentServiceFactory.getInstance().getSiteService();

        // Temporarily disabled
        // TODO find a way to be able to test the XML and "default" workflow at the same time
        //claimedTaskService = XmlWorkflowServiceFactoryImpl.getInstance().getClaimedTaskService();
        //inProgressUserService = XmlWorkflowServiceFactoryImpl.getInstance().getInProgressUserService();
        //poolTaskService = XmlWorkflowServiceFactoryImpl.getInstance().getPoolTaskService();
        //workflowItemRoleService = XmlWorkflowServiceFactoryImpl.getInstance().getWorkflowItemRoleService();
    }


    public static void destroy() {
        communityService = null;
        collectionService = null;
        itemService = null;
        installItemService = null;
        workspaceItemService = null;
        ePersonService = null;
        groupService = null;
        bundleService = null;
        bitstreamService = null;
        authorizeService = null;
        resourcePolicyService = null;
        indexingService = null;
        bitstreamFormatService = null;
        registrationDataService = null;
        versionHistoryService = null;
        claimedTaskService = null;
        inProgressUserService = null;
        poolTaskService = null;
        workflowItemRoleService = null;
        metadataFieldService = null;
        metadataSchemaService = null;
        siteService = null;
    }

    public static void cleanupObjects() throws Exception {
        for (AbstractBuilder builder : builders) {
            builder.cleanup();
        }

        // Bitstreams still leave a trace when deleted, so we need to fully "expunge" them
        try(Context c = new Context()) {
            List<Bitstream> bitstreams = bitstreamService.findAll(c);
            for (Bitstream bitstream : CollectionUtils.emptyIfNull(bitstreams)) {

                // We expect tests to clean up all the objects they create. This means, all bitstreams we find here
                // should have already been deleted. If that is not the case (e.g. when added functionality unexpectedly
                // creates a new bitstream which was not deleted), this method will throw an exception and the developer
                // should look into the unexpected creation of the bitstream.
                expungeBitstream(c, bitstream);
            }
            c.complete();
        }
    }

    protected abstract void cleanup() throws Exception;

    public abstract T build();

    public abstract void delete(T dso) throws Exception;

    protected abstract S getService();

    /**
     * Method to completely delete a bitstream from the database and asset store.
     * @param bit The deleted bitstream to remove completely
     */
    static void expungeBitstream(Context c, Bitstream bit) throws Exception {
        bit = c.reloadEntity(bit);
        c.turnOffAuthorisationSystem();
        if (bit != null) {
            bitstreamService.expunge(c, bit);
        }
    }
}
