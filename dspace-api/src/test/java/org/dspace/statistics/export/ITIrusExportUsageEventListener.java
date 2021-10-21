/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.CharEncoding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.export.factory.OpenURLTrackerLoggerServiceFactory;
import org.dspace.statistics.export.service.FailedOpenURLTrackerService;
import org.dspace.usage.UsageEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the IrusExportUsageEventListener
 */
//@RunWith(MockitoJUnitRunner.class)
public class ITIrusExportUsageEventListener extends AbstractIntegrationTestWithDatabase {

    private static final Logger log = LogManager.getLogger();

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected EntityTypeService entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
    protected FailedOpenURLTrackerService failedOpenURLTrackerService =
            OpenURLTrackerLoggerServiceFactory.getInstance().getOpenUrlTrackerLoggerService();

    protected ArrayList testProcessedUrls = DSpaceServicesFactory.getInstance().getServiceManager()
                                                                 .getServiceByName("testProcessedUrls",
                                                                                   ArrayList.class);

    private final IrusExportUsageEventListener exportUsageEventListener =
            DSpaceServicesFactory.getInstance()
                                 .getServiceManager()
                                 .getServicesByType(IrusExportUsageEventListener.class)
                                 .get(0);

    private Item item;
    private Item itemNotToBeProcessed;
    private Bitstream bitstream;
    private Bitstream bitstreamNotToBeProcessed;
    private EntityType entityType;
    private Community community;
    private Collection collection;

    private String encodedUrl;
    private String encodedUIUrl;


    /**
     * Initializes the test by setting up all objects needed to create a test item.
     * @throws java.lang.Exception passed through.
     */
    @Before()
    @Override
    public void setUp() throws Exception {
        super.setUp();

        configurationService.setProperty("irus.statistics.tracker.enabled", true);
        configurationService.setProperty("irus.statistics.tracker.type-field", "dc.type");
        configurationService.setProperty("irus.statistics.tracker.type-value", "Excluded type");


        context.turnOffAuthorisationSystem();
        try {

            entityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
            community = CommunityBuilder.createCommunity(context).build();
            collection = CollectionBuilder.createCollection(context, community)
                                          .withEntityType(entityType.getLabel())
                                          .build();
            item = ItemBuilder.createItem(context, collection)
                              .build();

            File f = new File(testProps.get("test.bitstream").toString());
            bitstream = BitstreamBuilder.createBitstream(context, item, new FileInputStream(f)).build();

            itemNotToBeProcessed = ItemBuilder.createItem(context, collection)
                                              .withType("Excluded type")
                                              .build();
            File itemNotToBeProcessedFile = new File(testProps.get("test.bitstream").toString());
            bitstreamNotToBeProcessed = BitstreamBuilder
                    .createBitstream(context, itemNotToBeProcessed, new FileInputStream(itemNotToBeProcessedFile))
                    .build();

            String dspaceUrl = configurationService.getProperty("dspace.server.url");
            encodedUrl = URLEncoder.encode(dspaceUrl, CharEncoding.UTF_8);
            String dspaceUIUrl = configurationService.getProperty("dspace.ui.url");
            encodedUIUrl = URLEncoder.encode(dspaceUIUrl, CharEncoding.UTF_8);


        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /**
     * Clean up the created objects.
     * Empty the testProcessedUrls used to store succeeded URLs.
     * Empty the database table where the failed URLs are logged.
     */
    @After
    @Override
    public void destroy() throws Exception {
        try {
            context.turnOffAuthorisationSystem();

            List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);
            for (OpenURLTracker tracker : all) {
                failedOpenURLTrackerService.remove(context, tracker);
            }

            // Clear the list of processedUrls
            testProcessedUrls.clear();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                context.complete();
            } catch (SQLException e) {
                log.error(e);
            }
        }
        super.destroy();
    }

    /**
     * Test whether the usage event of an item meeting all conditions is processed and succeeds
     */
    @Test
    public void testReceiveEventOnItemThatShouldBeProcessed() throws UnsupportedEncodingException, SQLException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("client-ip");
        when(request.getHeader(anyString())).thenReturn(null);

