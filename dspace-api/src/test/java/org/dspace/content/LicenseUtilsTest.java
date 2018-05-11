/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.HashMap;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.AbstractUnitTest;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.log4j.Logger;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.LicenseService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;

/**
 * Unit Tests for class LicenseUtils
 * @author pvillega
 */
public class LicenseUtilsTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(LicenseUtilsTest.class);

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected LicenseService licenseService = CoreServiceFactory.getInstance().getLicenseService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private Community owningCommunity;

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
        try
        {
            context.turnOffAuthorisationSystem();
            this.owningCommunity = communityService.create(null, context);
            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();
        }
        catch (SQLException | AuthorizeException ex)
        {
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
    public void destroy()
    {
        super.destroy();
    }

    /**
     * Test of getLicenseText method, of class LicenseUtils.
     */
    @Test
    public void testGetLicenseText_5args() throws SQLException, AuthorizeException, IOException {
        //parameters for the test
        Locale locale = null;
        Collection collection = null;
        Item item = null;
        EPerson person = null;
        Map<String, Object> additionalInfo = null;

        // We don't test attribute 4 as this is the date, and the date often differs between when the test
        // is executed, and when the LicenceUtils code gets the current date/time which causes the test to fail
        String template = "Template license: %1$s %2$s %3$s %5$s %6$s";
        String templateLong = "Template license: %1$s %2$s %3$s %5$s %6$s %8$s %9$s %10$s %11$s";
        String templateResult = "Template license: first name last name testgetlicensetext_5args@email.com  ";
        String templateLongResult = "Template license: first name last name testgetlicensetext_5args@email.com   arg1 arg2 arg3 arg4";
        String defaultLicense = licenseService.getDefaultSubmissionLicense();
        context.turnOffAuthorisationSystem();
        person = ePersonService.create(context);
        person.setFirstName(context, "first name");
        person.setLastName(context, "last name");
        person.setEmail("testGetLicenseText_5args@email.com");

        //TODO: the tested method doesn't verify the input, will throw NPE if any parameter is null

        //testing for default license
        locale = Locale.ENGLISH;
        collection = collectionService.create(context, owningCommunity);
        item = installItemService.installItem(context, workspaceItemService.create(context, collection, false));
        additionalInfo = null;
        assertThat("testGetLicenseText_5args 0", LicenseUtils.getLicenseText(locale, collection, item, person, additionalInfo), equalTo(defaultLicense));

        locale = Locale.GERMAN;
        collection = collectionService.create(context, owningCommunity);
        item = installItemService.installItem(context, workspaceItemService.create(context, collection, false));
        additionalInfo = null;
        assertThat("testGetLicenseText_5args 1", LicenseUtils.getLicenseText(locale, collection, item, person, additionalInfo), equalTo(defaultLicense));

        locale = Locale.ENGLISH;
        collection = collectionService.create(context, owningCommunity);
        item = installItemService.installItem(context, workspaceItemService.create(context, collection, false));
        additionalInfo = new HashMap<String, Object>();
        additionalInfo.put("arg1", "arg1");
        additionalInfo.put("arg2", "arg2");
        additionalInfo.put("arg3", "arg3");
        assertThat("testGetLicenseText_5args 2", LicenseUtils.getLicenseText(locale, collection, item, person, additionalInfo), equalTo(defaultLicense));

        //test collection template
        locale = Locale.ENGLISH;
        collection = collectionService.create(context, owningCommunity);
        collection.setLicense(context, template);
        item = installItemService.installItem(context, workspaceItemService.create(context, collection, false));
        additionalInfo = null;
        assertThat("testGetLicenseText_5args 3", LicenseUtils.getLicenseText(locale, collection, item, person, additionalInfo), equalTo(templateResult));

        locale = Locale.GERMAN;
        collection = collectionService.create(context, owningCommunity);
        collection.setLicense(context, template);
        item = installItemService.installItem(context, workspaceItemService.create(context, collection, false));
        additionalInfo = null;
        assertThat("testGetLicenseText_5args 4", LicenseUtils.getLicenseText(locale, collection, item, person, additionalInfo), equalTo(templateResult));

        locale = Locale.ENGLISH;
        collection = collectionService.create(context, owningCommunity);
        collection.setLicense(context, templateLong);
        item = installItemService.installItem(context, workspaceItemService.create(context, collection, false));
        additionalInfo = new LinkedHashMap<String, Object>();
        additionalInfo.put("arg1", "arg1");
        additionalInfo.put("arg2", "arg2");
        additionalInfo.put("arg3", "arg3");
        additionalInfo.put("arg4", "arg4");
        assertThat("testGetLicenseText_5args 5", LicenseUtils.getLicenseText(locale, collection, item, person, additionalInfo), equalTo(templateLongResult));

        context.restoreAuthSystemState();
    }

    /**
     * Test of getLicenseText method, of class LicenseUtils.
     */
    @Test
    public void testGetLicenseText_4args() throws SQLException, AuthorizeException, IOException {
        //parameters for the test
        Locale locale = null;
        Collection collection = null;
        Item item = null;
        EPerson person = null;

        String template = "Template license: %1$s %2$s %3$s %5$s %6$s";
        String templateResult = "Template license: first name last name testgetlicensetext_4args@email.com  ";
        context.turnOffAuthorisationSystem();
        person = ePersonService.create(context);
        person.setFirstName(context, "first name");
        person.setLastName(context, "last name");
        person.setEmail("testGetLicenseText_4args@email.com");
        ePersonService.update(context, person);

        String defaultLicense = licenseService.getDefaultSubmissionLicense();

        context.turnOffAuthorisationSystem();
        //TODO: the tested method doesn't verify the input, will throw NPE if any parameter is null

        //testing for default license
        locale = Locale.ENGLISH;
        collection = collectionService.create(context, owningCommunity);
        item = installItemService.installItem(context, workspaceItemService.create(context, collection, false));
        assertThat("testGetLicenseText_5args 0", LicenseUtils.getLicenseText(locale, collection, item, person), equalTo(defaultLicense));

        locale = Locale.GERMAN;
        collection = collectionService.create(context, owningCommunity);
        item = installItemService.installItem(context, workspaceItemService.create(context, collection, false));
        assertThat("testGetLicenseText_5args 1", LicenseUtils.getLicenseText(locale, collection, item, person), equalTo(defaultLicense));

        //test collection template
        locale = Locale.ENGLISH;
        collection = collectionService.create(context, owningCommunity);
        collection.setLicense(context, template);
        item = installItemService.installItem(context, workspaceItemService.create(context, collection, false));
        assertThat("testGetLicenseText_5args 3", LicenseUtils.getLicenseText(locale, collection, item, person), equalTo(templateResult));

        locale = Locale.GERMAN;
        collection = collectionService.create(context, owningCommunity);
        collection.setLicense(context, template);
        item = installItemService.installItem(context, workspaceItemService.create(context, collection, false));
        assertThat("testGetLicenseText_5args 4", LicenseUtils.getLicenseText(locale, collection, item, person), equalTo(templateResult));

        context.restoreAuthSystemState();
    }

    /**
     * Test of grantLicense method, of class LicenseUtils.
     */
    @Test
    public void testGrantLicense() throws Exception
    {
        context.turnOffAuthorisationSystem();
        Collection collection = collectionService.create(context, owningCommunity);
        Item item = installItemService.installItem(context, workspaceItemService.create(context, collection, false));
        String defaultLicense = licenseService.getDefaultSubmissionLicense();

        LicenseUtils.grantLicense(context, item, defaultLicense);

        StringWriter writer = new StringWriter();
        IOUtils.copy(bitstreamService.retrieve(context, itemService.getBundles(item, "LICENSE").get(0).getBitstreams().get(0)), writer);
        String license = writer.toString();

        assertThat("testGrantLicense 0",license, equalTo(defaultLicense));
        context.restoreAuthSystemState();
    }

}