/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link VirtualFieldVocabulary}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class VirtualFieldVocabularyIT extends AbstractIntegrationTestWithDatabase {

    private VirtualFieldVocabulary virtualField;

    private Collection collection;

    @Before
    public void setup() {

        virtualField = new DSpace().getServiceManager().getServiceByName("virtualFieldVocabulary",
            VirtualFieldVocabulary.class);

        context.setCurrentUser(admin);
        parentCommunity = createCommunity(context).build();
        collection = createCollection(context, parentCommunity).build();
    }

    @Test
    public void testWithAbsoluteIndex() {

        Item item = ItemBuilder.createItem(context, collection)
            .withType("first::second::third::fourth")
            .build();

        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.0"), is("first"));
        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.1"), is("second"));
        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.2"), is("third"));
        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.3"), is("fourth"));
        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.4"), is("fourth"));
        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.100"), is("fourth"));

    }

    @Test
    public void testWithRelativeIndexFromLeaf() {

        Item item = ItemBuilder.createItem(context, collection)
            .withType("first::second::third::fourth")
            .build();

        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.leaf"), is("fourth"));
        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.leaf-1"), is("third"));
        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.leaf-2"), is("second"));
        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.leaf-3"), is("first"));
        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.leaf-4"), is("first"));
        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.leaf-100"), is("first"));

    }

    @Test
    public void testWithManyMetadataValues() {

        Item item = ItemBuilder.createItem(context, collection)
            .withType("first::second::third::fourth")
            .withType("A::B::C::D")
            .build();

        String[] values = virtualField.getMetadata(context, item, "virtual.vocabulary.dc-type.0");
        assertThat(values.length, is(2));
        assertThat(values, arrayContaining("first", "A"));

        values = virtualField.getMetadata(context, item, "virtual.vocabulary.dc-type.1");
        assertThat(values.length, is(2));
        assertThat(values, arrayContaining("second", "B"));

        values = virtualField.getMetadata(context, item, "virtual.vocabulary.dc-type.2");
        assertThat(values.length, is(2));
        assertThat(values, arrayContaining("third", "C"));

        values = virtualField.getMetadata(context, item, "virtual.vocabulary.dc-type.3");
        assertThat(values.length, is(2));
        assertThat(values, arrayContaining("fourth", "D"));

        values = virtualField.getMetadata(context, item, "virtual.vocabulary.dc-type.4");
        assertThat(values.length, is(2));
        assertThat(values, arrayContaining("fourth", "D"));

        values = virtualField.getMetadata(context, item, "virtual.vocabulary.dc-type.leaf");
        assertThat(values.length, is(2));
        assertThat(values, arrayContaining("fourth", "D"));

        values = virtualField.getMetadata(context, item, "virtual.vocabulary.dc-type.leaf-1");
        assertThat(values.length, is(2));
        assertThat(values, arrayContaining("third", "C"));

    }

    @Test
    public void testWithInvalidIndex() {

        Item item = ItemBuilder.createItem(context, collection)
            .withType("first::second::third::fourth")
            .build();

        assertThat(virtualField.getMetadata(context, item, "virtual.vocabulary.dc-type.XXX").length, is(0));
        assertThat(virtualField.getMetadata(context, item, "virtual.vocabulary.dc-type.-1").length, is(0));
        assertThat(virtualField.getMetadata(context, item, "virtual.vocabulary.dc-type.leaf-").length, is(0));
        assertThat(virtualField.getMetadata(context, item, "virtual.vocabulary.dc-type.leaf-XXX").length, is(0));
        assertThat(virtualField.getMetadata(context, item, "virtual.vocabulary.dc-type.leaf--2").length, is(0));

    }

    @Test
    public void testWithInvalidVirtualFieldName() {

        Item item = ItemBuilder.createItem(context, collection)
            .withType("first::second::third::fourth")
            .build();

        assertThat(virtualField.getMetadata(context, item, "virtual.vocabulary.dc-type").length, is(0));

    }

    @Test
    public void testWithoutMetadataValue() {
        Item item = ItemBuilder.createItem(context, collection).build();
        assertThat(virtualField.getMetadata(context, item, "virtual.vocabulary.dc-type.0").length, is(0));
    }

    @Test
    public void testWithControlledVocabularyMetadataValueWithoutDelimiters() {

        Item item = ItemBuilder.createItem(context, collection)
            .withType("Book")
            .build();

        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.0"), is("Book"));
        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.1"), is("Book"));
        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.leaf"), is("Book"));
        assertThat(getVocabularySection(item, "virtual.vocabulary.dc-type.leaf-1"), is("Book"));

    }

    private String getVocabularySection(Item item, String virtualFieldName) {
        String[] metadata = virtualField.getMetadata(context, item, virtualFieldName);
        assertThat(metadata.length, is(1));
        return metadata[0];
    }

}
