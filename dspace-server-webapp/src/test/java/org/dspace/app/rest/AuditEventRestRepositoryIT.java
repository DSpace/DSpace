/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.AuditEventMatcher.matchAuditEvent;
import static org.dspace.app.rest.matcher.AuditEventMatcher.matchAuditEventFullProjection;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.app.audit.AuditEvent;
import org.dspace.app.audit.AuditSolrServiceImpl;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.AuditEventBuilder;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration Tests against the /api/system/auditevents endpoint
 */
public class AuditEventRestRepositoryIT extends AbstractControllerIntegrationTest {
    private final int TOTAL_ELEMENT = 18;
    private Collection collection;

    private Item item;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuditSolrServiceImpl auditSolrService;

    @Autowired
    private BitstreamService bitstreamService;

    private void loadSomeObjects() throws Exception {
        auditSolrService.deleteEvents(context, null, null);

        // We turn off the authorization system in order to create the structure as
        // defined below
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        collection = CollectionBuilder.createCollection(context, parentCommunity).withName("My Collection").build();
        item = ItemBuilder.createItem(context, collection).withTitle("My Item").withAuthor("Test, Author")
                .withIssueDate("2020-10-31").build();
        context.commit();
        auditSolrService.commit();
        context.restoreAuthSystemState();
    }

    @After
    public void cleanAuditCore() {
        auditSolrService.deleteEvents(context, null, null);
        auditSolrService.commit();
        // this is required if the configuration is not present in the files
        configurationService.setProperty("audit.enabled", false);
    }

