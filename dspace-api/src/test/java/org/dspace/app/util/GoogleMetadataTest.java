/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import org.apache.commons.io.Charsets;
import org.apache.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class GoogleMetadataTest extends AbstractUnitTest {

    /** log4j category */
    private static final Logger log = Logger.getLogger(GoogleMetadataTest.class);

    /**
     * Item instance for the tests
     */
    private Item it;

    private BundleService bundleService;

    private BitstreamFormatService bitstreamFormatService;

    private BitstreamService bitstreamService;

    private Community community;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init(){
        super.init();
        try
        {
            context.turnOffAuthorisationSystem();
            community = ContentServiceFactory.getInstance().getCommunityService().create(null, context);
            Collection collection = ContentServiceFactory.getInstance().getCollectionService().create(context, community);
            WorkspaceItem wi = ContentServiceFactory.getInstance().getWorkspaceItemService().create(context, collection, true);
            Item item = wi.getItem();
            ContentServiceFactory.getInstance().getInstallItemService().installItem(context, wi, null);
            context.restoreAuthSystemState();
            context.commit();
            it = item;
            bundleService = ContentServiceFactory.getInstance().getBundleService();
            bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
            bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        }
        catch (AuthorizeException ex)
        {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test to see the priorities work, the PDF should be returned
     * @throws Exception
     */
    @Test
    public void testGetPDFURLDifferentMimeTypes() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = ContentServiceFactory.getInstance().getBundleService().create(context, it, "ORIGINAL");
        Bitstream b = bitstreamService.create(context, new ByteArrayInputStream("Bitstream 1".getBytes(Charsets.UTF_8)));
        b.setName(context, "Word");
        b.setFormat(context, bitstreamFormatService.create(context));
        b.getFormat(context).setMIMEType("application/msword");
        bundleService.addBitstream(context, bundle, b);
        Bitstream b2 = bitstreamService.create(context, new ByteArrayInputStream("Bitstream 2".getBytes(Charsets.UTF_8)));
        b2.setName(context, "Pdf");
        b2.setFormat(context, bitstreamFormatService.create(context));
        b2.getFormat(context).setMIMEType("application/pdf");
        bundleService.addBitstream(context, bundle, b2);
        Bitstream b3 = bitstreamService.create(context, new ByteArrayInputStream("Bitstream 3".getBytes(Charsets.UTF_8)));
        b3.setName(context, "Rtf");
        b3.setFormat(context, bitstreamFormatService.create(context));
        b3.getFormat(context).setMIMEType("text/richtext");
        bundleService.addBitstream(context, bundle, b3);
        context.restoreAuthSystemState();
        context.commit();
        GoogleMetadata gm = new GoogleMetadata(this.context, it);
        String[] urlSplitted = gm.getPDFURL().get(0).split("/");
        assertEquals("Pdf", urlSplitted[urlSplitted.length - 1]);
    }

    /**
     * When multiple bitstreams with the sametype are found, it returns the largest one
     * @throws Exception
     */
    @Test
    public void testGetPDFURLSameMimeTypes() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = ContentServiceFactory.getInstance().getBundleService().create(context, it, "ORIGINAL");;
        Bitstream b = bitstreamService.create(context, new ByteArrayInputStream("123456789".getBytes(Charsets.UTF_8)));
        b.setName(context, "size9");
        b.setFormat(context, bitstreamFormatService.create(context));
        b.getFormat(context).setMIMEType("application/pdf");
        bundleService.addBitstream(context, bundle, b);
        Bitstream b2 = bitstreamService.create(context, new ByteArrayInputStream("1".getBytes(Charsets.UTF_8)));
        b2.setName(context, "size1");
        b2.setFormat(context, bitstreamFormatService.create(context));
        b2.getFormat(context).setMIMEType("application/pdf");
        bundleService.addBitstream(context, bundle, b2);
        Bitstream b3 = bitstreamService.create(context, new ByteArrayInputStream("12345".getBytes(Charsets.UTF_8)));
        b3.setName(context, "size5");
        b3.setFormat(context, bitstreamFormatService.create(context));
        b3.getFormat(context).setMIMEType("text/richtext");
        bundleService.addBitstream(context, bundle, b3);
        context.restoreAuthSystemState();
        context.commit();
        GoogleMetadata gm = new GoogleMetadata(this.context, it);
        String[] urlSplitted = gm.getPDFURL().get(0).split("/");
        assertEquals("size9", urlSplitted[urlSplitted.length - 1]);
    }

    /**
     * Multiple bitstreams with same mimetype and size, just returns the first one
     * @throws Exception
     */
    @Test
    public void testGetPDFURLSameMimeTypesSameSize() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = ContentServiceFactory.getInstance().getBundleService().create(context, it, "ORIGINAL");;
        Bitstream b = bitstreamService.create(context, new ByteArrayInputStream("1".getBytes(Charsets.UTF_8)));
        b.setName(context, "first");
        b.setFormat(context, bitstreamFormatService.create(context));
        b.getFormat(context).setMIMEType("application/pdf");
        bundleService.addBitstream(context, bundle, b);
        Bitstream b2 = bitstreamService.create(context, new ByteArrayInputStream("1".getBytes(Charsets.UTF_8)));
        b2.setName(context, "second");
        b2.setFormat(context, bitstreamFormatService.create(context));
        b2.getFormat(context).setMIMEType("application/pdf");
        bundleService.addBitstream(context, bundle, b2);
        Bitstream b3 = bitstreamService.create(context, new ByteArrayInputStream("1".getBytes(Charsets.UTF_8)));
        b3.setName(context, "third");
        b3.setFormat(context, bitstreamFormatService.create(context));
        b3.getFormat(context).setMIMEType("application/pdf");
        bundleService.addBitstream(context, bundle, b3);
        context.restoreAuthSystemState();
        context.commit();
        GoogleMetadata gm = new GoogleMetadata(this.context, it);
        String[] urlSplitted = gm.getPDFURL().get(0).split("/");
        assertEquals("first", urlSplitted[urlSplitted.length - 1]);
    }

    /**
     * Test to see if that when an item is marked as primary, that it will still be the result of getPdfURL()
     * @throws Exception
     */
    @Test
    public void testGetPDFURLWithPrimaryBitstream() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = ContentServiceFactory.getInstance().getBundleService().create(context, it, "ORIGINAL");;
        Bitstream b = bitstreamService.create(context, new ByteArrayInputStream("Larger file than primary".getBytes(Charsets.UTF_8)));
        b.setName(context, "first");
        b.setFormat(context, bitstreamFormatService.create(context));
        b.getFormat(context).setMIMEType("unknown");
        bundleService.addBitstream(context, bundle, b);
        Bitstream b2 = bitstreamService.create(context, new ByteArrayInputStream("Bitstream with more prioritized mimetype than primary".getBytes(Charsets.UTF_8)));
        b2.setName(context, "second");
        b2.setFormat(context, bitstreamFormatService.create(context));
        b2.getFormat(context).setMIMEType("application/pdf");
        bundleService.addBitstream(context, bundle, b2);
        Bitstream b3 = bitstreamService.create(context, new ByteArrayInputStream("1".getBytes(Charsets.UTF_8)));
        b3.setName(context, "primary");
        b3.setFormat(context, bitstreamFormatService.create(context));
        b3.getFormat(context).setMIMEType("Primary");
        bundleService.addBitstream(context, bundle, b3);
        bundle.setPrimaryBitstreamID(b3);
        context.restoreAuthSystemState();
        context.commit();
        GoogleMetadata gm = new GoogleMetadata(this.context, it);
        String[] urlSplitted = gm.getPDFURL().get(0).split("/");
        assertEquals("primary", urlSplitted[urlSplitted.length - 1]);
    }

    /**
     * Test to make sure mimetypes can be undefined in the property file, just give them lowest priority if
     * this is the case and return the largest.
     * @throws Exception
     */
    @Test
    public void testGetPDFURLWithUndefinedMimeTypes() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = ContentServiceFactory.getInstance().getBundleService().create(context, it, "ORIGINAL");;
        Bitstream b = bitstreamService.create(context, new ByteArrayInputStream("12".getBytes(Charsets.UTF_8)));
        b.setName(context, "small");
        b.setFormat(context, bitstreamFormatService.create(context));
        b.getFormat(context).setMIMEType("unknown type 1");
        bundleService.addBitstream(context, bundle, b);
        Bitstream b2 = bitstreamService.create(context, new ByteArrayInputStream("12121212".getBytes(Charsets.UTF_8)));
        b2.setName(context, "medium");
        b2.setFormat(context, bitstreamFormatService.create(context));
        b2.getFormat(context).setMIMEType("unknown type 2");
        bundleService.addBitstream(context, bundle, b2);
        Bitstream b3 = bitstreamService.create(context, new ByteArrayInputStream("12121212121212".getBytes(Charsets.UTF_8)));
        b3.setName(context, "large");
        b3.setFormat(context, bitstreamFormatService.create(context));
        b3.getFormat(context).setMIMEType("unknown type 3");
        bundleService.addBitstream(context, bundle, b3);
        context.restoreAuthSystemState();
        context.commit();
        GoogleMetadata gm = new GoogleMetadata(this.context, it);
        String[] urlSplitted = gm.getPDFURL().get(0).split("/");
        assertEquals("large", urlSplitted[urlSplitted.length - 1]);
    }


    /**
     * Test for crash when no bundle is given
     * @throws Exception
     */
    @Test
    public void testGetPDFURLWithNoBundle() throws Exception {
        GoogleMetadata gm = new GoogleMetadata(this.context, it);
        assertEquals(0, gm.getPDFURL().size());
    }

    /**
     * Test for crash when no bitstreams are in the bundle
     * @throws Exception
     */
    @Test
    public void testGetPDFURLWithNoBitstreams() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = ContentServiceFactory.getInstance().getBundleService().create(context, it, "ORIGINAL");;
        context.restoreAuthSystemState();
        context.commit();
        GoogleMetadata gm = new GoogleMetadata(this.context, it);
        assertEquals(0, gm.getPDFURL().size());
    }

    /**
     * Test empty bitstreams
     */
    @Test
    public void testGetPDFURLWithEmptyBitstreams() throws Exception{
        context.turnOffAuthorisationSystem();
        Bundle bundle = ContentServiceFactory.getInstance().getBundleService().create(context, it, "ORIGINAL");;
        Bitstream b = bitstreamService.create(context, new ByteArrayInputStream("".getBytes(Charsets.UTF_8)));
        b.setName(context,"small");
        b.setFormat(context, bitstreamFormatService.create(context));
        b.getFormat(context).setMIMEType("unknown type 1");
        bundleService.addBitstream(context, bundle, b);
        Bitstream b2 = bitstreamService.create(context, new ByteArrayInputStream("".getBytes(Charsets.UTF_8)));
        b2.setName(context, "medium");
        b2.setFormat(context, bitstreamFormatService.create(context));
        b2.getFormat(context).setMIMEType("unknown type 2");
        bundleService.addBitstream(context, bundle, b2);
        Bitstream b3 = bitstreamService.create(context, new ByteArrayInputStream("".getBytes(Charsets.UTF_8)));
        b3.setName(context, "large");
        b3.setFormat(context, bitstreamFormatService.create(context));
        b3.getFormat(context).setMIMEType("unknown type 3");
        bundleService.addBitstream(context, bundle, b3);
        context.restoreAuthSystemState();
        context.commit();
        GoogleMetadata gm = new GoogleMetadata(this.context, it);
        String[] urlSplitted = gm.getPDFURL().get(0).split("/");
        assertEquals("small", urlSplitted[urlSplitted.length - 1]);
    }

    @After
    @Override
    public void destroy()
    {
        try {
            context.turnOffAuthorisationSystem();

            //Context might have been committed in the test method, so best to reload to entity so we're sure that it is attached.
            community = context.reloadEntity(community);
            ContentServiceFactory.getInstance().getCommunityService().delete(context, community);
            community = null;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (AuthorizeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        it = null;
        super.destroy();
    }


}