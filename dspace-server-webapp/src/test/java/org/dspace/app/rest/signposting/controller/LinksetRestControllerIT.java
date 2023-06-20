/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.controller;

import static org.dspace.content.MetadataSchemaEnum.PERSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.text.MessageFormat;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.SimpleMapConverter;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class LinksetRestControllerIT extends AbstractControllerIntegrationTest {

    private static final String doiPattern = "https://doi.org/{0}";
    private static final String orcidPattern = "http://orcid.org/{0}";
    private static final String doi = "10.1007/978-3-642-35233-1_18";
    private static final String PERSON_ENTITY_TYPE = "Person";

    private Collection collection;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private MetadataAuthorityService metadataAuthorityService;

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private SimpleMapConverter mapConverterDSpaceToSchemaOrgUri;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection")
                .withEntityType("Publication")
                .build();
        context.restoreAuthSystemState();
    }

    @Test
    public void findAllItemsLinksets() throws Exception {
        getClient().perform(get("/signposting"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findOneItemGenericLinksets() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withMetadata("dc", "identifier", "doi", doi)
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/linksets/" + item.getID()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findOneItemJsonLinksets() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withMetadata("dc", "identifier", "doi", doi)
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/linksets/" + item.getID() + "/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkset",
                        Matchers.hasSize(1)))
                .andExpect(jsonPath("$.linkset[0].cite-as[0].href",
                        Matchers.hasToString(MessageFormat.format(doiPattern, doi))));
    }

    @Test
    public void findOneItemJsonLinksetsWithType() throws Exception {
        String articleUri = mapConverterDSpaceToSchemaOrgUri.getValue("Article");
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withMetadata("dc", "identifier", "doi", doi)
                .withType("Article")
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/linksets/" + item.getID() + "/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkset",
                        Matchers.hasSize(1)))
                .andExpect(jsonPath("$.linkset[0].cite-as[0].href",
                        Matchers.hasToString(MessageFormat.format(doiPattern, doi))))
                .andExpect(jsonPath("$.linkset[0].type",
                        Matchers.hasSize(2)))
                .andExpect(jsonPath("$.linkset[0].type[0].href",
                        Matchers.hasToString("https://schema.org/AboutPage")))
                .andExpect(jsonPath("$.linkset[0].type[0].type",
                        Matchers.hasToString("text/html")))
                .andExpect(jsonPath("$.linkset[0].type[1].href",
                        Matchers.hasToString(articleUri)))
                .andExpect(jsonPath("$.linkset[0].type[1].type",
                        Matchers.hasToString("text/html")));
    }

    @Test
    public void findOneItemJsonLinksetsWithLicence() throws Exception {
        String licenceUrl = "https://exmple.com/licence";
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withMetadata(MetadataSchemaEnum.DC.getName(), "rights", "uri", licenceUrl)
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/linksets/" + item.getID() + "/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkset",
                        Matchers.hasSize(1)))
                .andExpect(jsonPath("$.linkset[0].type[0].href",
                        Matchers.hasToString("https://schema.org/AboutPage")))
                .andExpect(jsonPath("$.linkset[0].type[0].type",
                        Matchers.hasToString("text/html")))
                .andExpect(jsonPath("$.linkset[0].license[0].href",
                        Matchers.hasToString(licenceUrl)))
                .andExpect(jsonPath("$.linkset[0].license[0].type",
                        Matchers.hasToString("text/html")));

    }

    @Test
    public void findOneItemJsonLinksetsWithBitstreams() throws Exception {
        String bitstream1Content = "ThisIsSomeDummyText";
        String bitstream1MimeType = "text/plain";
        String bitstream2Content = "ThisIsSomeAlternativeDummyText";
        String bitstream2MimeType = "application/pdf";

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withMetadata("dc", "identifier", "doi", doi)
                .build();
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstream1Content, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream 1")
                    .withDescription("description")
                    .withMimeType(bitstream1MimeType)
                    .build();
        }
        Bitstream bitstream2 = null;
        try (InputStream is = IOUtils.toInputStream(bitstream2Content, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream 2")
                    .withDescription("description")
                    .withMimeType(bitstream2MimeType)
                    .build();
        }
        context.restoreAuthSystemState();

        String url = configurationService.getProperty("dspace.ui.url");
        getClient().perform(get("/signposting/linksets/" + item.getID() + "/json")
                        .header("Accept", "application/linkset+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkset",
                        Matchers.hasSize(1)))
                .andExpect(jsonPath("$.linkset[0].cite-as[0].href",
                        Matchers.hasToString(MessageFormat.format(doiPattern, doi))))
                .andExpect(jsonPath("$.linkset[0].item[0].href",
                        Matchers.hasToString(url + "/bitstreams/" + bitstream1.getID() + "/download")))
                .andExpect(jsonPath("$.linkset[0].item[0].type",
                        Matchers.hasToString(bitstream1MimeType)))
                .andExpect(jsonPath("$.linkset[0].item[1].href",
                        Matchers.hasToString(url + "/bitstreams/" + bitstream2.getID() + "/download")))
                .andExpect(jsonPath("$.linkset[0].item[1].type",
                        Matchers.hasToString(bitstream2MimeType)))
                .andExpect(jsonPath("$.linkset[0].anchor",
                        Matchers.hasToString(url + "/entities/publication/" + item.getID())));
    }

    @Test
    public void findOneItemThatIsInWorkspaceJsonLinksets() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Workspace Item")
                .build();
        itemService.addMetadata(context, workspaceItem.getItem(), "dc", "identifier", "doi", Item.ANY, doi);
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/linksets/" + workspaceItem.getItem().getID() + "/json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneWithdrawnItemJsonLinksets() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Withdrawn Item")
                .withMetadata("dc", "identifier", "doi", doi)
                .withdrawn()
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/linksets/" + item.getID() + "/json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneEmbargoItemJsonLinksets() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Withdrawn Item")
                .withMetadata("dc", "identifier", "doi", doi)
                .withIssueDate("2017-11-18")
                .withEmbargoPeriod("2 week")
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/linksets/" + item.getID() + "/json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneRestrictedItemJsonLinksets() throws Exception {
        context.turnOffAuthorisationSystem();
        Group internalGroup = GroupBuilder.createGroup(context)
                .withName("Internal Group")
                .build();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Withdrawn Item")
                .withMetadata("dc", "identifier", "doi", doi)
                .withReaderGroup(internalGroup)
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/linksets/" + item.getID() + "/json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneUnDiscoverableItemJsonLinksets() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Withdrawn Item")
                .withMetadata("dc", "identifier", "doi", doi)
                .makeUnDiscoverable()
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/linksets/" + item.getID() + "/json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneBitstreamJsonLinksets() throws Exception {
        String bitstreamContent = "ThisIsSomeDummyText";
        String bitstreamMimeType = "text/plain";

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withMetadata("dc", "identifier", "doi", doi)
                .build();

        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream")
                    .withDescription("description")
                    .withMimeType(bitstreamMimeType)
                    .build();
        }
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/linksets/" + bitstream.getID() + "/json")
                        .header("Accept", "application/linkset+json"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void findOneCollectionJsonLinksets() throws Exception {
        getClient().perform(get("/signposting/linksets/" + collection.getID() + "/json")
                        .header("Accept", "application/linkset+json"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void findOneCommunityJsonLinksets() throws Exception {
        getClient().perform(get("/signposting/linksets/" + parentCommunity.getID() + "/json")
                        .header("Accept", "application/linkset+json"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void findOneItemLsetLinksets() throws Exception {
        String bitstream1Content = "ThisIsSomeDummyText";
        String bitstream1MimeType = "text/plain";

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withMetadata("dc", "identifier", "doi", doi)
                .build();
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstream1Content, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream 1")
                    .withDescription("description")
                    .withMimeType(bitstream1MimeType)
                    .build();
        }
        context.restoreAuthSystemState();

        String url = configurationService.getProperty("dspace.ui.url");
        String siteAsRelation = "<" + MessageFormat.format(doiPattern, doi) + "> ; rel=\"cite-as\" ; anchor=\"" +
                url + "/entities/publication/" + item.getID() + "\" ,";
        String itemRelation = "<" + url + "/bitstreams/" + bitstream1.getID() +
                "/download> ; rel=\"item\" ; " + "type=\"text/plain\" ; anchor=\"" + url + "/entities/publication/" +
                item.getID() + "\" ,";
        String typeRelation = "<https://schema.org/AboutPage> ; rel=\"type\" ; type=\"text/html\" ; anchor=\"" +
                url + "/entities/publication/" +
                item.getID() + "\" ,";

        getClient().perform(get("/signposting/linksets/" + item.getID())
                        .header("Accept", "application/linkset"))
                .andExpect(content().string(Matchers.containsString(siteAsRelation)))
                .andExpect(content().string(Matchers.containsString(itemRelation)))
                .andExpect(content().string(Matchers.containsString(typeRelation)));
    }

    @Test
    public void findOneUnDiscoverableItemLsetLinksets() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .makeUnDiscoverable()
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/linksets/" + item.getID())
                        .header("Accept", "application/linkset"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findTypedLinkForItemWithAuthor() throws Exception {
        String bitstreamContent = "ThisIsSomeDummyText";
        String bitstreamMimeType = "text/plain";
        String orcidValue = "orcidValue";

        context.turnOffAuthorisationSystem();

        Collection personCollection = CollectionBuilder.createCollection(context, parentCommunity)
                .withEntityType(PERSON_ENTITY_TYPE)
                .build();

        Item author = ItemBuilder.createItem(context, personCollection)
                .withPersonIdentifierLastName("familyName")
                .withPersonIdentifierFirstName("firstName")
                .withMetadata(PERSON.getName(), "identifier", "orcid", orcidValue)
                .build();
        Item publication = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withMetadata("dc", "identifier", "doi", doi)
                .withAuthor("John", author.getID().toString(), Choices.CF_ACCEPTED)
                .build();

        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, publication, is)
                    .withName("Bitstream")
                    .withDescription("description")
                    .withMimeType(bitstreamMimeType)
                    .build();
        }

        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType authorEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, PERSON_ENTITY_TYPE).build();
        RelationshipType isAuthorOfPublicationRelationshipType =
                RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationEntityType, authorEntityType,
                        "isAuthorOfPublication", "isPublicationOfAuthor",
                        null, null, null, null).build();
        isAuthorOfPublicationRelationshipType.setTilted(RelationshipType.Tilted.LEFT);
        isAuthorOfPublicationRelationshipType =
                relationshipTypeService.create(context, isAuthorOfPublicationRelationshipType);
        RelationshipBuilder.createRelationshipBuilder(context, publication, author,
                isAuthorOfPublicationRelationshipType).build();

        context.restoreAuthSystemState();

        String url = configurationService.getProperty("dspace.ui.url");
        String dcIdentifierUriMetadataValue = itemService
                .getMetadataFirstValue(publication, "dc", "identifier", "uri", Item.ANY);

        getClient().perform(get("/signposting/links/" + publication.getID())
                        .header("Accept", "application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        Matchers.hasSize(5)))
                .andExpect(jsonPath("$[?(@.href == '" + MessageFormat.format(orcidPattern, orcidValue) + "' " +
                        "&& @.rel == 'author' " +
                        "&& @.type == 'text/html')]").exists())
                .andExpect(jsonPath("$[?(@.href == '" + MessageFormat.format(doiPattern, doi) + "' " +
                        "&& @.rel == 'cite-as')]").exists())
                .andExpect(jsonPath("$[?(@.href == '" + dcIdentifierUriMetadataValue + "' " +
                        "&& @.rel == 'cite-as')]").exists())
                .andExpect(jsonPath("$[?(@.href == '" + url + "/bitstreams/" + bitstream.getID() + "/download' " +
                        "&& @.rel == 'item' " +
                        "&& @.type == 'text/plain')]").exists())
                .andExpect(jsonPath("$[?(@.href == 'https://schema.org/AboutPage' " +
                        "&& @.rel == 'type' " +
                        "&& @.type == 'text/html')]").exists());
    }

    @Test
    public void findTypedLinkForBitstream() throws Exception {
        String bitstreamContent = "ThisIsSomeDummyText";
        String bitstreamMimeType = "text/plain";

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withMetadata("dc", "identifier", "doi", doi)
                .build();

        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream")
                    .withDescription("description")
                    .withMimeType(bitstreamMimeType)
                    .build();
        }
        context.restoreAuthSystemState();

        String uiUrl = configurationService.getProperty("dspace.ui.url");
        getClient().perform(get("/signposting/links/" + bitstream.getID())
                        .header("Accept", "application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        Matchers.hasSize(3)))
                .andExpect(jsonPath("$[?(@.href == '" + uiUrl + "/entities/publication/" + item.getID() + "' " +
                        "&& @.rel == 'collection' " +
                        "&& @.type == 'text/html')]").exists())
                .andExpect(jsonPath("$[?(@.href == '" + uiUrl + "/signposting/linksets/" + item.getID() + "' " +
                        "&& @.rel == 'linkset' " +
                        "&& @.type == 'application/linkset')]").exists())
                .andExpect(jsonPath("$[?(@.href == '" + uiUrl + "/signposting/linksets/" + item.getID() + "/json" +
                        "' && @.rel == 'linkset' " +
                        "&& @.type == 'application/linkset+json')]").exists());

        DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();
        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();
    }

    @Test
    public void findTypedLinkForBitstreamWithType() throws Exception {
        String bitstreamContent = "ThisIsSomeDummyText";
        String bitstreamMimeType = "text/plain";

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withMetadata("dc", "identifier", "doi", doi)
                .build();

        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream")
                    .withDescription("description")
                    .withMimeType(bitstreamMimeType)
                    .build();
        }
        bitstreamService.addMetadata(context, bitstream, "dc", "type", null, Item.ANY, "Article");

        context.restoreAuthSystemState();

        String uiUrl = configurationService.getProperty("dspace.ui.url");
        getClient().perform(get("/signposting/links/" + bitstream.getID())
                        .header("Accept", "application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        Matchers.hasSize(4)))
                .andExpect(jsonPath("$[?(@.href == '" + uiUrl + "/entities/publication/" + item.getID() + "' " +
                        "&& @.rel == 'collection' " +
                        "&& @.type == 'text/html')]").exists())
                .andExpect(jsonPath("$[?(@.href == '" + uiUrl + "/signposting/linksets/" + item.getID() + "' " +
                        "&& @.rel == 'linkset' " +
                        "&& @.type == 'application/linkset')]").exists())
                .andExpect(jsonPath("$[?(@.href == '" + uiUrl + "/signposting/linksets/" + item.getID() + "/json" +
                        "' && @.rel == 'linkset' " +
                        "&& @.type == 'application/linkset+json')]").exists())
                .andExpect(jsonPath("$[?(@.href == 'https://schema.org/ScholarlyArticle' " +
                        "&& @.rel == 'type' " +
                        "&& @.type == 'text/html')]").exists());

        DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();
        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();
    }

    @Test
    public void findTypedLinkForRestrictedBitstream() throws Exception {
        String bitstreamContent = "ThisIsSomeDummyText";
        String bitstreamMimeType = "text/plain";

        context.turnOffAuthorisationSystem();
        Group internalGroup = GroupBuilder.createGroup(context)
                .withName("Internal Group")
                .build();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withMetadata("dc", "identifier", "doi", doi)
                .build();

        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream")
                    .withDescription("description")
                    .withMimeType(bitstreamMimeType)
                    .withReaderGroup(internalGroup)
                    .build();
        }
        context.restoreAuthSystemState();

        String uiUrl = configurationService.getProperty("dspace.ui.url");
        getClient().perform(get("/signposting/links/" + bitstream.getID())
                        .header("Accept", "application/json"))
                .andExpect(status().isUnauthorized());

        DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();
        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();
    }

    @Test
    public void findTypedLinkForBitstreamUnderEmbargo() throws Exception {
        String bitstreamContent = "ThisIsSomeDummyText";
        String bitstreamMimeType = "text/plain";

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withIssueDate("2017-10-17")
                .withMetadata("dc", "identifier", "doi", doi)
                .build();

        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, item, is)
                    .withName("Bitstream")
                    .withDescription("description")
                    .withMimeType(bitstreamMimeType)
                    .withEmbargoPeriod("6 months")
                    .build();
        }
        context.restoreAuthSystemState();

        String uiUrl = configurationService.getProperty("dspace.ui.url");
        getClient().perform(get("/signposting/links/" + bitstream.getID())
                        .header("Accept", "application/json"))
                .andExpect(status().isUnauthorized());

        DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();
        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();
    }

    @Test
    public void findTypedLinkForBitstreamOfWorkspaceItem() throws Exception {
        String bitstreamContent = "ThisIsSomeDummyText";
        String bitstreamMimeType = "text/plain";

        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Workspace Item")
                .build();
        Item item = workspaceItem.getItem();
        itemService.addMetadata(context, item, "dc", "identifier", "doi", Item.ANY, doi);

        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, workspaceItem.getItem(), is)
                    .withName("Bitstream")
                    .withDescription("description")
                    .withMimeType(bitstreamMimeType)
                    .build();
        }
        context.restoreAuthSystemState();

        String uiUrl = configurationService.getProperty("dspace.ui.url");
        getClient().perform(get("/signposting/links/" + bitstream.getID())
                        .header("Accept", "application/json"))
                .andExpect(status().isUnauthorized());

        DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();
        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();
    }

    @Test
    public void findTypedLinkForUnDiscoverableItem() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .makeUnDiscoverable()
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/links/" + item.getID())
                        .header("Accept", "application/json"))
                .andExpect(status().isUnauthorized());

        DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();
        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();
    }

}
