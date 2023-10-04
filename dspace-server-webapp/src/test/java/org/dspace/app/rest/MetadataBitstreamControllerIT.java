/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.Deflater;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataBitstreamControllerIT extends AbstractControllerIntegrationTest {
    private static final String METADATABITSTREAM_ENDPOINT = "/api/core/bitstreams/";
    private static final String METADATABITSTREAM_DOWNLOAD_SINGLE_ENDPOINT = METADATABITSTREAM_ENDPOINT + "/handle";
    private static final String METADATABITSTREAM_DOWNLOAD_ALL_ENDPOINT = METADATABITSTREAM_ENDPOINT + "/allzip";
    private static final String AUTHOR = "Test author name";
    private Collection col;

    private Item publicItem;
    private Bitstream bts;

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    BitstreamService bitstreamService;


    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection").build();

        publicItem = ItemBuilder.createItem(context, col)
                .withAuthor(AUTHOR)
                .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bts = BitstreamBuilder.
                    createBitstream(context, publicItem, is)
                    .withName("Bitstream")
                    .withDescription("Description")
                    .withMimeType("application/zip")
                    .build();
        }
        context.restoreAuthSystemState();
    }

    @Test
    public void downloadSingleFileNullPathVariable() throws Exception {
        getClient().perform(get(METADATABITSTREAM_DOWNLOAD_SINGLE_ENDPOINT)).andExpect(status().is4xxClientError());
    }

    @Test
    public void downloadSingleFileWithAuthorize() throws Exception {
        InputStream ip = bitstreamService.retrieve(context, bts);
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get(METADATABITSTREAM_DOWNLOAD_SINGLE_ENDPOINT +
                        "/" + publicItem.getHandle() + "/" + bts.getName()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream;charset=UTF-8"))
                .andExpect(content().bytes(IOUtils.toByteArray(ip)));
    }

    @Test
    public void downloadSingleFileWithNoAuthorize() throws Exception {
        getClient().perform(get(METADATABITSTREAM_DOWNLOAD_SINGLE_ENDPOINT +
                        "/" + publicItem.getHandle() + "/" + bts.getName()))
                .andExpect(status().isOk());
    }

    @Test
    public void downloadAllZip() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipArchiveOutputStream zip = new ZipArchiveOutputStream(byteArrayOutputStream);
        zip.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);
        zip.setLevel(Deflater.NO_COMPRESSION);
        ZipArchiveEntry ze = new ZipArchiveEntry(bts.getName());
        zip.putArchiveEntry(ze);
        InputStream is = bitstreamService.retrieve(context, bts);
        org.apache.commons.compress.utils.IOUtils.copy(is, zip);
        zip.closeArchiveEntry();
        is.close();
        zip.close();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get(METADATABITSTREAM_DOWNLOAD_ALL_ENDPOINT ).param("handleId",
                        publicItem.getHandle()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(byteArrayOutputStream.toByteArray()));

    }


}
