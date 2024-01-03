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
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.AbstractUnitTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.submit.factory.SubmissionServiceFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Tests for parsing and utilities on submission config forms / readers
 *
 * @author Kim Shepherd
 */
public class SubmissionConfigTest extends AbstractUnitTest {

    DCInputsReader inputReader;

    @Mock
    Community topcom;

    @Mock
    Community subcom1;

    @Mock
    Community subcom2;

    @Mock
    private Collection col1;

    @Mock
    private Collection col2;

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

        when(col1.getHandle()).thenReturn(typeBindHandle);

        // Expected field lists from typebindtest form
        List<String> allConfiguredFields = new ArrayList<>();
        allConfiguredFields.add("dc.title");
        allConfiguredFields.add("dc.date.issued");
        allConfiguredFields.add("dc.type");
        allConfiguredFields.add("dc.identifier.isbn");
        List<String> unboundFields = allConfiguredFields.subList(0, 3);

        // Get submission configuration
        SubmissionConfig submissionConfig =
                SubmissionServiceFactory.getInstance().getSubmissionConfigService()
                    .getSubmissionConfigByCollection(col1);
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

    @Test
    public void testSubmissionMapByCommunityHandleSubmissionConfig()
        throws SubmissionConfigReaderException, DCInputsReaderException, SQLException {

        // Sep up a structure with one top community and two subcommunities
        // with one collection
        when(col1.getHandle()).thenReturn("123456789/not-mapped1");
        when(col1.getCommunities()).thenReturn(List.of(subcom1));

        when(col2.getHandle()).thenReturn("123456789/not-mapped2");
        when(col2.getCommunities()).thenReturn(List.of(subcom2));

        when(subcom1.getHandle()).thenReturn("123456789/subcommunity-test");

        when(subcom2.getParentCommunities()).thenReturn(List.of(topcom));
        when(subcom2.getHandle()).thenReturn("123456789/not-mapped3");

        when(topcom.getHandle()).thenReturn("123456789/topcommunity-test");

        // for col1, it should return the item submission form defined for their parent subcom1
        SubmissionConfig submissionConfig1 =
            new SubmissionConfigReader().getSubmissionConfigByCollection(col1);
        assertEquals("subcommunitytest", submissionConfig1.getSubmissionName());

        // for col2, it should return the item submission form defined for topcom
        SubmissionConfig submissionConfig2 =
            new SubmissionConfigReader().getSubmissionConfigByCollection(col2);
        assertEquals("topcommunitytest", submissionConfig2.getSubmissionName());

    }
}
