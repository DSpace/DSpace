/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
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

import org.apache.log4j.Logger;
import org.dspace.AbstractIntegrationTest;
import org.dspace.authorize.AuthorizeException;
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
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.export.factory.OpenURLTrackerLoggerServiceFactory;
import org.dspace.statistics.export.service.FailedOpenURLTrackerService;
import org.dspace.usage.UsageEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Test class for the ExportUsageEventListener
 */
public class ITExportUsageEventListener extends AbstractIntegrationTest {

    private static Logger log = Logger.getLogger(ITExportUsageEventListener.class);


    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
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

    @Mock
    ExportUsageEventListener exportUsageEventListener = mock(ExportUsageEventListener.class, CALLS_REAL_METHODS);

    private Item item;
    private Bitstream bitstream;
    private EntityType entityType;
    private Community community;
    private Collection collection;


    /**
     * Initializes the test by setting up all objects needed to create a test item
     */
    @Before()
    public void init() {
        super.init();
        context.turnOffAuthorisationSystem();
        try {
            entityType = entityTypeService.create(context, "Publication");
            community = communityService.create(null, context);
            collection = collectionService.create(context, community);
            item = installItemService.installItem(context, workspaceItemService.create(context, collection, false));
            itemService.addMetadata(context, item, "relationship", "type", null, null, "Publication");
            File f = new File(testProps.get("test.bitstream").toString());
            bitstream = itemService.createSingleBitstream(context, new FileInputStream(f), item);
            itemService.update(context, item);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /**
     * Clean up the created objects
     * Empty the testProcessedUrls used to store succeeded urls
     * Empty the database table where the failed urls are logged
     */
    @After
    public void destroy() {
        try {
            context.turnOffAuthorisationSystem();

            itemService.delete(context, item);
            collectionService.delete(context, collection);
            communityService.delete(context, community);
            entityTypeService.delete(context, entityType);


            List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);
            for (OpenURLTracker tracker : all) {
                failedOpenURLTrackerService.remove(context, tracker);
            }

            entityType = null;
            community = null;
            collection = null;
            item = null;

            // Clear the list of processedUrls
            testProcessedUrls.clear();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            context.restoreAuthSystemState();
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
                "=localhost&url_tim=" + ".*" + "?&svc_dat=http%3A%2F%2Flocalhost%3A3000%2Fhandle%2F" + URLEncoder
                .encode(item.getHandle(), "UTF-8") + "&rft_dat=Investigation";

        boolean isMatch = matchesString(String.valueOf(testProcessedUrls.get(0)), regex);

        assertThat(testProcessedUrls.size(), is(1));
        assertThat(isMatch, is(true));
        assertThat(all.size(), is(0));


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
                "=localhost&url_tim=" + ".*" + "?&svc_dat=http%3A%2F%2Flocalhost%3A3000%2Fhandle%2F" + URLEncoder
                .encode(item.getHandle(), "UTF-8") + "&rft_dat=Investigation";

        boolean isMatch = matchesString(all.get(0).getUrl(), regex);

        assertThat(testProcessedUrls.size(), is(0));

        assertThat(all.size(), is(1));
        assertThat(isMatch, is(true));
    }

    /**
     * Test whether the usage event of an item that does not meet all conditions is not processed
     */
    @Test
    public void testReceiveEventOnItemThatShouldNotBeProcessed() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("client-ip-fail");
        when(request.getHeader(anyString())).thenReturn(null);

        UsageEvent usageEvent = mock(UsageEvent.class);
        when(usageEvent.getObject()).thenReturn(item);
        when(usageEvent.getRequest()).thenReturn(request);
        when(usageEvent.getContext()).thenReturn(new Context());

        itemService.clearMetadata(context, item, "relationship", "type", null, Item.ANY);
        itemService.addMetadata(context, item, "relationship", "type", null, null, "OrgUnit");
        itemService.update(context, item);

        context.restoreAuthSystemState();

        exportUsageEventListener.receiveEvent(usageEvent);

        List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);


        assertThat(testProcessedUrls.size(), is(0));
        assertThat(all.size(), is(0));
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
                "=localhost&url_tim=" + ".*" + "?&svc_dat=http%3A%2F%2Flocalhost%3A3000%2Fbitstream%2Fhandle%2F" +
                URLEncoder.encode(item.getHandle(), "UTF-8") + "%2F%3Fsequence%3D\\d+" + "&rft_dat=Request";

        boolean isMatch = matchesString(String.valueOf(testProcessedUrls.get(0)), regex);

        assertThat(testProcessedUrls.size(), is(1));
        assertThat(isMatch, is(true));

        List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);
        assertThat(all.size(), is(0));
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
                "=localhost&url_tim=" + ".*" + "?&svc_dat=http%3A%2F%2Flocalhost%3A3000%2Fbitstream%2Fhandle%2F" +
                URLEncoder.encode(item.getHandle(), "UTF-8") + "%2F%3Fsequence%3D\\d+" + "&rft_dat=Request";


        boolean isMatch = matchesString(all.get(0).getUrl(), regex);

        assertThat(all.size(), is(1));
        assertThat(isMatch, is(true));
        assertThat(testProcessedUrls.size(), is(0));

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
        when(usageEvent.getObject()).thenReturn(bitstream);
        when(usageEvent.getRequest()).thenReturn(request);
        when(usageEvent.getContext()).thenReturn(new Context());

        itemService.clearMetadata(context, item, "relationship", "type", null, Item.ANY);
        itemService.addMetadata(context, item, "relationship", "type", null, null, "OrgUnit");
        itemService.update(context, item);

        context.restoreAuthSystemState();

        exportUsageEventListener.receiveEvent(usageEvent);

        List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);


        assertThat(all.size(), is(0));
        assertThat(testProcessedUrls.size(), is(0));

    }

    /**
     * Test that an object that is not an Item or Bitstream is not processed
     */
    @Test
    public void testReceiveEventOnNonRelevantObject() throws SQLException {

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("client-ip-fail");
        when(request.getHeader(anyString())).thenReturn(null);

        UsageEvent usageEvent = mock(UsageEvent.class);
        when(usageEvent.getObject()).thenReturn(community);
        when(usageEvent.getRequest()).thenReturn(request);
        when(usageEvent.getContext()).thenReturn(new Context());

        exportUsageEventListener.receiveEvent(usageEvent);

        List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);


        assertThat(all.size(), is(0));
        assertThat(testProcessedUrls.size(), is(0));

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

        if (p.matcher(string).matches()) {
            return true;
        }
        return false;
    }


}
