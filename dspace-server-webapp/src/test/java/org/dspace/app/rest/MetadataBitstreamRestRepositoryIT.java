/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.util.Util;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.core.Constants;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataBitstreamRestRepositoryIT extends AbstractControllerIntegrationTest {

    private static final String METADATABITSTREAM_ENDPOINT = "/api/core/metadatabitstream/";
    private static final String METADATABITSTREAM_SEARCH_BY_HANDLE_ENDPOINT =
            METADATABITSTREAM_ENDPOINT + "search/byHandle";
    private static final String FILE_GRP_TYPE = "ORIGINAL";
    private static final String AUTHOR = "Test author name";
    private Collection col;

    private Item publicItem;
    private Bitstream bts;
    private String url;
    @Autowired
    ClarinLicenseResourceMappingService licenseService;

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    ConfigurationService configurationService;

    @Before
    public void setup() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection").build();

        publicItem = ItemBuilder.createItem(context, col)
                .withAuthor(AUTHOR)
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8);
        bts = BitstreamBuilder.
                createBitstream(context, publicItem, is)
                .withName("Bitstream")
                .withDescription("Description")
                .withMimeType("application/x-gzip")
                .build();

        context.restoreAuthSystemState();

        if (StringUtils.isBlank(url)) {
            composeURL();
        }
    }

    @Test
    public void findByHandleNullHandle() throws Exception {
        getClient().perform(get(METADATABITSTREAM_SEARCH_BY_HANDLE_ENDPOINT))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void findByHandle() throws Exception {
        // There is no restriction, so the user could preview the file
        boolean canPreview = true;

        getClient().perform(get(METADATABITSTREAM_SEARCH_BY_HANDLE_ENDPOINT)
                        .param("handle", publicItem.getHandle())
                        .param("fileGrpType", FILE_GRP_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.metadatabitstreams").exists())
                .andExpect(jsonPath("$._embedded.metadatabitstreams").isArray())
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].name")
                        .value(Matchers.containsInAnyOrder(Matchers.containsString("Bitstream"))))
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].description")
                        .value(Matchers.containsInAnyOrder(Matchers.containsString(bts.getFormatDescription(context)))))
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].format")
                        .value(Matchers.containsInAnyOrder(Matchers.containsString(
                                bts.getFormat(context).getMIMEType()))))
                // Convert the long into int because Marchers has a problem to compare long format
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].fileSize")
                        .value(hasItem(is((int) bts.getSizeBytes()))))
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].canPreview")
                        .value(Matchers.containsInAnyOrder(Matchers.is(canPreview))))
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].fileInfo").exists())
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].checksum")
                        .value(Matchers.containsInAnyOrder(Matchers.containsString(bts.getChecksum()))))
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].href")
                        .value(Matchers.containsInAnyOrder(Matchers.containsString(url))));


    }

    @Test
    public void previewingIsDisabledByCfg() throws Exception {
        boolean canPreview = configurationService.getBooleanProperty("file.preview.enabled", true);
        // Disable previewing
        configurationService.setProperty("file.preview.enabled", false);
        // There is no restriction, so the user could preview the file
        getClient().perform(get(METADATABITSTREAM_SEARCH_BY_HANDLE_ENDPOINT)
                        .param("handle", publicItem.getHandle())
                        .param("fileGrpType", FILE_GRP_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.metadatabitstreams").exists())
                .andExpect(jsonPath("$._embedded.metadatabitstreams").isArray())
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].name")
                        .value(Matchers.containsInAnyOrder(Matchers.containsString("Bitstream"))))
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].description")
                        .value(Matchers.containsInAnyOrder(Matchers.containsString(bts.getFormatDescription(context)))))
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].format")
                        .value(Matchers.containsInAnyOrder(Matchers.containsString(
                                bts.getFormat(context).getMIMEType()))))
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].fileSize")
                        .value(hasItem(is((int) bts.getSizeBytes()))))
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].canPreview")
                        .value(Matchers.containsInAnyOrder(Matchers.is(false))))
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].fileInfo").exists())
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].checksum")
                        .value(Matchers.containsInAnyOrder(Matchers.containsString(bts.getChecksum()))))
                .andExpect(jsonPath("$._embedded.metadatabitstreams[*].href")
                        .value(Matchers.containsInAnyOrder(Matchers.containsString(url))));

        configurationService.setProperty("file.preview.enabled", canPreview);
    }

    @Test
    public void findByHandleEmptyFileGrpType() throws Exception {
        getClient().perform(get(METADATABITSTREAM_SEARCH_BY_HANDLE_ENDPOINT)
                .param("handle", publicItem.getHandle())
                .param("fileGrpType", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(0)))
                .andExpect(jsonPath("$.page.totalPages", is(0)))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.number", is(0)))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString(METADATABITSTREAM_SEARCH_BY_HANDLE_ENDPOINT +
                                "?handle=" + publicItem.getHandle() + "&fileGrpType=")));
    }

    @Test
    public void searchMethodsExist() throws Exception {

        getClient().perform(get("/api/core/metadatabitstreams"))
                .andExpect(status().is5xxServerError());

        getClient().perform(get("/api/core/metadatabitstreams/search"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._links.byHandle", notNullValue()));
    }

    private void composeURL() {
        String identifier = null;
        if (publicItem != null && publicItem.getHandle() != null) {
            identifier = "handle/" + publicItem.getHandle();
        } else if (publicItem != null) {
            identifier = "item/" + publicItem.getID();
        } else {
            identifier = "id/" + bts.getID();
        }
        url = "/api/core/bitstreams/" + identifier + "/";
        try {
            if (bts.getName() != null) {
                url += Util.encodeBitstreamName(bts.getName(), "UTF-8");
            }
        } catch (UnsupportedEncodingException uee) { /* Do nothing */ }
        url += "?sequence=" + bts.getSequenceID();

        String isAllowed = "n";
        try {
            if (authorizeService.authorizeActionBoolean(context, bts, Constants.READ)) {
                isAllowed = "y";
            }
        } catch (SQLException e) { /* Do nothing */ }

        url += "&isAllowed=" + isAllowed;
    }
}
