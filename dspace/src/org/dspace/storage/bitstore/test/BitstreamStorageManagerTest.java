/*
 * BitstreamStorageManagerTest.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.storage.bitstore.test;


import java.io.*;
import java.sql.SQLException;
import java.util.*;

import junit.framework.*;
import junit.extensions.*;

import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.bitstore.BitstreamStorageManager;
import org.dspace.storage.rdbms.*;

/**
 * Test of BitstreamStorage suite
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class BitstreamStorageManagerTest extends TestCase
{
    /**
     * Asset store directory for testing
     */
    private static final String root = System.getProperty("java.io.tmpdir") + File.separator + "bitstoretest";

    /**
     * Constructor
     */
    public BitstreamStorageManagerTest (String testname)
    {
        super(testname);
    }

    /**
     * Test basic functionality: store, retrieve, delete
     */
    public void testBasic()
    {
        try
        {
            Context context = new Context();
            byte[] data = getTestData();

            int id = BitstreamStorageManager.store
                (context, new ByteArrayInputStream(data));

            TableRow bitstream = DatabaseManager.find(context, "bitstream", id);
            assertNotNull("Found stored bitstream", bitstream);
            assertEquals("Checksums are equal",
                         bitstream.getStringColumn("checksum"),
                         Utils.getMD5(data));
            assertEquals("Sizes are equal",
                         bitstream.getIntColumn("size"),
                         data.length);
            assertTrue("Deleted flag is false",
                       ! bitstream.getBooleanColumn("deleted"));

            InputStream istream = BitstreamStorageManager.retrieve(context, id);
            assertNotNull("Got a stream from retrieve method", istream);
            assertTrue("Data is byte-for-byte identical",
                       compareData(data, copyData(istream)));

            BitstreamStorageManager.delete(context, id);
            InputStream dstream = BitstreamStorageManager.retrieve(context, id);
            assertNull("Retrieve after delete returns null", dstream);

            bitstream = DatabaseManager.find(context, "bitstream", id);
            assertTrue("Bitstream is either null or marked deleted",
                       ((bitstream == null) ||
                        (bitstream.getBooleanColumn("deleted"))));

            context.complete();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            fail("Exception while running test: " + e);
        }
    }

    /**
     * Test what happens when we abort a transaction
     */
    public void testAbort()
    {
        try
        {
            Context context = new Context();
            byte[] data = getTestData();

            int id = BitstreamStorageManager.store
                (context, new ByteArrayInputStream(data));

            InputStream istream = BitstreamStorageManager.retrieve(context, id);
            assertNotNull("Got a stream from retrieve method", istream);

            context.abort();
            Context newContext = new Context();
            InputStream astream = BitstreamStorageManager.retrieve
                (newContext, id);
            assertNull("Retrieve after abort returns null", astream);

            TableRow bitstream = DatabaseManager.find(newContext, "bitstream", id);
            assertTrue("Bitstream is either null or marked deleted",
                       ((bitstream == null) ||
                        bitstream.getBooleanColumn("deleted")));
            newContext.complete();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            fail("Exception while running test: " + e);
        }
    }

    /**
     * Test behavior when using an invalid asset store root
     */
    public void testBadAssetstoreRoot()
    {
        String badroot = System.getProperty("java.io.tmpdir") +
            File.separator + "thisdirectorydoesnotexist";

        if (new File(badroot).exists())
        {
            System.out.println("Bogus directory actually exists, skipping test");
            return;
        }

        try
        {
            BitstreamStorageManagerShim.publicSetRoot(badroot);
            assertTrue("Should not reach this line", false);
        }
        catch (IOException ioe)
        {
            assertTrue("Expecting IOException", true);
        }
        finally
        {
            try
            {
                BitstreamStorageManagerShim.publicSetRoot(root);
            }
            catch (IOException ioe)
            {
                assertTrue("Caught IOException while resetting root", false);
            }
        }
    }

    /**
     * Test cleanup method
     */
    public void testCleanup()
    {
        try
        {
            Context context = new Context();
            byte[] data = getTestData();

            int id = BitstreamStorageManager.store
                (context, new ByteArrayInputStream(data));

            BitstreamStorageManager.delete(context, id);

            File file = BitstreamStorageManagerShim.publicForId(context, id, true);
            setLastModified(file);
            context.complete();

            BitstreamStorageManager.cleanup();

            context = new Context();
            assertNull("Bitstream has been deleted by cleanup method",
                       DatabaseManager.find(context, "bitstream", id));
            context.complete();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            fail("Exception while running test: " + e);
        }
    }

    ////////////////////////////////////////
    // Helper methods
    ////////////////////////////////////////

    /**
     * Obtain a byte array of test data
     */
    private byte[] getTestData()
        throws IOException
    {
        // Print the system properties out as a byte array

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        
        System.getProperties().list(ps);
        ps.flush();

        return baos.toByteArray();
    }

    /**
     * Copy stream to a byte array
     */
    private byte[] copyData(InputStream istream)
        throws IOException
    {
        ByteArrayOutputStream ros = new ByteArrayOutputStream();
        Utils.bufferedCopy(istream, ros);
        return ros.toByteArray();
    }

    /**
     * Compare two byte arrays
     */
    private boolean compareData(byte[] first, byte[] second)
    {
        assertNotNull("Data is non-null", first);
        assertNotNull("Data is non-null", second);
        assertEquals("Data is same length", first.length, second.length);

        for (int i = 0; i < first.length; i++)
        {
            if (first[i] != second[i])
                return false;
        }

        return true;
    }

    /**
     * Recursively delete FILE.
     */
    private static boolean recursiveDelete(File file)
    {
        if (!file.exists())
            return false;

        if (!file.isDirectory())
            return file.delete();

        File[] files = file.listFiles();

        for (int i = 0; i < files.length; i++)
            recursiveDelete(files[i]);

        return file.delete();
    }

    /**
     * Change the last modified time of this file.
     */
    private static void setLastModified(File file)
    {
        Calendar now = Calendar.getInstance();
        now.setTime(new java.util.Date());
        // Subtract 1 month from today
        now.add(Calendar.MONTH, -1);
        file.setLastModified(now.getTime().getTime());
    }

    /**
     * Test suite
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(BitstreamStorageManagerTest.class);
        // Wrapper to add setup and cleanup
        TestSetup wrapper = new TestSetup(suite)
            {
                public void setUp()
                {
                    try
                    {
                        File firstDir = new File(root);

                        recursiveDelete(firstDir);
                        firstDir.mkdir();

                        BitstreamStorageManagerShim.publicSetRoot(root);
                    }
                    catch (Exception e)
                    {
                        fail("Caught exception " + e);
                    }
                }

                public void tearDown()
                {
                    try
                    {
                        BitstreamStorageManagerShim.publicSetRoot();
                        recursiveDelete(new File(root));
                    }
                    catch (Exception e) {}
                }
            };

        return wrapper;
    }

    /**
     * Embedded test harness
     *
     * @param argv - Command-line arguments
     */
    public static void main(String[] argv)
    {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }
}

// Extension of the BitstreamStorageManager for test purposes
class BitstreamStorageManagerShim extends BitstreamStorageManager
{
    public static void publicSetRoot(String dir)
        throws IOException
    {
        setRoot(dir);
    }

    public static void publicSetRoot()
        throws IOException
    {
        setRoot();
    }

    public static File publicForId(Context context, int id, boolean flag)
        throws SQLException, IOException
    {
        return forId(context, id, flag);
    }

}
