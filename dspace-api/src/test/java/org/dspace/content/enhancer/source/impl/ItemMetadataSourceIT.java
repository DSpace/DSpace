/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.source.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.enhancer.source.RawMetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link ItemMetadataSource}.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class ItemMetadataSourceIT extends AbstractIntegrationTestWithDatabase {

    private static final String AUTHOR_FIELD = "dc.contributor.author";
    private static final String AUTHOR_VALUE = "Doe, Jane";
    private static final String SECOND_AUTHOR_VALUE = "Smith, John";
    private static final String AUTHOR_AUTHORITY = "author-authority";
    private static final int AUTHOR_CONFIDENCE = 600;

    private ItemService itemService;
    private MetadataFieldService metadataFieldService;
    private MetadataValueService metadataValueService;
    private Item item;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        itemService = ContentServiceFactory.getInstance().getItemService();
        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent community")
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .withEntityType("Person")
                                                 .build();
        item = ItemBuilder.createItem(context, collection)
                          .withTitle("Person")
                          .withMetadata("dc", "contributor", "author", SECOND_AUTHOR_VALUE)
                          .build();

        // dc.contributor.author is not authority controlled in the test configuration, so the builder would
        // reject the authority. Create the value directly to still exercise the authority/confidence mapping.
        MetadataField authorField = metadataFieldService.findByString(context, AUTHOR_FIELD, '.');
        MetadataValue authorityValue = metadataValueService.create(context, item, authorField);
        authorityValue.setValue(AUTHOR_VALUE);
        authorityValue.setLanguage("en");
        authorityValue.setAuthority(AUTHOR_AUTHORITY);
        authorityValue.setConfidence(AUTHOR_CONFIDENCE);
        itemService.update(context, item);

        context.restoreAuthSystemState();
    }

    @Test
    public void getValuesReturnsAllMatchingValues() {
        ItemMetadataSource source = new ItemMetadataSource(context, item, itemService);

        List<RawMetadataValue> values = source.getValues(AUTHOR_FIELD);

        assertEquals(2, values.size());
        assertTrue(values.contains(new RawMetadataValue(AUTHOR_VALUE, "en", AUTHOR_AUTHORITY, AUTHOR_CONFIDENCE)));
        assertTrue(values.contains(new RawMetadataValue(SECOND_AUTHOR_VALUE, null, null, -1)));
    }

    @Test
    public void getValuesReturnsEmptyListWhenFieldHasNoValues() {
        ItemMetadataSource source = new ItemMetadataSource(context, item, itemService);

        assertTrue(source.getValues("dc.description.abstract").isEmpty());
    }

    @Test
    public void getValuesReturnsEmptyListWhenFieldIsNull() {
        ItemMetadataSource source = new ItemMetadataSource(context, item, itemService);

        assertTrue(source.getValues(null).isEmpty());
    }

    @Test
    public void adapterDoesNotDependOnSolr() {
        Class<?> adapter = ItemMetadataSource.class;

        assertFalse(adapter.getName().contains("Solr"));
        for (Field field : adapter.getDeclaredFields()) {
            assertFalse(field.getType().getName().contains("Solr"));
        }
        for (Constructor<?> constructor : adapter.getDeclaredConstructors()) {
            for (Parameter parameter : constructor.getParameters()) {
                assertFalse(parameter.getType().getName().contains("Solr"));
            }
        }
        for (Method method : adapter.getDeclaredMethods()) {
            assertFalse(method.getReturnType().getName().contains("Solr"));
            for (Parameter parameter : method.getParameters()) {
                assertFalse(parameter.getType().getName().contains("Solr"));
            }
        }
    }

}
