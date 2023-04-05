/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.csv;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.matcher.RelationshipMatcher;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ProcessStatus;
import org.dspace.content.Relationship;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

public class CsvImportIT extends AbstractEntityIntegrationTest {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;

    @Test
    public void createRelationshipsWithCsvImportTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                                           .withEntityType("Publication").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                                           .withEntityType("Person").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits")
                                           .withEntityType("Project").build();

        // Create a new Publication (which is an Article)
        Item article = ItemBuilder.createItem(context, col1)
                                  .withTitle("Article")
                                  .withIssueDate("2017-10-17")
                                  .build();

        // Via CSV import, add two Authors to that Publication
        Item author1 = validateSpecificItemRelationCreationCsvImport(col2, article, "TestAuthor1", "Person",
                                                                   "isPublicationOfAuthor",
                                                                   "Relationship list size is 1", 1, 0, 0);
        Item author2 = validateSpecificItemRelationCreationCsvImport(col2, article, "TestAuthor2", "Person",
                                                                   "isPublicationOfAuthor",
                                                                   "Relationship list size is 1", 1, 1, 0);
        // Via CSV import, add a Project related to that Publication
        Item project = validateSpecificItemRelationCreationCsvImport(col3, article, "TestProject", "Project",
                                                                   "isPublicationOfProject",
                                                                   "Relationship list size is 1", 1, 0, 0);
        // Via CSV import, add a new Publication related to both author1 & author2
        Item article2 = validateSpecificItemRelationCreationCsvImportMultiple(col1, "TestArticle2", "Publication",
                                                                           "isAuthorOfPublication",
                                                                           "Relationship list size is 2", 2, 0, 1,
                                                                           author2, author1);

