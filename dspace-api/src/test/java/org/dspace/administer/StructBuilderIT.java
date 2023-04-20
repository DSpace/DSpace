/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Iterator;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.handle.Handle;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonFormatter;
import org.xmlunit.diff.DefaultComparisonFormatter;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

/**
 * Tests of {@link StructBuilder}.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class StructBuilderIT
        extends AbstractIntegrationTest {
    private static final Logger log = LogManager.getLogger();

    private static final CommunityService communityService
            = ContentServiceFactory.getInstance().getCommunityService();
    private static final CollectionService collectionService
            = ContentServiceFactory.getInstance().getCollectionService();

    public StructBuilderIT() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Ensure that there is no left-over structure to confuse a test.
     *
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     * @throws IOException passed through.
     */
    @Before
    public void setUp() throws SQLException, AuthorizeException, IOException {
        // Clear out all communities and collections.
        context.turnOffAuthorisationSystem();
        for (Community community : communityService.findAllTop(context)) {
            deleteSubCommunities(community);
            communityService.delete(context, community);
        }
        context.restoreAuthSystemState();
    }

    private static final String COMMUNITY_0_HANDLE = "https://hdl.handle.net/1/1";
    private static final String COMMUNITY_0_0_HANDLE = "https://hdl.handle.net/1/1.1";
    private static final String COLLECTION_0_0_0_HANDLE = "https://hdl.handle.net/1/1.1.1";
    private static final String COLLECTION_0_1_HANDLE = "https://hdl.handle.net/1/1.2";

    /** Test structure document. */
    private static final String IMPORT_DOCUMENT =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<import_structure>\n" +
            "  <community identifier='" + COMMUNITY_0_HANDLE + "'>\n" +
            "    <name>Top Community 0</name>\n" +
            "    <description>A top level community</description>\n" +
            "    <intro>Testing 1 2 3</intro>\n" +
            "    <copyright>1969</copyright>\n" +
            "    <sidebar>A sidebar</sidebar>\n" +
            "    <community identifier='" + COMMUNITY_0_0_HANDLE + "'>\n" +
            "      <name>Sub Community 0.0</name>\n" +
            "      <description>A sub community</description>\n" +
            "      <intro>Live from New York....</intro>\n" +
            "      <copyright>1957</copyright>\n" +
            "      <sidebar>Another sidebar</sidebar>\n" +
            "      <collection identifier='" + COLLECTION_0_0_0_HANDLE + "'>\n" +
            "        <name>Collection 0.0.0</name>\n" +
            "        <description>A collection</description>\n" +
            "        <intro>Our next guest needs no introduction</intro>\n" +
            "        <copyright>1776</copyright>\n" +
            "        <sidebar>Yet another sidebar</sidebar>\n" +
            "        <license>MIT</license>\n" +
            "        <provenance>Testing</provenance>\n" +
            "      </collection>\n" +
            "    </community>\n" +
            "    <community>\n" +
            "      <name>Sub Community 0.1</name>\n" +
            "      <description>A sub community with no handle</description>\n" +
            "      <intro>Stop me if you've heard this one</intro>\n" +
            "      <copyright>2525</copyright>\n" +
            "      <sidebar>One more sidebar</sidebar>\n" +
            "    </community>\n" +
            "    <collection identifier='" + COLLECTION_0_1_HANDLE + "'>\n" +
            "      <name>Collection 0.1</name>\n" +
            "      <description>Another collection</description>\n" +
            "      <intro>Fourscore and seven years ago</intro>\n" +
            "      <copyright>1863</copyright>\n" +
            "      <sidebar>No sidebar</sidebar>\n" +
            "      <license>Public domain</license>\n" +
            "      <provenance>Testing again</provenance>\n" +
            "    </collection>\n" +
            "  </community>\n" +
            "</import_structure>\n";

    private static final String EXPORT_DOCUMENT =
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<import_structure>\n" +
            "  <community>\n" +
            "    <name>Top Community 0</name>\n" +
            "    <description/><intro/><copyright/><sidebar/>\n" +
            "    <collection>\n" +
            "      <name>Collection 0.0</name>\n" +
            "      <description/><intro/><copyright/><sidebar/><license/>\n" +
            "    </collection>\n" +
            "  </community>\n" +
            "</import_structure>\n";

    /**
     * Test of importStructure method, of class StructBuilder.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testImportStructureWithoutHandles()
            throws Exception {
        System.out.println("importStructure");

        // Run the method under test and collect its output.
        ByteArrayOutputStream outputDocument
                = new ByteArrayOutputStream(IMPORT_DOCUMENT.length() * 2 * 2);
        byte[] inputBytes = IMPORT_DOCUMENT.getBytes(StandardCharsets.UTF_8);
        context.turnOffAuthorisationSystem();
        try (InputStream input = new ByteArrayInputStream(inputBytes);) {
            StructBuilder.importStructure(context, input, outputDocument, false);
        } finally {
            context.restoreAuthSystemState();
        }

        // Compare import's output with its input.
        // N.B. here we rely on StructBuilder to emit communities and
        // collections in the same order as the input document.  If that changes,
        // we will need a smarter NodeMatcher, probably based on <name> children.
        Source output = new StreamSource(
                new ByteArrayInputStream(outputDocument.toByteArray()));
        Source reference = new StreamSource(
                new ByteArrayInputStream(
                        IMPORT_DOCUMENT.getBytes(StandardCharsets.UTF_8)));
        Diff myDiff = DiffBuilder.compare(reference).withTest(output)
                .normalizeWhitespace()
                .withAttributeFilter((Attr attr) ->
                        !attr.getName().equals("identifier"))
                .checkForIdentical()
                .build();

        // Was there a difference?
        // Always output differences -- one is expected.
        ComparisonFormatter formatter = new DefaultComparisonFormatter();
        for (Difference difference : myDiff.getDifferences()) {
            System.err.println(difference.toString(formatter));
        }
        // Test for *significant* differences.
        assertFalse("Output does not match input.", isDifferent(myDiff));

        // TODO spot-check some objects.
    }

    /**
     * Test of importStructure method, with given Handles.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testImportStructureWithHandles()
            throws Exception {
        System.out.println("importStructure");

        // Run the method under test and collect its output.
        ByteArrayOutputStream outputDocument
                = new ByteArrayOutputStream(IMPORT_DOCUMENT.length() * 2 * 2);
        byte[] inputBytes = IMPORT_DOCUMENT.getBytes(StandardCharsets.UTF_8);
        context.turnOffAuthorisationSystem();
        try (InputStream input = new ByteArrayInputStream(inputBytes);) {
            StructBuilder.importStructure(context, input, outputDocument, true);
        } finally {
            context.restoreAuthSystemState();
        }

        boolean found;

        // Check a chosen Community for the right Handle.
        found = false;
        for (Community community : communityService.findAllTop(context)) {
            for (Handle handle : community.getHandles()) {
                if (handle.getHandle().equals(COMMUNITY_0_HANDLE)) {
                    found = true;
                    break;
                }
            }
        }
        assertTrue("A community should have its specified handle", found);

        // Check a chosen Collection for the right Handle.
        found = false;
        for (Collection collection : collectionService.findAll(context)) {
            for (Handle handle : collection.getHandles()) {
                if (handle.getHandle().equals(COLLECTION_0_1_HANDLE)) {
                    found = true;
                    break;
                }
            }
        }
        assertTrue("A collection should have its specified handle", found);

        // Compare import's output with its input.
        // N.B. here we rely on StructBuilder to emit communities and
        // collections in the same order as the input document.  If that changes,
        // we will need a smarter NodeMatcher, probably based on <name> children.
        Source output = new StreamSource(
                new ByteArrayInputStream(outputDocument.toByteArray()));
        Source reference = new StreamSource(
                new ByteArrayInputStream(
                        IMPORT_DOCUMENT.getBytes(StandardCharsets.UTF_8)));
        Diff myDiff = DiffBuilder.compare(reference).withTest(output)
                .normalizeWhitespace()
                .withAttributeFilter((Attr attr) ->
                        !attr.getName().equals("identifier"))
                .checkForIdentical()
                .build();

        // Was there a difference?
        // Always output differences -- one is expected.
        ComparisonFormatter formatter = new DefaultComparisonFormatter();
        for (Difference difference : myDiff.getDifferences()) {
            System.err.println(difference.toString(formatter));
        }
        // Test for *significant* differences.
        assertFalse("Output does not match input.", isDifferent(myDiff));

        // TODO spot-check some objects.
    }

    /**
     * Test of exportStructure method, of class StructBuilder.
     * @throws ParserConfigurationException passed through.
     * @throws org.xml.sax.SAXException passed through.
     * @throws java.io.IOException passed through.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    @Test
    public void testExportStructure()
            throws ParserConfigurationException, SAXException, IOException,
            SQLException, AuthorizeException {
        // Create some structure to test.
        context.turnOffAuthorisationSystem();
        Community community0 = communityService.create(null, context);
        communityService.setMetadataSingleValue(context, community0,
                MetadataSchemaEnum.DC.getName(), "title", null,
                null, "Top Community 0");
        Collection collection0_0 = collectionService.create(context, community0);
        collectionService.setMetadataSingleValue(context, collection0_0,
                MetadataSchemaEnum.DC.getName(), "title", null,
                null, "Collection 0.0");

        // Export the current structure.
        System.out.println("exportStructure");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        StructBuilder.exportStructure(context, outputStream);

        context.restoreAuthSystemState();

        // Compare the output to the expected output.
        Source output = new StreamSource(
                new ByteArrayInputStream(outputStream.toByteArray()));
        Source reference = new StreamSource(
                new ByteArrayInputStream(
                        EXPORT_DOCUMENT.getBytes(StandardCharsets.UTF_8)));
        Diff myDiff = DiffBuilder.compare(reference).withTest(output)
                .normalizeWhitespace()
                .withAttributeFilter((Attr attr) ->
                        !attr.getName().equals("identifier"))
                .checkForIdentical()
                .build();

        // Was there a difference?
        // Always output differences -- one is expected.
        ComparisonFormatter formatter = new DefaultComparisonFormatter();
        for (Difference difference : myDiff.getDifferences()) {
            System.err.println(difference.toString(formatter));
        }
        // Test for *significant* differences.
        assertFalse("Output does not match input.", myDiff.hasDifferences());
    }

    /**
     * Delete all child communities and collections of a given community.
     * All descendant collections must be empty of Items.
     *
     * @param c the Community to be pruned of all descendants.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     * @throws IOException passed through.
     */
    private void deleteSubCommunities(Community c)
            throws SQLException, AuthorizeException, IOException {
        for (Community subCommunity : c.getSubcommunities()) {
            deleteSubCommunities(subCommunity);
            communityService.delete(context, subCommunity);
        }
        for (Collection collection : c.getCollections()) {
            collectionService.delete(context, collection);
        }
    }

    /**
     * Test that the documents are not different, except that their root
     * elements have specific different names.
     *
     * @param diff
     * @return true if these are otherwise-identical "import_structure" and
     *          "imported_structure" documents.
     */
    private boolean isDifferent(Diff diff) {
        Iterator<Difference> diffIterator = diff.getDifferences().iterator();

        // There must be at least one difference.
        if (!diffIterator.hasNext()) {
            log.error("Not enough differences.");
            return true;
        }

        // The difference must be that the root nodes are named "import_structure"
        // and "imported_structure".
        Comparison comparison = diffIterator.next().getComparison();
        Node controlNode = comparison.getControlDetails().getTarget();
        Node testNode = comparison.getTestDetails().getTarget();
        if (!controlNode.getNodeName().equals("import_structure")
                || !testNode.getNodeName().equals("imported_structure")) {
            log.error("controlNode name:  {}", controlNode.getNodeName());
            log.error("test node name:  {}", testNode.getNodeName());
            return true;
        }
        if ((controlNode.getParentNode().getNodeType() != Node.DOCUMENT_NODE)
                || (testNode.getParentNode().getNodeType() != Node.DOCUMENT_NODE)) {
            log.error("control node's parent type is {}", controlNode.getParentNode().getNodeType());
            log.error("test node's parent type is {}", testNode.getParentNode().getNodeType());
            return true;
        }

        // There must be at most one difference.
        return diffIterator.hasNext();
    }
}
