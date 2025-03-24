/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;

import org.dspace.AbstractUnitTest;
import org.dspace.app.requestitem.factory.RequestItemServiceFactory;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RequestItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit testing for RequestItem and RequestItemService ("Request-a-copy" feature)
 *
 * @author Kim Shepherd
 */
public class RequestItemTest extends AbstractUnitTest {

    private static RequestItemService requestItemService;
    private static ConfigurationService configurationService;
    private static HandleService handleService;
    private static BitstreamService bitstreamService;

    private Community parentCommunity;
    private Collection collection;
    private Item item;
    private Bitstream bitstream;
    private Context context;

    @BeforeClass
    public static void setUpClass()
            throws SQLException {
        AbstractBuilder.init(); // AbstractUnitTest doesn't do this for us.
        Context ctx = new Context();
        ctx.turnOffAuthorisationSystem();
        ctx.restoreAuthSystemState();
        ctx.complete();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        // AbstractUnitTest doesn't do this for us.
        AbstractBuilder.cleanupObjects();
        AbstractBuilder.destroy();
    }

   // @Override
    @Before
    public void setUp() throws Exception {
        //super.setUp();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        handleService
                = HandleServiceFactory.getInstance().getHandleService();
        requestItemService = RequestItemServiceFactory.getInstance().getRequestItemService();
        bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        configurationService.setProperty("mail.server.disabled", "false");

        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
            context.setCurrentUser(eperson);
            // Create test resources
            parentCommunity = CommunityBuilder
                    .createCommunity(context)
                    .withName("Community")
                    .build();
            collection = CollectionBuilder
                    .createCollection(context, parentCommunity)
                    .withName("Collection")
                    .withAdminGroup(eperson)
                    .build();
            item = ItemBuilder
                    .createItem(context, collection)
                    .withTitle("Item")
                    .build();
            InputStream is = new ByteArrayInputStream(new byte[0]);
            bitstream = BitstreamBuilder
                    .createBitstream(context, item, is)
                    .withName("Bitstream")
                    .build();
        } catch (SQLException | AuthorizeException | IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAccessTokenGenerationWithLargeFile() throws Exception {
        // Create large bitstream over threshold
        byte[] bytes = new byte[21 * 1024 * 1024]; // 21MB
        InputStream is = new ByteArrayInputStream(bytes);
        Bitstream largeBitstream = BitstreamBuilder
                .createBitstream(context, item, is)
                .withName("LargeBitstream")
                .build();

        RequestItem request = RequestItemBuilder
                .createRequestItem(context, item, largeBitstream)
                .build();

        // Since we are over the threshold, the token should not be null
        assertNotNull("Request token should not be null", request.getAccess_token());
    }

    @Test
    public void testAccessTokenGenerationWithSmallFile() throws Exception {
        // Create small file under the default threshold of 20MB
        byte[] bytes = new byte[1 * 1024 * 1024]; // 1MB
        InputStream is = new ByteArrayInputStream(bytes);
        Bitstream smallBitstream = BitstreamBuilder
                .createBitstream(context, item, is)
                .withName("SmallBitstream")
                .build();

        RequestItem request = RequestItemBuilder
                .createRequestItem(context, item, smallBitstream)
                .build();

        // Since we are under the threshold, the token should be null and
        // the item will be sent via email attachment
        assertNull("Request token should be null", request.getAccess_token());
    }

    @Test
    public void testAuthorizeWithValidPeriod() throws Exception {
        Instant decisionDate = getYesterdayAsInstant();
        RequestItem request = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .withAcceptRequest(true)
                .withAccessToken("test-token")
                .withDecisionDate(decisionDate) // Yesterday
                .withAccessExpiry(getExpiryAsInstant("+10DAYS", decisionDate)) // 10 day period
                .build();

        // The access token should be valid so we expect no exceptions
        try {
            requestItemService.authorizeAccessByAccessToken(context, request, bitstream, request.getAccess_token());
            assertNotNull(request.getAccess_token());
        } catch (AuthorizeException e) {
            fail("AuthorizeException should not be thrown for a valid expiry period");
        }
    }

    @Test(expected = AuthorizeException.class)
    public void testAuthorizeWithExpiredPeriod() throws Exception {
        Instant decisionDate = getYesterdayAsInstant();
        RequestItem request = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .withAcceptRequest(true)
                .withAccessToken("test-token")
                .withDecisionDate(decisionDate) // Yesterday
                .withAccessExpiry(getExpiryAsInstant("+1DAY", decisionDate)) // 1 day period
                .build();

        // The access token should not be valid so we expect to catch an AuthorizeException
        // when we call RequestItemService.authorizeByAccessToken
        requestItemService.authorizeAccessByAccessToken(context, request, bitstream, request.getAccess_token());
    }


    @Test(expected = AuthorizeException.class)
    public void testAuthorizeWithNullToken() throws Exception {
        requestItemService.authorizeAccessByAccessToken(context, bitstream, null);
    }

    @Test(expected = AuthorizeException.class)
    public void testAuthorizeWithInvalidToken() throws Exception {
        requestItemService.authorizeAccessByAccessToken(context, bitstream, "invalid-token-123");
    }

    @Test(expected = AuthorizeException.class)
    public void testAuthorizeWithMismatchedToken() throws Exception {
        Instant decisionDate = getYesterdayAsInstant();
        RequestItem request = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .withAcceptRequest(true)
                .withAccessToken("test-token")
                .withDecisionDate(decisionDate) // Yesterday
                .withAccessExpiry(getExpiryAsInstant("FOREVER", decisionDate)) // forever
                .build();

        // The access token should NOT valid so we expect to catch an AuthorizeException
        // when we call RequestItemService.authorizeByAccessToken
        requestItemService.authorizeAccessByAccessToken(context, request, bitstream, "invalid-token-123");
    }
    @Test(expected = AuthorizeException.class)
    public void testAuthorizeWithMismatchedBitstream() throws Exception {
        // Create request for one bitstream
        RequestItem request = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .withAcceptRequest(true)
                .withAccessToken("test-token")
                .build();

        // Create different bitstream
        InputStream is = new ByteArrayInputStream(new byte[0]);
        Bitstream otherBitstream = BitstreamBuilder
                .createBitstream(context, item, is)
                .withName("OtherBitstream")
                .build();

        // Try to authorize access to different bitstream
        requestItemService.authorizeAccessByAccessToken(context, request, otherBitstream, request.getAccess_token());
    }

    @Test(expected = AuthorizeException.class)
    public void testAuthorizeWithAllFilesDisabled() throws Exception {
        // Create request for specific bitstream
        RequestItem request = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .withAcceptRequest(true)
                .withAccessToken("test-token")
                .withAllFiles(false)
                .build();

        // Create different bitstream
        InputStream is = new ByteArrayInputStream(new byte[0]);
        Bitstream otherBitstream = BitstreamBuilder
                .createBitstream(context, item, is)
                .withName("OtherBitstream")
                .build();

        // Try to access different bitstream when allfiles=false
        requestItemService.authorizeAccessByAccessToken(context, request, otherBitstream, request.getAccess_token());
    }

    @Test
    public void testGrantRequestWithAccessPeriod() throws Exception {
        Instant decisionDate = Instant.now();
        Instant expectedExpiryDate = decisionDate.plus(7, ChronoUnit.DAYS);
        RequestItem request = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .build();

        request.setAccept_request(true);
        request.setDecision_date(decisionDate);
        request.setAccess_expiry(getExpiryAsInstant("+7DAYS", decisionDate)); // 7 day access
        requestItemService.update(context, request);

        RequestItem found = requestItemService.findByToken(context, request.getToken());
        assertTrue(found.isAccept_request());
        assertEquals(decisionDate, found.getDecision_date());
        assertEquals(expectedExpiryDate, found.getAccess_expiry());
    }

    @Test
    public void testDenyRequest() throws Exception {
        Instant decisionDate = Instant.now();
        RequestItem request = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .build();

        request.setAccept_request(false);
        request.setDecision_date(decisionDate);
        requestItemService.update(context, request);

        RequestItem found = requestItemService.findByToken(context, request.getToken());
        assertFalse(found.isAccept_request());
        assertNotNull(found.getDecision_date());
    }

    @Test
    public void testFindRequestsByItem() throws Exception {
        // Create multiple requests
        RequestItem request1 = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .build();

        RequestItem request2 = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .build();

        Iterator<RequestItem> requests = requestItemService.findByItem(context, item);
        int count = 0;
        while (requests.hasNext()) {
            count++;
            requests.next();
        }
        assertEquals(2, count);
    }

    @Test
    public void testModifyGrantedRequest() throws Exception {
        Instant decisionDate = Instant.now();
        Instant expectedExpiryDate = decisionDate.plus(10, ChronoUnit.DAYS);
        RequestItem request = RequestItemBuilder
                .createRequestItem(context, item, bitstream)
                .withAcceptRequest(true)
                .withDecisionDate(decisionDate)
                .withAccessExpiry(getExpiryAsInstant("+1DAY", decisionDate))
                .build();

        // Manually set new expiry date
        request.setAccess_expiry(getExpiryAsInstant("+10DAYS", decisionDate));
        request.setAllfiles(true);
        requestItemService.update(context, request);

        RequestItem found = requestItemService.findByToken(context, request.getToken());
        assertEquals(expectedExpiryDate, found.getAccess_expiry());
        assertTrue(found.isAccept_request());
        assertTrue(found.isAllfiles());
    }
    /**
     * Test that generated links include the correct base URL, where the UI URL has a subpath like /subdir
     */
    @Test
    public void testGetLinkTokenEmailWithSubPath() throws MalformedURLException, URISyntaxException {
        String currentDspaceUrl = configurationService.getProperty("dspace.ui.url");
        String newDspaceUrl = currentDspaceUrl + "/subdir";
        // Add a /subdir to the url for this test
        configurationService.setProperty("dspace.ui.url", newDspaceUrl);
        String expectedUrl = newDspaceUrl + "/request-a-copy/token";
        String generatedLink = requestItemService.getLinkTokenEmail("token");
        // The URLs should match
        assertEquals(expectedUrl, generatedLink);
        configurationService.reloadConfig();
    }

    /**
     * Test that generated links include the correct base URL, with NO subpath elements
     */
    @Test
    public void testGetLinkTokenEmailWithoutSubPath() throws MalformedURLException, URISyntaxException {
        String currentDspaceUrl = configurationService.getProperty("dspace.ui.url");
        String expectedUrl = currentDspaceUrl + "/request-a-copy/token";
        String generatedLink = requestItemService.getLinkTokenEmail("token");
        // The URLs should match
        assertEquals(expectedUrl, generatedLink);
        configurationService.reloadConfig();
    }

    private Instant getYesterdayAsInstant() {
        return Instant.now().minus(Duration.ofDays(1));
    }

    private Instant getExpiryAsInstant(String dateOrDelta, Instant decision) {
        try {
            return RequestItemServiceImpl.parseDateOrDelta(dateOrDelta, decision);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }


}
