/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;
import org.dspace.AbstractUnitTest;
import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

public class HandleServiceTest extends AbstractUnitTest {
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected ConfigurationService configurationService = new DSpace().getConfigurationService();

    private static final String HANDLE_PREFIX = "123456789";

    @Before
    @Override
    public void init() {
        super.init();
        AbstractBuilder.init(); // AbstractUnitTest doesn't do this for us.

        configurationService.setProperty("handle.prefix", HANDLE_PREFIX);
        configurationService.setProperty("handle.canonical.prefix", "https://fake.canonical.prefix");
        configurationService.setProperty("handle.additional.prefixes", "987654321, 654321987");
    }

    @Test
    public void testParseHandleInvalid() {
        assertNull(handleService.parseHandle(null));
        assertNull(handleService.parseHandle("123456789"));
        assertNull(handleService.parseHandle("/123456789"));
        assertNull(handleService.parseHandle("https://duraspace.org/dspace/"));
        assertNull(handleService.parseHandle("10.70131/test_doi_5d2be995d35b6"));
        assertNull(handleService.parseHandle("not a handle"));
    }

    @Test
    public void testParseHandleByPrefix() {
        // note: handle pattern after prefix is not checked
        assertEquals("123456789/111", handleService.parseHandle("123456789/111"));
    }

    @Test
    public void testParseHandleByCanonicalPrefix() {
        // note: handle pattern after prefix is not checked
        assertEquals("111222333/111", handleService.parseHandle("https://fake.canonical.prefix/111222333/111"));
    }

    @Test
    public void testParseHandleByAdditionalPrefix() {
        // note: handle pattern after prefix is not checked
        assertEquals("987654321/111", handleService.parseHandle("987654321/111"));
        assertEquals("654321987/111", handleService.parseHandle("654321987/111"));
    }

    @Test
    public void testParseHandleByPattern() {
        assertEquals("111222333/111", handleService.parseHandle("hdl:111222333/111"));
        assertEquals("111222333/111", handleService.parseHandle("info:hdl/111222333/111"));
        assertEquals("111222333/111", handleService.parseHandle("https://hdl.handle.net/111222333/111"));
        assertEquals("111222333/111", handleService.parseHandle("http://hdl.handle.net/111222333/111"));
        assertEquals("111222333/111", handleService.parseHandle("https://whatever/handle/111222333/111"));
        assertEquals("111222333/111", handleService.parseHandle("http://whatever/handle/111222333/111"));
    }

    @Test
    public void testResolveToURL()
            throws SQLException, URISyntaxException {
        final String HOST = "dspace.example.com";
        configurationService.setProperty("dspace.ui.url", "https://" + HOST + "/");
        context.turnOffAuthorisationSystem();
        Community com = CommunityBuilder.createCommunity(context).build();
        Collection col = CollectionBuilder.createCollection(context, com).build();
        Item item = ItemBuilder.createItem(context, col).build();
        context.restoreAuthSystemState();

        HandleServiceImpl tested = new HandleServiceImpl();
        tested.configurationService = configurationService;
        tested.dspaceObjectService = item.getItemService();
        tested.handleDAO = new DummyHandleDAO(item);
        tested.siteService = ContentServiceFactory.getInstance().getSiteService();
        String url = tested.resolveToURL(context, HANDLE_PREFIX + "/1");

        // Check url
        URIBuilder builder = new URIBuilder(url);
        assertEquals("Wrong scheme:", "https", builder.getScheme());
        assertEquals("Wrong host:", HOST, builder.getHost());

        List<String> path = builder.getPathSegments();
        assertEquals("Wrong path length:", 2, path.size());
        assertEquals("Wrong Handle prefix:",
                Constants.typeTextPlural[Constants.ITEM].toLowerCase(),
                path.get(0));
        assertEquals("Wrong Handle suffix:",
                item.getID().toString(),
                path.get(1));

        assertThat("Should have no fragment:", builder.getFragment(), is(blankOrNullString()));
        assertThat("Should have no query:", builder.getQueryParams(), is(empty()));
        assertThat("Should have no credentials", builder.getUserInfo(), is(blankOrNullString()));
    }
}
