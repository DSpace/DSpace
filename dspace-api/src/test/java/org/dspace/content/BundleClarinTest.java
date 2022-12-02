/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.dspace.core.Constants.CONTENT_BUNDLE_NAME;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.content.factory.ClarinServiceFactory;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.core.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test class for testing of maintenance the clarin license in the bitstream.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class BundleClarinTest extends AbstractDSpaceObjectTest {
    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(BundleTest.class);

    private static final String LICENSE_LABEL = "TEST";
    private static final String LICENSE_NAME = "TEST NAME";
    private static final String LICENSE_URI = "TEST URI";

    /**
     * Bundle instance for the tests
     */
    private Bundle b;
    private Item item;
    private Collection collection;
    private Community owningCommunity;
    private ClarinLicenseLabel clarinLicenseLabel;
    private ClarinLicense clarinLicense;

    private ClarinLicenseLabel secondClarinLicenseLabel;
    private ClarinLicense secondClarinLicense;

    private ClarinLicenseLabelService clarinLicenseLabelService = ClarinServiceFactory.getInstance()
            .getClarinLicenseLabelService();
    private ClarinLicenseService clarinLicenseService = ClarinServiceFactory.getInstance().getClarinLicenseService();
    private ClarinLicenseResourceMappingService clarinLicenseResourceMappingService = ClarinServiceFactory
            .getInstance().getClarinLicenseResourceMappingService();

    /**
     * Spy of AuthorizeService to use for tests
     * (initialized / setup in @Before method)
     */
    private AuthorizeService authorizeServiceSpy;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init() {
        super.init();
        try {
            context.turnOffAuthorisationSystem();
            this.owningCommunity = communityService.create(null, context);
            this.collection = collectionService.create(context, owningCommunity);
            WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
            this.item = installItemService.installItem(context, workspaceItem);
            this.b = bundleService.create(context, item, CONTENT_BUNDLE_NAME);
            this.dspaceObject = b;

            // create clarin license label
            this.clarinLicenseLabel = clarinLicenseLabelService.create(context);
            this.clarinLicenseLabel.setLabel(LICENSE_LABEL);
            this.clarinLicenseLabel.setExtended(false);
            this.clarinLicenseLabel.setTitle("TEST TITLE");
            this.clarinLicenseLabel.setIcon(new byte[3]);
            this.clarinLicenseLabelService.update(context, this.clarinLicenseLabel);

            HashSet<ClarinLicenseLabel> cllSet = new HashSet<>();
            cllSet.add(this.clarinLicenseLabel);

            // create clarin license with clarin license labels
            this.clarinLicense = clarinLicenseService.create(context);
            this.clarinLicense.setLicenseLabels(cllSet);
            this.clarinLicense.setName(LICENSE_NAME);
            this.clarinLicense.setDefinition(LICENSE_URI);
            this.clarinLicense.setConfirmation(0);
            this.clarinLicenseService.update(context, this.clarinLicense);

            // initialize second clarin license and clarin license label
            // create second clarin license label
            this.secondClarinLicenseLabel = clarinLicenseLabelService.create(context);
            this.secondClarinLicenseLabel.setLabel("wrong");
            this.secondClarinLicenseLabel.setExtended(false);
            this.secondClarinLicenseLabel.setTitle("wrong title");
            this.secondClarinLicenseLabel.setIcon(new byte[3]);
            this.clarinLicenseLabelService.update(context, this.secondClarinLicenseLabel);

            HashSet<ClarinLicenseLabel> secondCllSet = new HashSet<>();
            secondCllSet.add(this.secondClarinLicenseLabel);

            // create second clarin license with clarin license labels
            this.secondClarinLicense = clarinLicenseService.create(context);
            this.secondClarinLicense.setLicenseLabels(secondCllSet);
            this.secondClarinLicense.setName("wrong name");
            this.secondClarinLicense.setDefinition("wrong uri");
            this.secondClarinLicense.setConfirmation(0);
            this.clarinLicenseService.update(context, this.secondClarinLicense);

            //we need to commit the changes, so we don't block the table for testing
            context.restoreAuthSystemState();

            // Initialize our spy of the autowired (global) authorizeService bean.
            // This allows us to customize the bean's method return values in tests below
            authorizeServiceSpy = spy(authorizeService);
            // "Wire" our spy to be used by the current loaded itemService, bundleService & bitstreamService
            // (To ensure it uses the spy instead of the real service)
            ReflectionTestUtils.setField(itemService, "authorizeService", authorizeServiceSpy);
            ReflectionTestUtils.setField(bundleService, "authorizeService", authorizeServiceSpy);
            ReflectionTestUtils.setField(bitstreamService, "authorizeService", authorizeServiceSpy);
        } catch (SQLException | AuthorizeException ex) {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
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
        b = null;
        item = null;
        collection = null;
        owningCommunity = null;
        clarinLicense = null;
        clarinLicenseLabel = null;
        super.destroy();
    }

    /**
     * The clarin license should be attached to the bitstream if the clarin license resource mapping record was added.
     */
    @Test
    public void testAttachLicenseToBitstream() throws IOException, SQLException, AuthorizeException {
        // the license is not attached to the bitstream
        assertEquals(clarinLicense.getNonDeletedBitstreams(), 0);

        context.turnOffAuthorisationSystem();
        // add clarin license data to the item metadata
        clarinLicenseService.addLicenseMetadataToItem(context, clarinLicense, item);
        this.addFileToBitstream();
        context.restoreAuthSystemState();

        List<ClarinLicenseResourceMapping> bitstreamAddedToLicense = clarinLicenseResourceMappingService
                .findAllByLicenseId(context, clarinLicense.getID());

        // the license is attached to the bitstream
        assertNotNull(bitstreamAddedToLicense);
        assertEquals(bitstreamAddedToLicense.size(), 1);
    }

    /**
     * On bitstream remove the clarin license should be removed from the bitstream - the clarin license resource
     * mapping record is removed.
     */
    @Test
    public void testDetachLicenseOnBitstreamRemove() throws IOException, SQLException, AuthorizeException {
        // 1. Attach the license to the bitstream
        context.turnOffAuthorisationSystem();
        clarinLicenseService.addLicenseMetadataToItem(context, clarinLicense, item);
        Bitstream bs = this.addFileToBitstream();

        List<ClarinLicenseResourceMapping> bitstreamAddedToLicense = clarinLicenseResourceMappingService
                .findAllByLicenseId(context, clarinLicense.getID());
        // the license is attached to the bitstream
        assertNotNull(bitstreamAddedToLicense);
        assertEquals(bitstreamAddedToLicense.size(), 1);

        // 2. Remove the bitstream, it should remove the license resource mapping
        bundleService.removeBitstream(context, b, bs);
        context.restoreAuthSystemState();
        List<ClarinLicenseResourceMapping> removedBitstreamResourceMapping = clarinLicenseResourceMappingService
                .findAllByLicenseId(context, clarinLicense.getID());

        assertNotNull(removedBitstreamResourceMapping);
        assertEquals(removedBitstreamResourceMapping.size(), 0);
    }

    /**
     * Add the clarin license to the bitstream and then change the clarin license - the clarin license
     * should be changed in the bitstream.
     */
    @Test
    public void changeBitstreamLicenseOnLicenseChange() throws SQLException, AuthorizeException, IOException {
        // 1. Attach the license to the bitstream
        context.turnOffAuthorisationSystem();
        clarinLicenseService.addLicenseMetadataToItem(context, clarinLicense, item);
        Bitstream bs = this.addFileToBitstream();

        List<ClarinLicenseResourceMapping> bitstreamAddedToLicense = clarinLicenseResourceMappingService
                .findAllByLicenseId(context, clarinLicense.getID());
        // the license is attached to the bitstream
        assertNotNull(bitstreamAddedToLicense);
        assertEquals(bitstreamAddedToLicense.size(), 1);

        // 2. Add another clarin license to the item
        // clear the actual clarin license metadata from the item
        clarinLicenseService.clearLicenseMetadataFromItem(context, item);
        // add a new clarin license metadata to the item
        clarinLicenseService.addLicenseMetadataToItem(context, secondClarinLicense, item);
        // add clarin license to the bitstream
        clarinLicenseService.addClarinLicenseToBitstream(context, item, b, bs);
        context.restoreAuthSystemState();

        // 3. Check if the clarin license was changed in the bitstream
        // the item metadata was changed
        String licenseName = itemService.getMetadataFirstValue(item, "dc", "rights", null, Item.ANY);
        assertEquals(secondClarinLicense.getName(), licenseName);

        // bitstream license was changed
        List<ClarinLicenseResourceMapping> changedBitstreamLicense = clarinLicenseResourceMappingService
                .findAllByLicenseId(context, secondClarinLicense.getID());

        // the license is attached to the bitstream
        assertNotNull(changedBitstreamLicense);
        assertEquals(changedBitstreamLicense.size(), 1);
        assertEquals(changedBitstreamLicense.get(0).getLicense().getName(), secondClarinLicense.getName());
    }

    /**
     * The clarin license metadata should be removed from the item.
     */
    @Test
    public void clearClarinLicenseMetadataFromItem() throws SQLException {
        context.turnOffAuthorisationSystem();
        clarinLicenseService.addLicenseMetadataToItem(context, clarinLicense, item);

        // check if the license metadata was added to the item
        String licenseName = itemService.getMetadataFirstValue(item, "dc", "rights", null, Item.ANY);
        assertEquals(clarinLicense.getName(), licenseName);

        // clear the clarin license metadata from the item
        clarinLicenseService.clearLicenseMetadataFromItem(context, item);
        String licenseNameNull = itemService.getMetadataFirstValue(item, "dc", "rights", null, Item.ANY);
        assertNull(licenseNameNull);
    }

    private Bitstream addFileToBitstream() throws SQLException, AuthorizeException, IOException {
        // run addBitstream method
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = bitstreamService.create(context, new FileInputStream(f));
        bundleService.addBitstream(context, b, bs);
        return bs;
    }

    /**
     * Test of getType method, of class Bundle.
     */
    @Override
    @Test
    public void testGetType() {
        assertThat("testGetType 0", b.getType(), equalTo(Constants.BUNDLE));
    }

    /**
     * Test of getID method, of class Bundle.
     */
    @Override
    @Test
    public void testGetID() {
        assertTrue("testGetID 0", b.getID() != null);
    }

    /**
     * Test of getHandle method, of class Bundle.
     */
    @Override
    @Test
    public void testGetHandle() {
        //no handle for bundles
        assertThat("testGetHandle 0", b.getHandle(), nullValue());
    }

    /**
     * Test of getName method, of class Bundle.
     */
    @Override
    @Test
    public void testGetName() {
        //created bundle has no name
        assertThat("testGetName 0", b.getName(), equalTo(CONTENT_BUNDLE_NAME));
    }

    /**
     * Test of getAdminObject method, of class Bundle.
     */
    @Test
    @Override
    public void testGetAdminObject() throws SQLException {
        //default bundle has no admin object
        assertThat("testGetAdminObject 0", bundleService.getAdminObject(context, b, Constants.REMOVE),
                instanceOf(Item.class));
        assertThat("testGetAdminObject 1", bundleService.getAdminObject(context, b, Constants.ADD),
                instanceOf(Item.class));
    }

    /**
     * Test of getParentObject method, of class Bundle.
     */
    @Test
    @Override
    public void testGetParentObject() throws SQLException {
        //default bundle has no parent
        assertThat("testGetParentObject 0", bundleService.getParentObject(context, b), notNullValue());
        assertThat("testGetParentObject 0", bundleService.getParentObject(context, b), instanceOf(Item.class));
    }
}
