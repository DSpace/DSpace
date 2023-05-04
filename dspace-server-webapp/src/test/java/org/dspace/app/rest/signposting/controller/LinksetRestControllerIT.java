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
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class LinksetRestControllerIT extends AbstractControllerIntegrationTest {

    private static final String doiPattern = "https://doi.org/{0}";
    private static final String doi = "10.1007/978-3-642-35233-1_18";

    private Collection collection;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    MetadataAuthorityService metadataAuthorityService;

    @Autowired
    ChoiceAuthorityService choiceAuthorityService;

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
                        Matchers.hasToString(url + "/handle/" + item.getHandle())))
                .andExpect(jsonPath("$.linkset[0].landingPage[0].href",
                        Matchers.hasToString(url + "/entities/publication/" + item.getID())))
                .andExpect(jsonPath("$.linkset[0].landingPage[0].type",
                        Matchers.hasToString("text/html")));
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
        String expectedResponse = "<" + MessageFormat.format(doiPattern, doi) + "> ; rel=\"cite-as\" ; anchor=\"" +
                url + "/handle/" + item.getHandle() + "\" , <" + url + "/entities/publication/" + item.getID() +
                "> ; rel=\"landing page\" ; type=\"text/html\" ; anchor=\"" + url + "/handle/" + item.getHandle() +
                "\" , <" + url + "/bitstreams/" + bitstream1.getID() + "/download> ; rel=\"item\" ; " +
                "type=\"text/plain\" ; anchor=\"" + url + "/handle/" + item.getHandle() + "\" ,";

        getClient().perform(get("/signposting/linksets/" + item.getID())
                        .header("Accept", "application/linkset"))
                .andExpect(content().string(expectedResponse));
    }

    @Test
    public void findTypedLinkForItem() throws Exception {
        configurationService.setProperty("choices.plugin.dc.contributor.author", "SolrAuthorAuthority");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();

        String bitstreamContent = "ThisIsSomeDummyText";
        String bitstreamMimeType = "text/plain";
        String orcidValue = "orcidValue";

        context.turnOffAuthorisationSystem();
        Item author = ItemBuilder.createItem(context, collection)
                .withType("John")
                .withMetadata(PERSON.getName(), "identifier", "orcid", orcidValue)
                .build();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Item Test")
                .withMetadata("dc", "identifier", "doi", doi)
                .withAuthor("John", author.getID().toString(), Choices.CF_ACCEPTED)
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

        String url = configurationService.getProperty("dspace.ui.url");
        getClient().perform(get("/signposting/links/" + item.getID())
                        .header("Accept", "application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        Matchers.hasSize(4)))
                .andExpect(jsonPath("$[0].href",
                        Matchers.hasToString(url + "/entities/publication/" + item.getID())))
                .andExpect(jsonPath("$[0].rel",
                        Matchers.hasToString("landing page")))
                .andExpect(jsonPath("$[0].type",
                        Matchers.hasToString("text/html")))
                .andExpect(jsonPath("$[1].href",
                        Matchers.hasToString(url + "/bitstreams/" + bitstream.getID() + "/download")))
                .andExpect(jsonPath("$[1].rel",
                        Matchers.hasToString("item")))
                .andExpect(jsonPath("$[1].type",
                        Matchers.hasToString("text/plain")))
                .andExpect(jsonPath("$[2].href",
                        Matchers.hasToString(MessageFormat.format(doiPattern, doi))))
                .andExpect(jsonPath("$[2].rel",
                        Matchers.hasToString("cite-as")))
                .andExpect(jsonPath("$[3].href",
                        Matchers.hasToString(url + "/entities/publication/" + author.getID())))
                .andExpect(jsonPath("$[3].rel",
                        Matchers.hasToString("author")))
                .andExpect(jsonPath("$[3].type",
                        Matchers.hasToString(orcidValue)));

        DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();
        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();
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
        String serverUrl = configurationService.getProperty("dspace.server.url");
        getClient().perform(get("/signposting/links/" + bitstream.getID())
                        .header("Accept", "application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        Matchers.hasSize(3)))
                .andExpect(jsonPath("$[0].href",
                        Matchers.hasToString(uiUrl + "/entities/publication/" + item.getID())))
                .andExpect(jsonPath("$[0].rel",
                        Matchers.hasToString("collection")))
                .andExpect(jsonPath("$[0].type",
                        Matchers.hasToString("text/html")))
                .andExpect(jsonPath("$[1].href",
                        Matchers.hasToString(serverUrl + "/signposting/linksets/" + item.getID())))
                .andExpect(jsonPath("$[1].rel",
                        Matchers.hasToString("linkset")))
                .andExpect(jsonPath("$[1].type",
                        Matchers.hasToString("application/linkset")))
                .andExpect(jsonPath("$[2].href",
                        Matchers.hasToString(serverUrl + "/signposting/linksets/" + item.getID() + "/json")))
                .andExpect(jsonPath("$[2].rel",
                        Matchers.hasToString("linkset")))
                .andExpect(jsonPath("$[2].type",
                        Matchers.hasToString("application/linkset+json")));

        DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();
        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();
    }

}