    @Test
    public void findAllTest() throws Exception {
        configurationService.setProperty("audit.enabled", true);
        loadSomeObjects();
        List<AuditEvent> events = auditSolrService.findAllEvents(context, Integer.MAX_VALUE, 0, false);
        assertTrue(events.size() > 0);
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/system/auditevents").param("size", "100")).andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.auditevents", Matchers.hasSize(Math.min(events.size(), 100))))
                // all the audit events must have received a uuid when stored
                .andExpect(jsonPath("$._embedded.auditevents.*.id", Matchers.not(Matchers.empty())))
                .andExpect(jsonPath("$.page.size", is(100)))
                .andExpect(jsonPath("$.page.totalElements", is(events.size())));
    }

    @Test
    public void findAllNotAdminTest() throws Exception {
        configurationService.setProperty("audit.enabled", true);
        loadSomeObjects();
        // anonymous cannot access the auditevents endpoint
        getClient().perform(get("/api/system/auditevents")).andExpect(status().isUnauthorized());
        // nor normal user
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/system/auditevents")).andExpect(status().isForbidden());
    }

    @Test
    public void findAllDisabledTest() throws Exception {
        configurationService.setProperty("audit.enabled", false);
        loadSomeObjects();
        List<AuditEvent> events = auditSolrService.findAllEvents(context, Integer.MAX_VALUE, 0, false);
        assertEquals(0, events.size());
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/system/auditevents")).andExpect(status().isNotFound());
    }

    @Test
    public void findAllPaginationTest() throws Exception {
        configurationService.setProperty("audit.enabled", true);
        loadSomeObjects();
        // enable now the audit system to have a predictable number of events
        context.turnOffAuthorisationSystem();
        AuditEvent audit = AuditEventBuilder.createAuditEvent(context).withEpersonUUID(eperson.getID())
                .withDetail("some information").withEventType("ADD").withSubject(collection).withObject(item).build();
        AuditEvent auditWithMissingEperson = AuditEventBuilder.createAuditEvent(context)
                .withEpersonUUID(UUID.randomUUID()).withDetail("some information").withEventType("MODIFY")
                .withSubject(item).build();
        AuditEvent auditWithMissingObject = AuditEventBuilder.createAuditEvent(context)
                .withEpersonUUID(UUID.randomUUID()).withDetail("some information").withEventType("MODIFY")
                .withSubject(UUID.randomUUID(), "ITEM").build();
        auditSolrService.commit();
        context.restoreAuthSystemState();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/system/auditevents")
                    .param("projection", "full")
                    .param("size", String.valueOf(TOTAL_ELEMENT)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.auditevents",
                        Matchers.hasItems(matchAuditEventFullProjection(audit, false, false, false))));

        getClient(adminToken).perform(get("/api/system/auditevents")
                .param("projection", "full")
                .param("size", String.valueOf(TOTAL_ELEMENT)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.auditevents",
                        Matchers.hasItems(matchAuditEvent(audit))));

        getClient(adminToken).perform(get("/api/system/auditevents")
                .param("projection", "full")
                .param("size", String.valueOf(TOTAL_ELEMENT)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.auditevents",
                        Matchers.hasItems(matchAuditEvent(auditWithMissingEperson))));

        getClient(adminToken).perform(get("/api/system/auditevents")
                .param("projection", "full")
                .param("size", String.valueOf(TOTAL_ELEMENT)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.auditevents",
                        Matchers.hasItems(matchAuditEvent(auditWithMissingObject))));

        getClient(adminToken).perform(get("/api/system/auditevents").param("size", "1")).andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(
                        jsonPath("$._links.self.href", Matchers.containsString("/api/system/auditevents")))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.allOf(Matchers.containsString("/api/system/auditevents?"),
                                Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(Matchers.containsString("/api/system/auditevents?"),
                                Matchers.containsString("page=" + (TOTAL_ELEMENT - 1)),
                                Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(Matchers.containsString("/api/system/auditevents?"),
                                Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.prev.href").doesNotExist())
                .andExpect(jsonPath("$.page.size", is(1)))
                .andExpect(jsonPath("$.page.totalElements", is(TOTAL_ELEMENT)));
        getClient(adminToken).perform(get("/api/system/auditevents").param("size", "1").param("page", "1"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(
                        jsonPath("$._links.self.href", Matchers.containsString("/api/system/auditevents")))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.allOf(Matchers.containsString("/api/system/auditevents?"),
                                Matchers.containsString("page=2"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(Matchers.containsString("/api/system/auditevents?"),
                                Matchers.containsString("page=" + (TOTAL_ELEMENT - 1)),
                                Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(Matchers.containsString("/api/system/auditevents?"),
                                Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.prev.href",
                        Matchers.allOf(Matchers.containsString("/api/system/auditevents?"),
                                Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$.page.size", is(1)))
                .andExpect(jsonPath("$.page.totalElements", is(TOTAL_ELEMENT)));
        getClient(adminToken).perform(get("/api/system/auditevents").param("size", "10").param("page", "2"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(
                        jsonPath("$._links.self.href", Matchers.containsString("/api/system/auditevents")))
                .andExpect(jsonPath("$._links.next.href").doesNotExist())
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(Matchers.containsString("/api/system/auditevents?"),
                                Matchers.containsString("page=1"), Matchers.containsString("size=10"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(Matchers.containsString("/api/system/auditevents?"),
                                Matchers.containsString("page=0"), Matchers.containsString("size=10"))))
                .andExpect(jsonPath("$._links.prev.href",
                        Matchers.allOf(Matchers.containsString("/api/system/auditevents?"),
                                Matchers.containsString("page=1"), Matchers.containsString("size=10"))))
                .andExpect(jsonPath("$.page.size", is(10)))
                .andExpect(jsonPath("$.page.totalElements", is(TOTAL_ELEMENT)));
    }

    @Test
    public void findOneTest() throws Exception {
        configurationService.setProperty("audit.enabled", true);
        loadSomeObjects();
        // enable now the audit system to have a predictable number of events
        context.turnOffAuthorisationSystem();
        AuditEvent audit = AuditEventBuilder.createAuditEvent(context).withEpersonUUID(eperson.getID())
                .withDetail("some information").withEventType("ADD").withSubject(collection).withObject(item).build();
        AuditEvent auditWithMissingEperson = AuditEventBuilder.createAuditEvent(context)
                .withEpersonUUID(UUID.randomUUID()).withDetail("some information").withEventType("REMOVE")
                .withSubject(collection).withObject(item).build();
        AuditEvent auditWithMissingObjectAndEperson = AuditEventBuilder.createAuditEvent(context)
                .withEpersonUUID(UUID.randomUUID()).withDetail("some information").withEventType("MODIFY")
                .withSubject(item).build();
        AuditEvent auditWithMissingObjectAndSubject = AuditEventBuilder.createAuditEvent(context)
                .withEpersonUUID(eperson.getID()).withDetail("some information").withEventType("ADD")
                .withSubject(UUID.randomUUID(), "COLLECTION").withObject(UUID.randomUUID(), "ITEM").build();
        auditSolrService.commit();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/system/auditevents/" + audit.getUuid().toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$", matchAuditEvent(audit)))
            .andExpect(jsonPath("$._links.self.href",
                    Matchers.endsWith("/api/system/auditevents/" + audit.getUuid().toString())));
        getClient(adminToken).perform(get("/api/system/auditevents/" + audit.getUuid().toString())
                    .param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", matchAuditEventFullProjection(audit, false, false, false)))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.endsWith("/api/system/auditevents/" + audit.getUuid().toString())));
        getClient(adminToken).perform(get("/api/system/auditevents/" + auditWithMissingEperson.getUuid().toString())
                    .param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", matchAuditEventFullProjection(auditWithMissingEperson, false, false, true)))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.endsWith("/api/system/auditevents/" + auditWithMissingEperson.getUuid().toString())));
        getClient(adminToken)
                .perform(get("/api/system/auditevents/" + auditWithMissingObjectAndEperson.getUuid().toString())
                    .param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$",
                        matchAuditEventFullProjection(auditWithMissingObjectAndEperson, false, true, true)))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.endsWith(
                                "/api/system/auditevents/" + auditWithMissingObjectAndEperson.getUuid().toString())));
        getClient(adminToken)
                .perform(get("/api/system/auditevents/" + auditWithMissingObjectAndSubject.getUuid().toString())
                    .param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$",
                        matchAuditEventFullProjection(auditWithMissingObjectAndSubject, true, true, false)))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.endsWith(
                                "/api/system/auditevents/" + auditWithMissingObjectAndSubject.getUuid().toString())));

    }

    @Test
    public void findOneNotAdminTest() throws Exception {
        configurationService.setProperty("audit.enabled", true);
        loadSomeObjects();
        // enable now the audit system to have a predictable number of events
        context.turnOffAuthorisationSystem();
        AuditEvent audit = AuditEventBuilder.createAuditEvent(context).withEpersonUUID(eperson.getID())
                .withDetail("some information").withEventType("ADD").withSubject(collection).withObject(item).build();
        auditSolrService.commit();
        context.restoreAuthSystemState();
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        String auditUUID = audit.getUuid().toString();
        getClient(epersonToken).perform(get("/api/system/auditevents/" + auditUUID))
                .andExpect(status().isForbidden());
        getClient(epersonToken).perform(get("/api/system/auditevents/" + UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
        getClient(epersonToken).perform(get("/api/system/auditevents/not-valid"))
                .andExpect(status().isForbidden());
        getClient().perform(get("/api/system/auditevents/" + auditUUID))
                .andExpect(status().isUnauthorized());
        getClient().perform(get("/api/system/auditevents/not-valid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findByObjectTest() throws Exception {
        configurationService.setProperty("audit.enabled", true);
        loadSomeObjects();
        List<AuditEvent> events = auditSolrService.findEvents(item.getID(), null, null, Integer.MAX_VALUE, 0,
                false);
        assertTrue(events.size() > 0);
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/system/auditevents/search/findByObject")
                    .param("size", "100")
                    .param("object", item.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.auditevents", Matchers.hasSize(Math.min(events.size(), 100))))
                // all the audit events must have received a uuid when stored
                .andExpect(jsonPath("$._embedded.auditevents.*.id", Matchers.not(Matchers.empty())))
                // all the audit events must be related to the item
                .andExpect(jsonPath("$._embedded.auditevents",
                        Matchers.everyItem(Matchers.anyOf(
                                hasJsonPath("$.subjectUUID", is(item.getID().toString())),
                                hasJsonPath("$.objectUUID", is(item.getID().toString()))
                        ))))
                .andExpect(jsonPath("$.page.size", is(100)))
                .andExpect(jsonPath("$.page.totalElements", is(events.size())));
    }

    @Test
    public void findByObjectNotAdminTest() throws Exception {
        configurationService.setProperty("audit.enabled", true);
        loadSomeObjects();
        // anonymous cannot access the auditevents endpoint
        getClient().perform(get("/api/system/auditevents/search/findByObject")
                        .param("object", item.getID().toString()))
                .andExpect(status().isUnauthorized());
        // nor normal user
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/system/auditevents/search/findByObject")
                .param("object", item.getID().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findByObjectDisabledTest() throws Exception {
        configurationService.setProperty("audit.enabled", false);
        loadSomeObjects();
        List<AuditEvent> events = auditSolrService.findAllEvents(context, Integer.MAX_VALUE, 0, false);
        assertEquals(0, events.size());
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/system/auditevents/search/findByObject")
                .param("object", item.getID().toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    public void findByObjectPaginationTest() throws Exception {
        configurationService.setProperty("audit.enabled", true);
        loadSomeObjects();
        // enable now the audit system to have a predictable number of events
        context.turnOffAuthorisationSystem();
        AuditEvent audit = AuditEventBuilder.createAuditEvent(context).withEpersonUUID(eperson.getID())
                .withDetail("some information").withEventType("ADD").withSubject(collection).withObject(item).build();
        AuditEvent auditWithMissingEperson = AuditEventBuilder.createAuditEvent(context)
                .withEpersonUUID(UUID.randomUUID()).withDetail("some information").withEventType("MODIFY")
                .withSubject(item).build();
        AuditEvent auditWithMissingObject = AuditEventBuilder.createAuditEvent(context)
                .withEpersonUUID(UUID.randomUUID()).withDetail("some information").withEventType("MODIFY")
                .withSubject(UUID.randomUUID(), "ITEM").build();
        auditSolrService.commit();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/system/auditevents/search/findByObject")
                .param("object", item.getID().toString())
                .param("size", String.valueOf(TOTAL_ELEMENT)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.auditevents",
                Matchers.hasItems(matchAuditEvent(audit))));

        getClient(adminToken).perform(get("/api/system/auditevents/search/findByObject")
                .param("size", "1")
                .param("object", item.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(
                        jsonPath("$._links.self.href",
                                Matchers.containsString("/api/system/auditevents/search/findByObject")))
            .andExpect(jsonPath("$._links.next.href",
                    Matchers.allOf(
                            Matchers.containsString("/api/system/auditevents/search/findByObject?"),
                            Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
            .andExpect(jsonPath("$._links.last.href",
                    Matchers.allOf(
                            Matchers.containsString("/api/system/auditevents/search/findByObject?"),
                            Matchers.containsString("page=8"), Matchers.containsString("size=1"))))
            .andExpect(jsonPath("$._links.first.href",
                    Matchers.allOf(
                            Matchers.containsString("/api/system/auditevents/search/findByObject?"),
                            Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
            .andExpect(jsonPath("$._links.prev.href").doesNotExist())
            .andExpect(jsonPath("$.page.size", is(1)))
            .andExpect(jsonPath("$.page.totalElements", is(9)));

        getClient(adminToken).perform(get("/api/system/auditevents/search/findByObject")
                .param("object", item.getID().toString())
                .param("size", String.valueOf(TOTAL_ELEMENT)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.auditevents",
                Matchers.hasItems(matchAuditEvent(auditWithMissingEperson))));

        getClient(adminToken).perform(get("/api/system/auditevents/search/findByObject")
                .param("size", "1").param("page", "1")
                .param("object", item.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._links.self.href",
                                Matchers.containsString("/api/system/auditevents/search/findByObject")))
            .andExpect(jsonPath("$._links.prev.href",
                    Matchers.allOf(
                            Matchers.containsString("/api/system/auditevents/search/findByObject?"),
                            Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
            .andExpect(jsonPath("$._links.last.href",
                    Matchers.allOf(
                            Matchers.containsString("/api/system/auditevents/search/findByObject?"),
                            Matchers.containsString("page=8"), Matchers.containsString("size=1"))))
            .andExpect(jsonPath("$._links.first.href",
                    Matchers.allOf(
                            Matchers.containsString("/api/system/auditevents/search/findByObject?"),
                            Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
            .andExpect(jsonPath("$.page.size", is(1)))
            .andExpect(jsonPath("$.page.totalElements", is(9)));
    }

    @Test
    public void findByObjectBitstreamTest() throws Exception {
        configurationService.setProperty("audit.enabled", true);
        loadSomeObjects();
        context.turnOffAuthorisationSystem();
        Bitstream bitstream = BitstreamBuilder.createBitstream(context, item, InputStream.nullInputStream())
                                              .withName("test image")
                                              .withFormat("test format type")
                                              .build();

        bitstreamService.delete(context, bitstream);
        context.commit();
        auditSolrService.commit();
        context.restoreAuthSystemState();

        List<AuditEvent> events = auditSolrService.findEvents(bitstream.getID(), null, null, Integer.MAX_VALUE, 0,
            false);
        assertTrue(events.size() > 4);
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/system/auditevents/search/findByObject")
                                 .param("size", "100")
                                 .param("object", bitstream.getID().toString()))
                             .andExpect(status().isOk())
                             .andExpect(content().contentType(contentType))
                             .andExpect(jsonPath("$._embedded.auditevents",
                                 Matchers.hasSize(Math.min(events.size(), 100))))
                             // all the audit events must have received a uuid when stored
                             .andExpect(jsonPath("$._embedded.auditevents.*.id", Matchers.not(Matchers.empty())))
                             // all the audit events must be related to the bitstream
                             .andExpect(jsonPath("$._embedded.auditevents",
                                 Matchers.everyItem(Matchers.anyOf(
                                     hasJsonPath("$.subjectUUID", is(bitstream.getID().toString())),
                                     hasJsonPath("$.objectUUID", is(bitstream.getID().toString()))
                                 ))))
                             .andExpect(jsonPath("$._embedded.auditevents",
                                 Matchers.containsInAnyOrder(
                                     events.stream()
                                           .map(event ->
                                               matchAuditEvent(event)).collect(
                                               Collectors.toList())
                                 )))
                             .andExpect(jsonPath("$._embedded.auditevents",
                                 hasItems(
                                     allOf(
                                         hasJsonPath("$.checksum", is(bitstream.getChecksum())),
                                         hasJsonPath("$.eventType", is("CREATE"))
                                     ),
                                     allOf(
                                         hasJsonPath("$.checksum", is(bitstream.getChecksum())),
                                         hasJsonPath("$.eventType", is("DELETE"))
                                     ))))
                             .andExpect(jsonPath("$.page.size", is(100)))
                             .andExpect(jsonPath("$.page.totalElements", is(events.size())));
    }

}