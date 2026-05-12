/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.junit.Test;

/**
 * Integration tests for {@link ObjectBoundHibernateDBConnection} and {@link Context.Type#OBJECT_BOUND},
 * verifying persistence, reusability across multiple commits, transactional isolation, and context lifecycle.
 */
public class ObjectBoundHibernateDBConnectionIT extends AbstractUnitTest {

    /**
     * Verify that changes committed through an OBJECT_BOUND context are
     * persisted and visible to a separate THREAD_BOUND context.
     */
    @Test
    public void testCommittedChangesAreVisibleFromOtherContext()
            throws SQLException, AuthorizeException, IOException {
        CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

        Context objectBoundContext = new Context(Context.Type.OBJECT_BOUND);
        objectBoundContext.turnOffAuthorisationSystem();

        Community community = communityService.create(null, objectBoundContext);
        communityService.setMetadataSingleValue(objectBoundContext, community,
            MetadataSchemaEnum.DC.getName(), "title", null, null, "Heartbeat Test Community");
        communityService.update(objectBoundContext, community);
        objectBoundContext.commit();

        Community found = communityService.find(context, community.getID());
        assertNotNull("Community committed in OBJECT_BOUND context should be visible to other contexts", found);

        objectBoundContext.complete();
        context.turnOffAuthorisationSystem();
        communityService.delete(context, context.reloadEntity(community));
        context.restoreAuthSystemState();
        context.commit();
    }

    /**
     * Verify that an OBJECT_BOUND context can be reused across multiple
     * commit cycles: create -> commit -> modify -> commit -> complete.
     */
    @Test
    public void testContextCanBeReusedAcrossMultipleCommits()
            throws SQLException, AuthorizeException, IOException {
        CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

        Context objectBoundContext = new Context(Context.Type.OBJECT_BOUND);
        objectBoundContext.turnOffAuthorisationSystem();

        Community community = communityService.create(null, objectBoundContext);
        communityService.setMetadataSingleValue(objectBoundContext, community,
            MetadataSchemaEnum.DC.getName(), "title", null, null, "First Commit");
        communityService.update(objectBoundContext, community);
        objectBoundContext.commit();
        assertTrue("Context should still be valid after first commit", objectBoundContext.isValid());

        community = objectBoundContext.reloadEntity(community);
        communityService.setMetadataSingleValue(objectBoundContext, community,
            MetadataSchemaEnum.DC.getName(), "title", null, null, "Second Commit");
        communityService.update(objectBoundContext, community);
        objectBoundContext.commit();
        assertTrue("Context should still be valid after second commit", objectBoundContext.isValid());

        objectBoundContext.complete();
        assertFalse("Context should be invalid after complete()", objectBoundContext.isValid());

        context.turnOffAuthorisationSystem();
        community = communityService.find(context, community.getID());
        communityService.delete(context, community);
        context.restoreAuthSystemState();
        context.commit();
    }

    /**
     * Verify that uncommitted changes in an OBJECT_BOUND context are not
     * visible to the thread-bound context before commit.
     */
    @Test
    public void testObjectBoundContextIsIsolatedFromThreadBoundContext()
            throws SQLException, AuthorizeException, IOException {
        CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

        Context objectBoundContext = new Context(Context.Type.OBJECT_BOUND);
        objectBoundContext.turnOffAuthorisationSystem();

        Community community = communityService.create(null, objectBoundContext);
        communityService.setMetadataSingleValue(objectBoundContext, community,
            MetadataSchemaEnum.DC.getName(), "title", null, null, "Uncommitted Community");
        communityService.update(objectBoundContext, community);


        Community notYetVisible = communityService.find(context, community.getID());
        assertTrue("Uncommitted changes should not be visible to thread-bound context",
                notYetVisible == null || !"Uncommitted Community".equals(notYetVisible.getName()));

        objectBoundContext.commit();
        Community nowVisible = communityService.find(context, community.getID());
        assertNotNull("After commit, community should be visible to thread-bound context", nowVisible);

        objectBoundContext.complete();
        context.turnOffAuthorisationSystem();
        communityService.delete(context, context.reloadEntity(community));
        context.restoreAuthSystemState();
        context.commit();
    }

    /**
     * Verify that after complete() the OBJECT_BOUND context is no longer valid.
     */
    @Test
    public void testContextIsInvalidAfterComplete() throws SQLException {
        Context objectBoundContext = new Context(Context.Type.OBJECT_BOUND);
        assertTrue("Context should be valid before complete()", objectBoundContext.isValid());

        objectBoundContext.complete();
        assertFalse("Context should be invalid after complete()", objectBoundContext.isValid());
    }
}