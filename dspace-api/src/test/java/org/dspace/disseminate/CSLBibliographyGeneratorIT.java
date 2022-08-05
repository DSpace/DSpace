/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class CSLBibliographyGeneratorIT
        extends AbstractIntegrationTestWithDatabase {
    private static Map<String, String> fields;
    private static Map<String, String> types;

    private static MetadataSchemaService metadataSchemaService;
    private static MetadataFieldService metadataFieldService;
    private static ItemService itemService;

    @BeforeClass
    public static void setup() {
        ServiceManager serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();
        types = serviceManager.getServiceByName("csl-types", HashMap.class);
        fields = serviceManager.getServiceByName("csl-fields", HashMap.class);

        ContentServiceFactory contentServiceFactory = ContentServiceFactory.getInstance();
        metadataSchemaService = contentServiceFactory.getMetadataSchemaService();
        metadataFieldService = contentServiceFactory.getMetadataFieldService();
        itemService = contentServiceFactory.getItemService();
    }

    /**
     * Test of addWork method, of class CSLBibliographyGenerator.
     * Also test render().  There is no good way to test them separately.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testAddWork()
            throws Exception {
        System.out.println("addWork");

        // Build an Item
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder
                .createCommunity(context)
                .withName("Top Community")
                .build();
        Collection collection = CollectionBuilder
                .createCollection(context, parentCommunity)
                .withName("My Collection")
                .build();
        Item item1 = ItemBuilder.createItem(context, collection)
                .withMetadata("dc", "title", null,
                        "The Endochronic Properties of Resublimated Thiotimoline")
                .withAuthor("Asimov, Isaac")
                .withIssueDate("1948-03")
                .withSubject("physical chemistry")
                .build();
        context.restoreAuthSystemState();

        // Instantiate the Unit Under Test
        CSLBibliographyGenerator instance
                = new CSLBibliographyGenerator(types, fields, "article");
        instance.setMetadataSchemaService(metadataSchemaService);
        instance.setMetadataFieldService(metadataFieldService);
        instance.setItemService(itemService);

        // Test!
        instance.addWork(context, item1);

        String style = "bibtex";
        CSLBibliographyGenerator.OutputFormat outputFormat
                = CSLBibliographyGenerator.OutputFormat.TEXT;
        String rendered = instance.render(style, outputFormat);
        System.out.println("BEGIN RENDERED");
        System.out.println(rendered);
        System.out.println("END RENDERED");
        assertTrue("Bibliography should mention 'thiotimoline'",
                rendered.contains("thiotimoline"));
    }

    /**
     * Test of render method, of class CSLBibliographyGenerator.
     * NOTE:  there is no good way to test render() separately.
     * See {@link #testAddWork}.
     *
     */
    @Ignore
    @Test
    public void testRender() {
    }

    /**
     * Test of getStyles method, of class CSLBibliographyGenerator.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetStyles()
            throws Exception {
        System.out.println("getStyles");
        Set<String> result = CSLBibliographyGenerator.getStyles();
        assertTrue("Styles should include 'bibtex'", result.contains("bibtex"));
    }
}
