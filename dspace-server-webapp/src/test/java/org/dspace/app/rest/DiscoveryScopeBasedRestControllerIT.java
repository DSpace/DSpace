/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.FacetEntryMatcher;
import org.dspace.app.rest.matcher.FacetValueMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.MetadataFieldBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class tests the correct inheritance of Discovery configurations for sub communities and collections.
 * To thoroughly test this, a community and collection structure is set up to where different communities have custom
 * configurations configured for them.
 * 
 * The following structure is uses:
 * - Parent Community 1     - Custom configuration: discovery-parent-community-1
 *  -- Subcommunity 11      - Custom configuration: discovery-sub-community-1-1
 *      -- Collection 111   - Custom configuration: discovery-collection-1-1-1
 *      -- Collection 112
 *  -- Subcommunity 12
 *      -- Collection 121   - Custom configuration: discovery-collection-1-2-1
 *      -- Collection 122
 * - Parent Community 2
 *  -- Subcommunity 21      - Custom configuration: discovery-sub-community-2-1
 *      -- Collection 211   - Custom configuration: discovery-collection-2-1-1
 *      -- Collection 212
 *  -- Subcommunity 22
 *      -- Collection 221   - Custom configuration: discovery-collection-2-2-1
 *      -- Collection 222
 *
 * Each custom configuration contains a unique index for a unique metadata field, to verify if correct information is
 * indexed and provided for the different search scopes.
 *
 * Each collection has an item in it. Next to these items, there are two mapped items, one in collection 111 and 222,
 * and one in collection 122 and 211.
 *
 * The tests will verify that for each object, the correct facets are provided and that all the necessary fields to
 * power these facets are indexed properly.
 *
 * This file requires the discovery configuration in the following test file:
 * src/test/data/dspaceFolder/config/spring/api/test-discovery.xml
 */
public class DiscoveryScopeBasedRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    CollectionService collectionService;

    private Community parentCommunity1;
    private Community subcommunity11;
    private Community subcommunity12;
    private Collection collection111;
    private Collection collection112;
    private Collection collection121;
    private Collection collection122;

    private Community parentCommunity2;
    private Community subcommunity21;
    private Community subcommunity22;
    private Collection collection211;
    private Collection collection212;
    private Collection collection221;
    private Collection collection222;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        MetadataFieldBuilder.createMetadataField(context, "test", "parentcommunity1field", "").build();
        MetadataFieldBuilder.createMetadataField(context, "test", "subcommunity11field", "").build();
        MetadataFieldBuilder.createMetadataField(context, "test", "collection111field", "").build();
        MetadataFieldBuilder.createMetadataField(context, "test", "collection121field", "").build();
        MetadataFieldBuilder.createMetadataField(context, "test", "subcommunity21field", "").build();
        MetadataFieldBuilder.createMetadataField(context, "test", "collection211field", "").build();
        MetadataFieldBuilder.createMetadataField(context, "test", "collection221field", "").build();

        parentCommunity1 = CommunityBuilder.createCommunity(context, "123456789/discovery-parent-community-1")
                                           .build();
        subcommunity11 = CommunityBuilder
                .createSubCommunity(context, parentCommunity1, "123456789/discovery-sub-community-1-1")
                .build();
        subcommunity12 = CommunityBuilder
                .createSubCommunity(context, parentCommunity1, "123456789/discovery-sub-community-1-2")
                .build();
        collection111 = CollectionBuilder
                .createCollection(context, subcommunity11, "123456789/discovery-collection-1-1-1")
                .build();
        collection112 = CollectionBuilder
                .createCollection(context, subcommunity11, "123456789/discovery-collection-1-1-2")
                .build();
        collection121 = CollectionBuilder
                .createCollection(context, subcommunity12, "123456789/discovery-collection-1-2-1")
                .build();

        collection122 = CollectionBuilder
                .createCollection(context, subcommunity12, "123456789/discovery-collection-1-2-2")
                .build();

        parentCommunity2 = CommunityBuilder.createCommunity(context, "123456789/discovery-parent-community-2")
                                           .build();


        subcommunity21 = CommunityBuilder
                .createSubCommunity(context, parentCommunity2, "123456789/discovery-sub-community-2-1")
                .build();
        subcommunity22 = CommunityBuilder
                .createSubCommunity(context, parentCommunity2, "123456789/discovery-sub-community-2-2")
                .build();
        collection211 = CollectionBuilder
                .createCollection(context, subcommunity21, "123456789/discovery-collection-2-1-1")
                .build();
        collection212 = CollectionBuilder
                .createCollection(context, subcommunity21, "123456789/discovery-collection-2-1-2")
                .build();
        collection221 = CollectionBuilder
                .createCollection(context, subcommunity22, "123456789/discovery-collection-2-2-1")
                .build();
        collection222 = CollectionBuilder
                .createCollection(context, subcommunity22, "123456789/discovery-collection-2-2-2")
                .build();


        Item item111 = ItemBuilder.createItem(context, collection111)
                                  .withMetadata("dc", "contributor", "author", "author-item111")
                                  .withMetadata("dc", "test", "parentcommunity1field", "parentcommunity1field-item111")
                                  .withMetadata("dc", "test", "subcommunity11field", "subcommunity11field-item111")
                                  .withMetadata("dc", "test", "collection111field", "collection111field-item111")
                                  .withMetadata("dc", "test", "collection121field", "collection121field-item111")
                                  .withMetadata("dc", "test", "subcommunity21field", "subcommunity21field-item111")
                                  .withMetadata("dc", "test", "collection211field", "collection211field-item111")
                                  .withMetadata("dc", "test", "collection221field", "collection221field-item111")
                                  .build();

        Item item112 = ItemBuilder.createItem(context, collection112)
                                  .withMetadata("dc", "contributor", "author", "author-item112")
                                  .withMetadata("dc", "test", "parentcommunity1field", "parentcommunity1field-item112")
                                  .withMetadata("dc", "test", "subcommunity11field", "subcommunity11field-item112")
                                  .withMetadata("dc", "test", "collection111field", "collection111field-item112")
                                  .withMetadata("dc", "test", "collection121field", "collection121field-item112")
                                  .withMetadata("dc", "test", "subcommunity21field", "subcommunity21field-item112")
                                  .withMetadata("dc", "test", "collection211field", "collection211field-item112")
                                  .withMetadata("dc", "test", "collection221field", "collection221field-item112")
                                  .build();

        Item item121 = ItemBuilder.createItem(context, collection121)
                                  .withMetadata("dc", "contributor", "author", "author-item121")
                                  .withMetadata("dc", "test", "parentcommunity1field", "parentcommunity1field-item121")
                                  .withMetadata("dc", "test", "subcommunity11field", "subcommunity11field-item121")
                                  .withMetadata("dc", "test", "collection111field", "collection111field-item121")
                                  .withMetadata("dc", "test", "collection121field", "collection121field-item121")
                                  .withMetadata("dc", "test", "subcommunity21field", "subcommunity21field-item121")
                                  .withMetadata("dc", "test", "collection211field", "collection211field-item121")
                                  .withMetadata("dc", "test", "collection221field", "collection221field-item121")
                                  .build();

        Item item122 = ItemBuilder.createItem(context, collection122)
                                  .withMetadata("dc", "contributor", "author", "author-item122")
                                  .withMetadata("dc", "test", "parentcommunity1field", "parentcommunity1field-item122")
                                  .withMetadata("dc", "test", "subcommunity11field", "subcommunity11field-item122")
                                  .withMetadata("dc", "test", "collection111field", "collection111field-item122")
                                  .withMetadata("dc", "test", "collection121field", "collection121field-item122")
                                  .withMetadata("dc", "test", "subcommunity21field", "subcommunity21field-item122")
                                  .withMetadata("dc", "test", "collection211field", "collection211field-item122")
                                  .withMetadata("dc", "test", "collection221field", "collection221field-item122")
                                  .build();

        Item item211 = ItemBuilder.createItem(context, collection211)
                                  .withMetadata("dc", "contributor", "author", "author-item211")
                                  .withMetadata("dc", "test", "parentcommunity1field", "parentcommunity1field-item211")
                                  .withMetadata("dc", "test", "subcommunity11field", "subcommunity11field-item211")
                                  .withMetadata("dc", "test", "collection111field", "collection111field-item211")
                                  .withMetadata("dc", "test", "collection121field", "collection121field-item211")
                                  .withMetadata("dc", "test", "subcommunity21field", "subcommunity21field-item211")
                                  .withMetadata("dc", "test", "collection211field", "collection211field-item211")
                                  .withMetadata("dc", "test", "collection221field", "collection221field-item211")
                                  .build();

        Item item212 = ItemBuilder.createItem(context, collection212)
                                  .withMetadata("dc", "contributor", "author", "author-item212")
                                  .withMetadata("dc", "test", "parentcommunity1field", "parentcommunity1field-item212")
                                  .withMetadata("dc", "test", "subcommunity11field", "subcommunity11field-item212")
                                  .withMetadata("dc", "test", "collection111field", "collection111field-item212")
                                  .withMetadata("dc", "test", "collection121field", "collection121field-item212")
                                  .withMetadata("dc", "test", "subcommunity21field", "subcommunity21field-item212")
                                  .withMetadata("dc", "test", "collection211field", "collection211field-item212")
                                  .withMetadata("dc", "test", "collection221field", "collection221field-item212")
                                  .build();

        Item item221 = ItemBuilder.createItem(context, collection221)
                                  .withMetadata("dc", "contributor", "author", "author-item221")
                                  .withMetadata("dc", "test", "parentcommunity1field", "parentcommunity1field-item221")
                                  .withMetadata("dc", "test", "subcommunity11field", "subcommunity11field-item221")
                                  .withMetadata("dc", "test", "collection111field", "collection111field-item221")
                                  .withMetadata("dc", "test", "collection121field", "collection121field-item221")
                                  .withMetadata("dc", "test", "subcommunity21field", "subcommunity21field-item221")
                                  .withMetadata("dc", "test", "collection211field", "collection211field-item221")
                                  .withMetadata("dc", "test", "collection221field", "collection221field-item221")
                                  .build();

        Item item222 = ItemBuilder.createItem(context, collection222)
                                  .withMetadata("dc", "contributor", "author", "author-item222")
                                  .withMetadata("dc", "test", "parentcommunity1field", "parentcommunity1field-item222")
                                  .withMetadata("dc", "test", "subcommunity11field", "subcommunity11field-item222")
                                  .withMetadata("dc", "test", "collection111field", "collection111field-item222")
                                  .withMetadata("dc", "test", "collection121field", "collection121field-item222")
                                  .withMetadata("dc", "test", "subcommunity21field", "subcommunity21field-item222")
                                  .withMetadata("dc", "test", "collection211field", "collection211field-item222")
                                  .withMetadata("dc", "test", "collection221field", "collection221field-item222")
                                  .build();

        Item mappedItem111222 = ItemBuilder
                .createItem(context, collection111)
                .withMetadata("dc", "contributor", "author", "author-mappedItem111222")
                .withMetadata("dc", "test", "parentcommunity1field", "parentcommunity1field-mappedItem111222")
                .withMetadata("dc", "test", "subcommunity11field", "subcommunity11field-mappedItem111222")
                .withMetadata("dc", "test", "collection111field", "collection111field-mappedItem111222")
                .withMetadata("dc", "test", "collection121field", "collection121field-mappedItem111222")
                .withMetadata("dc", "test", "subcommunity21field", "subcommunity21field-mappedItem111222")
                .withMetadata("dc", "test", "collection211field", "collection211field-mappedItem111222")
                .withMetadata("dc", "test", "collection221field", "collection221field-mappedItem111222")
                .build();


        Item mappedItem122211 = ItemBuilder
                .createItem(context, collection122)
                .withMetadata("dc", "contributor", "author", "author-mappedItem122211")
                .withMetadata("dc", "test", "parentcommunity1field", "parentcommunity1field-mappedItem122211")
                .withMetadata("dc", "test", "subcommunity11field", "subcommunity11field-mappedItem122211")
                .withMetadata("dc", "test", "collection111field", "collection111field-mappedItem122211")
                .withMetadata("dc", "test", "collection121field", "collection121field-mappedItem122211")
                .withMetadata("dc", "test", "subcommunity21field", "subcommunity21field-mappedItem122211")
                .withMetadata("dc", "test", "collection211field", "collection211field-mappedItem122211")
                .withMetadata("dc", "test", "collection221field", "collection221field-mappedItem122211")
                .build();


        collectionService.addItem(context, collection222, mappedItem111222);
        collectionService.addItem(context, collection211, mappedItem122211);


        context.dispatchEvents();
        context.restoreAuthSystemState();
    }

    @Test
    /**
     *  Verify that the custom configuration "discovery-parent-community-1" is correctly used for Parent Community 1.
     */
    public void ScopeBasedIndexingAndSearchTestParentCommunity1() throws Exception {

        getClient().perform(get("/api/discover/facets").param("scope", String.valueOf(parentCommunity1.getID())))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
                   .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(
                           FacetEntryMatcher.authorFacet(false),
                           FacetEntryMatcher.matchFacet(false, "parentcommunity1field", "text")))
                   );

        getClient().perform(get("/api/discover/facets/parentcommunity1field")
                                    .param("scope", String.valueOf(parentCommunity1.getID())))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._embedded.values",
                                       containsInAnyOrder(
                                               FacetValueMatcher.matchEntry("parentcommunity1field",
                                                                            "parentcommunity1field-item111", 1),
                                               FacetValueMatcher.matchEntry("parentcommunity1field",
                                                                            "parentcommunity1field-item112", 1),
                                               FacetValueMatcher.matchEntry("parentcommunity1field",
                                                                            "parentcommunity1field-item121", 1),
                                               FacetValueMatcher.matchEntry("parentcommunity1field",
                                                                            "parentcommunity1field-item122", 1),
                                               FacetValueMatcher.matchEntry("parentcommunity1field",
                                                                            "parentcommunity1field-mappedItem111222",
                                                                            1),
                                               FacetValueMatcher.matchEntry("parentcommunity1field",
                                                                            "parentcommunity1field-mappedItem122211", 1)
                                       )
                   ));


    }

    @Test
    /**
     *  Verify that the custom configuration "discovery-sub-community-1-1" is correctly used for Subcommunity 11.
     */
    public void ScopeBasedIndexingAndSearchTestSubCommunity11() throws Exception {

        getClient().perform(get("/api/discover/facets").param("scope", String.valueOf(subcommunity11.getID())))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
                   .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(
                           FacetEntryMatcher.authorFacet(false),
                           FacetEntryMatcher.matchFacet(false, "subcommunity11field", "text")))
                   );

        getClient().perform(get("/api/discover/facets/subcommunity11field")
                                    .param("scope", String.valueOf(subcommunity11.getID())))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._embedded.values",
                                       containsInAnyOrder(
                                               FacetValueMatcher.matchEntry("subcommunity11field",
                                                                            "subcommunity11field-item111", 1),
                                               FacetValueMatcher.matchEntry("subcommunity11field",
                                                                            "subcommunity11field-item112", 1),
                                               FacetValueMatcher.matchEntry("subcommunity11field",
                                                                            "subcommunity11field-mappedItem111222", 1)
                                       )
                   ));
    }

    @Test
    /**
     *  Verify that the custom configuration "discovery-collection-1-1-1" is correctly used for Collection 111.
     */
    public void ScopeBasedIndexingAndSearchTestCollection111() throws Exception {

        getClient().perform(get("/api/discover/facets").param("scope", String.valueOf(collection111.getID())))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
                   .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(
                           FacetEntryMatcher.authorFacet(false),
                           FacetEntryMatcher.matchFacet(false, "collection111field", "text")))
                   );

        getClient().perform(get("/api/discover/facets/collection111field")
                                    .param("scope", String.valueOf(collection111.getID())))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._embedded.values",
                                       containsInAnyOrder(
                                               FacetValueMatcher.matchEntry("collection111field",
                                                                            "collection111field-item111", 1),
                                               FacetValueMatcher.matchEntry("collection111field",
                                                                            "collection111field-mappedItem111222", 1)
                                       )
                   ));
    }

    @Test
    /**
     *  Verify that the first encountered custom parent configuration "discovery-sub-community-1-1" is inherited
     *  correctly for Collection 112.
     */
    public void ScopeBasedIndexingAndSearchTestCollection112() throws Exception {

        getClient().perform(get("/api/discover/facets").param("scope", String.valueOf(collection112.getID())))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
                   .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(
                           FacetEntryMatcher.authorFacet(false),
                           FacetEntryMatcher.matchFacet(false, "subcommunity11field", "text")))
                   );

        getClient().perform(get("/api/discover/facets/subcommunity11field")
                                    .param("scope", String.valueOf(collection112.getID())))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._embedded.values",
                                       containsInAnyOrder(
                                               FacetValueMatcher.matchEntry("subcommunity11field",
                                                                            "subcommunity11field-item112", 1)
                                       )
                   ));
    }

    @Test
    /**
     *  Verify that the first encountered custom parent configuration "discovery-parent-community-1" is inherited
     *  correctly for Subcommunity 12.
     */
    public void ScopeBasedIndexingAndSearchTestSubcommunity12() throws Exception {

        getClient().perform(get("/api/discover/facets").param("scope", String.valueOf(subcommunity12.getID())))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
                   .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(
                           FacetEntryMatcher.authorFacet(false),
                           FacetEntryMatcher.matchFacet(false, "parentcommunity1field", "text")))
                   );

        getClient().perform(get("/api/discover/facets/parentcommunity1field")
                                    .param("scope", String.valueOf(subcommunity12.getID())))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._embedded.values",
                                       containsInAnyOrder(
                                               FacetValueMatcher.matchEntry("parentcommunity1field",
                                                                            "parentcommunity1field-item121", 1),
                                               FacetValueMatcher.matchEntry("parentcommunity1field",
                                                                            "parentcommunity1field-item122", 1),
                                               FacetValueMatcher.matchEntry("parentcommunity1field",
                                                                            "parentcommunity1field-mappedItem122211", 1)
                                       )
                   ));
    }

    @Test
    /**
     *  Verify that the custom configuration "discovery-collection-1-2-1" is correctly used for Collection 121.
     */
    public void ScopeBasedIndexingAndSearchTestCollection121() throws Exception {

        getClient().perform(get("/api/discover/facets").param("scope", String.valueOf(collection121.getID())))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
                   .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(
                           FacetEntryMatcher.authorFacet(false),
                           FacetEntryMatcher.matchFacet(false, "collection121field", "text")))
                   );

        getClient().perform(get("/api/discover/facets/collection121field")
                                    .param("scope", String.valueOf(collection121.getID())))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._embedded.values",
                                       containsInAnyOrder(
                                               FacetValueMatcher.matchEntry("collection121field",
                                                                            "collection121field-item121", 1)
                                       )
                   ));
    }

    @Test
    /**
     *  Verify that the first encountered custom parent configuration "discovery-parent-community-1" is inherited
     *  correctly for Collection 122.
     */
    public void ScopeBasedIndexingAndSearchTestCollection122() throws Exception {

        getClient().perform(get("/api/discover/facets").param("scope", String.valueOf(collection122.getID())))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
                   .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(
                           FacetEntryMatcher.authorFacet(false),
                           FacetEntryMatcher.matchFacet(false, "parentcommunity1field", "text")))
                   );

        getClient().perform(get("/api/discover/facets/parentcommunity1field")
                                    .param("scope", String.valueOf(collection122.getID())))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._embedded.values",
                                       containsInAnyOrder(
                                               FacetValueMatcher.matchEntry("parentcommunity1field",
                                                                            "parentcommunity1field-item122", 1),
                                               FacetValueMatcher.matchEntry("parentcommunity1field",
                                                                            "parentcommunity1field-mappedItem122211", 1)
                                       )
                   ));
    }

    @Test
    /**
     *  Verify that the default configuration is inherited correctly when no other custom configuration can be inherited
     *  for Parent Community 2.
     */
    public void ScopeBasedIndexingAndSearchTestParentCommunity2() throws Exception {

        getClient().perform(get("/api/discover/facets").param("scope", String.valueOf(parentCommunity2.getID())))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
                   .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(
                                      FacetEntryMatcher.authorFacet(false),
                                      FacetEntryMatcher.subjectFacet(false),
                                      FacetEntryMatcher.dateIssuedFacet(false),
                                      FacetEntryMatcher.hasContentInOriginalBundleFacet(false),
                                      FacetEntryMatcher.entityTypeFacet(false)
                              ))
                   );
    }

    @Test
    /**
     *  Verify that the custom configuration "discovery-sub-community-2-1" is correctly used for Subcommunity 21.
     */
    public void ScopeBasedIndexingAndSearchTestSubCommunity21() throws Exception {

        getClient().perform(get("/api/discover/facets").param("scope", String.valueOf(subcommunity21.getID())))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
                   .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(
                           FacetEntryMatcher.authorFacet(false),
                           FacetEntryMatcher.matchFacet(false, "subcommunity21field", "text")))
                   );

        getClient().perform(get("/api/discover/facets/subcommunity21field")
                                    .param("scope", String.valueOf(subcommunity21.getID())))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._embedded.values",
                                       containsInAnyOrder(
                                               FacetValueMatcher.matchEntry("subcommunity21field",
                                                                            "subcommunity21field-item211", 1),
                                               FacetValueMatcher.matchEntry("subcommunity21field",
                                                                            "subcommunity21field-item212", 1),
                                               FacetValueMatcher.matchEntry("subcommunity21field",
                                                                            "subcommunity21field-mappedItem122211", 1)
                                       )
                   ));
    }

    @Test
    /**
     *  Verify that the custom configuration "discovery-collection-2-1-1" is correctly used for Collection 211.
     */
    public void ScopeBasedIndexingAndSearchTestCollection211() throws Exception {

        getClient().perform(get("/api/discover/facets").param("scope", String.valueOf(collection211.getID())))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
                   .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(
                           FacetEntryMatcher.authorFacet(false),
                           FacetEntryMatcher.matchFacet(false, "collection211field", "text")))
                   );

        getClient().perform(get("/api/discover/facets/collection211field")
                                    .param("scope", String.valueOf(collection211.getID())))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._embedded.values",
                                       containsInAnyOrder(
                                               FacetValueMatcher.matchEntry("collection211field",
                                                                            "collection211field-item211", 1),
                                               FacetValueMatcher.matchEntry("collection211field",
                                                                            "collection211field-mappedItem122211", 1)
                                       )
                   ));
    }

    @Test
    /**
     *  Verify that the first encountered custom parent configuration "discovery-sub-community-2-1" is inherited
     *  correctly for Collection 212.
     */
    public void ScopeBasedIndexingAndSearchTestCollection212() throws Exception {

        getClient().perform(get("/api/discover/facets").param("scope", String.valueOf(collection212.getID())))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
                   .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(
                           FacetEntryMatcher.authorFacet(false),
                           FacetEntryMatcher.matchFacet(false, "subcommunity21field", "text")))
                   );

        getClient().perform(get("/api/discover/facets/subcommunity21field")
                                    .param("scope", String.valueOf(collection212.getID())))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._embedded.values",
                                       containsInAnyOrder(
                                               FacetValueMatcher.matchEntry("subcommunity21field",
                                                                            "subcommunity21field-item212", 1)
                                       )
                   ));
    }

    @Test
    /**
     *  Verify that the default configuration is inherited correctly when no other custom configuration can be inherited
     *  for Subcommunity 22.
     */
    public void ScopeBasedIndexingAndSearchTestSubcommunity22() throws Exception {
        getClient().perform(get("/api/discover/facets").param("scope", String.valueOf(subcommunity22.getID())))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
                   .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(
                                      FacetEntryMatcher.authorFacet(false),
                                      FacetEntryMatcher.subjectFacet(false),
                                      FacetEntryMatcher.dateIssuedFacet(false),
                                      FacetEntryMatcher.hasContentInOriginalBundleFacet(false),
                                      FacetEntryMatcher.entityTypeFacet(false)
                              ))
                   );
    }

    @Test
    /**
     *  Verify that the custom configuration "discovery-collection-2-2-1" is correctly used for Collection 221.
     */
    public void ScopeBasedIndexingAndSearchTestCollection221() throws Exception {

        getClient().perform(get("/api/discover/facets").param("scope", String.valueOf(collection221.getID())))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
                   .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(
                           FacetEntryMatcher.authorFacet(false),
                           FacetEntryMatcher.matchFacet(false, "collection221field", "text")))
                   );

        getClient().perform(get("/api/discover/facets/collection221field")
                                    .param("scope", String.valueOf(collection221.getID())))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._embedded.values",
                                       containsInAnyOrder(
                                               FacetValueMatcher.matchEntry("collection221field",
                                                                            "collection221field-item221", 1)
                                       )
                   ));
    }

    @Test
    /**
     *  Verify that the default configuration is inherited correctly when no other custom configuration can be inherited
     *  for Collection 222.
     */
    public void ScopeBasedIndexingAndSearchTestCollection222() throws Exception {

        getClient().perform(get("/api/discover/facets").param("scope", String.valueOf(collection222.getID())))

                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
                   .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(
                                      FacetEntryMatcher.authorFacet(false),
                                      FacetEntryMatcher.subjectFacet(false),
                                      FacetEntryMatcher.dateIssuedFacet(false),
                                      FacetEntryMatcher.hasContentInOriginalBundleFacet(false),
                                      FacetEntryMatcher.entityTypeFacet(false)
                              ))
                   );
    }


}
