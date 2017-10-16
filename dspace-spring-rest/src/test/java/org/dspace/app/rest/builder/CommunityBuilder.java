/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import org.dspace.content.Community;
import org.dspace.content.MetadataSchema;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

/**
 * Builder to construct Community objects
 */
public class CommunityBuilder extends AbstractBuilder<Community> {

    private Community community;

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
        return setMetadataSingleValue(community, MetadataSchema.DC_SCHEMA, "title", null, communityName);
    }

    @Override
    public Community build() {
        try {
            communityService.update(context, community);
            context.dispatchEvents();

            indexingService.commit();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return community;
    }

    @Override
    protected DSpaceObjectService<Community> getDsoService() {
        return communityService;
    }
}
