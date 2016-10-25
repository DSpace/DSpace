/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import mockit.*;

import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Calendar;
import java.util.TimeZone;

import org.dspace.AbstractUnitTest;
import org.apache.log4j.Logger;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;
import org.junit.rules.ExpectedException;

/**
 * Unit Tests for class InstallItem
 * @author pvillega
 */
public class InstallItemTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(InstallItemTest.class);

    /** Used to check/verify thrown exceptions in below tests **/
    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
     * Test of installItem method (with a valid handle), of class InstallItem.
     */
    @Test
    public void testInstallItem_validHandle() throws Exception
    {
        context.turnOffAuthorisationSystem();
        String handle = "1345/567";
        Collection col = Collection.create(context);
        WorkspaceItem is = WorkspaceItem.create(context, col, false);
      
        //Test assigning a specified handle to an item
        // (this handle should not already be used by system, as it doesn't start with "1234567689" prefix)
        Item result = InstallItem.installItem(context, is, handle);
        context.restoreAuthSystemState();
        assertThat("testInstallItem_validHandle", result, equalTo(is.getItem()));
        assertThat("testInstallItem_validHandle", result.getHandle(), equalTo(handle));
    }

    /**
     * Test of installItem method (with an invalid handle), of class InstallItem.
     */
    @Test
    public void testInstallItem_invalidHandle() throws Exception
    {
        //Default to Full-Admin rights
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Deny Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = false;
            // Allow full Admin perms
            AuthorizeManager.isAdmin((Context) any); result = true;
        }};

        String handle = "1345/567";
        Collection col = Collection.create(context);
        WorkspaceItem is = WorkspaceItem.create(context, col, false);
        WorkspaceItem is2 = WorkspaceItem.create(context, col, false);
        
        //Test assigning the same Handle to two different items
        InstallItem.installItem(context, is, handle);

        // Assigning the same handle again should throw a RuntimeException
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Error while attempting to create identifier");
        InstallItem.installItem(context, is2, handle);
        fail("Exception expected");
    }


    /**
     * Test of restoreItem method, of class InstallItem.
     */
    @Test
    public void testRestoreItem() throws Exception
    {
        context.turnOffAuthorisationSystem();
        String handle = "1345/567";
        Collection col = Collection.create(context);
        WorkspaceItem is = WorkspaceItem.create(context, col, false);

        //get current date
        DCDate now = DCDate.getCurrent();
        String dayAndTime = now.toString();
        //parse out just the date, remove the time (format: yyyy-mm-ddT00:00:00Z)
        String date = dayAndTime.substring(0, dayAndTime.indexOf("T"));

        //Build the beginning of a dummy provenance message
        //(restoreItem should NEVER insert a provenance message with today's date)
        String provDescriptionBegins = "Made available in DSpace on " + date;
        
        Item result = InstallItem.restoreItem(context, is, handle);
        context.restoreAuthSystemState();

        //Make sure restore worked
        assertThat("testRestoreItem 0", result, equalTo(is.getItem()));

        //Make sure that restore did NOT insert a new provenance message with today's date
        Metadatum[] provMsgValues = result.getMetadata("dc", "description", "provenance", Item.ANY);
        int i = 1;
        for(Metadatum val : provMsgValues)
        {
            assertFalse("testRestoreItem " + i, val.value.startsWith(provDescriptionBegins));
            i++;
        }
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

    /**
     * Test passing in "today" as an issued date to InstallItem.
     */
    @Test
    public void testInstallItem_todayAsIssuedDate() throws Exception
    {
        //create a dummy WorkspaceItem
        context.turnOffAuthorisationSystem();
        String handle = "1345/567";
        Collection col = Collection.create(context);
        WorkspaceItem is = WorkspaceItem.create(context, col, false);

        // Set "today" as "dc.date.issued"
        is.getItem().addMetadata("dc", "date", "issued", Item.ANY, "today");
        is.getItem().addMetadata("dc", "date", "issued", Item.ANY, "2011-01-01");

        //get current date
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
       
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        String date = sdf.format(calendar.getTime());

        Item result = InstallItem.installItem(context, is, handle);
        context.restoreAuthSystemState();

        //Make sure the string "today" was replaced with today's date
        Metadatum[] issuedDates = result.getMetadata("dc", "date", "issued", Item.ANY);

        assertThat("testInstallItem_todayAsIssuedDate 0", issuedDates[0].value, equalTo(date));
        assertThat("testInstallItem_todayAsIssuedDate 1", issuedDates[1].value, equalTo("2011-01-01"));
    }

    /**
     * Test null issue date (when none set) in InstallItem
     */
    @Test
    public void testInstallItem_nullIssuedDate() throws Exception
    {
        //create a dummy WorkspaceItem with no dc.date.issued
        context.turnOffAuthorisationSystem();
        String handle = "1345/567";
        Collection col = Collection.create(context);
        WorkspaceItem is = WorkspaceItem.create(context, col, false);

        Item result = InstallItem.installItem(context, is, handle);
        context.restoreAuthSystemState();

        //Make sure dc.date.issued is NOT set
        Metadatum[] issuedDates = result.getMetadata("dc", "date", "issued", Item.ANY);
        assertThat("testInstallItem_nullIssuedDate 0", issuedDates.length, equalTo(0));
    }

    /**
     * Test passing in "today" as an issued date to restoreItem.
     */
    @Test
    public void testRestoreItem_todayAsIssuedDate() throws Exception
    {
        //create a dummy WorkspaceItem
        context.turnOffAuthorisationSystem();
        String handle = "1345/567";
        Collection col = Collection.create(context);
        WorkspaceItem is = WorkspaceItem.create(context, col, false);

        // Set "today" as "dc.date.issued"
        is.getItem().addMetadata("dc", "date", "issued", Item.ANY, "today");
        is.getItem().addMetadata("dc", "date", "issued", Item.ANY, "2011-01-01");

        //get current date
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(calendar.getTime());

        Item result = InstallItem.restoreItem(context, is, handle);
        context.restoreAuthSystemState();

        //Make sure the string "today" was replaced with today's date
        Metadatum[] issuedDates = result.getMetadata("dc", "date", "issued", Item.ANY);

        assertThat("testRestoreItem_todayAsIssuedDate 0", issuedDates[0].value, equalTo(date));
        assertThat("testRestoreItem_todayAsIssuedDate 1", issuedDates[1].value, equalTo("2011-01-01"));
    }
}
