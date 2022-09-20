/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.orcid.service.impl.PlainMetadataSignatureGeneratorImpl;
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

    private Collection collection;

    private MetadataSignatureGenerator generator = new PlainMetadataSignatureGeneratorImpl();

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
    }

    @Test
    public void testSignatureGenerationWithManyMetadataValues() {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Item title")
            .withIssueDate("2020-01-01")
            .withAuthor("Jesse Pinkman")
            .withEditor("Editor")
            .build();

        context.restoreAuthSystemState();

        MetadataValue author = getMetadata(item, "dc.contributor.author", 0);
        MetadataValue editor = getMetadata(item, "dc.contributor.editor", 0);

        String signature = generator.generate(context, List.of(author, editor));
        assertThat(signature, notNullValue());

        String expectedSignature = "dc.contributor.author::Jesse Pinkman§§"
            + "dc.contributor.editor::Editor";

        assertThat(signature, equalTo(expectedSignature));

        String anotherSignature = generator.generate(context, List.of(editor, author));
        assertThat(anotherSignature, equalTo(signature));

        List<MetadataValue> metadataValues = generator.findBySignature(context, item, signature);
        assertThat(metadataValues, hasSize(2));
        assertThat(metadataValues, containsInAnyOrder(author, editor));

    }

    @Test
    public void testSignatureGenerationWithSingleMetadataValue() {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Item title")
            .withDescription("Description")
            .withAuthor("Jesse Pinkman")
            .withUriIdentifier("https://www.4science.it/en")
            .build();

        context.restoreAuthSystemState();

        MetadataValue description = getMetadata(item, "dc.description", 0);
        String signature = generator.generate(context, List.of(description));
        assertThat(signature, notNullValue());
        assertThat(signature, equalTo("dc.description::Description"));

        List<MetadataValue> metadataValues = generator.findBySignature(context, item, signature);
        assertThat(metadataValues, hasSize(1));
        assertThat(metadataValues, containsInAnyOrder(description));

        MetadataValue url = getMetadata(item, "dc.identifier.uri", 0);
        signature = generator.generate(context, List.of(url));
        assertThat(signature, equalTo("dc.identifier.uri::https://www.4science.it/en"));

        metadataValues = generator.findBySignature(context, item, signature);
        assertThat(metadataValues, hasSize(1));
        assertThat(metadataValues, containsInAnyOrder(url));

    }

    @Test
    public void testSignatureGenerationWithManyEqualsMetadataValues() {
        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Item title")
            .withDescription("Description")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Jesse Pinkman")
            .build();

        context.restoreAuthSystemState();

        MetadataValue firstAuthor = getMetadata(item, "dc.contributor.author", 0);
        String firstSignature = generator.generate(context, List.of(firstAuthor));
        assertThat(firstSignature, notNullValue());
        assertThat(firstSignature, equalTo("dc.contributor.author::Jesse Pinkman"));

        MetadataValue secondAuthor = getMetadata(item, "dc.contributor.author", 1);
        String secondSignature = generator.generate(context, List.of(secondAuthor));
        assertThat(secondSignature, notNullValue());
        assertThat(secondSignature, equalTo("dc.contributor.author::Jesse Pinkman"));

        List<MetadataValue> metadataValues = generator.findBySignature(context, item, firstSignature);
        assertThat(metadataValues, hasSize(1));
        assertThat(metadataValues, anyOf(contains(firstAuthor), contains(secondAuthor)));
    }

    private MetadataValue getMetadata(Item item, String metadataField, int place) {
        List<MetadataValue> values = itemService.getMetadataByMetadataString(item, metadataField);
        assertThat(values.size(), greaterThan(place));
        return values.get(place);
    }

}
