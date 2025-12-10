/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test to verify the references resolving of
 * {@link ItemReferenceResolverConsumer}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ItemReferenceResolverConsumerIT extends AbstractControllerIntegrationTest {

    private EPerson submitter;

    private Collection publicationCollection;

    private Collection personCollection;

    private InstallItemService installItemService;

    @Autowired
    private ConfigurationService configurationService;

    @Before
    public void setup() throws Exception {

        installItemService = ContentServiceFactory.getInstance().getInstallItemService();

        context.turnOffAuthorisationSystem();

        submitter = EPersonBuilder.createEPerson(context)
                                  .withEmail("submitter@example.com")
                                  .withPassword(password)
                                  .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        publicationCollection = createCollection("Collection of publications", "Publication");
        personCollection = createCollection("Collection of persons", "Person");

        context.setCurrentUser(submitter);
        context.restoreAuthSystemState();

    }

    @Test
    public void testItemReferenceResolverConsumerOrcid() throws SQLException {

        context.turnOffAuthorisationSystem();

        String orcidAuthority = formatWillBeReferencedAuthority("ORCID", "0000-0002-1825-0097");

        Item firstItem = ItemBuilder.createItem(context, publicationCollection)
                                    .withTitle("First Item")
                                    .withAuthor("Author", orcidAuthority)
                                    .build();

        Item secondItem = ItemBuilder.createItem(context, publicationCollection)
                                     .withTitle("Second Item")
                                     .withAuthor("Author", orcidAuthority)
                                     .build();

        context.restoreAuthSystemState();

        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                         orcidAuthority, 0, -1)));

        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                          orcidAuthority, 0, -1)));

        context.turnOffAuthorisationSystem();

        Item itemWithOrcid = ItemBuilder.createItem(context, personCollection)
                                        .withTitle("Author")
                                        .withEntityType("Person")
                                        .withOrcidIdentifier("0000-0002-1825-0097")
                                        .build();

        context.restoreAuthSystemState();

        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                         itemWithOrcid.getID().toString(), 0, 600)));

        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                          itemWithOrcid.getID().toString(), 0, 600)));
    }

    @Test
    public void testItemReferenceResolverConsumerOrcidMetadataTitleToUpdate() throws SQLException {
        //When property cris.item-reference-resolution.override-metadata-value is true update Title
        context.turnOffAuthorisationSystem();
        boolean previousOverrideMetadataValue = configurationService
            .getBooleanProperty("cris.item-reference-resolution.override-metadata-value");
        configurationService.setProperty("cris.item-reference-resolution.override-metadata-value", true);
        String orcidAuthority = formatWillBeReferencedAuthority("ORCID", "0000-0002-1825-0097");

        Item firstItem = ItemBuilder.createItem(context, publicationCollection)
                                    .withTitle("First Item")
                                    .withAuthor("Stephen K.", orcidAuthority)
                                    .build();

        Item secondItem = ItemBuilder.createItem(context, publicationCollection)
                                     .withTitle("Second Item")
                                     .withAuthor("Stephen K.", orcidAuthority)
                                     .build();

        context.restoreAuthSystemState();

        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "Stephen K.", null,
                                                         orcidAuthority, 0, -1)));

        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "Stephen K.", null,
                                                          orcidAuthority, 0, -1)));

        context.turnOffAuthorisationSystem();

        Item itemWithOrcid = ItemBuilder.createItem(context, personCollection)
                                        .withTitle("Stephen King")
                                        .withEntityType("Person")
                                        .withOrcidIdentifier("0000-0002-1825-0097")
                                        .build();

        context.restoreAuthSystemState();

        //Check if item has dc.contributor.author updated with new name
        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "Stephen King", null,
                                                         itemWithOrcid.getID().toString(), 0, 600)));

        //Check if item has dc.contributor.author updated with new name
        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "Stephen King", null,
                                                          itemWithOrcid.getID().toString(), 0, 600)));
        configurationService.setProperty("previousOverrideMetadataValue", previousOverrideMetadataValue);
    }

    @Test
    public void testItemReferenceResolverConsumerOrcidMetadataTitleNotToUpdate() throws SQLException {
        //When cris.item-reference-resolution.override-metadata-value is true update Title
        context.turnOffAuthorisationSystem();
        boolean previousOverrideMetadataValue = configurationService
            .getBooleanProperty("cris.item-reference-resolution.override-metadata-value");
        configurationService.setProperty("cris.item-reference-resolution.override-metadata-value", false);
        String orcidAuthority = formatWillBeReferencedAuthority("ORCID", "0000-0002-1825-0097");

        Item firstItem = ItemBuilder.createItem(context, publicationCollection)
                                    .withTitle("First Item")
                                    .withAuthor("H. P. Lovecraft", orcidAuthority)
                                    .build();

        Item secondItem = ItemBuilder.createItem(context, publicationCollection)
                                     .withTitle("Second Item")
                                     .withAuthor("H. P. Lovecraft", orcidAuthority)
                                     .build();

        context.restoreAuthSystemState();

        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "H. P. Lovecraft", null,
                                                         orcidAuthority, 0, -1)));

        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "H. P. Lovecraft", null,
                                                          orcidAuthority, 0, -1)));

        context.turnOffAuthorisationSystem();

        Item itemWithOrcid = ItemBuilder.createItem(context, personCollection)
                                        .withTitle("Howard Phillips Lovecraft")
                                        .withEntityType("Person")
                                        .withOrcidIdentifier("0000-0002-1825-0097")
                                        .build();

        context.restoreAuthSystemState();

        //Check item doesn't have dc.contributor.author updated with new name
        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "H. P. Lovecraft", null,
                                                         itemWithOrcid.getID().toString(), 0, 600)));

        //Check item doesn't have dc.contributor.author updated with new name
        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "H. P. Lovecraft", null,
                                                          itemWithOrcid.getID().toString(), 0, 600)));
        configurationService.setProperty("previousOverrideMetadataValue", previousOverrideMetadataValue);
    }

    @Test
    public void testItemReferenceResolverConsumerWithRid() throws SQLException {

        context.turnOffAuthorisationSystem();

        String ridAuthority = formatWillBeReferencedAuthority("RID", "0000-1111");

        Item firstItem = ItemBuilder.createItem(context, publicationCollection)
                                    .withTitle("First Item")
                                    .withAuthor("Author", ridAuthority)
                                    .build();

        Item secondItem = ItemBuilder.createItem(context, publicationCollection)
                                     .withTitle("Second Item")
                                     .withAuthor("Author", ridAuthority)
                                     .build();

        context.commit();
        context.restoreAuthSystemState();

        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                         ridAuthority, 0, -1)));

        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                          ridAuthority, 0, -1)));

        context.turnOffAuthorisationSystem();

        Item itemWithRid = ItemBuilder.createItem(context, personCollection)
                                      .withTitle("Author")
                                      .withEntityType("Person")
                                      .withResearcherIdentifier("0000-1111")
                                      .build();

        context.restoreAuthSystemState();

        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                         itemWithRid.getID().toString(), 0, 600)));

        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                          itemWithRid.getID().toString(), 0, 600)));
    }

    @Test
    public void testItemReferenceResolverConsumerWithIsni() throws SQLException {

        context.turnOffAuthorisationSystem();

        String ridAuthority = formatWillBeReferencedAuthority("ISNI", "AAA-BBB");

        Item firstItem = ItemBuilder.createItem(context, publicationCollection)
                                    .withTitle("First Item")
                                    .withAuthor("Author", ridAuthority)
                                    .build();

        Item secondItem = ItemBuilder.createItem(context, publicationCollection)
                                     .withTitle("Second Item")
                                     .withAuthor("Author", ridAuthority)
                                     .build();

        context.commit();
        context.restoreAuthSystemState();

        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                         ridAuthority, 0, -1)));

        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                          ridAuthority, 0, -1)));

        context.turnOffAuthorisationSystem();

        Item itemWithIsni = ItemBuilder.createItem(context, personCollection)
                                       .withTitle("Author")
                                       .withEntityType("Person")
                                       .withIsniIdentifier("AAA-BBB")
                                       .build();

        context.restoreAuthSystemState();

        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                         itemWithIsni.getID().toString(), 0, 600)));

        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                          itemWithIsni.getID().toString(), 0, 600)));
    }

    @Test
    public void testItemReferenceResolverConsumerWithManyReferences() throws SQLException {

        context.turnOffAuthorisationSystem();

        String ridAuthority = formatWillBeReferencedAuthority("RID", "0000-1111");
        String orcidAuthority = formatWillBeReferencedAuthority("ORCID", "0000-0002-1825-0097");

        Item firstItem = ItemBuilder.createItem(context, publicationCollection)
                                    .withTitle("First Item")
                                    .withAuthor("Author", ridAuthority)
                                    .build();

        Item secondItem = ItemBuilder.createItem(context, publicationCollection)
                                     .withTitle("Second Item")
                                     .withAuthor("Author", orcidAuthority)
                                     .build();

        context.commit();
        context.restoreAuthSystemState();

        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                         ridAuthority, 0, -1)));

        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                          orcidAuthority, 0, -1)));

        context.turnOffAuthorisationSystem();

        Item itemWithRid = ItemBuilder.createItem(context, personCollection)
                                      .withTitle("Author")
                                      .withEntityType("Person")
                                      .withOrcidIdentifier("0000-0002-1825-0097")
                                      .withResearcherIdentifier("0000-1111")
                                      .build();

        context.restoreAuthSystemState();

        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                         itemWithRid.getID().toString(), 0, 600)));

        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                          itemWithRid.getID().toString(), 0, 600)));
    }

    @Test
    public void testItemReferenceResolverConsumerWithManyMetadata() throws SQLException {

        context.turnOffAuthorisationSystem();

        String firstRidAuthority = formatWillBeReferencedAuthority("RID", "0000-1111");
        String secondRidAuthority = formatWillBeReferencedAuthority("RID", "2222-3333");

        Item firstItem = ItemBuilder.createItem(context, publicationCollection)
                                    .withTitle("First Item")
                                    .withAuthor("Author", firstRidAuthority)
                                    .build();

        Item secondItem = ItemBuilder.createItem(context, publicationCollection)
                                     .withTitle("Second Item")
                                     .withAuthor("Author", secondRidAuthority)
                                     .build();

        context.commit();
        context.restoreAuthSystemState();

        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                         firstRidAuthority, 0, -1)));

        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                          secondRidAuthority, 0, -1)));

        context.turnOffAuthorisationSystem();

        Item itemWithRid = ItemBuilder.createItem(context, personCollection)
                                      .withTitle("Author")
                                      .withEntityType("Person")
                                      .withResearcherIdentifier("0000-1111")
                                      .withResearcherIdentifier("2222-3333")
                                      .build();

        context.restoreAuthSystemState();

        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                         itemWithRid.getID().toString(), 0, 600)));

        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
                                                          itemWithRid.getID().toString(), 0, 600)));
    }

    @Test
    public void testItemReferenceResolverConsumerWorksOnlyAfterArchiving() throws Exception {

        context.turnOffAuthorisationSystem();

        String orcidAuthority = formatWillBeReferencedAuthority("ORCID", "0000-0002-1825-0097");

        Item item = ItemBuilder.createItem(context, publicationCollection)
                               .withTitle("Publication")
                               .withAuthor("Author", orcidAuthority)
                               .build();

        context.restoreAuthSystemState();

        item = context.reloadEntity(item);
        assertThat(item.getMetadata(), hasItem(with("dc.contributor.author", "Author", null, orcidAuthority, 0, -1)));

        context.turnOffAuthorisationSystem();

        WorkspaceItem author = WorkspaceItemBuilder.createWorkspaceItem(context, personCollection)
                                                   .withTitle("Author")
                                                   .withEntityType("Person")
                                                   .withOrcidIdentifier("0000-0002-1825-0097")
                                                   .build();

        String authorId = author.getItem().getID().toString();

        context.restoreAuthSystemState();

        item = context.reloadEntity(item);
        assertThat(item.getMetadata(), hasItem(with("dc.contributor.author", "Author", null, orcidAuthority, 0, -1)));

        context.turnOffAuthorisationSystem();
        installItemService.installItem(context, author);
        context.commit();
        context.restoreAuthSystemState();

        item = context.reloadEntity(item);
        assertThat(item.getMetadata(), hasItem(with("dc.contributor.author", "Author", null, authorId, 0, 600)));

    }

    @Test
    public void testItemReferenceResolverConsumerUpdateAlsoInProgressItems() throws SQLException {

        context.turnOffAuthorisationSystem();

        String orcidAuthority = formatWillBeReferencedAuthority("ORCID", "0000-0002-1825-0097");

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, publicationCollection)
                                                          .withTitle("First Item")
                                                          .withAuthor("Author", orcidAuthority)
                                                          .build();

        Item item = workspaceItem.getItem();

        context.restoreAuthSystemState();

        item = context.reloadEntity(item);
        assertThat(item.getMetadata(), hasItem(with("dc.contributor.author", "Author", null, orcidAuthority, 0, 600)));

        context.turnOffAuthorisationSystem();

        Item author = ItemBuilder.createItem(context, personCollection)
                                 .withTitle("Author")
                                 .withEntityType("Person")
                                 .withOrcidIdentifier("0000-0002-1825-0097")
                                 .build();

        context.restoreAuthSystemState();

        item = context.reloadEntity(item);
        assertThat(item.getMetadata(),
                   hasItem(with("dc.contributor.author", "Author", null, author.getID().toString(), 0, 600)));
    }

    @Test
    public void testItemReferenceResolverConsumerWithManyReferenceToResolve() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item firstPublication = ItemBuilder.createItem(context, publicationCollection)
                                           .withTitle("First publication")
                                           .withAuthor("Author A",
                                                       formatWillBeReferencedAuthority("ORCID", "0000-0000-0000-0001"))
                                           .withAuthor("Author B",
                                                       formatWillBeReferencedAuthority("ORCID", "0000-0000-0000-0002"))
                                           .withAuthor("Author C",
                                                       formatWillBeReferencedAuthority("ORCID", "0000-0000-0000-0003"))
                                           .build();

        Item secondPublication = ItemBuilder.createItem(context, publicationCollection)
                                            .withTitle("Second Item")
                                            .withAuthor("Author B",
                                                        formatWillBeReferencedAuthority("ORCID", "0000-0000-0000-0002"))
                                            .withAuthor("Author D", formatWillBeReferencedAuthority("RID", "RID-01"))
                                            .build();

        Item thirdPublication = ItemBuilder.createItem(context, publicationCollection)
                                           .withTitle("Second Item")
                                           .withAuthor("Author E", formatWillBeReferencedAuthority("RID", "RID-02"))
                                           .withAuthor("Author F",
                                                       formatWillBeReferencedAuthority("ORCID", "0000-0000-0000-0004"))
                                           .build();

        context.commit();
        context.restoreAuthSystemState();

        firstPublication = context.reloadEntity(firstPublication);
        assertThat(firstPublication.getMetadata(), hasItem(with("dc.contributor.author", "Author A", null,
                                                                formatWillBeReferencedAuthority("ORCID",
                                                                                                "0000-0000-0000-0001"),
                                                                0, -1)));
        assertThat(firstPublication.getMetadata(), hasItem(with("dc.contributor.author", "Author B", null,
                                                                formatWillBeReferencedAuthority("ORCID",
                                                                                                "0000-0000-0000-0002"),
                                                                1, -1)));
        assertThat(firstPublication.getMetadata(), hasItem(with("dc.contributor.author", "Author C", null,
                                                                formatWillBeReferencedAuthority("ORCID",
                                                                                                "0000-0000-0000-0003"),
                                                                2, -1)));

        secondPublication = context.reloadEntity(secondPublication);
        assertThat(secondPublication.getMetadata(), hasItem(with("dc.contributor.author", "Author B", null,
                                                                 formatWillBeReferencedAuthority("ORCID",
                                                                                                 "0000-0000-0000-0002"),
                                                                 0, -1)));
        assertThat(secondPublication.getMetadata(), hasItem(with("dc.contributor.author", "Author D", null,
                                                                 formatWillBeReferencedAuthority("RID", "RID-01"), 1,
                                                                 -1)));

        thirdPublication = context.reloadEntity(thirdPublication);
        assertThat(thirdPublication.getMetadata(), hasItem(with("dc.contributor.author", "Author E", null,
                                                                formatWillBeReferencedAuthority("RID", "RID-02"), 0,
                                                                -1)));
        assertThat(thirdPublication.getMetadata(), hasItem(with("dc.contributor.author", "Author F", null,
                                                                formatWillBeReferencedAuthority("ORCID",
                                                                                                "0000-0000-0000-0004"),
                                                                1, -1)));

        context.turnOffAuthorisationSystem();

        Item authorA = ItemBuilder.createItem(context, personCollection)
                                  .withTitle("Author A")
                                  .withEntityType("Person")
                                  .withOrcidIdentifier("0000-0000-0000-0001")
                                  .build();

        Item authorB = ItemBuilder.createItem(context, personCollection)
                                  .withTitle("Author B")
                                  .withEntityType("Person")
                                  .withOrcidIdentifier("0000-0000-0000-0002")
                                  .build();

        Item authorD = ItemBuilder.createItem(context, personCollection)
                                  .withTitle("Author D")
                                  .withEntityType("Person")
                                  .withResearcherIdentifier("RID-01")
                                  .build();

        context.commit();
        context.restoreAuthSystemState();

        firstPublication = context.reloadEntity(firstPublication);
        assertThat(firstPublication.getMetadata(), hasItem(with("dc.contributor.author", "Author A", null,
                                                                authorA.getID().toString(), 0, 600)));
        assertThat(firstPublication.getMetadata(), hasItem(with("dc.contributor.author", "Author B", null,
                                                                authorB.getID().toString(), 1, 600)));
        assertThat(firstPublication.getMetadata(), hasItem(with("dc.contributor.author", "Author C", null,
                                                                formatWillBeReferencedAuthority("ORCID",
                                                                                                "0000-0000-0000-0003"),
                                                                2, -1)));

        secondPublication = context.reloadEntity(secondPublication);
        assertThat(secondPublication.getMetadata(), hasItem(with("dc.contributor.author", "Author B", null,
                                                                 authorB.getID().toString(), 0, 600)));
        assertThat(secondPublication.getMetadata(), hasItem(with("dc.contributor.author", "Author D", null,
                                                                 authorD.getID().toString(), 1, 600)));

        thirdPublication = context.reloadEntity(thirdPublication);
        assertThat(thirdPublication.getMetadata(), hasItem(with("dc.contributor.author", "Author E", null,
                                                                formatWillBeReferencedAuthority("RID", "RID-02"), 0,
                                                                -1)));
        assertThat(thirdPublication.getMetadata(), hasItem(with("dc.contributor.author", "Author F", null,
                                                                formatWillBeReferencedAuthority("ORCID",
                                                                                                "0000-0000-0000-0004"),
                                                                1, -1)));

    }

    @Test
    public void testItemReferenceResolverConsumerViaRest() throws Exception {

        context.turnOffAuthorisationSystem();

        String orcidAuthority = "will be referenced::ORCID::0000-0002-1825-0097";

        WorkspaceItem firstWsItem = WorkspaceItemBuilder.createWorkspaceItem(context, publicationCollection)
                                                        .withTitle("Submission Item")
                                                        .withIssueDate("2017-10-17")
                                                        .withFulltext("article.pdf", null, "test".getBytes())
                                                        .withAuthor("Mario Rossi", orcidAuthority)
                                                        .withAuthorAffilitation("4Science")
                                                        .withType("Article")
                                                        .withSubmitter(submitter)
                                                        .grantLicense()
                                                        .build();

        WorkspaceItem secondWsItem = WorkspaceItemBuilder.createWorkspaceItem(context, publicationCollection)
                                                         .withTitle("Another submission Item")
                                                         .withIssueDate("2020-10-17")
                                                         .withFulltext("another-article.pdf", null, "test".getBytes())
                                                         .withAuthor("Mario Rossi", orcidAuthority)
                                                         .withType("Article")
                                                         .withSubmitter(submitter)
                                                         .grantLicense()
                                                         .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(submitter.getEmail(), password);

        submitItemViaRest(authToken, firstWsItem.getID());

        getClient(authToken).perform(get(BASE_REST_SERVER_URL + "/api/core/items/{id}", firstWsItem.getItem().getID()))
                            .andExpect(status().isOk())
                            .andExpect(
                                jsonPath("$.metadata['dc.contributor.author'][0].authority", is(orcidAuthority)));

        getClient(authToken).perform(get(BASE_REST_SERVER_URL + "/api/core/items/{id}", secondWsItem.getItem().getID()))
                            .andExpect(status().isOk())
                            .andExpect(
                                jsonPath("$.metadata['dc.contributor.author'][0].authority", is(orcidAuthority)));

        context.turnOffAuthorisationSystem();

        WorkspaceItem authorItem = WorkspaceItemBuilder.createWorkspaceItem(context, personCollection)
                                                       .withTitle("Mario Rossi")
                                                       .withFulltext("cv.pdf", null, "test".getBytes())
                                                       .withOrcidIdentifier("0000-0002-1825-0097")
                                                       .withSubmitter(submitter)
                                                       .grantLicense()
                                                       .build();

        context.restoreAuthSystemState();

        getClient(authToken).perform(get(BASE_REST_SERVER_URL + "/api/core/items/{id}", firstWsItem.getItem().getID()))
                            .andExpect(status().isOk())
                            .andExpect(
                                jsonPath("$.metadata['dc.contributor.author'][0].authority", is(orcidAuthority)));

        getClient(authToken).perform(get(BASE_REST_SERVER_URL + "/api/core/items/{id}", secondWsItem.getItem().getID()))
                            .andExpect(status().isOk())
                            .andExpect(
                                jsonPath("$.metadata['dc.contributor.author'][0].authority", is(orcidAuthority)));

        submitItemViaRest(authToken, authorItem.getID());

        String authorUUID = authorItem.getItem().getID().toString();

        getClient(authToken).perform(get(BASE_REST_SERVER_URL + "/api/core/items/{id}", firstWsItem.getItem().getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.metadata['dc.contributor.author'][0].authority", is(authorUUID)));

        getClient(authToken).perform(get(BASE_REST_SERVER_URL + "/api/core/items/{id}", secondWsItem.getItem().getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.metadata['dc.contributor.author'][0].authority", is(authorUUID)));
    }

    private Collection createCollection(String name, String entityType) throws Exception {
        return createCollection(name, entityType, null);
    }


    private Collection createCollection(String name, String entityType, String submissionDefinition) throws Exception {
        return CollectionBuilder.createCollection(context, parentCommunity)
                                .withName(name)
                                .withEntityType(entityType)
                                .withSubmitterGroup(submitter)
                                .withSubmissionDefinition(submissionDefinition)
                                .build();
    }

    private void submitItemViaRest(String authToken, Integer wsId) throws Exception {
        getClient(authToken).perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
                                         .content("/api/submission/workspaceitems/" + wsId)
                                         .contentType(textUriContentType))
                            .andExpect(status().isCreated());
    }

    private String formatWillBeReferencedAuthority(String authorityPrefix, String value) {
        return AuthorityValueService.REFERENCE + authorityPrefix + "::" + value;
    }

}
