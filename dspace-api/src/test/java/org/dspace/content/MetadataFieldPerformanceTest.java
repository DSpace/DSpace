/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;

import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.MetadataFieldService;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit Tests for Performance of class MetadataField
 *
 * @author ben @ atmire . com
 */
public class MetadataFieldPerformanceTest extends AbstractUnitTest {

    private final MetadataFieldService metadataFieldService =
            ContentServiceFactory.getInstance().getMetadataFieldService();
    private final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    @Test
    public void testManyQueries() throws SQLException {

        long startTime = System.currentTimeMillis();

        int amount = 50000;
        for (int i = 0; i < amount; i++) {
            metadataFieldService.findByElement(context, "dc", "description", null);
        }
        long endTime = System.currentTimeMillis();

        long duration = (endTime - startTime);

        double maxDurationPerCall = 0.01;
        double maxDuration = maxDurationPerCall * amount;
        //Duration is 0.05798 without performance improvements
        //Duration is 0.0022 with performance improvements
        Assert.assertTrue("Duration (" + duration + ") should be smaller than " + maxDuration +
                " for " + amount + " tests." +
                " Max of " + maxDurationPerCall + " ms per operation exceeded: " +
                (((double) duration) / amount) + " ms.", duration < maxDuration);
    }

    @Test
    public void testManyMetadataAdds() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        Community owningCommunity = communityService.create(null, context);
        Collection collection = collectionService.create(context, owningCommunity);
        //we need to commit the changes so we don't block the table for testing
        context.restoreAuthSystemState();

        long startTime = System.currentTimeMillis();

        int amount = 5000;
        for (int i = 0; i < amount; i++) {
            collectionService.addMetadata(context, collection,
                    "dc", "description", null, null, "Test " + i);
            collectionService.clearMetadata(context, collection,
                    "dc", "description", null, null);
        }
        long endTime = System.currentTimeMillis();

        long duration = (endTime - startTime);

        double maxDurationPerCall = .4;
        double maxDuration = maxDurationPerCall * amount;
        //Duration is 1.542 without performance improvements
        //Duration is 0.0538 with performance improvements
        Assert.assertTrue("Duration (" + duration + ") should be smaller than " + maxDuration +
                " for " + amount + " tests." +
                " Max of " + maxDurationPerCall + " ms per operation exceeded: " +
                (((double) duration) / amount) + " ms.", duration < maxDuration);

        context.turnOffAuthorisationSystem();
        // Delete community & collection created in init()
        try {
            collectionService.delete(context, collection);
        } catch (Exception e) {
            // ignore
        }
        try {
            communityService.delete(context, owningCommunity);
        } catch (Exception e) {
            // ignore
        }
        context.restoreAuthSystemState();
    }
}
