/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit Tests for class Site
 *
 * @author pvillega
 */
public class SiteTest extends AbstractUnitTest {

    /**
     * log4j category
     */
    private static final Logger log = LogManager.getLogger(SiteTest.class);

    /**
     * Site instance for the tests
     */
    private Site s;

    private final SiteService siteService
            = ContentServiceFactory.getInstance().getSiteService();
    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

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
            //we have to create a new community in the database
            context.turnOffAuthorisationSystem();
            this.s = siteService.findSite(context);
            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();
        } catch (SQLException ex) {
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
        s = null;
        super.destroy();
    }

    /**
     * Test of getType method, of class Site.
     */
    @Test
    public void testGetType() {
        assertThat("testGetType 0", s.getType(), equalTo(Constants.SITE));
    }

    /**
     * Test of getID method, of class Site.
     */
    @Test
    public void testGetID() {
        assertTrue("testGetID 0", s.getID() != null);
    }

    /**
     * Test of getHandle method, of class Site.
     */
    @Test
    public void testGetHandle() {
        assertThat("testGetHandle 0", s.getHandle(),
                equalTo(configurationService.getProperty("handle.prefix") + "/0"));
    }

    /**
     * Test of getSiteHandle method, of class Site.
     */
    @Test
    public void testGetSiteHandle() {
        assertThat("testGetSiteHandle 0", s.getHandle(),
                equalTo(configurationService.getProperty("handle.prefix") + "/0"));
    }

    /**
     * Test of find method, of class Site.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testSiteFind() throws Exception {
        Site found = siteService.findSite(context);
        assertThat("testSiteFind 0", found, notNullValue());
        assertThat("testSiteFind 1", found, equalTo(s));
    }

    /**
     * Test of getName method, of class Site.
     */
    @Test
    public void testGetName() {
        assertThat("testGetName 0", s.getName(),
                equalTo(configurationService.getProperty("dspace.name")));
        assertThat("testGetName 1", siteService.getName(s),
                equalTo(configurationService.getProperty("dspace.name")));
    }

    /**
     * Test of getURL method, of class Site.
     */
    @Test
    public void testGetURL() {
        assertThat("testGetURL 0", s.getURL(),
                equalTo(configurationService.getProperty("dspace.ui.url")));
    }

    @Test
    public void testAnonymousReadRights() throws Exception {
        List<Group> groupList = authorizeService.getAuthorizedGroups(context, s, Constants.READ);
        boolean foundAnonInList = false;
        for (Group group : groupList) {
            if (StringUtils.equalsIgnoreCase(group.getName(), "Anonymous")) {
                foundAnonInList = true;
            }
        }
        assertTrue(foundAnonInList);

    }

}
