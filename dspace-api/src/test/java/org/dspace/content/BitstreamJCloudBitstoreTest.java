/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.Assert.assertTrue;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit Tests for class Bitstream
 *
 * @author Mark Diggory
 */
public class BitstreamJCloudBitstoreTest extends BitstreamTest {

    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance()
            .getBitstreamFormatService();

    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    protected BitstreamStorageService bitstreamService = StorageServiceFactory.getInstance()
            .getBitstreamStorageService();


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
        bitstreamService.setIncomingExternal(2);
        super.init();
    }

    /**
     * Test of getStoreNumber method, of class Bitstream.
     */
    @Test
    @Override
    public void testGetStoreNumber() {
        //stored in store 2 by default
        assertTrue("testGetStoreNumber 2", bs.getStoreNumber() == 2);
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
        bitstreamService.setIncomingExternal(0);
        super.destroy();
    }


}
