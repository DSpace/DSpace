/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.deletion.process.strategies;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;

/**
 * Deletion strategy for DSpace Community objects.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class CommunityDeletionStrategy implements DSpaceObjectDeletionStrategy {

    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

    @Override
    public void delete(Context context, DSpaceObject dso, String[] extraParams)
            throws SQLException, AuthorizeException, IOException {
        communityService.delete(context, (Community) dso);
    }

    @Override
    public boolean supports(DSpaceObject dso) {
        return dso instanceof Community;
    }

}