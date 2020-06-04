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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.logic.condition.MetadataValueMatchCondition;
import org.dspace.content.logic.operator.And;
import org.dspace.content.logic.operator.Nand;
import org.dspace.content.logic.operator.Nor;
import org.dspace.content.logic.operator.Not;
import org.dspace.content.logic.operator.Or;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.WorkspaceItemService;
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
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    private MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    private MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();

    // Logger
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LogicalFilterTest.class);

    // Items and repository structure for testing
    Community owningCommunity;
    Collection collection;
    Item itemOne;
    Item itemTwo;

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
            this.owningCommunity = communityService.create(null, context);
            this.collection = collectionService.create(context, owningCommunity);
            WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
            this.itemOne = installItemService.installItem(context, workspaceItem);
            workspaceItem = workspaceItemService.create(context, collection, false);
            this.itemTwo = installItemService.installItem(context, workspaceItem);
            // Initialise metadata field for later testing with both items
            this.metadataField = metadataFieldService.findByElement(context,
                MetadataSchemaEnum.DC.getName(), element, qualifier);
            context.restoreAuthSystemState();
        } catch (AuthorizeException ex) {
            log.error("Authorize Error in init", ex);
            fail("Authorize Error in init: " + ex.getMessage());
        } catch (SQLException ex) {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
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
            itemService.delete(context, itemOne);
            collectionService.delete(context, collection);
            communityService.delete(context, owningCommunity);
        } catch (Exception e) {
            // ignore
        }
        context.restoreAuthSystemState();

        // Set all class members to null
        owningCommunity = null;
        collection = null;
        itemOne = null;
        itemTwo = null;
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
        // Try tests - the item can be null, as the statements are simply returning booleans themselves
        try {
            // Set to True, True (expect True)
            and.setStatements(trueStatements);
            assertTrue("AND operator did not return true for a list of true statements",
                and.getResult(context, null));
            // Set to True, False (expect False)
            and.setStatements(trueFalseStatements);
            assertFalse("AND operator did not return false for a list of statements with at least one false",
                and.getResult(context, null));
            // Set to False, False (expect False)
            and.setStatements(falseStatements);
            assertFalse("AND operator did not return false for a list of false statements",
                and.getResult(context, null));
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
        // Try tests - the item can be null, as the statements are simply returning booleans themselves
        try {
            // Set to True, True (expect True)
            or.setStatements(trueStatements);
            assertTrue("OR operator did not return true for a list of true statements",
                or.getResult(context, null));
            // Set to True, False (expect True)
            or.setStatements(trueFalseStatements);
            assertTrue("OR operator did not return true for a list of statements with at least one false",
                or.getResult(context, null));
            // Set to False, False (expect False)
            or.setStatements(falseStatements);
            assertFalse("OR operator did not return false for a list of false statements",
                or.getResult(context, null));
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
        // Try tests - the item can be null, as the statements are simply returning booleans themselves
        try {
            // Set to True, True (expect False)
            nand.setStatements(trueStatements);
            assertFalse("NAND operator did not return false for a list of true statements",
                nand.getResult(context, null));
            // Set to True, False (expect True)
            nand.setStatements(trueFalseStatements);
            assertTrue("NAND operator did not return true for a list of statements with at least one false",
                nand.getResult(context, null));
            // Set to False, False (expect True)
            nand.setStatements(falseStatements);
            assertTrue("NAND operator did not return true for a list of false statements",
                nand.getResult(context, null));
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
        // Try tests - the item can be null, as the statements are simply returning booleans themselves
        try {
            // Set to True, True (expect False)
            nor.setStatements(trueStatements);
            assertFalse("NOR operator did not return false for a list of true statements",
                nor.getResult(context, null));
            // Set to True, False (expect False)
            nor.setStatements(trueFalseStatements);
            assertFalse("NOR operator did not return false for a list of statements with a true and a false",
                nor.getResult(context, null));
            // Set to False, False (expect True)
            nor.setStatements(falseStatements);
            assertTrue("NOR operator did not return true for a list of false statements",
                nor.getResult(context, null));
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
        // Try tests - the item can be null, as the statements are simply returning booleans themselves
        try {
            // Set to True (expect False)
            not.setStatements(trueStatementOne);
            assertFalse("NOT operator did not return false for a true statement",
                not.getResult(context, null));
            // Set to False (expect True)
            not.setStatements(falseStatementOne);
            assertTrue("NOT operator did not return true for a false statement",
                not.getResult(context, null));
        } catch (LogicalStatementException e) {
            log.error(e.getMessage());
            fail("LogicalStatementException thrown testing the NOT operator" + e.getMessage());
        }
    }

    /**
     * Test a simple filter with a single logical statement: the MetadataValueMatchCondition
     * looking for a dc.title field beginning with "TEST"
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
        DefaultFilter metadataMatchFilter = new DefaultFilter();
        //Filter metadataMatchFilter = DSpaceServicesFactory.getInstance().getServiceManager()
        //    .getServiceByName("starts_with_title_filter", DefaultFilter.class);
        log.debug("Filter class: " + metadataMatchFilter.getClass());
        // Create condition to match pattern on dc.title metadata
        MetadataValueMatchCondition condition = new MetadataValueMatchCondition();
        condition.setItemService(ContentServiceFactory.getInstance().getItemService());
        Map<String, Object> parameters = new HashMap<>();
        // Match on the dc.title field
        parameters.put("field", "dc.title");
        // "Starts with "TEST" (case sensitive)
        parameters.put("pattern", "^TEST");
        // Set up condition with these parameters and add it as the sole statement to the metadata filter
        try {
            condition.setParameters(parameters);
            metadataMatchFilter.setStatement(condition);
            // Test the filter on the first item - expected outcome is true
            assertTrue("itemOne unexpectedly did not match the 'dc.title starts with TEST' test",
                metadataMatchFilter.getResult(context, itemOne));
            // Test the filter on the second item - expected outcome is false
            assertFalse("itemTwo unexpectedly matched the 'dc.title starts with TEST' test",
                metadataMatchFilter.getResult(context, itemTwo));
        } catch (LogicalStatementException e) {
            log.error(e.getMessage());
            fail("LogicalStatementException thrown testing the MetadataValueMatchCondition filter" + e.getMessage());
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
