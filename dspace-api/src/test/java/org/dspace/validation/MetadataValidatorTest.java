/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.util.TypeBindUtils;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.validation.model.ValidationError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MetadataValidatorTest {

    private MetadataValidator metadataValidator;

    @Mock
    private DCInputsReader inputReader;

    @Mock
    private ItemService itemService;

    @Mock
    private MetadataAuthorityService metadataAuthorityService;

    @Mock
    private RelationshipTypeService relationshipTypeService;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private TypeBindUtils typeBindUtils;

    @Mock
    private Context context;

    @Mock
    private InProgressSubmission<?> obj;

    @Mock
    private Item item;

    @Mock
    private Collection collection;

    @Mock
    private SubmissionStepConfig config;

    @Mock
    private DCInputSet inputConfig;

    private static final String NOT_REPEATABLE_ERROR = "error.validation.notRepeatable";

    @Before
    public void setUp() throws Exception {
        metadataValidator = new MetadataValidator();
        metadataValidator.setInputReader(inputReader);
        metadataValidator.setItemService(itemService);
        metadataValidator.setMetadataAuthorityService(metadataAuthorityService);
        metadataValidator.setRelationshipTypeService(relationshipTypeService);
        metadataValidator.setRelationshipService(relationshipService);
        metadataValidator.setConfigurationService(configurationService);
        metadataValidator.setTypeBindUtils(typeBindUtils);

        // Creates the mock submission step 'testStep'
        when(obj.getItem()).thenReturn(item);
        when(obj.getCollection()).thenReturn(collection);
        when(config.getId()).thenReturn("testStep");
        when(inputReader.getInputsByFormName("testStep")).thenReturn(inputConfig);

        // Mock metadataAuthorityService to return a key for the field
        when(metadataAuthorityService.makeFieldKey("dc", "contributor", "author"))
            .thenReturn("dc_contributor_author");
        when(metadataAuthorityService.isAuthorityControlled("dc_contributor_author"))
            .thenReturn(false);
    }

    @Test
    public void testValidateRepeatableFieldWithDifferentTypeBindings() {
        // One for Article (repeatable)
        DCInput inputArticle = mock(DCInput.class);
        when(inputArticle.getSchema()).thenReturn("dc");
        when(inputArticle.getElement()).thenReturn("contributor");
        when(inputArticle.getQualifier()).thenReturn("author");
        when(inputArticle.getFieldName()).thenReturn("dc.contributor.author");
        when(inputArticle.isRepeatable()).thenReturn(true);
        when(inputArticle.isAllowedFor("Article")).thenReturn(true);
        when(inputArticle.isAllowedFor("Thesis")).thenReturn(false);
        when(inputArticle.validate(any())).thenReturn(true);

        // One for Thesis (not repeatable)
        DCInput inputThesis = mock(DCInput.class);
        when(inputThesis.getSchema()).thenReturn("dc");
        when(inputThesis.getElement()).thenReturn("contributor");
        when(inputThesis.getQualifier()).thenReturn("author");
        when(inputThesis.getFieldName()).thenReturn("dc.contributor.author");
        when(inputThesis.isRepeatable()).thenReturn(false);
        when(inputThesis.isAllowedFor("Article")).thenReturn(false);
        when(inputThesis.isAllowedFor("Thesis")).thenReturn(true);
        when(inputThesis.validate(any())).thenReturn(true);

        // Add them to the inputConfig
        DCInput[][] fields = new DCInput[][] { {inputArticle}, {inputThesis} };
        when(inputConfig.getFields()).thenReturn(fields);

        // Allowed field names for population
        when(inputConfig.populateAllowedFieldNames(any())).thenAnswer(invocation -> {
            List<String> types = invocation.getArgument(0);
            List<String> allowed = new ArrayList<>();
            if (types.contains("Article") || types.contains("Thesis")) {
                allowed.add("dc.contributor.author");
            }
            return allowed;
        });

        // Creates the 2 author metadata values
        MetadataValue author1 = mock(MetadataValue.class);
        when(author1.getValue()).thenReturn("Author 1");
        MetadataValue author2 = mock(MetadataValue.class);
        when(author2.getValue()).thenReturn("Author 2");
        List<MetadataValue> authors = List.of(author1, author2);
        when(itemService.getMetadataByMetadataString(item, "dc.contributor.author")).thenReturn(authors);

        List<ValidationError> errors;

        // Scenario 1: Item is an 'Article' with 2 authors - no errors
        MetadataValue typeArticle = mock(MetadataValue.class);
        when(typeArticle.getValue()).thenReturn("Article");
        when(typeBindUtils.getTypeBindMetadataValues(obj)).thenReturn(List.of(typeArticle));
        errors = metadataValidator.validate(context, obj, config);
        assertTrue(
            "Item of type 'Article' with 2 authors should be valid (repeatable=true)",
            errors.isEmpty()
        );

        // Scenario 2: Item is a 'Thesis' with 2 authors - should cause a 'NOT_REPEATABLE_ERROR' error
        MetadataValue typeThesis = mock(MetadataValue.class);
        when(typeThesis.getValue()).thenReturn("Thesis");
        when(typeBindUtils.getTypeBindMetadataValues(obj)).thenReturn(List.of(typeThesis));
        errors = metadataValidator.validate(context, obj, config);
        assertEquals(
            "Item of type 'Thesis' with 2 authors should have a validation error (repeatable=false)",
            1,
            errors.size()
        );
        assertEquals(NOT_REPEATABLE_ERROR, errors.getFirst().getMessage());

        // Scenario 3: Item is a 'Thesis' with 1 author - no errors
        when(itemService.getMetadataByMetadataString(item, "dc.contributor.author"))
            .thenReturn(List.of(author1));
        errors = metadataValidator.validate(context, obj, config);
        assertTrue(
            "Item of type 'Thesis' with 1 author should be valid",
            errors.isEmpty()
        );
    }
}
