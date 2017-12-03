/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.MutablePeriod;
import org.joda.time.format.PeriodFormat;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

/**
 * Abstract builder to construct DSpace Objects
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Raf Ponsaerts (raf dot ponsaerts at atmire dot com)
 */
public abstract class AbstractBuilder<T extends DSpaceObject> {

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

    protected Context context;

    /** log4j category */
    private static final Logger log = Logger.getLogger(AbstractBuilder.class);

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
    }

    protected <B> B handleException(final Exception e) {
        log.error(e.getMessage(), e);
        return null;
    }

    protected abstract DSpaceObjectService<T> getDsoService();

    protected <B extends AbstractBuilder<T>> B addMetadataValue(final T dso, final String schema, final String element, final String qualifier, final String value) {
        try {
            getDsoService().addMetadata(context, dso, schema, element, qualifier, Item.ANY, value);
        } catch (Exception e) {
            return handleException(e);
        }
        return (B) this;
    }

    protected <B extends AbstractBuilder<T>> B setMetadataSingleValue(final T dso, final String schema, final String element, final String qualifier, final String value) {
        try {
            getDsoService().setMetadataSingleValue(context, dso, schema, element, qualifier, Item.ANY, value);
        } catch (Exception e) {
            return handleException(e);
        }

        return (B) this;
    }

    protected <B extends AbstractBuilder<T>> B setEmbargo(String embargoPeriod, DSpaceObject dso) {
        // add policy just for anonymous
        try {
            MutablePeriod period = PeriodFormat.getDefault().parseMutablePeriod(embargoPeriod);
            Date embargoDate = DateTime.now(DateTimeZone.UTC).plus(period).toDate();

            return setOnlyReadPermission(dso, groupService.findByName(context, Group.ANONYMOUS), embargoDate);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    protected <B extends AbstractBuilder<T>> B setOnlyReadPermission(DSpaceObject dso, Group group, Date startDate) {
        // add policy just for anonymous
        try {
            authorizeService.removeAllPolicies(context, dso);

            ResourcePolicy rp = authorizeService.createOrModifyPolicy(null, context, null, group,
                    null, startDate, Constants.READ, "Integration Test", dso);
            if (rp != null) {
                resourcePolicyService.update(context, rp);
            }
        } catch (Exception e) {
            return handleException(e);
        }
        return (B) this;
    }

    public abstract T build();

    public void delete(T dso) throws SQLException, IOException, AuthorizeException {
        Context c = new Context();
        c.turnOffAuthorisationSystem();
        T attachedDso = c.reloadEntity(dso);
        if(attachedDso != null) {
            getDsoService().delete(c, attachedDso);
        }
    }
}
