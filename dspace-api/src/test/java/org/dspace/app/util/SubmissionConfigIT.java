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
    public void testSubmissionConfigMapByCollectionOrEntityType()
        throws SubmissionConfigReaderException {

        context.turnOffAuthorisationSystem();
        // Sep up a structure with one top community and two collections
        Community topcom = CommunityBuilder.createCommunity(context, "123456789/topcommunity-test")
            .withName("Parent Community")
            .build();
        // col1 should use the item submission form directly mapped for this collection
        Collection col1 = CollectionBuilder.createCollection(context, topcom, "123456789/collection-test")
            .withName("Collection 1")
            .withEntityType("CustomEntityType")
            .build();
        // col2 should use the item submission form mapped for the entity type CustomEntityType
        Collection col2 = CollectionBuilder.createCollection(context, topcom, "123456789/not-mapped1")
            .withName("Collection 2")
            .withEntityType("CustomEntityType")
            .build();
        context.restoreAuthSystemState();

        SubmissionConfigService submissionConfigService = SubmissionServiceFactory.getInstance()
            .getSubmissionConfigService();

        // for col1, it should return the item submission form defined directly for the collection
        SubmissionConfig submissionConfig1 = submissionConfigService.getSubmissionConfigByCollection(col1);
        assertEquals("collectiontest", submissionConfig1.getSubmissionName());

        // for col2, it should return the item submission form defined for the entitytype CustomEntityType
        SubmissionConfig submissionConfig2 = submissionConfigService.getSubmissionConfigByCollection(col2);
        assertEquals("entitytypetest", submissionConfig2.getSubmissionName());
    }
}