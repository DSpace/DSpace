/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import static org.junit.Assert.assertEquals;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.submit.factory.SubmissionServiceFactory;
import org.dspace.submit.service.SubmissionConfigService;
import org.junit.Test;

/**
 * Integration Tests for parsing and utilities on submission config forms / readers
 *
 * @author Toni Prieto
 */
public class SubmissionConfigIT extends AbstractIntegrationTestWithDatabase {

    @Test
    public void testSubmissionMapByCommunityHandleSubmissionConfig()
        throws SubmissionConfigReaderException {

        context.turnOffAuthorisationSystem();
        // Sep up a structure with one top community and two subcommunities with collections
        Community topcom = CommunityBuilder.createCommunity(context, "123456789/topcommunity-test")
            .withName("Parent Community")
            .build();
        Community subcom1 = CommunityBuilder.createSubCommunity(context, topcom, "123456789/subcommunity-test")
            .withName("Subcommunity 1")
            .build();
        Community subcom2 = CommunityBuilder.createSubCommunity(context, topcom, "123456789/not-mapped3")
            .withName("Subcommunity 2")
            .build();
        // col1 should use the form item submission form mapped for subcom1
        Collection col1 = CollectionBuilder.createCollection(context, subcom1, "123456789/not-mapped1")
            .withName("Collection 1")
            .build();
        // col2 should use the item submission form mapped for the top community
        Collection col2 = CollectionBuilder.createCollection(context, subcom2, "123456789/not-mapped2")
            .withName("Collection 2")
            .build();
        // col3 should use the item submission form directly mapped for this collection
        Collection col3 = CollectionBuilder.createCollection(context, subcom1, "123456789/collection-test")
            .withName("Collection 3")
            .withEntityType("CustomEntityType")
            .build();
        // col4 should use the item submission form mapped for the entity type CustomEntityType
        Collection col4 = CollectionBuilder.createCollection(context, subcom1, "123456789/not-mapped4")
            .withName("Collection 4")
            .withEntityType("CustomEntityType")
            .build();
        context.restoreAuthSystemState();

        SubmissionConfigService submissionConfigService = SubmissionServiceFactory.getInstance()
            .getSubmissionConfigService();

        // for col1, it should return the item submission form defined for their parent subcom1
        SubmissionConfig submissionConfig1 = submissionConfigService.getSubmissionConfigByCollection(col1);
        assertEquals("subcommunitytest", submissionConfig1.getSubmissionName());

        // for col2, it should return the item submission form defined for topcom
        SubmissionConfig submissionConfig2 = submissionConfigService.getSubmissionConfigByCollection(col2);
        assertEquals("topcommunitytest", submissionConfig2.getSubmissionName());

        // for col3, it should return the item submission form defined directly for the collection
        SubmissionConfig submissionConfig3 = submissionConfigService.getSubmissionConfigByCollection(col3);
        assertEquals("collectiontest", submissionConfig3.getSubmissionName());

        // for col4, it should return the item submission form defined for the entitytype CustomEntityType
        SubmissionConfig submissionConfig4 = submissionConfigService.getSubmissionConfigByCollection(col4);
        assertEquals("entitytypetest", submissionConfig4.getSubmissionName());
    }
}
