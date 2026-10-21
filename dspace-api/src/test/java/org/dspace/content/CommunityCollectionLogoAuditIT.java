/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.audit.AuditEvent;
import org.dspace.app.audit.AuditSolrServiceImpl;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests verifying that an audit log entry is produced whenever a
 * logo is added to or removed from a Community or a Collection.
 *
 * <p>These tests cover the changes introduced in DURACOM-467: both
 * {@link CommunityServiceImpl#setLogo(org.dspace.core.Context, Community, InputStream)}
 * and {@link CollectionServiceImpl#setLogo(org.dspace.core.Context, Collection, InputStream)}
 * now fire {@link org.dspace.event.Event#ADD} / {@link org.dspace.event.Event#REMOVE}
 * events carrying the bitstream checksum so that the audit system records the
 * logo change.</p>
 *
 * @author Emanuele Sanna (emanuele.sanna at 4science.it)
 */
public class CommunityCollectionLogoAuditIT extends AbstractIntegrationTestWithDatabase {

    private static final String ADD = "ADD";
    private static final String REMOVE = "REMOVE";
    private static final String TYPE_COMMUNITY = "COMMUNITY";
    private static final String TYPE_COLLECTION = "COLLECTION";

    private final CommunityService communityService =
        ContentServiceFactory.getInstance().getCommunityService();
    private final CollectionService collectionService =
        ContentServiceFactory.getInstance().getCollectionService();

    private ConfigurationService configurationService;
    private AuditSolrServiceImpl auditSolrService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        ServiceManager serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        auditSolrService = serviceManager.getServiceByName(
            AuditSolrServiceImpl.class.getName(), AuditSolrServiceImpl.class);

        // make sure we start from a clean audit core and auditing is active
        auditSolrService.deleteEvents(context, null, null);
        auditSolrService.commit();
        configurationService.setProperty("audit.enabled", true);
    }

    @After
    public void cleanAuditCore() throws Exception {
        auditSolrService.deleteEvents(context, null, null);
        auditSolrService.commit();
        configurationService.setProperty("audit.enabled", false);
    }

    @Test
    public void addLogoToCommunityIsAudited() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("Audited Community").build();

        Bitstream logo;
        try (InputStream is = IOUtils.toInputStream("community-logo", StandardCharsets.UTF_8)) {
            logo = communityService.setLogo(context, community, is);
        }
        String checksum = logo.getChecksum();
        context.commit();
        auditSolrService.commit();
        context.restoreAuthSystemState();

        List<AuditEvent> events = findEvents(community);

        assertEquals("Exactly one ADD audit event is expected after adding a logo",
            1, countByType(events, ADD));
        AuditEvent addEvent = firstByType(events, ADD);
        assertEquals(community.getID(), addEvent.getSubjectUUID());
        assertEquals(TYPE_COMMUNITY, addEvent.getSubjectType());
        assertEquals(logo.getID(), addEvent.getObjectUUID());
        assertEquals(checksum, addEvent.getChecksum());
    }

    @Test
    public void replaceCommunityLogoAuditsRemoveAndAdd() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("Audited Community").build();

        Bitstream firstLogo;
        try (InputStream is = IOUtils.toInputStream("first-logo", StandardCharsets.UTF_8)) {
            firstLogo = communityService.setLogo(context, community, is);
        }
        String firstChecksum = firstLogo.getChecksum();

        Bitstream secondLogo;
        try (InputStream is = IOUtils.toInputStream("second-logo", StandardCharsets.UTF_8)) {
            secondLogo = communityService.setLogo(context, community, is);
        }
        String secondChecksum = secondLogo.getChecksum();
        context.commit();
        auditSolrService.commit();
        context.restoreAuthSystemState();

        List<AuditEvent> events = findEvents(community);

        // two ADD (first + second logo) and one REMOVE (old logo replaced)
        assertEquals("Both logos should produce an ADD audit event", 2, countByType(events, ADD));
        assertEquals("Replacing the logo should produce a REMOVE audit event", 1, countByType(events, REMOVE));

        AuditEvent removeEvent = firstByType(events, REMOVE);
        assertEquals(community.getID(), removeEvent.getSubjectUUID());
        assertEquals(TYPE_COMMUNITY, removeEvent.getSubjectType());
        assertEquals(firstLogo.getID(), removeEvent.getObjectUUID());
        assertEquals("REMOVE event must carry the replaced logo checksum",
            firstChecksum, removeEvent.getChecksum());

        assertTrue("The new logo checksum must appear in an ADD event",
            events.stream().anyMatch(e -> ADD.equals(e.getEventType())
                && secondChecksum.equals(e.getChecksum())));
    }

    @Test
    public void removeCommunityLogoIsAudited() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("Audited Community").build();

        Bitstream logo;
        try (InputStream is = IOUtils.toInputStream("to-be-removed", StandardCharsets.UTF_8)) {
            logo = communityService.setLogo(context, community, is);
        }
        String checksum = logo.getChecksum();

        // start fresh so we only observe the REMOVE events generated below
        context.commit();
        auditSolrService.deleteEvents(context, null, null);
        auditSolrService.commit();

        communityService.setLogo(context, community, null);
        context.commit();
        auditSolrService.commit();
        context.restoreAuthSystemState();

        List<AuditEvent> events = findEvents(community);

        assertEquals("Removing the logo should produce a REMOVE audit event", 1, countByType(events, REMOVE));
        AuditEvent removeEvent = firstByType(events, REMOVE);
        assertEquals(community.getID(), removeEvent.getSubjectUUID());
        assertEquals(TYPE_COMMUNITY, removeEvent.getSubjectType());
        assertEquals(logo.getID(), removeEvent.getObjectUUID());
        assertEquals(checksum, removeEvent.getChecksum());
    }

    @Test
    public void addLogoToCollectionIsAudited() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Audited Collection").build();

        Bitstream logo;
        try (InputStream is = IOUtils.toInputStream("collection-logo", StandardCharsets.UTF_8)) {
            logo = collectionService.setLogo(context, collection, is);
        }
        String checksum = logo.getChecksum();
        context.commit();
        auditSolrService.commit();
        context.restoreAuthSystemState();

        List<AuditEvent> events = findEvents(collection);

        assertEquals("Exactly one ADD audit event is expected after adding a logo",
            1, countByType(events, ADD));
        AuditEvent addEvent = firstByType(events, ADD);
        assertEquals(collection.getID(), addEvent.getSubjectUUID());
        assertEquals(TYPE_COLLECTION, addEvent.getSubjectType());
        assertEquals(logo.getID(), addEvent.getObjectUUID());
        assertEquals(checksum, addEvent.getChecksum());
    }

    @Test
    public void replaceCollectionLogoAuditsRemoveAndAdd() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Audited Collection").build();

        Bitstream firstLogo;
        try (InputStream is = IOUtils.toInputStream("first-logo", StandardCharsets.UTF_8)) {
            firstLogo = collectionService.setLogo(context, collection, is);
        }
        String firstChecksum = firstLogo.getChecksum();

        Bitstream secondLogo;
        try (InputStream is = IOUtils.toInputStream("second-logo", StandardCharsets.UTF_8)) {
            secondLogo = collectionService.setLogo(context, collection, is);
        }
        String secondChecksum = secondLogo.getChecksum();
        context.commit();
        auditSolrService.commit();
        context.restoreAuthSystemState();

        List<AuditEvent> events = findEvents(collection);

        assertEquals("Both logos should produce an ADD audit event", 2, countByType(events, ADD));
        assertEquals("Replacing the logo should produce a REMOVE audit event", 1, countByType(events, REMOVE));

        AuditEvent removeEvent = firstByType(events, REMOVE);
        assertEquals(collection.getID(), removeEvent.getSubjectUUID());
        assertEquals(TYPE_COLLECTION, removeEvent.getSubjectType());
        assertEquals(firstLogo.getID(), removeEvent.getObjectUUID());
        assertEquals("REMOVE event must carry the replaced logo checksum",
            firstChecksum, removeEvent.getChecksum());

        assertTrue("The new logo checksum must appear in an ADD event",
            events.stream().anyMatch(e -> ADD.equals(e.getEventType())
                && secondChecksum.equals(e.getChecksum())));
    }

    @Test
    public void removeCollectionLogoIsAudited() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Audited Collection").build();

        Bitstream logo;
        try (InputStream is = IOUtils.toInputStream("to-be-removed", StandardCharsets.UTF_8)) {
            logo = collectionService.setLogo(context, collection, is);
        }
        String checksum = logo.getChecksum();

        // start fresh so we only observe the REMOVE event generated below
        context.commit();
        auditSolrService.deleteEvents(context, null, null);
        auditSolrService.commit();

        collectionService.setLogo(context, collection, null);
        context.commit();
        auditSolrService.commit();
        context.restoreAuthSystemState();

        List<AuditEvent> events = findEvents(collection);

        assertEquals("Removing the logo should produce a REMOVE audit event", 1, countByType(events, REMOVE));
        AuditEvent removeEvent = firstByType(events, REMOVE);
        assertEquals(collection.getID(), removeEvent.getSubjectUUID());
        assertEquals(TYPE_COLLECTION, removeEvent.getSubjectType());
        assertEquals(logo.getID(), removeEvent.getObjectUUID());
        assertEquals(checksum, removeEvent.getChecksum());
    }

    private List<AuditEvent> findEvents(DSpaceObject dso) {
        return auditSolrService.findEvents(dso.getID(), null, null, Integer.MAX_VALUE, 0, true);
    }

    private long countByType(List<AuditEvent> events, String eventType) {
        return events.stream().filter(e -> eventType.equals(e.getEventType())).count();
    }

    private AuditEvent firstByType(List<AuditEvent> events, String eventType) {
        return events.stream()
            .filter(e -> eventType.equals(e.getEventType()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No audit event of type " + eventType + " was found"));
    }
}
