/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.DSpaceCSVLine;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.service.MetadataDSpaceCsvExportService;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.utils.DSpace;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class MetadataDSpaceCsvExportServiceImplIT
        extends AbstractIntegrationTestWithDatabase {
    /**
     * Test of handleExport method, of class MetadataDSpaceCsvExportServiceImpl.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testHandleExport()
            throws Exception {
        System.out.println("handleExport");
        boolean exportAllItems = false;
        boolean exportAllMetadata = false;
        String identifier = "";
        DSpaceRunnableHandler handler = null;
        MetadataDSpaceCsvExportServiceImpl instance = new MetadataDSpaceCsvExportServiceImpl();
        DSpaceCSV expResult = null;
        DSpaceCSV result = instance.handleExport(context, exportAllItems,
                exportAllMetadata, identifier, handler);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of export method, of class MetadataDSpaceCsvExportServiceImpl.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testExport_3args_1()
            throws Exception {
        System.out.println("export");
        Iterator<Item> toExport = null;
        boolean exportAll = false;
        MetadataDSpaceCsvExportServiceImpl instance = new MetadataDSpaceCsvExportServiceImpl();
        DSpaceCSV expResult = null;
        DSpaceCSV result = instance.export(context, toExport, exportAll);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of export with mapped Item.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testMappedItem()
            throws Exception {
        System.out.println("export");

        // Create some content with which to test.
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Community")
                .build();
        Collection collection1 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection1")
                .build();
        Collection collection2 = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection2")
                .build();
        context.setCurrentUser(eperson);
        Item item = ItemBuilder.createItem(context, collection1)
                .withTitle("Item")
                .withIssueDate("1957")
                .build();
        item.addCollection(collection2);
        context.restoreAuthSystemState();

        // Test!
        MetadataDSpaceCsvExportService instance = new DSpace()
                .getServiceManager()
                .getServiceByName(MetadataDSpaceCsvExportServiceImpl.class.getCanonicalName(),
                        MetadataDSpaceCsvExportService.class);
        DSpaceCSV result = instance.export(context, parentCommunity, false);

        // Examine the result.
        List<DSpaceCSVLine> csvLines = result.getCSVLines();
        assertEquals("One item mapped twice should produce one line",
                1, csvLines.size());
    }
}
