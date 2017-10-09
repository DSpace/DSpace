package org.dspace.app.rest.builder;

import java.sql.SQLException;

import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * TODO TOM UNIT TEST
 */
public class CommunityBuilder {

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected IndexingService indexingService = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(IndexingService.class.getName(),IndexingService.class);

    private Community community;
    private Context context;

    public CommunityBuilder createCommunity(final Context context) {
        return createSubCommunity(context, null);
    }

    public CommunityBuilder createSubCommunity(final Context context, final Community parent) {
        this.context = context;
        try {
            community = communityService.create(parent, context);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return this;
    }

    public CommunityBuilder withName(final String communityName) {
        try {
            communityService.setMetadataSingleValue(context, community, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY, communityName);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return this;
    }

    public Community build() {
        context.dispatchEvents();
        try {
            indexingService.commit();
        } catch (SearchServiceException e) {
            e.printStackTrace();
            return null;
        }
        return community;
    }

}
