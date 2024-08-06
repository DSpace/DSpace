/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DuplicateDetectionService;
import org.dspace.content.virtual.PotentialDuplicate;
import org.dspace.discovery.SearchServiceException;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * Integration tests for the duplicate detection service
 *
 * @author Kim Shepherd
 */
public class DuplicateDetectionTest extends AbstractIntegrationTestWithDatabase {
    private DuplicateDetectionService duplicateDetectionService = ContentServiceFactory.getInstance()
            .getDuplicateDetectionService();
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private Collection col;
    private Collection workflowCol;
    private Item item1;
    private Item item2;
    private Item item3;
    private final String item1IssueDate = "2011-10-17";
    private final String item1Subject = "ExtraEntry 1";
    private final String item1Title = "Public item I";
    private final String item1Author = "Smith, Donald";

    private static final Logger log = LogManager.getLogger();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Temporarily enable duplicate detection and set comparison distance to 1
        configurationService.setProperty("duplicate.enable", true);
        configurationService.setProperty("duplicate.comparison.distance", 1);
        configurationService.setProperty("duplicate.comparison.normalise.lowercase", true);
        configurationService.setProperty("duplicate.comparison.normalise.whitespace", true);
        configurationService.setProperty("duplicate.comparison.solr.field", "deduplication_keyword");
        configurationService.setProperty("duplicate.comparison.metadata.field", new String[]{"dc.title"});
        configurationService.setProperty("duplicate.preview.metadata.field",
                new String[]{"dc.date.issued", "dc.subject"});

        context.turnOffAuthorisationSystem();
        context.setDispatcher("default");

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection").build();
        workflowCol = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Workflow Collection")
                .withWorkflowGroup("reviewer", admin)
                .build();

