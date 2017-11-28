/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Community;
import org.dspace.content.MetadataSchema;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

/**
 * Builder to construct Community objects
 *
 * @author Atmire NV (info at atmire dot com)
 */
public class CommunityBuilder extends AbstractBuilder<Community> {

    private Community community;

    protected CommunityBuilder(Context context) {
        super(context);
    }

    public static CommunityBuilder createCommunity(final Context context) {
        CommunityBuilder builder = new CommunityBuilder(context);
        return builder.create(context);
    }

    private CommunityBuilder create(final Context context) {
        return createSubCommunity(context, null);
    }

    public static CommunityBuilder createSubCommunity(final Context context, final Community parent) {
        CommunityBuilder builder = new CommunityBuilder(context);
        return builder.createSub(context, parent);
    }

    private CommunityBuilder createSub(final Context context, final Community parent) {
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

    public CommunityBuilder withLogo(String content) throws AuthorizeException, IOException, SQLException {
        try(InputStream is = IOUtils.toInputStream(content, CharEncoding.UTF_8)) {
            communityService.setLogo(context, community, is);
        }
        return this;
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

    protected void cleanup() throws Exception {
        delete(community);
    }

    @Override
    protected DSpaceObjectService<Community> getDsoService() {
        return communityService;
    }
}
