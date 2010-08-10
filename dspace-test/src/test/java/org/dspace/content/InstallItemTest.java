/*
 * InstallItemTest.java
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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

package org.dspace.content;

import java.io.FileInputStream;
import java.io.File;
import org.dspace.AbstractUnitTest;
import org.apache.log4j.Logger;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;


/**
 * Unit Tests for class InstallItem
 * @author pvillega
 */
public class InstallItemTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(InstallItemTest.class);

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init()
    {
        super.init();
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
    public void destroy()
    {
        super.destroy();
    }

    /**
     * Test of installItem method, of class InstallItem.
     */
    @Test
    public void testInstallItem_Context_InProgressSubmission() throws Exception 
    {
        context.turnOffAuthorisationSystem();
        Collection col = Collection.create(context);
        WorkspaceItem is = WorkspaceItem.create(context, col, false);

        Item result = InstallItem.installItem(context, is);
        context.restoreAuthSystemState();
        assertThat("testInstallItem_Context_InProgressSubmission 0", result, equalTo(is.getItem()));
    }

    /**
     * Test of installItem method, of class InstallItem.
     */
    @Test
    public void testInstallItem_3args() throws Exception
    {
        context.turnOffAuthorisationSystem();
        String handle = "1345/567";
        Collection col = Collection.create(context);
        WorkspaceItem is = WorkspaceItem.create(context, col, false);

        Item result = InstallItem.installItem(context, is, handle);
        context.restoreAuthSystemState();
        assertThat("testInstallItem_3args 0", result, equalTo(is.getItem()));
        assertThat("testInstallItem_3args 1", result.getHandle(), equalTo(handle));
    }

    /**
     * Test of getBitstreamProvenanceMessage method, of class InstallItem.
     */
    @Test
    public void testGetBitstreamProvenanceMessage() throws Exception
    {
        File f = new File(testProps.get("test.bitstream").toString());
        context.turnOffAuthorisationSystem();
        Item item = Item.create(context);
        context.commit();

        Bitstream one = item.createSingleBitstream(new FileInputStream(f));
        one.setName("one");
        context.commit();

        Bitstream two = item.createSingleBitstream(new FileInputStream(f));
        two.setName("two");
        context.commit();
        
        context.restoreAuthSystemState();

        // Create provenance description
        String testMessage = "No. of bitstreams: 2\n";
        testMessage += "one: "
                    + one.getSize() + " bytes, checksum: "
                    + one.getChecksum() + " ("
                    + one.getChecksumAlgorithm() + ")\n";
        testMessage += "two: "
                    + two.getSize() + " bytes, checksum: "
                    + two.getChecksum() + " ("
                    + two.getChecksumAlgorithm() + ")\n";

        assertThat("testGetBitstreamProvenanceMessage 0", InstallItem.getBitstreamProvenanceMessage(item), equalTo(testMessage));
    }

}