        // Ingest three example items with slightly different titles
        // item2 is 1 edit distance from item1 and item3
        // item1 and item3 are 2 edit distance from each other
        item1 = ItemBuilder.createItem(context, col)
                .withTitle(item1Title) // Public item I
                .withIssueDate(item1IssueDate)
                .withAuthor(item1Author)
                .withSubject(item1Subject)
                .build();
        item2 = ItemBuilder.createItem(context, col)
                .withTitle("Public item II")
                .withIssueDate("2012-10-17")
                .withAuthor("Smith, Donald X.")
                .withSubject("ExtraEntry 2")
                .build();
        item3 = ItemBuilder.createItem(context, col)
                .withTitle("Public item III")
                .withIssueDate("2013-10-17")
                .withAuthor("Smith, Donald Y.")
                .withSubject("ExtraEntry 3")
                .build();


    }

    /**
     * Test instantiation of simple potential duplicate object
     */
    @Test
    public void testPotentialDuplicateInstantatation() {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate();
        // The constructor should instantiate a new list for metadata
        assertEquals("Metadata value list size should be 0",
                0, potentialDuplicate.getMetadataValueList().size());
        // Other properties should not be set
        assertNull("Title should be null", potentialDuplicate.getTitle());
        //StringUtils.getLevenshteinDistance()
    }

    /**
     * Test instantiation of simple potential duplicate object given an item as a constructor argument
     */
    @Test
    public void testPotentialDuplicateInstantiationWithItem() {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate(item1);
        // We should have title, uuid, owning collection name set and metadata value list instantiated to empty
        assertEquals("UUID should match item1 uuid", item1.getID(), potentialDuplicate.getUuid());
        assertEquals("Title should match item1 title", item1Title, potentialDuplicate.getTitle());
        assertEquals("Owning collection should match item1 owning collection",
                item1.getOwningCollection().getName(), potentialDuplicate.getOwningCollectionName());
        assertEquals("Metadata value list size should be 0",
                0, potentialDuplicate.getMetadataValueList().size());
    }

    /**
     * Test that a search for getPotentialDuplicates returns the expected results, populated with the expected
     * preview values and metadata. This is the core method used by the duplicate item link repository and
     * detect duplicates submission step.
     *
     * @throws Exception
     */
    @Test
    public void testSearchDuplicates() throws Exception {

        // Get potential duplicates of item 1:
        // Expected: Public item II should appear as it has the configured levenshtein distance of 1
        List<PotentialDuplicate> potentialDuplicates = duplicateDetectionService.getPotentialDuplicates(context, item1);

        // Make sure result list is size 1
        int size = 1;
        assertEquals("Potential duplicates of item1 should have size " + size,
                size, potentialDuplicates.size());

        // The only member should be Public item II (one distance from public item I)
        assertEquals("Item II should be be the detected duplicate",
                item2.getID(), potentialDuplicates.get(0).getUuid());

        // Get potential duplicates of item2:
        // Expected: BOTH other items should appear as they are both 1 distance away from "Public item II"
        potentialDuplicates = duplicateDetectionService.getPotentialDuplicates(context, item2);

        // Sort by title
        potentialDuplicates.sort(Comparator.comparing(PotentialDuplicate::getTitle));

        // Make sure result list is size 1
        size = 2;
        assertEquals("Potential duplicates of item2 should have size " + size,
                size, potentialDuplicates.size());

        // The result list should contain both item1 and item3 in the expected order
        assertEquals("item1 should be the first detected duplicate",
                item1.getID(), potentialDuplicates.get(0).getUuid());
        assertEquals("item3 should be be the second detected duplicate",
                item3.getID(), potentialDuplicates.get(1).getUuid());

        // Check metadata is populated as per configuration, using item1 (first in results)
        // Check for date
        Optional<String> foundDate = potentialDuplicates.get(0).getMetadataValueList().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().toString('.')
                        .equals("dc.date.issued"))
                .map(MetadataValue::getValue).findFirst();
        assertThat("There should be an issue date found", foundDate.isPresent());
        assertEquals("item1 issue date should match the duplicate obj metadata issue date",
                item1IssueDate, foundDate.get());
        // Check for subject
        Optional<String> foundSubject = potentialDuplicates.get(0).getMetadataValueList().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().toString('.').equals("dc.subject"))
                .map(MetadataValue::getValue).findFirst();
        assertThat("There should be a subject found", foundSubject.isPresent());
        assertEquals("item1 subject should match the duplicate obj metadata subject",
                item1Subject, foundSubject.get());

        // Check for author, which was NOT configured to be copied
        Optional<String> foundAuthor = potentialDuplicates.get(0).getMetadataValueList().stream()
                .filter(metadataValue -> metadataValue.getMetadataField().toString('.')
                        .equals("dc.contributor.author"))
                .map(MetadataValue::getValue).findFirst();
        assertThat("There should NOT be an author found", foundAuthor.isEmpty());

    }

    /**
     * Test that a search for getPotentialDuplicates properly escapes Solr reserved characters
     * e.g. +  -  &&  | |  !  ( )  { }  [ ]  ^  "  ~  *  ?  :  \
     *
     * @throws Exception
     */
    @Test
    public void testSearchDuplicatesWithReservedSolrCharacters() throws Exception {



        Item item4 = ItemBuilder.createItem(context, col)
                .withTitle("Testing: An Important Development Step")
                .withIssueDate(item1IssueDate)
                .withAuthor(item1Author)
                .withSubject(item1Subject)
                .build();
        Item item5 = ItemBuilder.createItem(context, col)
                .withTitle("Testing an important development step")
                .withIssueDate("2012-10-17")
                .withAuthor("Smith, Donald X.")
                .withSubject("ExtraEntry 2")
                .build();

        // Get potential duplicates of item 4 and make sure no exceptions are thrown
        List<PotentialDuplicate> potentialDuplicates = new ArrayList<>();
        try {
            potentialDuplicates = duplicateDetectionService.getPotentialDuplicates(context, item4);
        } catch (SearchServiceException e) {
            fail("Duplicate search with special characters should NOT result in search exception (" +
                    e.getMessage() + ")");
        }

        // Make sure result list is size 1
        int size = 1;
        assertEquals("Potential duplicates of item4 (special characters) should have size " + size,
                size, potentialDuplicates.size());

        // The only member should be item 5
        assertEquals("Item 5 should be be the detected duplicate",
                item5.getID(), potentialDuplicates.get(0).getUuid());

    }

    //configurationService.setProperty("duplicate.comparison.metadata.field", new String[]{"dc.title"});

    /**
     * Test that a search for a very long title which also contains reserved characters
     *
     * @throws Exception
     */
    @Test
    public void testSearchDuplicatesWithVeryLongTitle() throws Exception {

        Item item6 = ItemBuilder.createItem(context, col)
                .withTitle("Testing: This title is over 200 characters long and should behave just the same as a " +
                        "shorter title, with or without reserved characters. This integration test will prove that " +
                        "long titles are detected as potential duplicates.")
                .withIssueDate(item1IssueDate)
                .withAuthor(item1Author)
                .withSubject(item1Subject)
                .build();
        // This item is the same as above, just missing a comma from the title.
        Item item7 = ItemBuilder.createItem(context, col)
                .withTitle("Testing: This title is over 200 characters long and should behave just the same as a " +
                        "shorter title with or without reserved characters. This integration test will prove that " +
                        "long titles are detected as potential duplicates.")
                .withIssueDate("2012-10-17")
                .withAuthor("Smith, Donald X.")
                .withSubject("ExtraEntry 2")
                .build();

        // Get potential duplicates of item 4 and make sure no exceptions are thrown
        List<PotentialDuplicate> potentialDuplicates = new ArrayList<>();
        try {
            potentialDuplicates = duplicateDetectionService.getPotentialDuplicates(context, item6);
        } catch (SearchServiceException e) {
            fail("Duplicate search with special characters (long title) should NOT result in search exception (" +
                    e.getMessage() + ")");
        }

        // Make sure result list is size 1
        int size = 1;
        assertEquals("Potential duplicates of item6 (long title) should have size " + size,
                size, potentialDuplicates.size());

        // The only member should be item 5
        assertEquals("Item 7's long title should match Item 6 as a potential duplicate",
                item7.getID(), potentialDuplicates.get(0).getUuid());

    }

    /**
     * Test that a search for a very long title which also contains reserved characters
     *
     * @throws Exception
     */
    @Test
    public void testSearchDuplicatesExactMatch() throws Exception {

        // Set distance to 0 manually
        configurationService.setProperty("duplicate.comparison.distance", 0);

        Item item8 = ItemBuilder.createItem(context, col)
                .withTitle("This integration test will prove that the edit distance of 0 results in an exact match")
                .withIssueDate(item1IssueDate)
                .withAuthor(item1Author)
                .withSubject(item1Subject)
                .build();
        // This item is the same as above
        Item item9 = ItemBuilder.createItem(context, col)
                .withTitle("This integration test will prove that the edit distance of 0 results in an exact match")
                .withIssueDate("2012-10-17")
                .withAuthor("Smith, Donald X.")
                .withSubject("ExtraEntry")
                .build();
        // This item has one character different, greater than the edit distance
        Item item10 = ItemBuilder.createItem(context, col)
                .withTitle("This integration test will prove that the edit distance of 0 results in an exact match.")
                .withIssueDate("2012-10-17")
                .withAuthor("Smith, Donald X.")
                .withSubject("ExtraEntry")
                .build();

        // Get potential duplicates of item 4 and make sure no exceptions are thrown
        List<PotentialDuplicate> potentialDuplicates = new ArrayList<>();
        try {
            potentialDuplicates = duplicateDetectionService.getPotentialDuplicates(context, item8);
        } catch (SearchServiceException e) {
            fail("Duplicate search with special characters (long title) should NOT result in search exception (" +
                    e.getMessage() + ")");
        }

        // Make sure result list is size 1 - we do NOT expect item 10 to appear
        int size = 1;
        assertEquals("ONLY one exact match should be found (item 9) " + size,
                size, potentialDuplicates.size());

        // The only member should be item 9
        assertEquals("Item 9 should match Item 8 as a potential duplicate",
                item9.getID(), potentialDuplicates.get(0).getUuid());

    }

    @Test
    public void testSearchDuplicatesInWorkflow() throws Exception {
        // Get potential duplicates of item 1:
        // Expected: Public item II should appear as it has the configured levenshtein distance of 1
        context.turnOffAuthorisationSystem();
        //context.setDispatcher("default");
        XmlWorkflowItem workflowItem1 = WorkflowItemBuilder.createWorkflowItem(context, workflowCol)
                .withTitle("Unique title")
                .withSubmitter(eperson)
                .build();
        XmlWorkflowItem workflowItem2 = WorkflowItemBuilder.createWorkflowItem(context, workflowCol)
                .withTitle("Unique title")
                .withSubmitter(eperson)
                .build();

        //indexingService.commit();
        context.restoreAuthSystemState();
        context.setCurrentUser(admin);
        List<PotentialDuplicate> potentialDuplicates =
                duplicateDetectionService.getPotentialDuplicates(context, workflowItem1.getItem());

        // Make sure result list is size 1
        int size = 1;
        assertEquals("Potential duplicates of item1 should have size " + size,
                size, potentialDuplicates.size());

        // The only member should be workflow item 2
        assertEquals("Workflow item 2 should be be the detected duplicate",
                workflowItem2.getItem().getID(), potentialDuplicates.get(0).getUuid());
    }

    /**
     * Test that a search for getPotentialDuplicates with multiple fields configured as comparison value
     * gives the expected results
     *
     * @throws Exception
     */
    @Test
    public void testSearchDuplicatesWithMultipleFields() throws Exception {
        // Set configure to use both title and author fields
        configurationService.setProperty("duplicate.comparison.metadata.field",
                new String[]{"dc.title", "dc.contributor.author"});

        Item item10 = ItemBuilder.createItem(context, col)
                .withTitle("Compare both title and author")
                .withIssueDate(item1IssueDate)
                .withAuthor("Surname, F.")
                .withSubject(item1Subject)
                .build();
        Item item11 = ItemBuilder.createItem(context, col)
                .withTitle("Compare both title and author")
                .withIssueDate("2012-10-17")
                .withAuthor("Surname, F.")
                .withSubject("ExtraEntry 2")
                .build();

        Item item12 = ItemBuilder.createItem(context, col)
                .withTitle("Compare both title and author")
                .withIssueDate("2012-10-17")
                .withAuthor("Lastname, First.")
                .withSubject("ExtraEntry 2")
                .build();

        // Get potential duplicates of item 10 and make sure no exceptions are thrown
        List<PotentialDuplicate> potentialDuplicates = new ArrayList<>();
        try {
            potentialDuplicates = duplicateDetectionService.getPotentialDuplicates(context, item10);
        } catch (SearchServiceException e) {
            fail("Duplicate search with title and author (" +
                    e.getMessage() + ")");
        }

        // Make sure result list is size 1
        int size = 1;
        assertEquals("Potential duplicates of item10 (title + author) should have size " + size,
                size, potentialDuplicates.size());

        // The only member should be item 11 since item 12 has a different author (but hte same title
        assertEquals("Item 11 should be be the detected duplicate",
                item11.getID(), potentialDuplicates.get(0).getUuid());

    }

}
