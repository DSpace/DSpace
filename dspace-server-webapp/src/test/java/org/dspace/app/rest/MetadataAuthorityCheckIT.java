/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest;

import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;

import java.util.List;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * @author Jurgen Mamani
 */
public class MetadataAuthorityCheckIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private MetadataAuthorityService metadataAuthorityService;

    private Item item;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withEntityType("Publication")
                .withName("Collection 1")
                .build();

        item = ItemBuilder.createItem(context, col1)
                .withIssueDate("2022-02-01")
                .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void addAuthorityControlledMetadataWithAuthorities() throws Exception {
        itemService.addMetadata(context, item, "dc", "contributor", "author", null, "Smith, Maria");
        String metadataValue = itemService.getMetadataFirstValue(item, "dc", "contributor", "author", null);
        MatcherAssert.assertThat(metadataValue, Matchers.is("Smith, Maria"));
    }

    @Test
    public void addAuthorityControlledMetadataWithoutAuthorities() throws Exception {
        itemService.addMetadata(context, item, "dc", "contributor","author", null,
                List.of("Smith, Maria"), null, null);
        String metadataValue = itemService.getMetadataFirstValue(item, "dc", "contributor", "author", null);
        MatcherAssert.assertThat(metadataValue, Matchers.is("Smith, Maria"));
    }

    @Test
    public void addNonAuthorityControlledMetadataWithAuthorities() {
        Throwable throwable = Assert.assertThrows(IllegalArgumentException.class,
                () -> itemService.addMetadata(context, item, "dc", "title",null, null,
                        List.of("Public Item A"), List.of("test_authority"), null));

        MatcherAssert.assertThat(throwable.getMessage(),
                Matchers.is("The metadata field \"dc_title\" is not authority controlled " +
                       "but authorities were provided. Values:\"[test_authority]\""));
    }

    @Test
    public void addNonAuthorityControlledMetadataWithoutAuthorities() throws Exception {
        itemService.addMetadata(context, item, "dc", "title", null, null, "Public Item A");
        String metadataValue = itemService.getMetadataFirstValue(item, "dc", "title", null, null);
        MatcherAssert.assertThat(metadataValue, Matchers.is("Public Item A"));
    }

    @Test
    public void addRequiredAuthorityControlledMetadataWithoutAuthorities() throws Exception {
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        metadataAuthorityService.clearCache();
        try {
            configurationService.setProperty("authority.required.dc.contributor.author", true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> itemService.addMetadata(context, item,
                    "dc", "contributor", "author",null, "Smith, Maria", null, -1)
            );

            assertThat(exception.getMessage(), containsString("The metadata field \"dc_contributor_author\"" +
                " requires an authority key but none was provided."));
        } finally {
            configurationService.setProperty("authority.required.dc.contributor.author", false);
            metadataAuthorityService.clearCache();
        }
    }

    @Test
    public void addRequiredAuthorityControlledMetadataWithPlaceholderValue() throws Exception {
        metadataAuthorityService.clearCache();
        try {
            configurationService.setProperty("authority.required.dc.contributor.author", true);
            itemService.addMetadata(context, item,
                "dc", "contributor", "author",null, PLACEHOLDER_PARENT_METADATA_VALUE, null, -1);

            String metadataValue = itemService.getMetadataFirstValue(item, "dc", "contributor", "author", null);
            MatcherAssert.assertThat(metadataValue, Matchers.is(PLACEHOLDER_PARENT_METADATA_VALUE));
        } finally {
            configurationService.setProperty("authority.required.dc.contributor.author", false);
            metadataAuthorityService.clearCache();
        }
    }

}
