/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.requestitem.factory.RequestItemServiceFactory;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.builder.util.AbstractBuilderCleanupUtil;
import org.dspace.content.Bitstream;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.SiteService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.RegistrationDataService;
import org.dspace.orcid.factory.OrcidServiceFactory;
import org.dspace.orcid.service.OrcidHistoryService;
import org.dspace.orcid.service.OrcidQueueService;
import org.dspace.orcid.service.OrcidTokenService;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ProcessService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.InProgressUserService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;

/**
 * Abstract builder class that holds references to all available services
 *
 * @param <T>   This parameter represents the Model object for the Builder
 * @param <S>   This parameter represents the Service object for the builder
 * @author Jonas Van Goolen - (jonas@atmire.com)
 */
public abstract class AbstractBuilder<T, S> {

    static CommunityService communityService;
    static CollectionService collectionService;
    static ItemService itemService;
    static InstallItemService installItemService;
    static WorkspaceItemService workspaceItemService;
    static XmlWorkflowItemService workflowItemService;
    static XmlWorkflowService workflowService;
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
    static RelationshipService relationshipService;
    static RelationshipTypeService relationshipTypeService;
    static EntityTypeService entityTypeService;
    static ProcessService processService;
    static RequestItemService requestItemService;
    static VersioningService versioningService;
    static OrcidHistoryService orcidHistoryService;
    static OrcidQueueService orcidQueueService;
    static OrcidTokenService orcidTokenService;

    protected Context context;

    /**
     * This static class will make sure that the objects built with the builders are disposed of in a foreign-key
     * constraint safe manner by predefining an order
     */
    private static final AbstractBuilderCleanupUtil abstractBuilderCleanupUtil
            = new AbstractBuilderCleanupUtil();
    /**
     * log4j category
     */
    private static final Logger log = LogManager.getLogger();

    protected AbstractBuilder(Context context) {
        this.context = context;
        //Register this specific builder to be deleted later on
        abstractBuilderCleanupUtil.addToMap(this);
    }

    public static void init() {
        communityService = ContentServiceFactory.getInstance().getCommunityService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        installItemService = ContentServiceFactory.getInstance().getInstallItemService();
        workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        workflowItemService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();
        workflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();
        ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        groupService = EPersonServiceFactory.getInstance().getGroupService();
        bundleService = ContentServiceFactory.getInstance().getBundleService();
        bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
        authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
        indexingService = DSpaceServicesFactory.getInstance().getServiceManager()
                                               .getServiceByName(IndexingService.class.getName(),
                                                                 IndexingService.class);
        registrationDataService = EPersonServiceFactory.getInstance().getRegistrationDataService();
        versionHistoryService = VersionServiceFactory.getInstance().getVersionHistoryService();
        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
        siteService = ContentServiceFactory.getInstance().getSiteService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        relationshipTypeService = ContentServiceFactory.getInstance().getRelationshipTypeService();
        entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
        processService = ScriptServiceFactory.getInstance().getProcessService();
        requestItemService = RequestItemServiceFactory.getInstance().getRequestItemService();
        versioningService = DSpaceServicesFactory.getInstance().getServiceManager()
                                 .getServiceByName(VersioningService.class.getName(), VersioningService.class);

        // Temporarily disabled
        claimedTaskService = XmlWorkflowServiceFactory.getInstance().getClaimedTaskService();
        inProgressUserService = XmlWorkflowServiceFactory.getInstance().getInProgressUserService();
        poolTaskService = XmlWorkflowServiceFactory.getInstance().getPoolTaskService();
        workflowItemRoleService = XmlWorkflowServiceFactory.getInstance().getWorkflowItemRoleService();
        orcidHistoryService = OrcidServiceFactory.getInstance().getOrcidHistoryService();
        orcidQueueService = OrcidServiceFactory.getInstance().getOrcidQueueService();
        orcidTokenService = OrcidServiceFactory.getInstance().getOrcidTokenService();
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
        relationshipService = null;
        relationshipTypeService = null;
        entityTypeService = null;
        processService = null;
        requestItemService = null;
        versioningService = null;
        orcidTokenService = null;

    }

    public static void cleanupObjects() throws Exception {

        // This call will make sure that the map with AbstractBuilders will be cleaned up
        abstractBuilderCleanupUtil.cleanupBuilders();

        // Bitstreams still leave a trace when deleted, so we need to fully "expunge" them
        try (Context c = new Context()) {
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

    /**
     * This method will cleanup the map of builders
     */
    public static void cleanupBuilderCache() {
        abstractBuilderCleanupUtil.cleanupMap();
    }

    /**
     * This method will ensure that the DSpaceObject contained within the Builder will be cleaned up properly
     * @throws Exception    If something goes wrong
     */
    public abstract void cleanup() throws Exception;

    /**
     * Create the object from the values that have been set on this builder.
     * @return the initialized object.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     */
    public abstract T build() throws SQLException, AuthorizeException;

    /**
     * Remove the object from the persistence store.
     *
     * @param c current DSpace session.
     * @param dso the object to be removed.
     * @throws Exception passed through.
     */
    public abstract void delete(Context c, T dso) throws Exception;

    protected abstract S getService();

    /**
     * Log an exception.
     *
     * @param <B> type of Builder which caught the exception.
     * @param e exception to be handled.
     * @return {@code null} always.
     */
    protected <B> B handleException(final Exception e) {
        log.error(e.getMessage(), e);
        return null;
    }

    /**
     * Method to completely delete a bitstream from the database and asset store.
     *
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
