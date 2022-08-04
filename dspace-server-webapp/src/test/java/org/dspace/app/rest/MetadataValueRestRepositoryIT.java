/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.matcher.MetadataValueMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataValueRestRepositoryIT extends AbstractControllerIntegrationTest {

    private static final String AUTHOR = "Test author name";
    private static final String SCHEMA = "dc";
    private static final String ELEMENT = "contributor";
    private static final String QUALIFIER = "author";

    private static final String SPONSOR_SCHEMA = "local";
    private static final String SPONSOR_ELEMENT = "sponsor";
    private static final String SPONSOR_VALUE = WorkspaceItemRestRepositoryIT.EU_SPONSOR;

    private Item publicItem;
    private Item sponsorItem;

    public static final String METADATAVALUES_ENDPOINT = "/api/core/metadatavalues/";
    private static final String SEARCH_BYVALUE_ENDPOINT = METADATAVALUES_ENDPOINT + "search/byValue";

    @Autowired
    private ItemService itemService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Before
    public void setup() throws Exception {
        context.turnOffAuthorisationSystem();
        // 1. A community-collection structure with one parent community and one collection
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection").build();

        // 2. Create item and add it to the collection
        publicItem = ItemBuilder.createItem(context, col)
                .withAuthor(AUTHOR)
                .withMetadata(SPONSOR_SCHEMA, SPONSOR_ELEMENT, null, SPONSOR_VALUE )
                .build();

        // two items has the same sponsor
        sponsorItem = ItemBuilder.createItem(context, col)
                .withAuthor(AUTHOR)
                .withMetadata(SPONSOR_SCHEMA, SPONSOR_ELEMENT, null, SPONSOR_VALUE )
                .build();
        context.restoreAuthSystemState();
    }

    @Test
    public void findAll() throws Exception {
        // Get title metadata from the item
        MetadataValue titleMetadataValue = this.getTitleMetadataValue();

        getClient().perform(get("/api/core/metadatavalues")
                        .param("size", String.valueOf(100)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.metadatavalues", Matchers.hasItem(
                        MetadataValueMatcher.matchMetadataValueByKeys(titleMetadataValue.getValue(),
                                titleMetadataValue.getLanguage(), titleMetadataValue.getAuthority(),
                                titleMetadataValue.getConfidence(), titleMetadataValue.getPlace()))
                ))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/core/metadatavalues")))
                .andExpect(jsonPath("$.page.size", is(100)));
    }

    @Test
    public void findOne() throws Exception {
        // Get title metadata from the item
        MetadataValue titleMetadataValue = this.getTitleMetadataValue();

        getClient().perform(get("/api/core/metadatavalues/" + titleMetadataValue.getID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.is(
                        MetadataValueMatcher.matchMetadataValue(titleMetadataValue)
                )))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/core/metadatavalues")));
    }

    @Test
    public void searchMethodsExist() throws Exception {

        getClient().perform(get("/api/core/metadatavalues"))
                .andExpect(jsonPath("$._links.search.href", notNullValue()));

        getClient().perform(get("/api/core/metadatavalues/search"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._links.byValue", notNullValue()));
    }

    @Test
    public void findByNullValue() throws Exception {

        getClient().perform(get(SEARCH_BYVALUE_ENDPOINT))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void findByValue_nullSchema() throws Exception {

        getClient().perform(get(SEARCH_BYVALUE_ENDPOINT)
                        .param("schema", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void findByValue_nullSchemaAndElement() throws Exception {

        getClient().perform(get(SEARCH_BYVALUE_ENDPOINT)
                        .param("schema", "")
                        .param("element",""))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void findByValue_searchValue() throws Exception {
        MetadataValue titleMetadataValue = this.getTitleMetadataValue();

        String metadataSchema = titleMetadataValue.getMetadataField().getMetadataSchema().getName();
        String metadataElement = titleMetadataValue.getMetadataField().getElement();
        String metadataQualifier = titleMetadataValue.getMetadataField().getQualifier();
        String searchValue = titleMetadataValue.getValue();

        getClient().perform(get(SEARCH_BYVALUE_ENDPOINT)
                        .param("schema", metadataSchema)
                        .param("element", metadataElement)
                        .param("qualifier", metadataQualifier)
                        .param("searchValue", searchValue))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.metadatavalues", Matchers.hasItem(
                        MetadataValueMatcher.matchMetadataValueByKeys(titleMetadataValue.getValue(),
                                titleMetadataValue.getLanguage(), titleMetadataValue.getAuthority(),
                                titleMetadataValue.getConfidence(), titleMetadataValue.getPlace()))
                ))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findByValue_searchValueWithStringAndNumber() throws Exception {
        MetadataValue titleMetadataValue = this.getTitleMetadataValue();

        // add number to the title
        titleMetadataValue.setValue(titleMetadataValue.getValue() + "1");
        context.turnOffAuthorisationSystem();

        // update item with the new title
        itemService.setMetadataSingleValue(context, publicItem, SCHEMA, ELEMENT, QUALIFIER, null,
                titleMetadataValue.getValue());
        itemService.update(context, publicItem);

        context.restoreAuthSystemState();


        String metadataSchema = titleMetadataValue.getMetadataField().getMetadataSchema().getName();
        String metadataElement = titleMetadataValue.getMetadataField().getElement();
        String metadataQualifier = titleMetadataValue.getMetadataField().getQualifier();
        String searchValue = titleMetadataValue.getValue();

        getClient().perform(get(SEARCH_BYVALUE_ENDPOINT)
                        .param("schema", metadataSchema)
                        .param("element", metadataElement)
                        .param("qualifier", metadataQualifier)
                        .param("searchValue", searchValue))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.metadatavalues", Matchers.hasItem(
                        MetadataValueMatcher.matchMetadataValueByKeys(titleMetadataValue.getValue(),
                                titleMetadataValue.getLanguage(), titleMetadataValue.getAuthority(),
                                titleMetadataValue.getConfidence(), titleMetadataValue.getPlace()))
                ))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findByValue_searchValueIsNumber() throws Exception {
        MetadataValue titleMetadataValue = this.getTitleMetadataValue();

        // add number to the title
        titleMetadataValue.setValue("123");
        context.turnOffAuthorisationSystem();

        // update item with the new title
        itemService.setMetadataSingleValue(context, publicItem, SCHEMA, ELEMENT, QUALIFIER, null,
                titleMetadataValue.getValue());
        itemService.update(context, publicItem);

        context.restoreAuthSystemState();


        String metadataSchema = titleMetadataValue.getMetadataField().getMetadataSchema().getName();
        String metadataElement = titleMetadataValue.getMetadataField().getElement();
        String metadataQualifier = titleMetadataValue.getMetadataField().getQualifier();
        String searchValue = titleMetadataValue.getValue();

        getClient().perform(get(SEARCH_BYVALUE_ENDPOINT)
                        .param("schema", metadataSchema)
                        .param("element", metadataElement)
                        .param("qualifier", metadataQualifier)
                        .param("searchValue", searchValue))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.metadatavalues", Matchers.hasItem(
                        MetadataValueMatcher.matchMetadataValueByKeys(titleMetadataValue.getValue(),
                                titleMetadataValue.getLanguage(), titleMetadataValue.getAuthority(),
                                titleMetadataValue.getConfidence(), titleMetadataValue.getPlace()))
                ))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void shouldReturnDistinctSuggestion() throws Exception {
        MetadataValue titleMetadataValue = this.getTitleMetadataValue();


        String metadataSchema = titleMetadataValue.getMetadataField().getMetadataSchema().getName();
        String metadataElement = titleMetadataValue.getMetadataField().getElement();
        String metadataQualifier = titleMetadataValue.getMetadataField().getQualifier();
        String searchValue = titleMetadataValue.getValue();

        getClient().perform(get(SEARCH_BYVALUE_ENDPOINT)
                        .param("schema", metadataSchema)
                        .param("element",metadataElement)
                        .param("qualifier",metadataQualifier)
                        .param("searchValue",searchValue))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.metadatavalues", Matchers.hasItem(
                        MetadataValueMatcher.matchMetadataValueByKeys(titleMetadataValue.getValue(),
                                titleMetadataValue.getLanguage(), titleMetadataValue.getAuthority(),
                                titleMetadataValue.getConfidence(), titleMetadataValue.getPlace()))
                ))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void shouldNotReturnSponsorSuggestion() throws Exception {
        String searchValue = "grantAgreement";

        getClient().perform(get(SEARCH_BYVALUE_ENDPOINT)
                        .param("schema", SPONSOR_SCHEMA)
                        .param("element", SPONSOR_ELEMENT)
                        .param("searchValue", searchValue))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    private MetadataValue getTitleMetadataValue() {
        return itemService.getMetadataByMetadataString(publicItem,
                        StringUtils.join(Arrays.asList(SCHEMA, ELEMENT, QUALIFIER),".")).get(0);
    }
}
