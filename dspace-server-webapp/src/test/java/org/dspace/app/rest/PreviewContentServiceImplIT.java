/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.PreviewContentBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.PreviewContent;
import org.dspace.content.service.PreviewContentService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PreviewContentServiceImplIT extends AbstractControllerIntegrationTest {

    @Autowired
    PreviewContentService previewContentService;
    PreviewContent previewContent0;
    PreviewContent previewContent1;
    PreviewContent previewContent2;
    Bitstream bitstream1;
    Bitstream bitstream2;

    @Before
    public void setup() throws SQLException, AuthorizeException, IOException {
        context.turnOffAuthorisationSystem();
        // create bitstream
        Community comm = CommunityBuilder.createCommunity(context)
                .withName("Community Test")
                .build();
        Collection col = CollectionBuilder.createCollection(context, comm).withName("Collection Test").build();
        Item publicItem = ItemBuilder.createItem(context, col)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .build();
        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem)
                .withName("Bundle Test")
                .build();
        String bitstreamContent = "ThisIsSomeDummyText";
        bitstream1 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
        bitstream1 = BitstreamBuilder.
                createBitstream(context, bundle1, is)
                .withName("Bitstream1 Test")
                .withDescription("description")
                .withMimeType("text/plain")
                .build();
        }
        bitstream2 = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder.
                    createBitstream(context, bundle1, is)
                    .withName("Bitstream2 Test")
                    .withDescription("description")
                    .withMimeType("text/plain")
                    .build();
        }
        // create content previews
        previewContent0 =  PreviewContentBuilder.createPreviewContent(context, bitstream1, "test1.txt",
                null, false, "100", null).build();
        Map<String, PreviewContent> previewContentMap = new HashMap<>();
        previewContentMap.put(previewContent0.getName(), previewContent0);
        previewContent1 = PreviewContentBuilder.createPreviewContent(context, bitstream1, "", null,
                true, "0", previewContentMap).build();
        previewContent2 = PreviewContentBuilder.createPreviewContent(context, bitstream2, "test2.txt", null,
                false, "200", previewContentMap).build();
    }

    @After
    @Override
    public void destroy() throws Exception {
        //clean all
        BitstreamBuilder.deleteBitstream(bitstream1.getID());
        PreviewContentBuilder.deletePreviewContent(previewContent0.getID());
        PreviewContentBuilder.deletePreviewContent(previewContent1.getID());
        BitstreamBuilder.deleteBitstream(bitstream2.getID());
        PreviewContentBuilder.deletePreviewContent(previewContent2.getID());
        super.destroy();
    }

    @Test
    public void testFindAll() throws Exception {
        List<PreviewContent> previewContentList = previewContentService.findAll(context);
        Assert.assertEquals(previewContentList.size(), 3);
        Assert.assertEquals(previewContent0.getID(), previewContentList.get(0).getID());
        Assert.assertEquals(previewContent1.getID(), previewContentList.get(1).getID());
        Assert.assertEquals(previewContent2.getID(), previewContentList.get(2).getID());
    }

    @Test
    public void testFindByBitstream() throws Exception {
        List<PreviewContent> previewContentList = previewContentService.findByBitstream(context, bitstream2.getID());
        Assert.assertEquals(previewContentList.size(), 1);
        Assert.assertEquals(previewContent2.getID(), previewContentList.get(0).getID());
    }

    @Test
    public void testFindRootByBitstream() throws Exception {
        List<PreviewContent> previewContentList =
                previewContentService.findRootByBitstream(context, bitstream1.getID());
        Assert.assertEquals(previewContentList.size(), 1);
        Assert.assertEquals(previewContent1.getID(), previewContentList.get(0).getID());
    }
}
