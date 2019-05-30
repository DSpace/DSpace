/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;

/**
 * Builder to construct Community objects
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Raf Ponsaerts (raf dot ponsaerts at atmire dot com)
 */
public class CommunityBuilder extends AbstractDSpaceObjectBuilder<Community> {

    private Community community;

    protected CommunityBuilder(Context context) {
        super(context);
    }

    public static CommunityBuilder createCommunity(final Context context) {
        CommunityBuilder builder = new CommunityBuilder(context);
        return builder.create();
    }

    private CommunityBuilder create() {
        return createSubCommunity(context, null);
    }

    public static CommunityBuilder createSubCommunity(final Context context, final Community parent) {
        CommunityBuilder builder = new CommunityBuilder(context);
        return builder.createSub(parent);
    }

    private CommunityBuilder createSub(final Community parent) {
        try {
            community = communityService.create(parent, context);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return this;
    }

    public CommunityBuilder withName(final String communityName) {
        return setMetadataSingleValue(community, MetadataSchemaEnum.DC.getName(), "title", null, communityName);
    }

    public CommunityBuilder withLogo(String content) throws AuthorizeException, IOException, SQLException {
        try (InputStream is = IOUtils.toInputStream(content, CharEncoding.UTF_8)) {
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

    @Override
    public void cleanup() throws Exception {
        delete(community);
    }

    @Override
    protected DSpaceObjectService<Community> getService() {
        return communityService;
    }

    /**
     * Delete the Test Community referred to by the given UUID
     * @param uuid UUID of Test Community to delete
     * @throws SQLException
     * @throws IOException
     */
    public static void deleteCommunity(UUID uuid) throws SQLException, IOException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            Community community = communityService.find(c, uuid);
            if (community != null) {
                try {
                    communityService.delete(c, community);
                } catch (AuthorizeException e) {
                    // cannot occur, just wrap it to make the compiler happy
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            c.complete();
        }
    }
}
