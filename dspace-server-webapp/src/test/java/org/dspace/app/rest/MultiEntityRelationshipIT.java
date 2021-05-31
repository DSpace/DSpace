/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.authorize.AuthorizeServiceImpl;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Constants;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * The scope of this IT is to verify behaviour of relationship involving all entities on its left or right side,
 * for example "all entities" in relation with Person.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class MultiEntityRelationshipIT extends AbstractEntityIntegrationTest {

    private Item author1;
    private Item author2;
    private Item publication1;
    private Item patent1;

    private RelationshipType selectedResearchOutput;
    private RelationshipType publicationWithJolly;
    private RelationshipType personToPerson;
    private RelationshipType nullEntityToNullRelationship;
    private EPerson user1;

    @Autowired
    private EntityTypeService entityTypeService;
    @Autowired
    private RelationshipTypeService relationshipTypeService;
    @Autowired
    private AuthorizeServiceImpl authorizeService;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        final EntityType personEntity = getEntityType("Person");
        final EntityType publicationEntity = getEntityType("Publication");
        final EntityType patentEntity = getEntityType("Patent");

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        final Community childCommunity = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                         .withName("Sub Community")
                                                         .build();
        final Collection personCollection = CollectionBuilder.createCollection(context, childCommunity)
                                                             .withEntityType("Person")
                                                             .withName("Person").build();

        final Collection publicationCollection = CollectionBuilder.createCollection(context, childCommunity)
                                                                  .withEntityType("Publication")
                                                                  .withName("Publications").build();

        final Collection patentCollection = CollectionBuilder.createCollection(context, childCommunity)
                                                             .withEntityType("Patent")
                                                             .withName("Patent").build();

        author1 = ItemBuilder.createItem(context, personCollection)
                             .withTitle("Author1")
                             .withIssueDate("2017-10-17")
                             .withAuthor("Smith, Donald")
                             .withPersonIdentifierLastName("Smith")
                             .withPersonIdentifierFirstName("Donald")
                             .withEntityType("Person")
                             .build();

        author2 = ItemBuilder.createItem(context, personCollection)
                             .withTitle("Author2")
                             .withIssueDate("2016-02-13")
                             .withAuthor("Smith, Maria")
                             .withEntityType("Person")
                             .build();


        publication1 = ItemBuilder.createItem(context, publicationCollection)
                                  .withTitle("Publication1")
                                  .withAuthor("Testy, TEst")
                                  .withIssueDate("2015-01-01")
                                  .withEntityType("Publication")
                                  .build();

        patent1 = ItemBuilder.createItem(context, patentCollection)
                                  .withTitle("Patent1")
                                  .withAuthor("Testy, Foo")
                                  .withIssueDate("2015-08-01")
                                  .withEntityType("Patent")
                                  .build();

        selectedResearchOutput = RelationshipTypeBuilder
                                     .createRelationshipTypeBuilder(
                                                                      context,
                                                                      null,
                                                                      personEntity,
                                                                      "isResearchoutputsSelectedFor",
                                                                      "hasSelectedResearchoutputs",
                                                                      0, null,
                                                                      0, null).build();

        publicationWithJolly = RelationshipTypeBuilder
                                     .createRelationshipTypeBuilder(
                                         context,
                                         publicationEntity,
                                         null,
                                         "isPublicationRelatedTo",
                                         "relatedFrom",
                                         0, null,
                                         0, null).build();

        personToPerson = RelationshipTypeBuilder
                                     .createRelationshipTypeBuilder(
                                         context,
                                         personEntity,
                                         personEntity,
                                         "isTeamLeaderOf",
                                         "isLeadBy",
                                         0, null,
                                         0, null).build();

        nullEntityToNullRelationship = RelationshipTypeBuilder
                             .createRelationshipTypeBuilder(
                                 context,
                                 null,
                                 null,
                                 "isTeamLeaderOf",
                                 "isLeadBy",
                                 0, null,
                                 0, null).build();

        user1 = EPersonBuilder.createEPerson(context)
                                      .withNameInMetadata("first", "last")
                                      .withEmail("testaze@gmail.com")
                                      .withPassword(password)
                                      .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                                      .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void onlyRightEntityTypeDefined() throws Exception {

        context.turnOffAuthorisationSystem();

        context.setCurrentUser(user1);

        authorizeService.addPolicy(context, publication1, Constants.WRITE, user1);
        authorizeService.addPolicy(context, patent1, Constants.WRITE, user1);

        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<>();
        AtomicReference<Integer> secondId = new AtomicReference<>();
        try {
            String token = getAuthToken(user1.getEmail(), password);

            getClient(token).perform(post("/api/core/relationships")
                                         .param("relationshipType",
                                                selectedResearchOutput.getID().toString())
                                         .contentType(MediaType.parseMediaType(TEXT_URI_LIST_VALUE))
                                         .content(
                                             "https://localhost:8080/server/api/core/items/" + publication1
                                                                                                   .getID() + "\n" +
                                                 "https://localhost:8080/server/api/core/items/" + author1
                                                                                                       .getID()))
                            .andExpect(status().isCreated())
                            .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(token).perform(post("/api/core/relationships")
                                         .param("relationshipType",
                                                selectedResearchOutput.getID().toString())
                                         .contentType(MediaType.parseMediaType(TEXT_URI_LIST_VALUE))
                                         .content(
                                             "https://localhost:8080/server/api/core/items/" + patent1
                                                                                                   .getID() + "\n" +
                                                 "https://localhost:8080/server/api/core/items/" + author1
                                                                                                       .getID()))
                            .andExpect(status().isCreated())
                            .andDo(result -> secondId.set(read(result.getResponse().getContentAsString(), "$.id")));


            getClient().perform(get("/api/core/relationships/" + idRef))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._links.leftItem.href",
                                           containsString(publication1.getID().toString())))
                       .andExpect(jsonPath("$._links.rightItem.href",
                                           containsString(author1.getID().toString())));

            getClient().perform(get("/api/core/relationships/" + secondId))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._links.leftItem.href",
                                           containsString(patent1.getID().toString())))
                       .andExpect(jsonPath("$._links.rightItem.href",
                                           containsString(author1.getID().toString())));
        } finally {
            if (idRef.get() != null) {
                RelationshipBuilder.deleteRelationship(idRef.get());
            }
            if (secondId.get() != null) {
                RelationshipBuilder.deleteRelationship(secondId.get());
            }
        }


    }

    @Test
    public void onlyLeftEntityTypeDefined() throws Exception {

        context.turnOffAuthorisationSystem();

        context.setCurrentUser(user1);

        authorizeService.addPolicy(context, publication1, Constants.WRITE, user1);
        authorizeService.addPolicy(context, patent1, Constants.WRITE, user1);

        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<>();
        AtomicReference<Integer> secondId = new AtomicReference<>();
        try {
            String token = getAuthToken(user1.getEmail(), password);

            getClient(token).perform(post("/api/core/relationships")
                                         .param("relationshipType",
                                                publicationWithJolly.getID().toString())
                                         .contentType(MediaType.parseMediaType(TEXT_URI_LIST_VALUE))
                                         .content(
                                             "https://localhost:8080/server/api/core/items/" + publication1
                                                                                                   .getID() + "\n" +
                                                 "https://localhost:8080/server/api/core/items/" + author1
                                                                                                       .getID()))
                            .andExpect(status().isCreated())
                            .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(token).perform(post("/api/core/relationships")
                                         .param("relationshipType",
                                                publicationWithJolly.getID().toString())
                                         .contentType(MediaType.parseMediaType(TEXT_URI_LIST_VALUE))
                                         .content(
                                             "https://localhost:8080/server/api/core/items/" + publication1
                                                                                                   .getID() + "\n" +
                                                 "https://localhost:8080/server/api/core/items/" + patent1
                                                                                                       .getID()))
                            .andExpect(status().isCreated())
                            .andDo(result -> secondId.set(read(result.getResponse().getContentAsString(), "$.id")));


            getClient().perform(get("/api/core/relationships/" + idRef))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._links.leftItem.href",
                                           containsString(publication1.getID().toString())))
                       .andExpect(jsonPath("$._links.rightItem.href",
                                           containsString(author1.getID().toString())));

            getClient().perform(get("/api/core/relationships/" + secondId))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._links.leftItem.href",
                                           containsString(publication1.getID().toString())))
                       .andExpect(jsonPath("$._links.rightItem.href",
                                           containsString(patent1.getID().toString())));
        } finally {
            if (idRef.get() != null) {
                RelationshipBuilder.deleteRelationship(idRef.get());
            }
            if (secondId.get() != null) {
                RelationshipBuilder.deleteRelationship(secondId.get());
            }
        }


    }

    @Test
    public void bothTypesDefined() throws Exception {

        context.turnOffAuthorisationSystem();

        context.setCurrentUser(user1);

        authorizeService.addPolicy(context, author1, Constants.WRITE, user1);
        authorizeService.addPolicy(context, author2, Constants.WRITE, user1);

        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<>();
        AtomicReference<Integer> secondId = new AtomicReference<>();
        try {
            String token = getAuthToken(user1.getEmail(), password);

            getClient(token).perform(post("/api/core/relationships")
                                         .param("relationshipType",
                                                personToPerson.getID().toString())
                                         .contentType(MediaType.parseMediaType(TEXT_URI_LIST_VALUE))
                                         .content(
                                             "https://localhost:8080/server/api/core/items/" + author2
                                                                                                   .getID() + "\n" +
                                                 "https://localhost:8080/server/api/core/items/" + author1
                                                                                                       .getID()))
                            .andExpect(status().isCreated())
                            .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            getClient(token).perform(post("/api/core/relationships")
                                         .param("relationshipType",
                                                personToPerson.getID().toString())
                                         .contentType(MediaType.parseMediaType(TEXT_URI_LIST_VALUE))
                                         .content(
                                             "https://localhost:8080/server/api/core/items/" + author1
                                                                                                   .getID() + "\n" +
                                                 "https://localhost:8080/server/api/core/items/" + author2
                                                                                                       .getID()))
                            .andExpect(status().isCreated())
                            .andDo(result -> secondId.set(read(result.getResponse().getContentAsString(), "$.id")));


            getClient().perform(get("/api/core/relationships/" + idRef))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._links.leftItem.href",
                                           containsString(author2.getID().toString())))
                       .andExpect(jsonPath("$._links.rightItem.href",
                                           containsString(author1.getID().toString())));

            getClient().perform(get("/api/core/relationships/" + secondId))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._links.leftItem.href",
                                           containsString(author1.getID().toString())))
                       .andExpect(jsonPath("$._links.rightItem.href",
                                           containsString(author2.getID().toString())));
        } finally {
            if (idRef.get() != null) {
                RelationshipBuilder.deleteRelationship(idRef.get());
            }
            if (secondId.get() != null) {
                RelationshipBuilder.deleteRelationship(secondId.get());
            }
        }


    }

    @Test
    public void nullToNullCannotStoreRelation() throws Exception {

        context.turnOffAuthorisationSystem();

        context.setCurrentUser(user1);

        authorizeService.addPolicy(context, author1, Constants.WRITE, user1);
        authorizeService.addPolicy(context, author2, Constants.WRITE, user1);

        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<>();
        AtomicReference<Integer> secondId = new AtomicReference<>();
        try {
            String token = getAuthToken(user1.getEmail(), password);

            getClient(token).perform(post("/api/core/relationships")
                                         .param("relationshipType",
                                                nullEntityToNullRelationship.getID().toString())
                                         .contentType(MediaType.parseMediaType(TEXT_URI_LIST_VALUE))
                                         .content(
                                             "https://localhost:8080/server/api/core/items/" + author2
                                                                                                   .getID() + "\n" +
                                                 "https://localhost:8080/server/api/core/items/" + author1
                                                                                                       .getID()))
                            .andExpect(status().is4xxClientError());
        } finally {
            if (idRef.get() != null) {
                RelationshipBuilder.deleteRelationship(idRef.get());
            }
        }


    }

    @Test
    public void relationshipWithNullEntitiesAreFound() throws SQLException {

        assertThat(relationshipTypeService.findbyTypesAndTypeName(
            context, null, selectedResearchOutput.getRightType(),
            selectedResearchOutput.getLeftwardType(),
            selectedResearchOutput.getRightwardType()),
                   is(selectedResearchOutput));
        assertThat(relationshipTypeService.findbyTypesAndTypeName(
            context, publicationWithJolly.getLeftType(), null,
            publicationWithJolly.getLeftwardType(),
            publicationWithJolly.getRightwardType()),
                   is(publicationWithJolly));

        assertThat(relationshipTypeService.findbyTypesAndTypeName(
            context, personToPerson.getLeftType(), personToPerson.getRightType(),
            personToPerson.getLeftwardType(),
            personToPerson.getRightwardType()),
                   is(personToPerson));

    }

    private EntityType getEntityType(final String entityType) throws SQLException {
        final EntityType result = entityTypeService.findByEntityType(context, entityType);
        return Optional.ofNullable(result)
            .orElseGet(() -> EntityTypeBuilder.createEntityTypeBuilder(context, entityType)
                                              .build());
    }
}
