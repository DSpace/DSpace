/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Iterator;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.dspace.AbstractIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(StructBuilderIT.class);

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

    @After
    public void tearDown() {
    }

    /** Test structure document. */
    private static final String IMPORT_DOCUMENT =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<import_structure>\n" +
            "  <community>\n" +
            "    <name>Top Community 0</name>\n" +
            "    <description/><intro/><copyright/><sidebar/>" +
            "    <community>\n" +
            "      <name>Sub Community 0.0</name>\n" +
            "      <description/><intro/><copyright/><sidebar/>" +
            "      <collection>\n" +
            "        <name>Collection 0.0.0</name>\n" +
            "        <description/><intro/><copyright/><sidebar/><license/><provenance/>" +
            "      </collection>\n" +
            "    </community>\n" +
            "    <collection>\n" +
            "      <name>Collection 0.1</name>\n" +
            "      <description/><intro/><copyright/><sidebar/><license/><provenance/>" +
            "    </collection>\n" +
            "  </community>\n" +
            "</import_structure>\n";

    private static final String EXPORT_DOCUMENT =
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<import_structure>\n" +
            "  <community>\n" +
            "    <name>Top Community 0</name>" +
            "    <description/><intro/><copyright/><sidebar/>" +
            "    <collection>\n" +
            "      <name>Collection 0.0</name>" +
            "      <description/><intro/><copyright/><sidebar/><license/>" +
            "    </collection>\n" +
            "  </community>\n" +
            "</import_structure>\n";

    /**
     * Test of main method, of class StructBuilder.
     * @throws java.lang.Exception
/*
    @Test
    public void testMain()
            throws Exception {
        System.out.println("main");
        String[] argv = null;
        StructBuilder.main(argv);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of importStructure method, of class StructBuilder.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testImportStructure()
            throws Exception {
        System.out.println("importStructure");

        // Run the method under test and collect its output.
        ByteArrayOutputStream outputDocument
                = new ByteArrayOutputStream(IMPORT_DOCUMENT.length() * 2 * 2);
        byte[] inputBytes = IMPORT_DOCUMENT.getBytes(StandardCharsets.UTF_8);
        context.turnOffAuthorisationSystem();
        try (InputStream input = new ByteArrayInputStream(inputBytes);) {
            StructBuilder.importStructure(context, input, outputDocument);
        } catch (IOException | SQLException
                | ParserConfigurationException | TransformerException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
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
//                .withNodeFilter(new MyNodeFilter())
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
//                .withNodeFilter(new MyNodeFilter())
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

    /**
     * Reject uninteresting nodes. (currently commented out of tests above)
     */
    /*private static class MyNodeFilter implements Predicate<Node> {
        private static final List<String> dontCare = Arrays.asList(
            "description",
            "intro",
            "copyright",
            "sidebar",
            "license",
            "provenance");

        @Override
        public boolean test(Node node) {
            String type = node.getLocalName();
            return ! dontCare.contains(type);
        }
    }*/
}
