/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.commons.io.Charsets;
import org.apache.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.app.util.GoogleMetadata;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import sun.net.www.content.text.PlainTextInputStream;

import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class GoogleMetadataTest extends AbstractUnitTest {

    /** log4j category */
    private static final Logger log = Logger.getLogger(GoogleMetadataTest.class);

    /**
     * Item instance for the tests
     */
    private Item it;

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
        try
        {
            context.turnOffAuthorisationSystem();
            Collection c = Collection.create(context);
            WorkspaceItem wi = WorkspaceItem.create(context, c, false);
            Item item = wi.getItem();

            InstallItem.installItem(context, wi, null);
            item.update();
            context.restoreAuthSystemState();
            context.commit();
            it = item;
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
        }
        catch (IOException ex) {
            log.error("IO Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        }
    }

    /**
     * Test to see the priorities work, the PDF should be returned
     * @throws Exception
     */
    @Test
    public void testGetPDFURLDifferentMimeTypes() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = it.createBundle("ORIGINAL");
        Bitstream b = bundle.createBitstream(new ByteArrayInputStream("Bitstream 1".getBytes(Charsets.UTF_8)));
        b.setName("Word");
        b.setFormat(BitstreamFormat.create(context));
        b.getFormat().setMIMEType("application/msword");
        bundle.addBitstream(b);
        Bitstream b2 = bundle.createBitstream(new ByteArrayInputStream("Bitstream 2".getBytes(Charsets.UTF_8)));
        b2.setName("Pdf");
        b2.setFormat(BitstreamFormat.create(context));
        b2.getFormat().setMIMEType("application/pdf");
        bundle.addBitstream(b2);
        Bitstream b3 = bundle.createBitstream(new ByteArrayInputStream("Bitstream 3".getBytes(Charsets.UTF_8)));
        b3.setName("Rtf");
        b3.setFormat(BitstreamFormat.create(context));
        b3.getFormat().setMIMEType("text/richtext");
        bundle.addBitstream(b3);
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
        Bundle bundle = it.createBundle("ORIGINAL");
        Bitstream b = bundle.createBitstream(new ByteArrayInputStream("123456789".getBytes(Charsets.UTF_8)));
        b.setName("size9");
        b.setFormat(BitstreamFormat.create(context));
        b.getFormat().setMIMEType("application/pdf");
        bundle.addBitstream(b);
        Bitstream b2 = bundle.createBitstream(new ByteArrayInputStream("1".getBytes(Charsets.UTF_8)));
        b2.setName("size1");
        b2.setFormat(BitstreamFormat.create(context));
        b2.getFormat().setMIMEType("application/pdf");
        bundle.addBitstream(b2);
        Bitstream b3 = bundle.createBitstream(new ByteArrayInputStream("12345".getBytes(Charsets.UTF_8)));
        b3.setName("size5");
        b3.setFormat(BitstreamFormat.create(context));
        b3.getFormat().setMIMEType("text/richtext");
        bundle.addBitstream(b3);
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
        Bundle bundle = it.createBundle("ORIGINAL");
        Bitstream b = bundle.createBitstream(new ByteArrayInputStream("1".getBytes(Charsets.UTF_8)));
        b.setName("first");
        b.setFormat(BitstreamFormat.create(context));
        b.getFormat().setMIMEType("application/pdf");
        bundle.addBitstream(b);
        Bitstream b2 = bundle.createBitstream(new ByteArrayInputStream("1".getBytes(Charsets.UTF_8)));
        b2.setName("second");
        b2.setFormat(BitstreamFormat.create(context));
        b2.getFormat().setMIMEType("application/pdf");
        bundle.addBitstream(b2);
        Bitstream b3 = bundle.createBitstream(new ByteArrayInputStream("1".getBytes(Charsets.UTF_8)));
        b3.setName("third");
        b3.setFormat(BitstreamFormat.create(context));
        b3.getFormat().setMIMEType("application/pdf");
        bundle.addBitstream(b3);
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
        Bundle bundle = it.createBundle("ORIGINAL");
        Bitstream b = bundle.createBitstream(new ByteArrayInputStream("Larger file than primary".getBytes(Charsets.UTF_8)));
        b.setName("first");
        b.setFormat(BitstreamFormat.create(context));
        b.getFormat().setMIMEType("unknown");
        bundle.addBitstream(b);
        Bitstream b2 = bundle.createBitstream(new ByteArrayInputStream("Bitstream with more prioritized mimetype than primary".getBytes(Charsets.UTF_8)));
        b2.setName("second");
        b2.setFormat(BitstreamFormat.create(context));
        b2.getFormat().setMIMEType("application/pdf");
        bundle.addBitstream(b2);
        Bitstream b3 = bundle.createBitstream(new ByteArrayInputStream("1".getBytes(Charsets.UTF_8)));
        b3.setName("primary");
        b3.setFormat(BitstreamFormat.create(context));
        b3.getFormat().setMIMEType("Primary");
        bundle.addBitstream(b3);
        bundle.setPrimaryBitstreamID(b3.getID());
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
        Bundle bundle = it.createBundle("ORIGINAL");
        Bitstream b = bundle.createBitstream(new ByteArrayInputStream("12".getBytes(Charsets.UTF_8)));
        b.setName("small");
        b.setFormat(BitstreamFormat.create(context));
        b.getFormat().setMIMEType("unknown type 1");
        bundle.addBitstream(b);
        Bitstream b2 = bundle.createBitstream(new ByteArrayInputStream("12121212".getBytes(Charsets.UTF_8)));
        b2.setName("medium");
        b2.setFormat(BitstreamFormat.create(context));
        b2.getFormat().setMIMEType("unknown type 2");
        bundle.addBitstream(b2);
        Bitstream b3 = bundle.createBitstream(new ByteArrayInputStream("12121212121212".getBytes(Charsets.UTF_8)));
        b3.setName("large");
        b3.setFormat(BitstreamFormat.create(context));
        b3.getFormat().setMIMEType("unknown type 3");
        bundle.addBitstream(b3);
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
        Bundle bundle = it.createBundle("ORIGINAL");
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
        Bundle bundle = it.createBundle("ORIGINAL");
        Bitstream b = bundle.createBitstream(new ByteArrayInputStream("".getBytes(Charsets.UTF_8)));
        b.setName("small");
        b.setFormat(BitstreamFormat.create(context));
        b.getFormat().setMIMEType("unknown type 1");
        bundle.addBitstream(b);
        Bitstream b2 = bundle.createBitstream(new ByteArrayInputStream("".getBytes(Charsets.UTF_8)));
        b2.setName("medium");
        b2.setFormat(BitstreamFormat.create(context));
        b2.getFormat().setMIMEType("unknown type 2");
        bundle.addBitstream(b2);
        Bitstream b3 = bundle.createBitstream(new ByteArrayInputStream("".getBytes(Charsets.UTF_8)));
        b3.setName("large");
        b3.setFormat(BitstreamFormat.create(context));
        b3.getFormat().setMIMEType("unknown type 3");
        bundle.addBitstream(b3);
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
        it = null;
        super.destroy();
    }


}