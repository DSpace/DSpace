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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
import org.dspace.core.Constants;
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
    public void findOneItemJsonLinksets() throws Exception {
        String url = configurationService.getProperty("dspace.ui.url");
        String signpostingUrl = configurationService.getProperty("signposting.path");
        String mimeType = "application/vnd.datacite.datacite+xml";
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withMetadata("dc", "identifier", "doi", doi)
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/linksets/" + item.getID() + "/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkset",
                        Matchers.hasSize(2)))
                .andExpect(jsonPath("$.linkset[0].cite-as[0].href",
                        Matchers.hasToString(url + "/handle/" + item.getHandle())))
                .andExpect(jsonPath("$.linkset[0].describedby[0].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/describedby/" + item.getID())))
                .andExpect(jsonPath("$.linkset[0].describedby[0].type",
                        Matchers.hasToString(mimeType)))
                .andExpect(jsonPath("$.linkset[0].linkset[0].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString())))
                .andExpect(jsonPath("$.linkset[0].linkset[0].type",
                        Matchers.hasToString("application/linkset")))
                .andExpect(jsonPath("$.linkset[0].linkset[1].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString() +
                                "/json")))
                .andExpect(jsonPath("$.linkset[0].linkset[1].type",
                        Matchers.hasToString("application/linkset+json")))
                .andExpect(jsonPath("$.linkset[1].describes[0].href",
                        Matchers.hasToString(url + "/entities/publication/" + item.getID())))
                .andExpect(jsonPath("$.linkset[1].describes[0].type",
                        Matchers.hasToString("text/html")))
                .andExpect(jsonPath("$.linkset[1].anchor",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/describedby/" + item.getID())))
                .andExpect(header().stringValues("Content-Type", "application/linkset+json;charset=UTF-8"));
    }

    @Test
    public void findOneItemJsonLinksetsWithType() throws Exception {
        String url = configurationService.getProperty("dspace.ui.url");
        String signpostingUrl = configurationService.getProperty("signposting.path");
        String mimeType = "application/vnd.datacite.datacite+xml";
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
                        Matchers.hasSize(2)))
                .andExpect(jsonPath("$.linkset[0].cite-as[0].href",
                        Matchers.hasToString(url + "/handle/" + item.getHandle())))
                .andExpect(jsonPath("$.linkset[0].describedby[0].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/describedby/" + item.getID())))
                .andExpect(jsonPath("$.linkset[0].describedby[0].type",
                        Matchers.hasToString(mimeType)))
                .andExpect(jsonPath("$.linkset[0].type",
                        Matchers.hasSize(2)))
                .andExpect(jsonPath("$.linkset[0].type[0].href",
                        Matchers.hasToString("https://schema.org/AboutPage")))
                .andExpect(jsonPath("$.linkset[0].type[1].href",
                        Matchers.hasToString(articleUri)))
                .andExpect(jsonPath("$.linkset[0].linkset[0].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString())))
                .andExpect(jsonPath("$.linkset[0].linkset[0].type",
                        Matchers.hasToString("application/linkset")))
                .andExpect(jsonPath("$.linkset[0].linkset[1].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString() +
                                "/json")))
                .andExpect(jsonPath("$.linkset[0].linkset[1].type",
                        Matchers.hasToString("application/linkset+json")))
                .andExpect(jsonPath("$.linkset[1].describes[0].href",
                        Matchers.hasToString(url + "/entities/publication/" + item.getID())))
                .andExpect(jsonPath("$.linkset[1].describes[0].type",
                        Matchers.hasToString("text/html")))
                .andExpect(jsonPath("$.linkset[1].anchor",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/describedby/" + item.getID())))
                .andExpect(header().stringValues("Content-Type", "application/linkset+json;charset=UTF-8"));
    }

    @Test
    public void findOneItemJsonLinksetsWithLicence() throws Exception {
        String licenceUrl = "https://exmple.com/licence";
        String url = configurationService.getProperty("dspace.ui.url");
        String signpostingUrl = configurationService.getProperty("signposting.path");
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withMetadata(MetadataSchemaEnum.DC.getName(), "rights", "uri", licenceUrl)
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/linksets/" + item.getID() + "/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkset",
                        Matchers.hasSize(2)))
                .andExpect(jsonPath("$.linkset[0].type[0].href",
                        Matchers.hasToString("https://schema.org/AboutPage")))
                .andExpect(jsonPath("$.linkset[0].license[0].href",
                        Matchers.hasToString(licenceUrl)))
                .andExpect(jsonPath("$.linkset[0].linkset[0].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString())))
                .andExpect(jsonPath("$.linkset[0].linkset[0].type",
                        Matchers.hasToString("application/linkset")))
                .andExpect(jsonPath("$.linkset[0].linkset[1].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString() +
                                "/json")))
                .andExpect(jsonPath("$.linkset[0].linkset[1].type",
                        Matchers.hasToString("application/linkset+json")))
                .andExpect(jsonPath("$.linkset[1].describes[0].href",
                        Matchers.hasToString(url + "/entities/publication/" + item.getID())))
                .andExpect(jsonPath("$.linkset[1].describes[0].type",
                        Matchers.hasToString("text/html")))
                .andExpect(jsonPath("$.linkset[1].anchor",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/describedby/" + item.getID())))
                .andExpect(header().stringValues("Content-Type", "application/linkset+json;charset=UTF-8"));
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
        String signpostingUrl = configurationService.getProperty("signposting.path");
        String mimeType = "application/vnd.datacite.datacite+xml";
        getClient().perform(get("/signposting/linksets/" + item.getID() + "/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkset",
                        Matchers.hasSize(4)))
                .andExpect(jsonPath("$.linkset[0].cite-as[0].href",
                        Matchers.hasToString(url + "/handle/" + item.getHandle())))
                .andExpect(jsonPath("$.linkset[0].describedby[0].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/describedby/" + item.getID())))
                .andExpect(jsonPath("$.linkset[0].describedby[0].type",
                        Matchers.hasToString(mimeType)))
                .andExpect(jsonPath("$.linkset[0].item[0].href",
                        Matchers.hasToString(url + "/bitstreams/" + bitstream1.getID() + "/download")))
                .andExpect(jsonPath("$.linkset[0].item[0].type",
                        Matchers.hasToString(bitstream1MimeType)))
                .andExpect(jsonPath("$.linkset[0].item[1].href",
                        Matchers.hasToString(url + "/bitstreams/" + bitstream2.getID() + "/download")))
                .andExpect(jsonPath("$.linkset[0].item[1].type",
                        Matchers.hasToString(bitstream2MimeType)))
                .andExpect(jsonPath("$.linkset[0].anchor",
                        Matchers.hasToString(url + "/entities/publication/" + item.getID())))
                .andExpect(jsonPath("$.linkset[0].linkset[0].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString())))
                .andExpect(jsonPath("$.linkset[0].linkset[0].type",
                        Matchers.hasToString("application/linkset")))
                .andExpect(jsonPath("$.linkset[0].linkset[1].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString() +
                                "/json")))
                .andExpect(jsonPath("$.linkset[0].linkset[1].type",
                        Matchers.hasToString("application/linkset+json")))
                .andExpect(jsonPath("$.linkset[1].collection[0].href",
                        Matchers.hasToString(url + "/entities/publication/" + item.getID())))
                .andExpect(jsonPath("$.linkset[1].collection[0].type",
                        Matchers.hasToString("text/html")))
                .andExpect(jsonPath("$.linkset[1].linkset[0].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString())))
                .andExpect(jsonPath("$.linkset[1].linkset[0].type",
                        Matchers.hasToString("application/linkset")))
                .andExpect(jsonPath("$.linkset[1].linkset[1].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString() +
                                "/json")))
                .andExpect(jsonPath("$.linkset[1].linkset[1].type",
                        Matchers.hasToString("application/linkset+json")))
                .andExpect(jsonPath("$.linkset[1].anchor",
                        Matchers.hasToString(url + "/bitstreams/" + bitstream1.getID() + "/download")))
                .andExpect(jsonPath("$.linkset[2].collection[0].href",
                        Matchers.hasToString(url + "/entities/publication/" + item.getID())))
                .andExpect(jsonPath("$.linkset[2].collection[0].type",
                        Matchers.hasToString("text/html")))
                .andExpect(jsonPath("$.linkset[2].linkset[0].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString())))
                .andExpect(jsonPath("$.linkset[2].linkset[0].type",
                        Matchers.hasToString("application/linkset")))
                .andExpect(jsonPath("$.linkset[2].linkset[1].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString() +
                                "/json")))
                .andExpect(jsonPath("$.linkset[2].linkset[1].type",
                        Matchers.hasToString("application/linkset+json")))
                .andExpect(jsonPath("$.linkset[2].anchor",
                        Matchers.hasToString(url + "/bitstreams/" + bitstream2.getID() + "/download")))
                .andExpect(jsonPath("$.linkset[3].describes[0].href",
                        Matchers.hasToString(url + "/entities/publication/" + item.getID())))
                .andExpect(jsonPath("$.linkset[3].describes[0].type",
                        Matchers.hasToString("text/html")))
                .andExpect(jsonPath("$.linkset[3].anchor",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/describedby/" + item.getID())))
                .andExpect(header().stringValues("Content-Type", "application/linkset+json;charset=UTF-8"));
    }

    @Test
    public void findOneItemJsonLinksetsWithBitstreamsFromDifferentBundles() throws Exception {
        String bitstream1Content = "ThisIsSomeDummyText";
        String bitstream1MimeType = "text/plain";

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withMetadata("dc", "identifier", "doi", doi)
                .build();
        Bitstream bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstream1Content, CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, item, is, Constants.DEFAULT_BUNDLE_NAME)
                    .withName("Bitstream 1")
                    .withDescription("description")
                    .withMimeType(bitstream1MimeType)
                    .build();
        }

        try (InputStream is = IOUtils.toInputStream("test", CharEncoding.UTF_8)) {
            Bitstream bitstream2 = BitstreamBuilder.createBitstream(context, item, is, "TEXT")
                    .withName("Bitstream 2")
                    .withDescription("description")
                    .withMimeType("application/pdf")
                    .build();
        }

        try (InputStream is = IOUtils.toInputStream("test", CharEncoding.UTF_8)) {
            Bitstream bitstream3 = BitstreamBuilder.createBitstream(context, item, is, "THUMBNAIL")
                    .withName("Bitstream 3")
                    .withDescription("description")
                    .withMimeType("application/pdf")
                    .build();
        }

        try (InputStream is = IOUtils.toInputStream("test", CharEncoding.UTF_8)) {
            Bitstream bitstream4 = BitstreamBuilder.createBitstream(context, item, is, "LICENSE")
                    .withName("Bitstream 4")
                    .withDescription("description")
                    .withMimeType("application/pdf")
                    .build();
        }

        context.restoreAuthSystemState();

        String url = configurationService.getProperty("dspace.ui.url");
        String signpostingUrl = configurationService.getProperty("signposting.path");
        String mimeType = "application/vnd.datacite.datacite+xml";
        getClient().perform(get("/signposting/linksets/" + item.getID() + "/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkset",
                        Matchers.hasSize(3)))
                .andExpect(jsonPath("$.linkset[0].cite-as[0].href",
                        Matchers.hasToString(url + "/handle/" + item.getHandle())))
                .andExpect(jsonPath("$.linkset[0].describedby[0].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/describedby/" + item.getID())))
                .andExpect(jsonPath("$.linkset[0].describedby[0].type",
                        Matchers.hasToString(mimeType)))
                .andExpect(jsonPath("$.linkset[0].item",
                        Matchers.hasSize(1)))
                .andExpect(jsonPath("$.linkset[0].item[0].href",
                        Matchers.hasToString(url + "/bitstreams/" + bitstream1.getID() + "/download")))
                .andExpect(jsonPath("$.linkset[0].item[0].type",
                        Matchers.hasToString(bitstream1MimeType)))
                .andExpect(jsonPath("$.linkset[0].anchor",
                        Matchers.hasToString(url + "/entities/publication/" + item.getID())))
                .andExpect(jsonPath("$.linkset[0].linkset[0].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString())))
                .andExpect(jsonPath("$.linkset[0].linkset[0].type",
                        Matchers.hasToString("application/linkset")))
                .andExpect(jsonPath("$.linkset[0].linkset[1].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString() +
                                "/json")))
                .andExpect(jsonPath("$.linkset[0].linkset[1].type",
                        Matchers.hasToString("application/linkset+json")))
                .andExpect(jsonPath("$.linkset[1].collection[0].href",
                        Matchers.hasToString(url + "/entities/publication/" + item.getID())))
                .andExpect(jsonPath("$.linkset[1].collection[0].type",
                        Matchers.hasToString("text/html")))
                .andExpect(jsonPath("$.linkset[1].linkset[0].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString())))
                .andExpect(jsonPath("$.linkset[1].linkset[0].type",
                        Matchers.hasToString("application/linkset")))
                .andExpect(jsonPath("$.linkset[1].linkset[1].href",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/linksets/" + item.getID().toString() +
                                "/json")))
                .andExpect(jsonPath("$.linkset[1].linkset[1].type",
                        Matchers.hasToString("application/linkset+json")))
                .andExpect(jsonPath("$.linkset[1].anchor",
                        Matchers.hasToString(url + "/bitstreams/" + bitstream1.getID() + "/download")))
                .andExpect(jsonPath("$.linkset[2].describes[0].href",
                        Matchers.hasToString(url + "/entities/publication/" + item.getID())))
                .andExpect(jsonPath("$.linkset[2].describes[0].type",
                        Matchers.hasToString("text/html")))
                .andExpect(jsonPath("$.linkset[2].anchor",
                        Matchers.hasToString(url + "/" + signpostingUrl + "/describedby/" + item.getID())))
                .andExpect(header().stringValues("Content-Type", "application/linkset+json;charset=UTF-8"));
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

        getClient().perform(get("/signposting/linksets/" + bitstream.getID() + "/json"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void findOneCollectionJsonLinksets() throws Exception {
        getClient().perform(get("/signposting/linksets/" + collection.getID() + "/json"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void findOneCommunityJsonLinksets() throws Exception {
        getClient().perform(get("/signposting/linksets/" + parentCommunity.getID() + "/json"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void findOneItemLsetLinksets() throws Exception {
        String bitstream1Content = "ThisIsSomeDummyText";
        String bitstream1MimeType = "text/plain";

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
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
        String signpostingUrl = configurationService.getProperty("signposting.path");
        String mimeType = "application/vnd.datacite.datacite+xml";
        String siteAsRelation = "<" + url + "/handle/" + item.getHandle() + "> ; rel=\"cite-as\" ; anchor=\"" +
                url + "/entities/publication/" + item.getID() + "\" ,";
        String itemRelation = "<" + url + "/bitstreams/" + bitstream1.getID() +
                "/download> ; rel=\"item\" ; " + "type=\"text/plain\" ; anchor=\"" + url + "/entities/publication/" +
                item.getID() + "\" ,";
        String typeRelation = "<https://schema.org/AboutPage> ; rel=\"type\" ; anchor=\"" +
                url + "/entities/publication/" + item.getID() + "\" ,";
        String linksetRelation = "<" + url + "/" + signpostingUrl + "/linksets/" + item.getID().toString() +
                "> ; rel=\"linkset\" ; type=\"application/linkset\" ;" +
                " anchor=\"" + url + "/entities/publication/" + item.getID() + "\" ,";
        String jsonLinksetRelation = "<" + url + "/" + signpostingUrl + "/linksets/" + item.getID().toString() +
                "/json> ; rel=\"linkset\" ; type=\"application/linkset+json\" ;" +
                " anchor=\"" + url + "/entities/publication/" + item.getID() + "\" ,";
        String describedByRelation = "<" + url + "/" + signpostingUrl + "/describedby/" + item.getID() +
                "> ; rel=\"describedby\" ;" + " type=\"" + mimeType + "\" ; anchor=\"" + url +
                "/entities/publication/" + item.getID() + "\" ,";

        String bitstreamCollectionLink = "<" + url + "/entities/publication/" + item.getID() + "> ;" +
                " rel=\"collection\" ; type=\"text/html\" ; anchor=\"" + url + "/bitstreams/"
                + bitstream1.getID() + "/download\"";
        String bitstreamLinksetLink = "<" + url + "/" + signpostingUrl + "/linksets/" + item.getID() + "> ; " +
                "rel=\"linkset\" ; type=\"application/linkset\" ; " +
                "anchor=\"" + url + "/bitstreams/" + bitstream1.getID() + "/download\"";
        String bitstreamLinksetJsonLink = "<" + url + "/" + signpostingUrl + "/linksets/" + item.getID() + "/json> ; " +
                "rel=\"linkset\" ; type=\"application/linkset+json\" ; " +
                "anchor=\"" + url + "/bitstreams/" + bitstream1.getID() + "/download\"";

        String describesMetadataLink = "<" + url + "/entities/publication/" + item.getID() + "> ; " +
                "rel=\"describes\" ; type=\"text/html\" ; " +
                "anchor=\"" + url + "/" + signpostingUrl + "/describedby/" + item.getID() + "\"";

        getClient().perform(get("/signposting/linksets/" + item.getID()))
                .andExpect(content().string(Matchers.containsString(siteAsRelation)))
                .andExpect(content().string(Matchers.containsString(itemRelation)))
                .andExpect(content().string(Matchers.containsString(typeRelation)))
                .andExpect(content().string(Matchers.containsString(linksetRelation)))
                .andExpect(content().string(Matchers.containsString(jsonLinksetRelation)))
                .andExpect(content().string(Matchers.containsString(describedByRelation)))
                .andExpect(content().string(Matchers.containsString(bitstreamCollectionLink)))
                .andExpect(content().string(Matchers.containsString(bitstreamLinksetLink)))
                .andExpect(content().string(Matchers.containsString(bitstreamLinksetJsonLink)))
                .andExpect(content().string(Matchers.containsString(describesMetadataLink)))
                .andExpect(header().stringValues("Content-Type", "application/linkset;charset=UTF-8"));
    }

    @Test
    public void findOneUnDiscoverableItemLsetLinksets() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .makeUnDiscoverable()
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/linksets/" + item.getID()))
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
        String signpostingUrl = configurationService.getProperty("signposting.path");
        String mimeType = "application/vnd.datacite.datacite+xml";
        String dcIdentifierUriMetadataValue = itemService
                .getMetadataFirstValue(publication, "dc", "identifier", "uri", Item.ANY);

        getClient().perform(get("/signposting/links/" + publication.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        Matchers.hasSize(7)))
                .andExpect(jsonPath("$[?(@.href == '" + MessageFormat.format(orcidPattern, orcidValue) + "' " +
                        "&& @.rel == 'author')]").exists())
                .andExpect(jsonPath("$[?(@.href == '" + url + "/" + signpostingUrl + "/describedby/"
                        + publication.getID() + "' " +
                        "&& @.rel == 'describedby' " +
                        "&& @.type == '" + mimeType + "')]").exists())
                .andExpect(jsonPath("$[?(@.href == '" + dcIdentifierUriMetadataValue + "' " +
                        "&& @.rel == 'cite-as')]").exists())
                .andExpect(jsonPath("$[?(@.href == '" + url + "/bitstreams/" + bitstream.getID() + "/download' " +
                        "&& @.rel == 'item' " +
                        "&& @.type == 'text/plain')]").exists())
                .andExpect(jsonPath("$[?(@.href == 'https://schema.org/AboutPage' " +
                        "&& @.rel == 'type')]").exists())
                .andExpect(jsonPath("$[?(@.href == '" + url + "/" + signpostingUrl + "/linksets/" +
                        publication.getID().toString() + "' " +
                        "&& @.rel == 'linkset' " +
                        "&& @.type == 'application/linkset')]").exists())
                .andExpect(jsonPath("$[?(@.href == '" + url + "/" + signpostingUrl + "/linksets/" +
                        publication.getID().toString() + "/json' " +
                        "&& @.rel == 'linkset' " +
                        "&& @.type == 'application/linkset+json')]").exists());
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
        getClient().perform(get("/signposting/links/" + bitstream.getID()))
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
        getClient().perform(get("/signposting/links/" + bitstream.getID()))
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
                        "&& @.rel == 'type')]").exists());

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

        getClient().perform(get("/signposting/links/" + bitstream.getID()))
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

        getClient().perform(get("/signposting/links/" + bitstream.getID()))
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

        getClient().perform(get("/signposting/links/" + bitstream.getID()))
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

        getClient().perform(get("/signposting/links/" + item.getID()))
                .andExpect(status().isUnauthorized());

        DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();
        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();
    }

    @Test
    public void getDescribedBy() throws Exception {
        context.turnOffAuthorisationSystem();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currentDateInFormat = dateFormat.format(new Date());
        String title = "Item Test";
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle(title)
                .withMetadata("dc", "identifier", "doi", doi)
                .build();
        String responseMimeType = "application/vnd.datacite.datacite+xml";
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/describedby/" + item.getID()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(title)))
                .andExpect(header().stringValues("Content-Type", responseMimeType + ";charset=UTF-8"));
    }

    @Test
    public void getDescribedByItemThatIsInWorkspace() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Workspace Item")
                .build();
        itemService.addMetadata(context, workspaceItem.getItem(), "dc", "identifier", "doi", Item.ANY, doi);
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/describedby/" + workspaceItem.getItem().getID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getDescribedByWithdrawnItem() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Withdrawn Item")
                .withMetadata("dc", "identifier", "doi", doi)
                .withdrawn()
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/describedby/" + item.getID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getDescribedByEmbargoItem() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Withdrawn Item")
                .withMetadata("dc", "identifier", "doi", doi)
                .withIssueDate("2017-11-18")
                .withEmbargoPeriod("2 week")
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/describedby/" + item.getID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getDescribedByRestrictedItem() throws Exception {
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

        getClient().perform(get("/signposting/describedby/" + item.getID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getDescribedByUnDiscoverableItem() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Withdrawn Item")
                .withMetadata("dc", "identifier", "doi", doi)
                .makeUnDiscoverable()
                .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/signposting/describedby/" + item.getID()))
                .andExpect(status().isUnauthorized());
    }
}
