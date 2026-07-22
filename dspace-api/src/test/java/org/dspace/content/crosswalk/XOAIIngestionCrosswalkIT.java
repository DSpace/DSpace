/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test for XOAIIngestionCrosswalk
 * 
 * This test verifies that XOAI metadata with authority and confidence values
 * is properly ingested into DSpace items.
 */
public class XOAIIngestionCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private XOAIIngestionCrosswalk crosswalk;
    @Autowired
    private ItemService itemService;
    private Collection collection;
    private Item item;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        
        crosswalk = new XOAIIngestionCrosswalk();
        itemService = ContentServiceFactory.getInstance().getItemService();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                      .build();
        item = workspaceItem.getItem();
        
        context.restoreAuthSystemState();
    }

    @Test
    public void testIngestXOAIMetadataWithAuthorityAndConfidence() throws Exception {
        // Sample XOAI XML with authority and confidence values
        String xmlContent = getTestXMLContent();
        
        // Parse XML
        SAXBuilder builder = new SAXBuilder();
        InputStream xmlStream = new ByteArrayInputStream(xmlContent.getBytes("UTF-8"));
        Document document = builder.build(xmlStream);
        Element rootElement = document.getRootElement();
        
        // Execute crosswalk
        context.turnOffAuthorisationSystem();
        crosswalk.ingest(context, item, rootElement, true);
        context.restoreAuthSystemState();
        //context.commit();
        
        // Verify author metadata with authority and confidence
        List<MetadataValue> authors = itemService.getMetadata(item, "dc", "contributor", "author", Item.ANY);
        assertNotNull("Authors metadata should not be null", authors);
        assertTrue("Should have at least 4 author entries", authors.size() >= 4);
        
        // Verify first author (English) with authority and confidence
        MetadataValue author1 = findMetadataByValue(authors, "Smith, John A.");
        assertNotNull("First author should exist", author1);
        assertEquals("First author authority should match", "smith-john-a-0000-0001-2345-6789", author1.getAuthority());
        assertEquals("First author confidence should match", 600, author1.getConfidence());
        assertEquals("First author language should be English", "en", author1.getLanguage());
        
        // Verify second author (English) with authority and confidence
        MetadataValue author2 = findMetadataByValue(authors, "Doe, Jane B.");
        assertNotNull("Second author should exist", author2);
        assertEquals("Second author authority should match", "doe-jane-b-0000-0002-3456-7890", author2.getAuthority());
        assertEquals("Second author confidence should match", 500, author2.getConfidence());
        assertEquals("Second author language should be English", "en", author2.getLanguage());
        
        // Verify third author (English) without authority/confidence
        MetadataValue author3 = findMetadataByValue(authors, "Brown, Michael C.");
        assertNotNull("Third author should exist", author3);
        assertEquals("Third author should have no authority", null, author3.getAuthority());
        assertEquals("Third author confidence should be default", -1, author3.getConfidence());
        assertEquals("Third author language should be English", "en", author3.getLanguage());
        
        // Verify fourth author (German) with authority and confidence
        MetadataValue author4 = findMetadataByValue(authors, "Mueller, Hans D.");
        assertNotNull("Fourth author should exist", author4);
        assertEquals("Fourth author authority should match", "mueller-hans-d-0000-0003-4567-8901", author4.getAuthority());
        assertEquals("Fourth author confidence should match", 400, author4.getConfidence());
        assertEquals("Fourth author language should be German", "de", author4.getLanguage());
        
        // Verify subject metadata with authority and confidence
        List<MetadataValue> subjects = itemService.getMetadata(item, "dc", "subject", "lcsh", Item.ANY);
        assertNotNull("Subjects metadata should not be null", subjects);
        assertTrue("Should have at least 2 subject entries", subjects.size() >= 2);
        
        MetadataValue subject1 = findMetadataByValue(subjects, "Computer Science");
        assertNotNull("First subject should exist", subject1);
        assertEquals("First subject authority should match", "sh85029552", subject1.getAuthority());
        assertEquals("First subject confidence should match", 600, subject1.getConfidence());
        
        MetadataValue subject2 = findMetadataByValue(subjects, "Artificial Intelligence");
        assertNotNull("Second subject should exist", subject2);
        assertEquals("Second subject authority should match", "sh85008180", subject2.getAuthority());
        assertEquals("Second subject confidence should match", 500, subject2.getConfidence());
        
        // Verify title metadata (no authority/confidence expected)
        List<MetadataValue> titles = itemService.getMetadata(item, "dc", "title", null, Item.ANY);
        assertNotNull("Titles metadata should not be null", titles);
        assertTrue("Should have at least 1 title entry", titles.size() >= 1);
        
        MetadataValue title = findMetadataByValue(titles, "Advanced Research in Computational Science");
        assertNotNull("Title should exist", title);
        assertEquals("Title should have no authority", null, title.getAuthority());
        assertEquals("Title confidence should be default", -1, title.getConfidence());
    }

    @Test
    public void testIngestHandlesNullRoot() throws Exception {
        // Test that crosswalk handles null root element gracefully
        crosswalk.ingest(context, item, (Element) null, true);
        
        // Should not throw exception and item should remain unchanged
        List<MetadataValue> metadata = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        assertTrue("Item should have no metadata after null root ingestion", metadata.isEmpty());
    }

    /**
     * Helper method to find metadata value by its text content
     */
    private MetadataValue findMetadataByValue(List<MetadataValue> metadataList, String value) {
        return metadataList.stream()
                .filter(mv -> value.equals(mv.getValue()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the test XML content with authority and confidence values
     */
    private String getTestXMLContent() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<metadata xmlns=\"http://www.lyncode.com/xoai\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"http://www.lyncode.com/xoai http://www.lyncode.com/xsd/xoai.xsd\">\n" +
                "    <element name=\"dc\">\n" +
                "        <element name=\"contributor\">\n" +
                "            <element name=\"author\">\n" +
                "                <element name=\"en\">\n" +
                "                    <field name=\"value\">Smith, John A.</field>\n" +
                "                    <field name=\"authority\">smith-john-a-0000-0001-2345-6789</field>\n" +
                "                    <field name=\"confidence\">600</field>\n" +
                "                    <field name=\"value\">Doe, Jane B.</field>\n" +
                "                    <field name=\"authority\">doe-jane-b-0000-0002-3456-7890</field>\n" +
                "                    <field name=\"confidence\">500</field>\n" +
                "                    <field name=\"value\">Brown, Michael C.</field>\n" +
                "                </element>\n" +
                "                <element name=\"de\">\n" +
                "                    <field name=\"value\">Mueller, Hans D.</field>\n" +
                "                    <field name=\"authority\">mueller-hans-d-0000-0003-4567-8901</field>\n" +
                "                    <field name=\"confidence\">400</field>\n" +
                "                </element>\n" +
                "            </element>\n" +
                "        </element>\n" +
                "        <element name=\"title\">\n" +
                "            <element name=\"none\">\n" +
                "                <field name=\"value\">Advanced Research in Computational Science</field>\n" +
                "            </element>\n" +
                "        </element>\n" +
                "        <element name=\"subject\">\n" +
                "            <element name=\"lcsh\">\n" +
                "                <element name=\"en\">\n" +
                "                    <field name=\"value\">Computer Science</field>\n" +
                "                    <field name=\"authority\">sh85029552</field>\n" +
                "                    <field name=\"confidence\">600</field>\n" +
                "                    <field name=\"value\">Artificial Intelligence</field>\n" +
                "                    <field name=\"authority\">sh85008180</field>\n" +
                "                    <field name=\"confidence\">500</field>\n" +
                "                </element>\n" +
                "            </element>\n" +
                "        </element>\n" +
                "        <element name=\"description\">\n" +
                "            <element name=\"abstract\">\n" +
                "                <element name=\"en\">\n" +
                "                    <field name=\"value\">This paper presents a comprehensive analysis of modern computational methods.</field>\n" +
                "                </element>\n" +
                "            </element>\n" +
                "        </element>\n" +
                "        <element name=\"date\">\n" +
                "            <element name=\"issued\">\n" +
                "                <element name=\"none\">\n" +
                "                    <field name=\"value\">2024-03-15</field>\n" +
                "                </element>\n" +
                "            </element>\n" +
                "        </element>\n" +
                "        <element name=\"type\">\n" +
                "            <element name=\"document\">\n" +
                "                <element name=\"en\">\n" +
                "                    <field name=\"value\">journal article</field>\n" +
                "                </element>\n" +
                "            </element>\n" +
                "        </element>\n" +
                "    </element>\n" +
                "</metadata>";
    }
}
