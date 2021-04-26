/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service;

import static java.util.List.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.notNullValue;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.orcid.service.impl.PlainMetadataSignatureGeneratorImpl;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link PlainMetadataSignatureGeneratorImpl}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class PlainMetadataSignatureGeneratorIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private PlainMetadataSignatureGeneratorImpl generator;

    private Collection collection;

    @Before
    public void setup() {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withTitle("Parent community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withEntityType("Person")
            .build();

        context.restoreAuthSystemState();

        generator = new PlainMetadataSignatureGeneratorImpl(itemService);
    }

    @Test
    public void testSignatureGeneration() {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Item title")
            .withAuthor("Walter White")
            .withAuthor("Jesse Pinkman")
            .build();

        context.restoreAuthSystemState();

        String signature = generator.generate(context, item, of("dc.title", "dc.contributor.author"));
        assertThat(signature, notNullValue());
        assertThat(signature, equalTo(metadataId(item, "dc.title", 0) + "/"
            + metadataId(item, "dc.contributor.author", 0) + "/"
            + metadataId(item, "dc.contributor.author", 1)));

        String otherSignature = generator.generate(context, item, of("dc.contributor.author", "dc.title"));
        assertThat(otherSignature, equalTo(signature));

    }

    @Test
    public void testSignatureGenerationWithMissingMetadata() {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Item title")
            .withAuthor("Walter White")
            .withAuthor("Jesse Pinkman")
            .build();

        context.restoreAuthSystemState();

        String signature = generator.generate(context, item, of("dc.title", "dc.contributor.author", "dc.subject"));
        assertThat(signature, notNullValue());
        assertThat(signature, equalTo(metadataId(item, "dc.title", 0) + "/"
            + metadataId(item, "dc.contributor.author", 0) + "/"
            + metadataId(item, "dc.contributor.author", 1)));

        String otherSignature = generator.generate(context, item, of("dc.subject", "dc.contributor.editor"));
        assertThat(otherSignature, isEmptyString());

    }

    private String metadataId(Item item, String metadataField, int place) {
        return itemService.getMetadataByMetadataString(item, metadataField).get(place).getID().toString();
    }
}
