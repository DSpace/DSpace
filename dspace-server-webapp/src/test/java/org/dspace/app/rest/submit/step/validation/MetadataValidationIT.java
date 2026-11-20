/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.validation.MetadataValidator;
import org.dspace.validation.model.ValidationError;
import org.junit.Before;
import org.junit.Test;


public class MetadataValidationIT extends AbstractIntegrationTestWithDatabase {

    private static final String RELATIONSHIP_TYPE = "isAuthorOfPublication";
    private static final String AUTHOR_FIELD = "dc.contributor.author";

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private final DCInputsReader reader = mock(DCInputsReader.class);

    private MetadataValidator validator;
    private WorkspaceItem workspaceItem;
    private Item item;
    private EntityType publicationType;
    private EntityType personType;

    @Before
    public void setup() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();

        publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        personType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        Collection publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                            .withEntityType("Publication")
                                                            .build();

        workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, publicationCollection).build();
        item = workspaceItem.getItem();

        validator = new MetadataValidator() {
            @Override
            public DCInputsReader getInputReader() {
                return reader;
            }
        };
        validator.setItemService(itemService);
        validator.setMetadataAuthorityService(ContentAuthorityServiceFactory.getInstance()
                                                                             .getMetadataAuthorityService());
        validator.setConfigurationService(DSpaceServicesFactory.getInstance().getConfigurationService());
    }

    @Test
    public void requiredRelationshipAcceptsPlainTextMetadata() throws Exception {
        itemService.addMetadata(context, item, "dc", "contributor", "author", null, "An author");

        assertTrue(validate(relationshipInput(true, Map.of())).isEmpty());
    }

    @Test
    public void plainTextFallbackStillHasItsMetadataValidated() throws Exception {
        itemService.addMetadata(context, item, "dc", "contributor", "author", null, "An author");

        List<ValidationError> errors = validate(relationshipInput(true, Map.of("regex", ".+,.+")));

        assertEquals(1, errors.size());
        assertEquals("error.validation.regex", errors.get(0).getMessage());
    }

    @Test
    public void requiredRelationshipAcceptsRelationshipMetadata() throws Exception {
        buildAuthorRelationship();

        assertTrue(validate(relationshipInput(true, Map.of())).isEmpty());
    }

    @Test
    public void requiredRelationshipAcceptsStoredRelationshipWithoutLinkedMetadata() throws Exception {
        buildAuthorRelationship();

        assertTrue(validate(relationshipInput(true, Map.of())).isEmpty());
        assertTrue(validate(relationshipInput(false, Map.of())).isEmpty());
    }

    @Test
    public void requiredRelationshipWithoutAnyValueReportsOneError() throws Exception {
        List<ValidationError> errors = validate(relationshipInput(true, Map.of()));

        assertEquals(1, errors.size());
        assertEquals("error.validation.required", errors.get(0).getMessage());
        assertEquals(List.of("/sections/publication/" + AUTHOR_FIELD), errors.get(0).getPaths());
    }

    @Test
    public void requiredRelationshipWithoutLinkedMetadataReportsMissingRelationship() throws Exception {
        List<ValidationError> errors = validate(relationshipInput(false, Map.of()));

        assertEquals(1, errors.size());
        assertEquals("error.validation.required", errors.get(0).getMessage());
    }

    @Test
    public void hiddenOrDisallowedRelationshipIsNotRequired() throws Exception {
        assertTrue(validate(relationshipInput(true, Map.of("visibility", "workflow"))).isEmpty());
        assertTrue(validate(relationshipInput(true, Map.of("type-bind", "Book"))).isEmpty());
    }

    @Test
    public void optionalRelationshipWithoutValuesIsValid() throws Exception {
        assertTrue(validate(relationshipInput(true, Map.of("required", ""))).isEmpty());
    }

    @Test
    public void plainMetadataFieldRemainsRequired() throws Exception {
        Map<String, String> fields = new HashMap<>();
        fields.put("dc-schema", "dc");
        fields.put("dc-element", "contributor");
        fields.put("dc-qualifier", "author");
        fields.put("required", "An author is required");

        assertEquals("error.validation.required", validate(fields).get(0).getMessage());

        itemService.addMetadata(context, item, "dc", "contributor", "author", null, "An author");
        assertTrue(validate(fields).isEmpty());
    }

    /**
     * Builds a real {@link RelationshipType} named {@value #RELATIONSHIP_TYPE} together with an
     * author {@link Item} and a {@link org.dspace.content.Relationship} linking it to the
     * submission's item. Once built, DSpace's own virtual metadata machinery will expose the
     * relationship as {@code relation.isAuthorOfPublication} metadata on the submission's item.
     */
    private void buildAuthorRelationship() throws Exception {
        Collection personCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                       .withEntityType("Person")
                                                       .build();

        RelationshipType isAuthorOfPublication = RelationshipTypeBuilder
            .createRelationshipTypeBuilder(context, publicationType, personType, RELATIONSHIP_TYPE,
                                           "isPublicationOfAuthor", 0, null, 0, null)
            .build();

        Item author = ItemBuilder.createItem(context, personCollection).withTitle("An author").build();

        RelationshipBuilder.createRelationshipBuilder(context, item, author, isAuthorOfPublication).build();
    }

    private List<ValidationError> validate(Map<String, String> fields) throws Exception {
        DCInputSet inputSet = new DCInputSet(reader, "publication", List.of(List.of(fields)), Map.of());
        when(reader.getInputsByFormName("publication")).thenReturn(inputSet);
        return validator.validate(context, workspaceItem, new SubmissionStepConfig(Map.of("id", "publication")));
    }

    private Map<String, String> relationshipInput(boolean linkedMetadata, Map<String, String> additionalFields) {
        Map<String, String> fields = new HashMap<>(additionalFields);
        fields.put("relationship-type", RELATIONSHIP_TYPE);
        fields.putIfAbsent("required", "An author is required");
        fields.put("repeatable", "true");
        if (linkedMetadata) {
            fields.put("dc-schema", "dc");
            fields.put("dc-element", "contributor");
            fields.put("dc-qualifier", "author");
            fields.put("input-type", "onebox");
        }
        return fields;
    }

}
