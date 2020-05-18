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
import static org.mockito.Mockito.doCallRealMethod;
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
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.export.factory.OpenURLTrackerLoggerServiceFactory;
import org.dspace.statistics.export.service.FailedOpenURLTrackerService;
import org.dspace.usage.UsageEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for the ExportUsageEventListener
 */
@RunWith(MockitoJUnitRunner.class)
public class ITExportUsageEventListener extends AbstractIntegrationTest {

    private static Logger log = Logger.getLogger(ITExportUsageEventListener.class);


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

    @Spy
    ExportUsageEventListener exportUsageEventListener;

    private Item item;
    private Item itemNotToBeProcessed;
    private Bitstream bitstream;
    private Bitstream bitstreamNotToBeProcessed;
    private EntityType entityType;
    private Community community;
    private Collection collection;

    private String encodedUrl;


    /**
     * Initializes the test by setting up all objects needed to create a test item
     */
    @Before()
    public void init() {
        super.init();

        configurationService.setProperty("stats.tracker.enabled", true);
        configurationService.setProperty("stats.tracker.type-field", "dc.type");
        configurationService.setProperty("stats.tracker.type-value", "Excluded type");


        context.turnOffAuthorisationSystem();
        try {
            exportUsageEventListener.configurationService = configurationService;

            entityType = entityTypeService.create(context, "Publication");
            community = communityService.create(null, context);
            collection = collectionService.create(context, community);
            item = installItemService.installItem(context, workspaceItemService.create(context, collection, false));
            itemService.addMetadata(context, item, "relationship", "type", null, null, "Publication");
            File f = new File(testProps.get("test.bitstream").toString());
            bitstream = itemService.createSingleBitstream(context, new FileInputStream(f), item);
            itemService.update(context, item);

            itemNotToBeProcessed = installItemService
                    .installItem(context, workspaceItemService.create(context, collection, false));
            itemService.addMetadata(context, itemNotToBeProcessed, "relationship", "type", null, null, "Publication");
            itemService.addMetadata(context, itemNotToBeProcessed, "dc", "type", null, null, "Excluded type");
            File itemNotToBeProcessedFile = new File(testProps.get("test.bitstream").toString());
            bitstreamNotToBeProcessed = itemService.createSingleBitstream(context,
                                                          new FileInputStream(itemNotToBeProcessedFile),
                                                          itemNotToBeProcessed);
            itemService.update(context, itemNotToBeProcessed);

            String dspaceUrl = configurationService.getProperty("dspace.ui.url");
            encodedUrl = URLEncoder.encode(dspaceUrl, CharEncoding.UTF_8);


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

        doCallRealMethod().when(exportUsageEventListener).receiveEvent(usageEvent);
        exportUsageEventListener.receiveEvent(usageEvent);


        List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);


        String regex = "https://irus.jisc.ac.uk/counter/test/\\?url_ver=Z39.88-2004&req_id=" +
                URLEncoder.encode(request.getRemoteAddr(), "UTF-8") + "&req_dat=&rft" +
                ".artnum=oai%3Alocalhost%3A" + URLEncoder.encode(item.getHandle(), "UTF-8") + "&rfr_dat=&rfr_id" +
                "=localhost&url_tim=" + ".*" + "?&svc_dat=" + encodedUrl + "%2Fhandle%2F" + URLEncoder
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

        doCallRealMethod().when(exportUsageEventListener).receiveEvent(usageEvent);
        exportUsageEventListener.receiveEvent(usageEvent);


        List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);

        String regex = "https://irus.jisc.ac.uk/counter/test/\\?url_ver=Z39.88-2004&req_id=" +
                URLEncoder.encode(request.getRemoteAddr(), "UTF-8") + "&req_dat=&rft" +
                ".artnum=oai%3Alocalhost%3A" + URLEncoder.encode(item.getHandle(), "UTF-8") + "&rfr_dat=&rfr_id" +
                "=localhost&url_tim=" + ".*" + "?&svc_dat=" + encodedUrl + "%2Fhandle%2F" + URLEncoder
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

        UsageEvent usageEvent = mock(UsageEvent.class);
        when(usageEvent.getObject()).thenReturn(itemNotToBeProcessed);
        when(usageEvent.getRequest()).thenReturn(request);
        when(usageEvent.getContext()).thenReturn(new Context());

        itemService.clearMetadata(context, item, "relationship", "type", null, Item.ANY);
        itemService.addMetadata(context, item, "relationship", "type", null, null, "OrgUnit");
        itemService.update(context, item);

        context.restoreAuthSystemState();

        doCallRealMethod().when(exportUsageEventListener).receiveEvent(usageEvent);
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

        doCallRealMethod().when(exportUsageEventListener).receiveEvent(usageEvent);
        exportUsageEventListener.receiveEvent(usageEvent);

        String regex = "https://irus.jisc.ac.uk/counter/test/\\?url_ver=Z39.88-2004&req_id=" +
                URLEncoder.encode(request.getRemoteAddr(), "UTF-8") + "&req_dat=&rft" +
                ".artnum=oai%3Alocalhost%3A" + URLEncoder.encode(item.getHandle(), "UTF-8") + "&rfr_dat=&rfr_id" +
                "=localhost&url_tim=" + ".*" + "?&svc_dat=" + encodedUrl + "%2Fbitstream%2Fhandle%2F" +
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

        doCallRealMethod().when(exportUsageEventListener).receiveEvent(usageEvent);
        exportUsageEventListener.receiveEvent(usageEvent);

        List<OpenURLTracker> all = failedOpenURLTrackerService.findAll(context);

        String regex = "https://irus.jisc.ac.uk/counter/test/\\?url_ver=Z39.88-2004&req_id=" +
                URLEncoder.encode(request.getRemoteAddr(), "UTF-8") + "&req_dat=&rft" +
                ".artnum=oai%3Alocalhost%3A" + URLEncoder.encode(item.getHandle(), "UTF-8") + "&rfr_dat=&rfr_id" +
                "=localhost&url_tim=" + ".*" + "?&svc_dat=" + encodedUrl + "%2Fbitstream%2Fhandle%2F" +
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
        when(usageEvent.getObject()).thenReturn(bitstreamNotToBeProcessed);
        when(usageEvent.getRequest()).thenReturn(request);
        when(usageEvent.getContext()).thenReturn(new Context());

        itemService.clearMetadata(context, item, "relationship", "type", null, Item.ANY);
        itemService.addMetadata(context, item, "relationship", "type", null, null, "OrgUnit");
        itemService.update(context, item);

        context.restoreAuthSystemState();

        doCallRealMethod().when(exportUsageEventListener).receiveEvent(usageEvent);
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

        UsageEvent usageEvent = mock(UsageEvent.class);
        when(usageEvent.getObject()).thenReturn(community);
        when(usageEvent.getContext()).thenReturn(new Context());

        doCallRealMethod().when(exportUsageEventListener).receiveEvent(usageEvent);
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
