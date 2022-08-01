/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step.validation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.AbstractDSpaceObjectTest;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.MetadataValueTest;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Test class for the {@link CMDIFileBundleMaintainer}
 */
public class CMDIFileBundleMaintainerTest extends AbstractDSpaceObjectTest {

    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataValueTest.class);
    /**
     * Item instance for the tests
     */
    private Item it;
    /**
     * MetadataValue instance for the tests
     */
    private MetadataValue mv = null;
    /**
     * List of MetadataValue instances for the tests
     */
    private List<MetadataValue> listMv;
    /**
     * MetadataField instance for the tests
     */
    private MetadataField mf;
    /**
     * Collection instance for the tests
     */
    private Collection collection;
    /**
     * Community instance for the tests
     */
    private Community owningCommunity;
    private BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance()
            .getBitstreamFormatService();
    private MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    private MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
    /**
     * Spy of AuthorizeService to use for tests
     * (initialized / setup in @Before method)
     */
    private AuthorizeService authorizeServiceSpy;

    @Before
    public void setUp() throws SQLException, AuthorizeException, IOException {
        context.turnOffAuthorisationSystem();
        this.owningCommunity = communityService.create(null, context);
        this.collection = collectionService.create(context, owningCommunity);
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, true);
        this.it = installItemService.installItem(context, workspaceItem);
        this.mf = metadataFieldService.findByString(context, "local.hasCMDI", '.');
        this.mv = metadataValueService.create(context, it, mf);
        this.mv.setValue("yes");
        this.listMv = new ArrayList<>();
        listMv.add(this.mv);
        this.dspaceObject = it;
        context.restoreAuthSystemState();

        // Initialize our spy of the autowired (global) authorizeService bean.
        // This allows us to customize the bean's method return values in tests below
        authorizeServiceSpy = spy(authorizeService);
        // "Wire" our spy to be used by the current loaded object services
        // (To ensure these services use the spy instead of the real service)
        ReflectionTestUtils.setField(collectionService, "authorizeService", authorizeServiceSpy);
        ReflectionTestUtils.setField(itemService, "authorizeService", authorizeServiceSpy);
        ReflectionTestUtils.setField(workspaceItemService, "authorizeService", authorizeServiceSpy);
        ReflectionTestUtils.setField(bundleService, "authorizeService", authorizeServiceSpy);
        ReflectionTestUtils.setField(bitstreamService, "authorizeService", authorizeServiceSpy);
        // Also wire into current AuthorizeServiceFactory, as that is used for some checks (e.g. AuthorizeUtil)
        ReflectionTestUtils.setField(AuthorizeServiceFactory.getInstance(), "authorizeService",
                authorizeServiceSpy);

        // Allow Item ADD perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.ADD);
        // Allow Item WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.WRITE, true);
        // Allow Bundle ADD perms
        doNothing().when(authorizeServiceSpy).authorizeAction(any(Context.class), any(Bundle.class), eq(Constants.ADD));
        // Allow Bitstream WRITE perms
        doNothing().when(authorizeServiceSpy)
                .authorizeAction(any(Context.class), any(Bitstream.class), eq(Constants.WRITE));
        // Allow Bitstream DELETE perms
        doNothing().when(authorizeServiceSpy)
                .authorizeAction(any(Context.class), any(Bitstream.class), eq(Constants.DELETE));


        // Upload CMDI file
        MultipartFile f = new MockMultipartFile("exampleCMDI.cmdi","exampleCMDI.cmdi",
                "text/plain", IOUtils.toByteArray(testProps.get("test.CMDIfile").toString()));
        InputStream inputStream = new BufferedInputStream(f.getInputStream());
        Bitstream result = itemService.createSingleBitstream(context, inputStream, it,
                Constants.METADATA_BUNDLE_NAME);

        result.setName(context, Utils.getFileName(f));
        result.setSource(context, f.getOriginalFilename());

        BitstreamFormat bf = bitstreamFormatService.guessFormat(context, result);
        result.setFormat(context, bf);

        bitstreamService.update(context, result);
        itemService.update(context, this.it);
    }

    /**
     * Test of uploading the CMDI file into METADATA bundle
     */
    @Test
    public void testUploadedCMDIFileIntoMetadataBundle() {
        Bitstream bitstream = this.it.getBundles(Constants.METADATA_BUNDLE_NAME).get(0).getBitstreams().get(0);

        assertThat("testUploadedCMDIFileIntoMetadataBundle 0",
                this.it.getBundles(Constants.METADATA_BUNDLE_NAME).size(), is(1));
        assertThat("testUploadedCMDIFileIntoMetadataBundle 1",
                bitstream, notNullValue());
    }

    /**
     * Test of size of the ORIGINAL bundle after uploading the CMDI file into the METADATA bundle
     */
    @Test
    public void testBundleOriginalIsEmpty() {
        List<Bundle> bundlesOriginal = this.it.getBundles(Constants.CONTENT_BUNDLE_NAME);

        assertThat("testBundleOriginalIsEmpty 0", bundlesOriginal.size(), is(0));
    }

    /**
     * Test of not moving the CMDI file from the METADATA bundle
     */
    @Test
    public void testDoNothingWhenHasCmdiIsTrueAndCmdiFileIsInMetadataBundle() throws SQLException,
            AuthorizeException, IOException {
        CMDIFileBundleMaintainer.updateCMDIFileBundle(this.context,this.it, this.listMv);

        assertThat("testDoNothingWhenHasCmdiIsTrueAndCmdiFileIsInMetadataBundle 0",
                this.it.getBundles(Constants.METADATA_BUNDLE_NAME).size(), is(1));
    }

    /**
     * Test of moving the CMDI file from the METADATA bundle to the ORIGINAL bundle
     */
    @Test
    public void testChangeCmdiFileFromMetadataBundleToOriginalBundle() throws SQLException,
            AuthorizeException, IOException {

        this.listMv.remove(0);
        CMDIFileBundleMaintainer.updateCMDIFileBundle(this.context, this.it, this.listMv);

        List<Bitstream> bitstreamMetadataBundle = this.it.getBundles(Constants.METADATA_BUNDLE_NAME).get(0)
                .getBitstreams();
        List<Bitstream> bitstreamMetadataOriginal = this.it.getBundles(Constants.CONTENT_BUNDLE_NAME).get(0)
                .getBitstreams();

        assertThat("testChangeCmdiFileFromMetadataBundleToOriginalBundle 0",
                this.it.getBundles(Constants.METADATA_BUNDLE_NAME).size(), is(1));
        assertThat("testChangeCmdiFileFromMetadataBundleToOriginalBundle 1",
                this.it.getBundles(Constants.CONTENT_BUNDLE_NAME).size(), is(1));
        assertThat("testChangeCmdiFileFromMetadataBundleToOriginalBundle 2",
                bitstreamMetadataBundle.size(), is(0));
        assertThat("testChangeCmdiFileFromMetadataBundleToOriginalBundle 3",
                bitstreamMetadataOriginal.size(), is(1));
    }

    /**
     * Test of not moving the CMDI file from the ORIGINAL bundle
     */
    @Test
    public void testDoNothingWhenHasCmdiIsFalseAndCmdiFileIsInOriginalBundle() throws SQLException,
            AuthorizeException, IOException {
        // add CMDI file to the ORIGINAL bundle
        this.listMv.remove(0);
        CMDIFileBundleMaintainer.updateCMDIFileBundle(this.context,this.it, this.listMv);

        assertThat("testDoNothingWhenHasCmdiIsFalseAndCmdiFileIsInOriginalBundle 0",
                this.it.getBundles(Constants.CONTENT_BUNDLE_NAME).size(), is(1));

        CMDIFileBundleMaintainer.updateCMDIFileBundle(this.context,this.it, this.listMv);

        assertThat("testDoNothingWhenHasCmdiIsFalseAndCmdiFileIsInOriginalBundle 1",
                this.it.getBundles(Constants.CONTENT_BUNDLE_NAME).size(), is(1));
    }



    /**
     * Test of moving the CMDI file from the ORIGINAL bundle to the METADATA bundle
     */
    @Test
    public void testChangeCmdiFileFromOriginalBundleToMetadataBundle() throws SQLException,
            AuthorizeException, IOException {
        // add CMDI file to the ORIGINAL bundle
        this.listMv.remove(0);
        CMDIFileBundleMaintainer.updateCMDIFileBundle(this.context, this.it, this.listMv);

        this.listMv.add(this.mv);
        CMDIFileBundleMaintainer.updateCMDIFileBundle(this.context, this.it, this.listMv);

        List<Bitstream> bitstreamMetadataBundle = this.it.getBundles(Constants.METADATA_BUNDLE_NAME).get(0)
                .getBitstreams();
        List<Bitstream> bitstreamMetadataOriginal = this.it.getBundles(Constants.CONTENT_BUNDLE_NAME).get(0)
                .getBitstreams();

        assertThat("testChangeCmdiFileFromOriginalBundleToMetadataBundle 0",
                this.it.getBundles(Constants.METADATA_BUNDLE_NAME).size(), is(1));
        assertThat("testChangeCmdiFileFromOriginalBundleToMetadataBundle 1",
                this.it.getBundles(Constants.CONTENT_BUNDLE_NAME).size(), is(1));
        assertThat("testChangeCmdiFileFromOriginalBundleToMetadataBundle 2",
                bitstreamMetadataBundle.size(), is(1));
        assertThat("testChangeCmdiFileFromOriginalBundleToMetadataBundle 3",
                bitstreamMetadataOriginal.size(), is(0));
    }

    /**
     * Test of getType method, of class Item.
     */
    @Override
    @Test
    public void testGetType() {
        assertThat("testGetType 0", it.getType(), equalTo(Constants.ITEM));
    }


    /**
     * Test of getID method, of class Item.
     */
    @Override
    @Test
    public void testGetID() {
        assertTrue("testGetID 0", it.getID() != null);
    }

    /**
     * Test of getHandle method, of class Item.
     */
    @Override
    @Test
    public void testGetHandle() {
        //default instance has a random handle
        assertThat("testGetHandle 0", it.getHandle(), notNullValue());
    }

    /**
     * Test of getName method, of class Item.
     */
    @Override
    @Test
    public void testGetName() {
        assertThat("testGetName 0", it.getName(), nullValue());
    }

    /**
     * Test of getParentObject method, of class Item.
     */
    @Test
    @Override
    public void testGetParentObject() throws SQLException {
        try {
            //default has no parent
            assertThat("testGetParentObject 0", itemService.getParentObject(context, it), notNullValue());

            context.turnOffAuthorisationSystem();
            Collection parent = collectionService.create(context, owningCommunity);
            it.setOwningCollection(parent);
            context.restoreAuthSystemState();
            assertThat("testGetParentObject 1", itemService.getParentObject(context, it), notNullValue());
            assertThat("testGetParentObject 2", (Collection) itemService.getParentObject(context, it), equalTo(parent));
        } catch (AuthorizeException ex) {
            throw new AssertionError("Authorize Exception occurred", ex);
        }
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy() {
        try {
            context.turnOffAuthorisationSystem();
            itemService.delete(context, it);
            collectionService.delete(context, collection);
            communityService.delete(context, owningCommunity);
        } catch (SQLException | AuthorizeException | IOException ex) {
            log.error("Error in destroy", ex);
            fail("Error in destroy: " + ex.getMessage());
        } finally {
            context.restoreAuthSystemState();
            context.restoreAuthSystemState();
            it = null;
            mf = null;
            mv = null;
            collection = null;
            owningCommunity = null;
        }

        try {
            super.destroy();
        } catch (Exception e) {
            // ignore
        }
        super.destroy();
    }
}
