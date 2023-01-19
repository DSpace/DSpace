/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.dspace.AbstractUnitTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for parsing and utilities on submission config forms / readers
 *
 * @author Kim Shepherd
 */
public class SubmissionConfigTest extends AbstractUnitTest {

    DCInputsReader inputReader;

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws DCInputsReaderException {
        inputReader = new DCInputsReader();
    }

    @After
    public void tearDown() {
        inputReader = null;
    }

    @Test
    public void testReadAndProcessTypeBindSubmissionConfig()
            throws SubmissionConfigReaderException, DCInputsReaderException {
        // Set up test data. This should match the typebind test submission / form config
        String typeBindHandle = "123456789/typebind-test";
        String typeBindSubmissionName = "typebindtest";
        String typeBindSubmissionStepName = "typebindtest";

        // Expected field lists from typebindtest form
        List<String> allConfiguredFields = new ArrayList<>();
        allConfiguredFields.add("dc.title");
        allConfiguredFields.add("dc.date.issued");
        allConfiguredFields.add("dc.type");
        allConfiguredFields.add("dc.identifier.isbn");
        List<String> unboundFields = allConfiguredFields.subList(0, 3);

        // Get submission configuration
        SubmissionConfig submissionConfig =
                new SubmissionConfigReader().getSubmissionConfigByCollection(typeBindHandle);
        // Submission name should match name defined in item-submission.xml
        assertEquals(typeBindSubmissionName, submissionConfig.getSubmissionName());
        // Step 0 - our process only has one step. It should not be null and have the ID typebindtest
        SubmissionStepConfig submissionStepConfig = submissionConfig.getStep(0);
        assertNotNull(submissionStepConfig);
        assertEquals(typeBindSubmissionStepName, submissionStepConfig.getId());
        // Get inputs and allowed fields
        DCInputSet inputConfig = inputReader.getInputsByFormName(submissionStepConfig.getId());
        List<String> allowedFieldsForBook = inputConfig.populateAllowedFieldNames("Book");
        List<String> allowedFieldsForBookChapter = inputConfig.populateAllowedFieldNames("Book chapter");
        List<String> allowedFieldsForArticle = inputConfig.populateAllowedFieldNames("Article");
        List<String> allowedFieldsForNoType = inputConfig.populateAllowedFieldNames(null);
        // Book and book chapter should be allowed all 5 fields (each is bound to dc.identifier.isbn)
        assertEquals(allConfiguredFields, allowedFieldsForBook);
        assertEquals(allConfiguredFields, allowedFieldsForBookChapter);
        // Article and type should match a subset of the fields without ISBN
        assertEquals(unboundFields, allowedFieldsForArticle);
        assertEquals(unboundFields, allowedFieldsForNoType);
    }
}
