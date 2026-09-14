/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.time.Period;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.dspace.access.status.AccessStatusServiceImpl;
import org.dspace.access.status.service.AccessStatusService;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

public class ItemOrderedAccessStatusRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AccessStatusService accessStatusService;

    @Before
    public void setup() throws Exception {
        configurationService.setProperty(
            "plugin.single.org.dspace.access.status.AccessStatusHelper",
            "org.dspace.access.status.OrderedAccessStatusHelper"
        );
        // This is needed because the AccessStatusHelper is a plugin that is created and set in the
        // AccessStatusServiceImpl.init method. AccessStatusService is a spring managed bean, and the context has
        // already been created by the time the test method runs.
        ReflectionTestUtils.setField(accessStatusService, "helper", null);
        ((AccessStatusServiceImpl) accessStatusService).init();
    }

    @After
    public void reset() throws Exception {
        configurationService.setProperty(
            "plugin.single.org.dspace.access.status.AccessStatusHelper",
            "org.dspace.access.status.DefaultAccessStatusHelper"
        );
        // This is needed because the AccessStatusHelper is a plugin that is created and set in the
        // AccessStatusServiceImpl.init method. AccessStatusService is a spring managed bean, and the context has
        // already been created by the time the test method runs.
        ReflectionTestUtils.setField(accessStatusService, "helper", null);
        ((AccessStatusServiceImpl) accessStatusService).init();
    }

    @Test
    public void findAccessStatusForItemBadRequestTest() throws Exception {
        getClient().perform(get("/api/core/items/{uuid}/accessStatus", "1"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void findAccessStatusForItemNotFoundTest() throws Exception {
        UUID fakeUUID = UUID.randomUUID();
        getClient().perform(get("/api/core/items/{uuid}/accessStatus", fakeUUID))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void findAccessStatusWithMetatdataOnlyForItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection owningCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                       .withName("Owning Collection")
                                                       .build();
        Item item = ItemBuilder.createItem(context, owningCollection)
                               .withTitle("Test item")
                               .build();
        context.restoreAuthSystemState();
        getClient().perform(get("/api/core/items/{uuid}/accessStatus", item.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.status", is("metadata.only")))
                   .andExpect(jsonPath("$.embargoDate", nullValue()));
    }

    @Test
    public void findAccessStatusWithOpenAccessForItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection owningCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Owning Collection")
            .build();
        Item item = ItemBuilder.createItem(context, owningCollection)
            .withTitle("Test item")
            .build();
        Bundle originalBundle = BundleBuilder.createBundle(context, item)
            .withName(Constants.DEFAULT_BUNDLE_NAME)
            .build();
        InputStream is = IOUtils.toInputStream("openaccesstext", "utf-8");
        BitstreamBuilder.createBitstream(context, originalBundle, is)
            .withName("test.pdf")
            .withMimeType("application/pdf")
            .build();
        context.restoreAuthSystemState();
        getClient().perform(get("/api/core/items/{uuid}/accessStatus", item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("open.access")))
            .andExpect(jsonPath("$.embargoDate", nullValue()));
    }

    @Test
    public void findAccessStatusWithEmbargoForItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection owningCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                       .withName("Owning Collection")
                                                       .build();
        Item item = ItemBuilder.createItem(context, owningCollection)
                               .withTitle("Test item")
                               .build();
        Bundle originalBundle = BundleBuilder.createBundle(context, item)
                                             .withName(Constants.DEFAULT_BUNDLE_NAME)
                                             .build();
        InputStream is = IOUtils.toInputStream("embargoedtext", "utf-8");
        BitstreamBuilder.createBitstream(context, originalBundle, is)
                                              .withName("test.pdf")
                                              .withMimeType("application/pdf")
                                              .withEmbargoPeriod(Period.ofMonths(6))
                                              .build();
        context.restoreAuthSystemState();
        getClient().perform(get("/api/core/items/{uuid}/accessStatus", item.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.status", is("embargo")))
                   .andExpect(jsonPath("$.embargoDate", notNullValue()));
    }

    @Test
    public void findAccessStatusWithRestrictedForItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection owningCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Owning Collection")
            .build();
        Item item = ItemBuilder.createItem(context, owningCollection)
            .withTitle("Test item")
            .build();
        Bundle originalBundle = BundleBuilder.createBundle(context, item)
            .withName(Constants.DEFAULT_BUNDLE_NAME)
            .build();
        GroupBuilder internalGroupBuilder = GroupBuilder.createGroup(context)
            .withName("Internal Group");
        Group internalGroup = internalGroupBuilder.build();
        InputStream is2 = IOUtils.toInputStream("restrictedtext", "utf-8");
        BitstreamBuilder.createBitstream(context, originalBundle, is2)
            .withName("test-restricted.pdf")
            .withMimeType("application/pdf")
            .withReaderGroup(internalGroup)
            .build();
        context.restoreAuthSystemState();
        getClient().perform(get("/api/core/items/{uuid}/accessStatus", item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("restricted")))
            .andExpect(jsonPath("$.embargoDate", nullValue()));
    }

    @Test
    public void findAccessStatusWithEmbargoAndOpenAccessForItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection owningCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Owning Collection")
            .build();
        Item item = ItemBuilder.createItem(context, owningCollection)
            .withTitle("Test item")
            .build();
        Bundle originalBundle = BundleBuilder.createBundle(context, item)
            .withName(Constants.DEFAULT_BUNDLE_NAME)
            .build();
        InputStream is1 = IOUtils.toInputStream("openaccesstext", "utf-8");
        BitstreamBuilder.createBitstream(context, originalBundle, is1)
            .withName("test-open.pdf")
            .withMimeType("application/pdf")
            .build();
        InputStream is2 = IOUtils.toInputStream("embargoedtext", "utf-8");
        BitstreamBuilder.createBitstream(context, originalBundle, is2)
            .withName("test-embargo.pdf")
            .withMimeType("application/pdf")
            .withEmbargoPeriod(Period.ofMonths(6))
            .build();
        context.restoreAuthSystemState();
        getClient().perform(get("/api/core/items/{uuid}/accessStatus", item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("embargo")))
            .andExpect(jsonPath("$.embargoDate", notNullValue()));
    }

    @Test
    public void findAccessStatusWithRestrictedAndOpenAccessForItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection owningCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Owning Collection")
            .build();
        Item item = ItemBuilder.createItem(context, owningCollection)
            .withTitle("Test item")
            .build();
        Bundle originalBundle = BundleBuilder.createBundle(context, item)
            .withName(Constants.DEFAULT_BUNDLE_NAME)
            .build();
        InputStream is1 = IOUtils.toInputStream("openaccesstext", "utf-8");
        BitstreamBuilder.createBitstream(context, originalBundle, is1)
            .withName("test-open.pdf")
            .withMimeType("application/pdf")
            .build();
        GroupBuilder internalGroupBuilder = GroupBuilder.createGroup(context)
            .withName("Internal Group");
        Group internalGroup = internalGroupBuilder.build();
        InputStream is2 = IOUtils.toInputStream("restrictedtext", "utf-8");
        BitstreamBuilder.createBitstream(context, originalBundle, is2)
            .withName("test-restricted.pdf")
            .withMimeType("application/pdf")
            .withReaderGroup(internalGroup)
            .build();
        context.restoreAuthSystemState();
        getClient().perform(get("/api/core/items/{uuid}/accessStatus", item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("restricted")))
            .andExpect(jsonPath("$.embargoDate", nullValue()));
    }

    @Test
    public void findAccessStatusWithEmbargoAndRestrictedAndOpenAccessForItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection owningCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Owning Collection")
            .build();
        Item item = ItemBuilder.createItem(context, owningCollection)
            .withTitle("Test item")
            .build();
        Bundle originalBundle = BundleBuilder.createBundle(context, item)
            .withName(Constants.DEFAULT_BUNDLE_NAME)
            .build();
        InputStream is1 = IOUtils.toInputStream("openaccesstext", "utf-8");
        BitstreamBuilder.createBitstream(context, originalBundle, is1)
            .withName("test-open.pdf")
            .withMimeType("application/pdf")
            .build();
        GroupBuilder internalGroupBuilder = GroupBuilder.createGroup(context)
            .withName("Internal Group");
        Group internalGroup = internalGroupBuilder.build();
        InputStream is2 = IOUtils.toInputStream("restrictedtext", "utf-8");
        BitstreamBuilder.createBitstream(context, originalBundle, is2)
            .withName("test-restricted.pdf")
            .withMimeType("application/pdf")
            .withReaderGroup(internalGroup)
            .build();
        InputStream is3 = IOUtils.toInputStream("embargoedtext", "utf-8");
        BitstreamBuilder.createBitstream(context, originalBundle, is3)
            .withName("test-embargo.pdf")
            .withMimeType("application/pdf")
            .withEmbargoPeriod(Period.ofMonths(6))
            .build();
        context.restoreAuthSystemState();
        getClient().perform(get("/api/core/items/{uuid}/accessStatus", item.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("embargo")))
            .andExpect(jsonPath("$.embargoDate", notNullValue()));
    }

}