        // Verify that the new Publication is related to both author2 and author1 (in that exact order)
        List<Relationship> relationships = relationshipService.findByItem(context, article2);
        assertEquals(2, relationships.size());
        getClient().perform(get("/api/core/relationships/" + relationships.get(0).getID()).param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.leftPlace", is(0)))
                   .andExpect(jsonPath("$._links.rightItem.href", containsString(author2.getID().toString())))
                   .andExpect(jsonPath("$.rightPlace", is(1)))
                   .andExpect(jsonPath("$", Matchers.is(RelationshipMatcher.matchRelationship(relationships.get(0)))));
        getClient().perform(get("/api/core/relationships/" + relationships.get(1).getID()).param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.leftPlace", is(1)))
                   .andExpect(jsonPath("$._links.rightItem.href", containsString(author1.getID().toString())))
                   .andExpect(jsonPath("$.rightPlace", is(1)))
                   .andExpect(jsonPath("$", Matchers.is(RelationshipMatcher.matchRelationship(relationships.get(1)))));

        // Via CSV import add a new Author to the new Publication, as the *third* author.
        // At this point the new Publication has three authors in this order: author2, author1, author3
        Item author3 = validateSpecificItemRelationCreationCsvImport(col2, article2, "TestAuthor3", "Person",
                                                                   "isPublicationOfAuthor",
                                                                   "Relationship list size is 1", 1, 2, 0);

        // Verify the new Publication now has 3 relationships (3 authors)
        relationships = relationshipService.findByItem(context, article2);
        assertEquals(3,relationships.size());

        // Now, *remove* the first listed author (author2) from this new Publication
        updateArticle2ToDeleteRelationshipToAuthor2(article2, author1, author3, col1, "TestArticle2");

        // Verify new Publication still exists after author removal
        getClient().perform(get("/api/core/items/" + article2.getID())).andExpect(status().isOk());

        // Check relationships to new Publication. Verify it's only related to author1 and author3
        assertArticle2Relationships(article2, author1, author3);

        // Update original Article to now be related to all three authors
        updateArticleItemToAddAnotherRelationship(col1, article, author1, author2, author3);

        getClient().perform(get("/api/core/items/" + article.getID())).andExpect(status().isOk());

        // Verify original article is now related to all three authors
        assertArticleRelationships(article, author1, author2, author3);
    }

    private void assertArticle2Relationships(Item article2, Item author1, Item author3) throws SQLException {
        List<Relationship> relationshipsForArticle2 = relationshipService.findByItem(context, article2);
        assertEquals(2, relationshipsForArticle2.size());
        assertEquals(author3, relationshipsForArticle2.get(0).getRightItem());
        assertEquals(author1, relationshipsForArticle2.get(1).getRightItem());
    }

    private void assertArticleRelationships(Item article, Item author1, Item author2, Item author3)
        throws SQLException {
        List<Relationship> relationshipsForArticle = relationshipService
            .findByItemAndRelationshipType(context, article, relationshipTypeService
                .findbyTypesAndTypeName(context, entityTypeService.findByEntityType(context, "Publication"),
                                        entityTypeService.findByEntityType(context, "Person"),
                                        "isAuthorOfPublication", "isPublicationOfAuthor"));
        assertEquals(3, relationshipsForArticle.size());
        List<Item> expectedRelationshipsItemsForArticle = new ArrayList<>();
        expectedRelationshipsItemsForArticle.add(author2);
        expectedRelationshipsItemsForArticle.add(author3);
        expectedRelationshipsItemsForArticle.add(author1);

        List<Item> actualRelationshipsItemsForArticle = new ArrayList<>();
        for (Relationship relationship : relationshipsForArticle) {
            if (relationship.getLeftItem().getID() == article.getID()) {
                actualRelationshipsItemsForArticle.add(relationship.getLeftItem());
            } else {
                actualRelationshipsItemsForArticle.add(relationship.getRightItem());
            }
        }
        assertTrue(actualRelationshipsItemsForArticle.containsAll(expectedRelationshipsItemsForArticle));
    }

    private void updateArticleItemToAddAnotherRelationship(Collection col1, Item article, Item author1, Item author2,
                                                           Item author3) throws Exception {
        String csvLineString = article.getID().toString() + "," + col1
            .getHandle() + "," + "Article" + "," + "Publication" + "," +
            author1.getID().toString() + "||" + author2.getID().toString() + "||" + author3
            .getID().toString();
        String[] csv = {"id,collection,dc.title,dspace.entity.type,relation." + "isAuthorOfPublication", csvLineString};
        performImportScript(csv);
    }

    private void updateArticle2ToDeleteRelationshipToAuthor2(Item article2, Item author1, Item author3,
                                                             Collection owningCollection, String title)
        throws Exception {
        String csvLineString = article2.getID().toString() + "," + owningCollection
            .getHandle() + "," + title + "," + "Publication" + "," + author1.getID().toString() + "||" + author3.getID()
                                                                                                       .toString();
        String[] csv = {"id,collection,dc.title,dspace.entity.type,relation." + "isAuthorOfPublication", csvLineString};
        performImportScript(csv);

    }

    private Item validateSpecificItemRelationCreationCsvImport(Collection col1, Item relatedItem, String itemTitle,
                                                               String entityType,
                                                               String relationshipTypeLabel,
                                                               String reasonAssertCheck, int sizeToCheck,
                                                               int leftPlaceToCheck,
                                                               int rightPlaceToCheck) throws Exception {
        String csvLineString = "+," + col1.getHandle() + "," + itemTitle + "," + entityType + "," + relatedItem
            .getID().toString();
        String[] csv = {"id,collection,dc.title,dspace.entity.type,relation." + relationshipTypeLabel, csvLineString};
        performImportScript(csv);

        Iterator<Item> itemIteratorItem = itemService.findByMetadataField(context, "dc", "title", null, itemTitle);
        Item item = itemIteratorItem.next();

        List<Relationship> relationships = relationshipService.findByItem(context, item);
        assertEquals(reasonAssertCheck, sizeToCheck, relationships.size());
        getClient().perform(get("/api/core/items/" + item.getID())).andExpect(status().isOk());
        getClient().perform(get("/api/core/relationships/" + relationships.get(0).getID()).param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.leftPlace", is(leftPlaceToCheck)))
                   .andExpect(jsonPath("$.rightPlace", is(rightPlaceToCheck)))
                   .andExpect(jsonPath("$", Matchers.is(RelationshipMatcher.matchRelationship(relationships.get(0)))));

        return item;
    }

    private Item validateSpecificItemRelationCreationCsvImportMultiple(Collection col1, String itemTitle,
                                                                       String entityType,
                                                                       String relationshipTypeLabel,
                                                                       String reasonAssertCheck, Integer sizeToCheck,
                                                                       Integer leftPlaceToCheck,
                                                                       Integer rightPlaceToCheck, Item... relatedItem)
        throws Exception {
        String idStringRelatedItems = "";
        for (Item item : relatedItem) {
            idStringRelatedItems += item.getID().toString();
            idStringRelatedItems += "||";
        }
        idStringRelatedItems = idStringRelatedItems.substring(0, idStringRelatedItems.length() - 2);
        String csvLineString = "+," + col1
            .getHandle() + "," + itemTitle + "," + entityType + "," + idStringRelatedItems;
        String[] csv = {"id,collection,dc.title,dspace.entity.type,relation." + relationshipTypeLabel, csvLineString};
        performImportScript(csv);
        Iterator<Item> itemIteratorItem = itemService.findByMetadataField(context, "dc", "title", null, itemTitle);
        Item item = itemIteratorItem.next();


        return item;
    }

    private void performImportScript(String[] csv) throws Exception {
        InputStream inputStream = new ByteArrayInputStream(String.join(System.lineSeparator(),
                                                                       Arrays.asList(csv))
                                                                 .getBytes(StandardCharsets.UTF_8));

        MockMultipartFile bitstreamFile = new MockMultipartFile("file",
                                                                "test.csv", MediaType.TEXT_PLAIN_VALUE,
                                                                inputStream);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-f", "test.csv"));
        parameters.add(new DSpaceCommandLineParameter("-s", ""));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        try {
            String token = getAuthToken(admin.getEmail(), password);

            getClient(token)
                .perform(multipart("/api/system/scripts/metadata-import/processes").file(bitstreamFile)
                                          .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andDo(result -> idRef
                    .set(read(result.getResponse().getContentAsString(), "$.processId")));
            String t = "";
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void csvImportWithSpecifiedEPersonParameterTestShouldFailProcess() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                                           .withEntityType("Publication").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        Collection col3 = CollectionBuilder.createCollection(context, child1).withName("OrgUnits").build();

        Item article = ItemBuilder.createItem(context, col1)
                                  .withTitle("Article")
                                  .withIssueDate("2017-10-17")
                                  .build();

        String csvLineString = "+," + col1.getHandle() + ",TestItemB,Person," + article
            .getID().toString();
        String[] csv = {"id,collection,dc.title,dspace.entity.type,relation.isPublicationOfAuthor", csvLineString};

        InputStream inputStream = new ByteArrayInputStream(String.join(System.lineSeparator(),
                                                                       Arrays.asList(csv))
                                                                 .getBytes(StandardCharsets.UTF_8));

        MockMultipartFile bitstreamFile = new MockMultipartFile("file",
                                                                "test.csv", MediaType.TEXT_PLAIN_VALUE,
                                                                inputStream);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-f", "test.csv"));
        parameters.add(new DSpaceCommandLineParameter("-s", ""));
        parameters.add(new DSpaceCommandLineParameter("-e", "dspace@dspace.com"));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                      .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        try {
            String token = getAuthToken(admin.getEmail(), password);

            getClient(token)
                .perform(multipart("/api/system/scripts/metadata-import/processes").file(bitstreamFile)
                                             .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", is(
                    ProcessMatcher.matchProcess("metadata-import",
                                                String.valueOf(admin.getID()), parameters,
                                                ProcessStatus.FAILED))))
                .andDo(result -> idRef
                    .set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }

        Iterator<Item> itemIteratorItem = itemService.findByMetadataField(context, "dc", "title", null, "TestItemB");
        assertFalse(itemIteratorItem.hasNext());
    }
}