        UsageEvent usageEvent = mock(UsageEvent.class);
        when(usageEvent.getObject()).thenReturn(item);
        when(usageEvent.getRequest()).thenReturn(request);
        when(usageEvent.getContext()).thenReturn(new Context());

        exportUsageEventListener.receiveEvent(usageEvent);


        List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);


        String regex = "https://irus.jisc.ac.uk/counter/test/\\?url_ver=Z39.88-2004&req_id=" +
                URLEncoder.encode(request.getRemoteAddr(), "UTF-8") + "&req_dat=&rft" +
                ".artnum=oai%3Alocalhost%3A" + URLEncoder.encode(item.getHandle(), "UTF-8") + "&rfr_dat=&rfr_id" +
                "=localhost&url_tim=" + ".*" + "?&svc_dat=" + encodedUIUrl + "%2Fhandle%2F" + URLEncoder
                .encode(item.getHandle(), "UTF-8") + "&rft_dat=Investigation";

        boolean isMatch = matchesString(String.valueOf(testProcessedUrls.get(0)), regex);

        assertEquals(1, testProcessedUrls.size());
        assertTrue(isMatch);
        assertEquals(0, all.size());


    }

    /**
     * Test whether the usage event of an item meeting all conditions is processed but fails
     */
    @Test
    public void testReceiveEventOnItemThatShouldBeProcessedFailed() throws SQLException, UnsupportedEncodingException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("client-ip-fail");
        when(request.getHeader(anyString())).thenReturn(null);

        UsageEvent usageEvent = mock(UsageEvent.class);
        when(usageEvent.getObject()).thenReturn(item);
        when(usageEvent.getRequest()).thenReturn(request);
        when(usageEvent.getContext()).thenReturn(new Context());

        exportUsageEventListener.receiveEvent(usageEvent);


        List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);

        String regex = "https://irus.jisc.ac.uk/counter/test/\\?url_ver=Z39.88-2004&req_id=" +
                URLEncoder.encode(request.getRemoteAddr(), "UTF-8") + "&req_dat=&rft" +
                ".artnum=oai%3Alocalhost%3A" + URLEncoder.encode(item.getHandle(), "UTF-8") + "&rfr_dat=&rfr_id" +
                "=localhost&url_tim=" + ".*" + "?&svc_dat=" + encodedUIUrl + "%2Fhandle%2F" + URLEncoder
                .encode(item.getHandle(), "UTF-8") + "&rft_dat=Investigation";

        boolean isMatch = matchesString(all.get(0).getUrl(), regex);

        assertEquals(0, testProcessedUrls.size());

        assertEquals(1, all.size());
        assertTrue(isMatch);
    }

    /**
     * Test whether the usage event of an item that does not meet all conditions is not processed
     */
    @Test
    public void testReceiveEventOnItemThatShouldNotBeProcessed() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();

        HttpServletRequest request = mock(HttpServletRequest.class);

        UsageEvent usageEvent = mock(UsageEvent.class);
        when(usageEvent.getObject()).thenReturn(itemNotToBeProcessed);
        when(usageEvent.getRequest()).thenReturn(request);
        when(usageEvent.getContext()).thenReturn(new Context());

        itemService.clearMetadata(context, item, "dspace", "entity", "type", Item.ANY);
        itemService.addMetadata(context, item, "dspace", "entity", "type", null, "OrgUnit");
        itemService.update(context, item);

        context.restoreAuthSystemState();

        // doCallRealMethod().when(IrusExportUsageEventListener).receiveEvent(usageEvent);
        exportUsageEventListener.receiveEvent(usageEvent);

        List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);


        assertEquals(0, testProcessedUrls.size());
        assertEquals(0, all.size());
    }

    /**
     * Test whether the usage event of a bitstream meeting all conditions is processed and succeeds
     */
    @Test
    public void testReceiveEventOnBitstreamThatShouldBeProcessed() throws SQLException, UnsupportedEncodingException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("client-ip");
        when(request.getHeader(anyString())).thenReturn(null);

        UsageEvent usageEvent = mock(UsageEvent.class);
        when(usageEvent.getObject()).thenReturn(bitstream);
        when(usageEvent.getRequest()).thenReturn(request);
        when(usageEvent.getContext()).thenReturn(new Context());

        exportUsageEventListener.receiveEvent(usageEvent);

        String regex = "https://irus.jisc.ac.uk/counter/test/\\?url_ver=Z39.88-2004&req_id=" +
                URLEncoder.encode(request.getRemoteAddr(), "UTF-8") + "&req_dat=&rft" +
                ".artnum=oai%3Alocalhost%3A" + URLEncoder.encode(item.getHandle(), "UTF-8") + "&rfr_dat=&rfr_id" +
                "=localhost&url_tim=" + ".*" + "?&svc_dat=" + encodedUrl + "%2Fapi%2Fcore%2Fbitstreams" +
                "%2F" + bitstream.getID() + "%2Fcontent" + "&rft_dat=Request";

        boolean isMatch = matchesString(String.valueOf(testProcessedUrls.get(0)), regex);

        assertEquals(1, testProcessedUrls.size());
        assertTrue(isMatch);

        List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);
        assertEquals(0, all.size());
    }

    /**
     * Test whether the usage event of a bitstream meeting all conditions is processed but fails
     */
    @Test
    public void testReceiveEventOnBitstreamThatShouldBeProcessedFail() throws UnsupportedEncodingException,
            SQLException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("client-ip-fail");
        when(request.getHeader(anyString())).thenReturn(null);

        UsageEvent usageEvent = mock(UsageEvent.class);
        when(usageEvent.getObject()).thenReturn(bitstream);
        when(usageEvent.getRequest()).thenReturn(request);
        when(usageEvent.getContext()).thenReturn(new Context());

        exportUsageEventListener.receiveEvent(usageEvent);

        List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);

        String regex = "https://irus.jisc.ac.uk/counter/test/\\?url_ver=Z39.88-2004&req_id=" +
                URLEncoder.encode(request.getRemoteAddr(), "UTF-8") + "&req_dat=&rft" +
                ".artnum=oai%3Alocalhost%3A" + URLEncoder.encode(item.getHandle(), "UTF-8") + "&rfr_dat=&rfr_id" +
                "=localhost&url_tim=" + ".*" + "?&svc_dat=" + encodedUrl + "%2Fapi%2Fcore%2Fbitstreams" +
                "%2F" + bitstream.getID() + "%2Fcontent" + "&rft_dat=Request";


        boolean isMatch = matchesString(all.get(0).getUrl(), regex);

        assertEquals(1, all.size());
        assertEquals(true, isMatch);
        assertEquals(0, testProcessedUrls.size());

    }

    /**
     * Test whether the usage event of a bitstream that does not meet all conditions is not processed
     */
    @Test
    public void testReceiveEventOnBitstreamThatShouldNotBeProcessed() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("client-ip-fail");
        when(request.getHeader(anyString())).thenReturn(null);

        UsageEvent usageEvent = mock(UsageEvent.class);
        when(usageEvent.getObject()).thenReturn(bitstreamNotToBeProcessed);
        when(usageEvent.getRequest()).thenReturn(request);
        when(usageEvent.getContext()).thenReturn(new Context());

        itemService.clearMetadata(context, item, "dspace", "entity", "type", Item.ANY);
        itemService.addMetadata(context, item, "dspace", "entity", "type", null, "OrgUnit");
        itemService.update(context, item);

        context.restoreAuthSystemState();

        exportUsageEventListener.receiveEvent(usageEvent);

        List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);


        assertEquals(0, all.size());
        assertEquals(0, testProcessedUrls.size());

    }

    /**
     * Test that an object that is not an Item or Bitstream is not processed
     * @throws java.sql.SQLException passed through.
     */
    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void testReceiveEventOnNonRelevantObject() throws SQLException {

        mock(HttpServletRequest.class);

        UsageEvent usageEvent = mock(UsageEvent.class);
        when(usageEvent.getObject()).thenReturn(community);
        when(usageEvent.getContext()).thenReturn(new Context());

        exportUsageEventListener.receiveEvent(usageEvent);

        List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);


        assertEquals(0, all.size());
        assertEquals(0, testProcessedUrls.size());
    }

    /**
     * Method to test if a string matches a regex
     *
     * @param string
     * @param regex
     * @return whether the regex matches the string
     */
    private boolean matchesString(String string, String regex) {

        Pattern p = Pattern.compile(regex);

        return p.matcher(string).matches();
    }
}
