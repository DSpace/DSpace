/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BitstreamFormatBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Integration test to test the /api/clarin/import/core/* endpoints
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
public class ClarinBitstreamImportControllerIT extends AbstractEntityIntegrationTest {
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private BitstreamService bitstreamService;
    @Autowired
    private BundleService bundleService;
    private JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(true);
    @Autowired
    private BitstreamFormatService bitstreamFormatService;

    private Bundle bundle;
    private String token;
    private BitstreamFormat bitstreamFormat;
    private Bitstream bitstream;
    private UUID uuid;
    private String checkSumsAlg = "MD5";
    private long sizeBytes;
    private String checkSum;
    private String internalId;
    private int storeNumber;
    private boolean deleted;
    private int sequence;


    @Before
    public void setup() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Item item = ItemBuilder.createItem(context, col1)
                .withTitle("Author1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .build();
        bundle = BundleBuilder.createBundle(context, item)
                .withName("TESTINGBUNDLE")
                .build();
        token = getAuthToken(admin.getEmail(), password);
        bitstreamFormat = BitstreamFormatBuilder.createBitstreamFormat(context).build();
        String input = "Hello, World!";
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                input.getBytes());
        context.restoreAuthSystemState();

        //create bitstream and store file
        MvcResult mvcResult = getClient(token)
                .perform(MockMvcRequestBuilders.fileUpload("/api/core/bundles/" +
                                bundle.getID() + "/bitstreams")
                        .file(file))
                .andExpect(status().isCreated())
                .andReturn();
        ObjectMapper mapper = new ObjectMapper();
        String content = mvcResult.getResponse().getContentAsString();
        Map<String, Object> map = mapper.readValue(content, Map.class);
        String bitstreamId = String.valueOf(map.get("id"));
        bitstream = bitstreamService.find(context, UUID.fromString(bitstreamId));
        uuid = bitstream.getID();
        sizeBytes = bitstream.getSizeBytes();
        checkSum = bitstream.getChecksum();
        internalId = bitstream.getInternalId();
        storeNumber = bitstream.getStoreNumber();
        deleted = bitstream.isDeleted();
        sequence = bitstream.getSequenceID();

        //delete bitstream
        context.turnOffAuthorisationSystem();
        BitstreamBuilder.deleteBitstream(bitstream.getID());
        context.restoreAuthSystemState();
        bitstream = bitstreamService.find(context, UUID.fromString(bitstreamId));
        assertNull(bitstream);
    }

    @Test
    public void importBitstreamForExistingFileWithBundleTest() throws Exception {
        //input data
        ObjectNode checksumNode = jsonNodeFactory.objectNode();
        checksumNode.set("checkSumAlgorithm", jsonNodeFactory.textNode(checkSumsAlg));
        checksumNode.set("value", jsonNodeFactory.textNode(checkSum));
        ObjectNode node = jsonNodeFactory.objectNode();
        node.set("sizeBytes", jsonNodeFactory.textNode(Long.toString(sizeBytes)));
        node.set("checkSum", checksumNode);

        //create new bitstream for existing file
        ObjectMapper mapper = new ObjectMapper();
        uuid = UUID.fromString(read( getClient(token).perform(post("/api/clarin/import/core/bitstream")
                        .content(mapper.writeValueAsBytes(node))
                        .contentType(contentType)
                        .param("internal_id", internalId)
                        .param("storeNumber", Integer.toString(storeNumber))
                        .param("bitstreamFormat", Integer.toString(bitstreamFormat.getID()))
                        .param("deleted", Boolean.toString(deleted))
                        .param("sequenceId", Integer.toString(sequence))
                        .param("primaryBundle_id", "")
                        .param("bundle_id", bundle.getID().toString()))
                .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.id"));

        checkCreatedBitstream(uuid, internalId, storeNumber, bitstreamFormat.getID(), sequence, deleted, sizeBytes,
                checkSum);

        //clean all
        context.turnOffAuthorisationSystem();
        BitstreamBuilder.deleteBitstream(uuid);
        context.restoreAuthSystemState();
    }

    @Test
    public void importBitstreamForExistingFileWithoutBundleTest() throws Exception {
        //input data
        ObjectNode checksumNode = jsonNodeFactory.objectNode();
        checksumNode.set("checkSumAlgorithm", jsonNodeFactory.textNode(checkSumsAlg));
        checksumNode.set("value", jsonNodeFactory.textNode(checkSum));
        ObjectNode node = jsonNodeFactory.objectNode();
        node.set("sizeBytes", jsonNodeFactory.textNode(Long.toString(sizeBytes)));
        node.set("checkSum", checksumNode);

        //create new bitstream for existing file
        ObjectMapper mapper = new ObjectMapper();
        uuid = UUID.fromString(read( getClient(token).perform(post("/api/clarin/import/core/bitstream")
                                .content(mapper.writeValueAsBytes(node))
                                .contentType(contentType)
                                .param("internal_id", internalId)
                                .param("storeNumber", Integer.toString(storeNumber))
                                .param("bitstreamFormat", Integer.toString(bitstreamFormat.getID()))
                                .param("deleted", Boolean.toString(deleted))
                                .param("sequenceId", Integer.toString(sequence))
                                .param("primaryBundle_id", "")
                                .param("bundle_id", ""))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.id"));

        checkCreatedBitstream(uuid, internalId, storeNumber, bitstreamFormat.getID(), sequence, deleted, sizeBytes,
                checkSum);

        //clean all
        context.turnOffAuthorisationSystem();
        BitstreamBuilder.deleteBitstream(uuid);
        context.restoreAuthSystemState();
    }

    @Test
    public void importBitstreamForExistingFileAsPrimaryBitstreamOfBundleTest() throws Exception {
        //input data
        ObjectNode checksumNode = jsonNodeFactory.objectNode();
        checksumNode.set("checkSumAlgorithm", jsonNodeFactory.textNode(checkSumsAlg));
        checksumNode.set("value", jsonNodeFactory.textNode(checkSum));
        ObjectNode node = jsonNodeFactory.objectNode();
        node.set("sizeBytes", jsonNodeFactory.textNode(Long.toString(sizeBytes)));
        node.set("checkSum", checksumNode);

        //create new bitstream for existing file
        ObjectMapper mapper = new ObjectMapper();
        uuid = UUID.fromString(read( getClient(token).perform(post("/api/clarin/import/core/bitstream")
                                .content(mapper.writeValueAsBytes(node))
                                .contentType(contentType)
                                .param("internal_id", internalId)
                                .param("storeNumber", Integer.toString(storeNumber))
                                .param("bitstreamFormat", Integer.toString(bitstreamFormat.getID()))
                                .param("deleted", Boolean.toString(deleted))
                                .param("sequenceId", Integer.toString(sequence))
                                .param("primaryBundle_id", bundle.getID().toString())
                                .param("bundle_id", ""))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                "$.id"));

        checkCreatedBitstream(uuid, internalId, storeNumber, bitstreamFormat.getID(), sequence, deleted, sizeBytes,
                checkSum);
        bundle = bundleService.find(context, bundle.getID());
        assertEquals(bundle.getPrimaryBitstream().getID(), bitstream.getID());

        //clean all
        bundle.setPrimaryBitstreamID(null);
        bundleService.update(context, bundle);
        context.turnOffAuthorisationSystem();
        BitstreamBuilder.deleteBitstream(uuid);
        context.restoreAuthSystemState();
    }

    @Test
    public void importBitstreamForExistingFileValidationErrorTest() throws Exception {
        assertEquals(bitstreamService.findAll(context).size(), 0);
        //input data
        ObjectNode checksumNode = jsonNodeFactory.objectNode();
        checksumNode.set("checkSumAlgorithm", jsonNodeFactory.textNode(checkSumsAlg));
        checksumNode.set("value", jsonNodeFactory.textNode("555"));
        ObjectNode node = jsonNodeFactory.objectNode();
        node.set("sizeBytes", jsonNodeFactory.textNode(Long.toString(sizeBytes)));
        node.set("checkSum", checksumNode);

        //create new bitstream for existing file
        ObjectMapper mapper = new ObjectMapper();
        boolean emptyResponse = getClient(token).perform(post("/api/clarin/import/core/bitstream")
                        .content(mapper.writeValueAsBytes(node))
                        .contentType(contentType)
                        .param("internal_id", internalId)
                        .param("storeNumber", Integer.toString(storeNumber))
                        .param("bitstreamFormat", Integer.toString(bitstreamFormat.getID()))
                        .param("deleted", Boolean.toString(deleted))
                        .param("sequenceId", Integer.toString(sequence))
                        .param("primaryBundle_id", "")
                        .param("bundle_id", bundle.getID().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString().isEmpty();

        //bitstream with validation error cannot be created
        assertTrue(emptyResponse);
        assertEquals(bitstreamService.findAll(context).size(), 0);
    }

    private void checkCreatedBitstream(UUID uuid, String internalId, int storeNumber,
                                       Integer bitstreamFormat, int sequence, boolean deleted, long sizeBytes,
                                       String checkSum) throws SQLException {
        bitstream = bitstreamService.find(context, uuid);
        assertEquals(bitstream.getChecksum(), checkSum);
        assertEquals(bitstream.getSizeBytes(), sizeBytes);
        assertEquals(bitstream.getFormat(context).getID(), bitstreamFormat);
        assertEquals(bitstream.getInternalId(), internalId);
        assertEquals(bitstream.getStoreNumber(), storeNumber);
        assertEquals(bitstream.getSequenceID(), sequence);
        assertEquals(bitstream.isDeleted(), deleted);
        assertEquals(bitstream.getChecksumAlgorithm(), "MD5");
    }
}