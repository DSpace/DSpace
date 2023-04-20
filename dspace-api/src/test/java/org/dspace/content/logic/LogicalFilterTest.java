/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.logic.condition.BitstreamCountCondition;
import org.dspace.content.logic.condition.Condition;
import org.dspace.content.logic.condition.InCollectionCondition;
import org.dspace.content.logic.condition.InCommunityCondition;
import org.dspace.content.logic.condition.IsWithdrawnCondition;
import org.dspace.content.logic.condition.MetadataValueMatchCondition;
import org.dspace.content.logic.condition.MetadataValuesMatchCondition;
import org.dspace.content.logic.condition.ReadableByGroupCondition;
import org.dspace.content.logic.operator.And;
import org.dspace.content.logic.operator.Nand;
import org.dspace.content.logic.operator.Nor;
import org.dspace.content.logic.operator.Not;
import org.dspace.content.logic.operator.Or;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for logical filters, conditions and operators
 * @author Kim Shepherd
 */
public class LogicalFilterTest extends AbstractUnitTest {
    // Required services
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    private MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    private MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
    private AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    private GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    // Logger
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LogicalFilterTest.class);

    // Items and repository structure for testing
    Community communityOne;
    Community communityTwo;
    Collection collectionOne;
    Collection collectionTwo;
    Item itemOne;
    Item itemTwo;
    Item itemThree;

    // Some simple statement lists for testing
    List<LogicalStatement> trueStatements;
    List<LogicalStatement> trueFalseStatements;
    List<LogicalStatement> falseStatements;
    LogicalStatement trueStatementOne;
    LogicalStatement falseStatementOne;

    // Field and values used to set title metadata
    String element = "title";
    String qualifier = null;
    MetadataField metadataField;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init() {
        super.init();
        try {
            context.turnOffAuthorisationSystem();
            // Set up logical statement lists for operator testing
            setUpStatements();
            // Set up DSpace resources for condition and filter testing
            // Set up first community, collection and item
            this.communityOne = communityService.create(null, context);
            this.collectionOne = collectionService.create(context, communityOne);
            WorkspaceItem workspaceItem = workspaceItemService.create(context, collectionOne, false);
            this.itemOne = installItemService.installItem(context, workspaceItem);
            // Add one bitstream to item one, but put it in THUMBNAIL bundle
            bundleService.addBitstream(context, bundleService.create(context, itemOne, "THUMBNAIL"),
                bitstreamService.create(context,
                    new ByteArrayInputStream("Item 1 Thumbnail 1".getBytes(StandardCharsets.UTF_8))));
            // Set up second community, collection and item, and third item
            this.communityTwo = communityService.create(null, context);
            this.collectionTwo = collectionService.create(context, communityTwo);
            // Item two
            workspaceItem = workspaceItemService.create(context, collectionTwo, false);
            this.itemTwo = installItemService.installItem(context, workspaceItem);
            // Add two bitstreams to item two
            Bundle bundleTwo = bundleService.create(context, itemTwo, "ORIGINAL");
            bundleService.addBitstream(context, bundleTwo, bitstreamService.create(context,
                new ByteArrayInputStream("Item 2 Bitstream 1".getBytes(StandardCharsets.UTF_8))));
            bundleService.addBitstream(context, bundleTwo, bitstreamService.create(context,
                new ByteArrayInputStream("Item 2 Bitstream 2".getBytes(StandardCharsets.UTF_8))));
            // Item three
            workspaceItem = workspaceItemService.create(context, collectionTwo, false);
            this.itemThree = installItemService.installItem(context, workspaceItem);
            // Add three bitstreams to item three
            Bundle bundleThree = bundleService.create(context, itemThree, "ORIGINAL");
            bundleService.addBitstream(context, bundleThree, bitstreamService.create(context,
                new ByteArrayInputStream("Item 3 Bitstream 1".getBytes(StandardCharsets.UTF_8))));
            bundleService.addBitstream(context, bundleThree, bitstreamService.create(context,
                new ByteArrayInputStream("Item 3 Bitstream 2".getBytes(StandardCharsets.UTF_8))));
            bundleService.addBitstream(context, bundleThree, bitstreamService.create(context,
                new ByteArrayInputStream("Item 3 Bitstream 2".getBytes(StandardCharsets.UTF_8))));

            // Withdraw the second item for later testing
            itemService.withdraw(context, itemTwo);
            // Initialise metadata field for later testing with both items
            this.metadataField = metadataFieldService.findByElement(context,
                MetadataSchemaEnum.DC.getName(), element, qualifier);
            context.restoreAuthSystemState();
        } catch (AuthorizeException | SQLException | IOException e) {
            log.error("Error encountered during init", e);
            fail("Error encountered during init: " + e.getMessage());
        }
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy() {
        context.turnOffAuthorisationSystem();
        // Delete resources
        try {
            itemService.delete(context, itemOne);
            itemService.delete(context, itemTwo);
            itemService.delete(context, itemThree);
            collectionService.delete(context, collectionOne);
            collectionService.delete(context, collectionTwo);
            communityService.delete(context, communityOne);
            communityService.delete(context, communityTwo);
        } catch (Exception e) {
            // ignore
            log.error("Error cleaning up test resources: " + e.getMessage());
        }
        context.restoreAuthSystemState();

        // Set all class members to null
        communityOne = null;
        communityTwo = null;
        collectionOne = null;
        collectionTwo = null;
        itemOne = null;
        itemTwo = null;
        itemThree = null;
        trueStatements = null;
        trueFalseStatements = null;
        falseStatements = null;
        trueStatementOne = null;
        falseStatementOne = null;
        element = null;
        qualifier = null;
        metadataField = null;

        super.destroy();
    }

    /**
     * Test the AND operator with simple lists of logical statements
     */
    @Test
    public void testAndOperator() {
        // Blank operator
        And and = new And();
        // Try tests
        try {
            // Set to True, True (expect True)
            and.setStatements(trueStatements);
            assertTrue("AND operator did not return true for a list of true statements",
                and.getResult(context, itemOne));
            // Set to True, False (expect False)
            and.setStatements(trueFalseStatements);
            assertFalse("AND operator did not return false for a list of statements with at least one false",
                and.getResult(context, itemOne));
            // Set to False, False (expect False)
            and.setStatements(falseStatements);
            assertFalse("AND operator did not return false for a list of false statements",
                and.getResult(context, itemOne));
        } catch (LogicalStatementException e) {
            log.error(e.getMessage());
            fail("LogicalStatementException thrown testing the AND operator" + e.getMessage());
        }
    }

    /**
     * Test the OR operator with simple lists of logical statements
     */
    @Test
    public void testOrOperator() {
        // Blank operator
        Or or = new Or();
        // Try tests
        try {
            // Set to True, True (expect True)
            or.setStatements(trueStatements);
            assertTrue("OR operator did not return true for a list of true statements",
                or.getResult(context, itemOne));
            // Set to True, False (expect True)
            or.setStatements(trueFalseStatements);
            assertTrue("OR operator did not return true for a list of statements with at least one false",
                or.getResult(context, itemOne));
            // Set to False, False (expect False)
            or.setStatements(falseStatements);
            assertFalse("OR operator did not return false for a list of false statements",
                or.getResult(context, itemOne));
        } catch (LogicalStatementException e) {
            log.error(e.getMessage());
            fail("LogicalStatementException thrown testing the OR operator" + e.getMessage());
        }
    }

    /**
     * Test the NAND operator with simple lists of logical statements
     */
    @Test
    public void testNandOperator() {
        // Blank operator
        Nand nand = new Nand();
        // Try tests
        try {
            // Set to True, True (expect False)
            nand.setStatements(trueStatements);
            assertFalse("NAND operator did not return false for a list of true statements",
                nand.getResult(context, itemOne));
            // Set to True, False (expect True)
            nand.setStatements(trueFalseStatements);
            assertTrue("NAND operator did not return true for a list of statements with at least one false",
                nand.getResult(context, itemOne));
            // Set to False, False (expect True)
            nand.setStatements(falseStatements);
            assertTrue("NAND operator did not return true for a list of false statements",
                nand.getResult(context, itemOne));
        } catch (LogicalStatementException e) {
            log.error(e.getMessage());
            fail("LogicalStatementException thrown testing the NAND operator" + e.getMessage());
        }
    }

    /**
     * Test the NOR operator with simple lists of logical statements
     */
    @Test
    public void testNorOperator() {
        // Blank operator
        Nor nor = new Nor();
        // Try tests
        try {
            // Set to True, True (expect False)
            nor.setStatements(trueStatements);
            assertFalse("NOR operator did not return false for a list of true statements",
                nor.getResult(context, itemOne));
            // Set to True, False (expect False)
            nor.setStatements(trueFalseStatements);
            assertFalse("NOR operator did not return false for a list of statements with a true and a false",
                nor.getResult(context, itemOne));
            // Set to False, False (expect True)
            nor.setStatements(falseStatements);
            assertTrue("NOR operator did not return true for a list of false statements",
                nor.getResult(context, itemOne));
        } catch (LogicalStatementException e) {
            log.error(e.getMessage());
            fail("LogicalStatementException thrown testing the NOR operator" + e.getMessage());
        }
    }

    /**
     * Test the NOT operator with simple individual true/false statements
     */
    @Test
    public void testNotOperator() {
        // Blank operator
        Not not = new Not();
        // Try tests
        try {
            // Set to True (expect False)
            not.setStatements(trueStatementOne);
            assertFalse("NOT operator did not return false for a true statement",
                not.getResult(context, itemOne));
            // Set to False (expect True)
            not.setStatements(falseStatementOne);
            assertTrue("NOT operator did not return true for a false statement",
                not.getResult(context, itemOne));
        } catch (LogicalStatementException e) {
            log.error(e.getMessage());
            fail("LogicalStatementException thrown testing the NOT operator" + e.getMessage());
        }
    }

    /**
     * Test a simple filter with a single logical statement: the MetadataValueMatchCondition
     * looking for a dc.title field beginning with "TEST", and an item that doesn't match this test
     */
    @Test
    public void testMetadataValueMatchCondition() {
        try {
            MetadataValue metadataValueOne = metadataValueService.create(context, itemOne, metadataField);
            MetadataValue metadataValueTwo = metadataValueService.create(context, itemTwo, metadataField);
            metadataValueOne.setValue("TEST title should match the condition");
            metadataValueTwo.setValue("This title should not match the condition");
        } catch (SQLException e) {
            fail("Encountered SQL error creating metadata value on item: " + e.getMessage());
        }

        // Instantiate new filter for testing this condition
        DefaultFilter filter = new DefaultFilter();

        // Create condition to match pattern on dc.title metadata
        Condition condition = new MetadataValueMatchCondition();
        condition.setItemService(ContentServiceFactory.getInstance().getItemService());
        Map<String, Object> parameters = new HashMap<>();
        // Match on the dc.title field
        parameters.put("field", "dc.title");
        // "Starts with "TEST" (case sensitive)
        parameters.put("pattern", "^TEST");
        // Set up condition with these parameters and add it as the sole statement to the metadata filter
        try {
            condition.setParameters(parameters);
            filter.setStatement(condition);
            // Test the filter on the first item - expected outcome is true
            assertTrue("itemOne unexpectedly did not match the 'dc.title starts with TEST' test",
                filter.getResult(context, itemOne));
            // Test the filter on the second item - expected outcome is false
            assertFalse("itemTwo unexpectedly matched the 'dc.title starts with TEST' test",
                filter.getResult(context, itemTwo));
        } catch (LogicalStatementException e) {
            log.error(e.getMessage());
            fail("LogicalStatementException thrown testing the MetadataValueMatchCondition filter" + e.getMessage());
        }
    }

    /**
     * Test a simple filter with a single logical statement: the MetadataValuesMatchCondition
     * looking for a dc.title field beginning with "TEST" or "ALSO", and an item that doesn't match this test
     */
    @Test
    public void testMetadataValuesMatchCondition() {
        try {
            MetadataValue metadataValueOne = metadataValueService.create(context, itemOne, metadataField);
            MetadataValue metadataValueTwo = metadataValueService.create(context, itemTwo, metadataField);
            MetadataValue metadataValueThree = metadataValueService.create(context, itemThree, metadataField);
            metadataValueOne.setValue("TEST this title should match the condition");
            metadataValueTwo.setValue("This title should match the condition, yEs");
            metadataValueThree.setValue("This title should not match the condition");
        } catch (SQLException e) {
            fail("Encountered SQL error creating metadata value on item: " + e.getMessage());
        }

        // Instantiate new filter for testing this condition
        DefaultFilter filter = new DefaultFilter();

        // Create condition to match pattern on dc.title metadata
        Condition condition = new MetadataValuesMatchCondition();
        condition.setItemService(ContentServiceFactory.getInstance().getItemService());
        Map<String, Object> parameters = new HashMap<>();
        // Match on the dc.title field
        parameters.put("field", "dc.title");

        List<String> patterns = new ArrayList<>();
        // "Starts with "TEST" (case sensitive)
        patterns.add("^TEST");
        // "Ends with 'yes' (case insensitive)
        patterns.add("(?i)yes$");
        // Add the list of possible patterns
        parameters.put("patterns", patterns);

        // Alternate parameters to test for a field where the item has no values
        Map<String, Object> missingParameters = new HashMap<>();
        // Match on the dc.subject field - none of our test items have this field set
        missingParameters.put("field", "dc.subject");
        // Add a pattern to the missing parameters
        missingParameters.put("patterns", new ArrayList<>().add("TEST"));

        // Set up condition with these parameters and add it as the sole statement to the metadata filter
        try {
            condition.setParameters(parameters);
            filter.setStatement(condition);
            // Test the filter on the first item - expected outcome is true
            assertTrue("itemOne unexpectedly did not match the " +
                "'dc.title starts with TEST or ends with yes' test", filter.getResult(context, itemOne));
            // Test the filter on the second item - expected outcome is true
            assertTrue("itemTwo unexpectedly did not match the " +
                "'dc.title starts with TEST or ends with yes' test", filter.getResult(context, itemTwo));
            // Test the filter on the third item - expected outcome is false
            assertFalse("itemThree unexpectedly matched the " +
                "'dc.title starts with TEST or ends with yes' test", filter.getResult(context, itemThree));
            // Set condition and filter to use the missing field instead
            condition.setParameters(missingParameters);
            filter.setStatement(condition);
            // Test this updated filter against the first item - expected outcome is false
            assertFalse("itemOne unexpectedly matched the 'dc.subject contains TEST' test" +
                "(it has no dc.subject metadata value)", filter.getResult(context, itemOne));
        } catch (LogicalStatementException e) {
            log.error(e.getMessage());
            fail("LogicalStatementException thrown testing the MetadataValuesMatchCondition filter" + e.getMessage());
        }
    }

    /**
     * Test a simple filter with a single logical statement: the InCollectionCondition
     * looking for an item that is in collectionOne, and one that is not in collectionOne
     */
    @Test
    public void testInCollectionCondition() {
        // Instantiate new filter for testing this condition
        DefaultFilter filter = new DefaultFilter();
        Condition condition = new InCollectionCondition();
        condition.setItemService(ContentServiceFactory.getInstance().getItemService());
        Map<String, Object> parameters = new HashMap<>();

        // Add collectionOne handle to the collections parameter - ie. we are testing to see if the item is
        // in collectionOne only
        List<String> collections = new ArrayList<>();
        collections.add(collectionOne.getHandle());
        parameters.put("collections", collections);

        try {
            // Set parameters and condition
            condition.setParameters(parameters);
            filter.setStatement(condition);

            // Test the filter on the first item - this item is in collectionOne: expected outcome is true
            assertTrue("itemOne unexpectedly did not match the 'item in collectionOne' test",
                filter.getResult(context, itemOne));
            // Test the filter on the second item - this item is NOT in collectionOne: expected outcome is false
            assertFalse("itemTwo unexpectedly matched the 'item in collectionOne' test",
                filter.getResult(context, itemTwo));
        } catch (LogicalStatementException e) {
            log.error(e.getMessage());
            fail("LogicalStatementException thrown testing the InCollectionCondition filter" + e.getMessage());
        }
    }

    /**
     * Test a simple filter with a single logical statement: the InCommunityCondition
     * looking for an item that is in communityOne, and one that is not in communityOne
     */
    @Test
    public void testInCommunityCondition() {
        // Instantiate new filter for testing this condition
        DefaultFilter filter = new DefaultFilter();
        Condition condition = new InCommunityCondition();
        condition.setItemService(ContentServiceFactory.getInstance().getItemService());
        Map<String, Object> parameters = new HashMap<>();

        // Add communitynOne handle to the communities parameter - ie. we are testing to see if the item is
        // in communityOne only
        List<String> communities = new ArrayList<>();
        communities.add(communityOne.getHandle());
        parameters.put("communities", communities);

        try {
            // Set parameters and condition
            condition.setParameters(parameters);
            filter.setStatement(condition);

            // Test the filter on the first item - this item is in communityOne: expected outcome is true
            assertTrue("itemOne unexpectedly did not match the 'item in communityOne' test",
                filter.getResult(context, itemOne));
            // Test the filter on the second item - this item is NOT in communityOne: expected outcome is false
            assertFalse("itemTwo unexpectedly matched the 'item in communityOne' test",
                filter.getResult(context, itemTwo));
        } catch (LogicalStatementException e) {
            log.error(e.getMessage());
            fail("LogicalStatementException thrown testing the InCommunityCondition filter" + e.getMessage());
        }
    }

    /**
     * Test a simple filter with the IsWithdrawnCondition. During setup, itemTwo was withdrawn.
     */
    @Test
    public void testIsWithdrawnCondition() {
        // Instantiate new filter for testing this condition
        DefaultFilter filter = new DefaultFilter();
        Condition condition = new IsWithdrawnCondition();

        try {
            condition.setItemService(ContentServiceFactory.getInstance().getItemService());
            condition.setParameters(new HashMap<>());
            filter.setStatement(condition);

            // Test the filter on itemOne - this item is not withdrawn: expected outcome is false
            assertFalse("itemOne unexpectedly matched the 'item is withdrawn' test",
                filter.getResult(context, itemOne));
            // Test the filter on itemTwo - this item was withdrawn in setup: expected outcome is true
            assertTrue("itemTwo unexpectedly did NOT match the 'item is withdrawn' test",
                filter.getResult(context, itemTwo));
        } catch (LogicalStatementException e) {
            log.error(e.getMessage());
            fail("LogicalStatementException thrown testing the IsWithdrawnCondition filter" + e.getMessage());
        }
    }

    /**
     * Test a simple filter with the BitstreamCountCondition.
     */
    @Test
    public void testBitstreamCountCondition() {
        // Instantiate new filter for testing this condition
        DefaultFilter filter = new DefaultFilter();
        Condition condition = new BitstreamCountCondition();

        try {
            condition.setItemService(ContentServiceFactory.getInstance().getItemService());

            // Set parameters to check for items with at least 1 and at most 2 bitstreams in the ORIGINAL bundle
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("bundle", "ORIGINAL");
            parameters.put("min", String.valueOf(1));
            parameters.put("max", String.valueOf(2));
            condition.setParameters(parameters);
            filter.setStatement(condition);

            // Test the filter on itemOne - this item has one THUMBNAIL but zero ORIGINAL bitstreams: expect false
            assertFalse("itemOne unexpectedly matched the '>=1 and <=2 ORIGINAL bitstreams' test" +
                    " (it has zero ORIGINAL bitstreams)", filter.getResult(context, itemOne));
            // Test the filter on itemTwo - this item has two ORIGINAL bitstreams: expect true
            assertTrue("itemTwo unexpectedly did NOT match the '>=1 and <=2 ORIGINAL bitstreams' test" +
                    " (it has 2 ORIGINAL bitstreams)", filter.getResult(context, itemTwo));
            // Test the filter on itemTwo - this item has three ORIGINAL bitstreams: expect false
            assertFalse("itemThree unexpectedly did NOT match the '>=1 and <=2 ORIGINAL bitstreams' test" +
                " (it has 3 ORIGINAL bitstreams)", filter.getResult(context, itemThree));
        } catch (LogicalStatementException e) {
            log.error(e.getMessage());
            fail("LogicalStatementException thrown testing the IsWithdrawnCondition filter: " + e.getMessage());
        }
    }

    /**
     * Test a simple filter using the ReadableByGroupCondition
     */
    @Test
    public void testReadableByGroupCondition() {
        // Instantiate new filter for testing this condition
        DefaultFilter filter = new DefaultFilter();
        Condition condition = new ReadableByGroupCondition();

        try {
            condition.setItemService(ContentServiceFactory.getInstance().getItemService());

            // Make item one readable by Test Group
            try {
                context.turnOffAuthorisationSystem();
                Group g = groupService.create(context);
                groupService.setName(g, "Test Group");
                groupService.update(context, g);
                authorizeService.addPolicy(context, itemOne, Constants.READ, g);
                context.restoreAuthSystemState();
            } catch (AuthorizeException | SQLException e) {
                fail("Exception thrown adding group READ policy to item: " + itemOne + ": " + e.getMessage());
            }
            // Set parameters to check for items with Anonymous READ permission
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("group", "Test Group");
            parameters.put("action", "READ");
            condition.setParameters(parameters);
            filter.setStatement(condition);

            // Test the filter on itemOne - this item was explicitly set with expected group READ policy
            assertTrue("itemOne unexpectedly did not match the 'is readable by Test Group' test",
                filter.getResult(context, itemOne));
            // Test the filter on itemTwo - this item has no policies: expect false
            assertFalse("itemTwo unexpectedly matched the 'is readable by Test Group' test",
                filter.getResult(context, itemTwo));
        } catch (LogicalStatementException e) {
            log.error(e.getMessage());
            fail("LogicalStatementException thrown testing the ReadableByGroup filter" + e.getMessage());
        }
    }

    /**
     * Set up some simple statements for testing out operators
     */
    private void setUpStatements() {
        // Simple lambdas to define statements
        // The two class members are used elsewhere, as direct statements for NOT testing
        trueStatementOne = (context, item) -> true;
        LogicalStatement trueStatementTwo = (context, item) -> true;
        falseStatementOne = (context, item) -> false;
        LogicalStatement falseStatementTwo = (context, item) -> false;

        // Create lists and add the statements
        // True, True
        trueStatements = new ArrayList<>();
        trueStatements.add(trueStatementOne);
        trueStatements.add(trueStatementTwo);
        // True, False
        trueFalseStatements = new ArrayList<>();
        trueFalseStatements.add(trueStatementOne);
        trueFalseStatements.add(falseStatementOne);
        // False, False
        falseStatements = new ArrayList<>();
        falseStatements.add(falseStatementOne);
        falseStatements.add(falseStatementTwo);
    }
}
