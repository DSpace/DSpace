/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.eperson.EPerson;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Test class for OREIngestionCrosswalk
 *
 * @author Kim Shepherd
 */
public class OREIngestionCrosswalkTest extends AbstractIntegrationTestWithDatabase {

    private Bundle bundle;
    private OREIngestionCrosswalk crosswalk;
    private Item item;
    private ItemService itemService;
    private EPerson prevUser;

    private static final Namespace ATOM_NS = Namespace.getNamespace("atom", "http://www.w3.org/2005/Atom");
    private static final Namespace ORE_NS = Namespace.getNamespace("ore", "http://www.openarchives.org/ore/terms/");

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        crosswalk = new OREIngestionCrosswalk();
        itemService = ContentServiceFactory.getInstance().getItemService();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).build();
        item = ItemBuilder.createItem(context, collection).withTitle("Test ORE Object").build();
        bundle = BundleBuilder.createBundle(context, item).withName("ORIGINAL").build();
        context.restoreAuthSystemState();
        prevUser = context.getCurrentUser();
        context.setCurrentUser(admin);
    }

    @After
    public void tearDown() throws Exception {
        context.setCurrentUser(prevUser);
    }

    @Test
    public void testIngestNullRoot() throws Exception {
        // should just return without exception
        crosswalk.ingest(context, item, (Element) null, false);
    }

    @Test
    public void testIngestEmptyResources() throws Exception {
        Element entry = createOREEntryWithoutResources();
        crosswalk.ingest(context, item, entry, false);

        List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
        assertTrue("Should have no bundles or empty bundle",
                bundles.isEmpty() || bundles.getFirst().getBitstreams().isEmpty());
    }

    @Test(expected = CrosswalkException.class)
    public void testIngestInvalidURISyntaxThrowsException() throws Exception {
        Element entry = createOREEntryWithInvalidURI();
        crosswalk.ingest(context, item, entry, false);
    }

    @Test
    public void testHostValidation() {
        // localhost = forbidden
        boolean result = crosswalk.validResourceUri("http://localhost:8080/resource");
        assertFalse("localhost should be forbidden", result);

        // 127.0.0.1 = forbidden
        result = crosswalk.validResourceUri("http://127.0.0.1:8080/resource");
        assertFalse("127.0.0.1 should be forbidden", result);

        // allowed.example.com = not forbidden
        result = crosswalk.validResourceUri("http://allowed.example.com/resource");
        assertTrue("External host should be allowed", result);
    }

    @Test
    public void testSchemeValidation() throws Exception {
        boolean result = crosswalk.validResourceUri("file:///etc/passwd");
        assertFalse("file:// scheme should be forbidden", result);

        result = crosswalk.validResourceUri("ftp://example.com/resource");
        assertFalse("ftp:// scheme should be forbidden", result);

        result = crosswalk.validResourceUri("https://example.com/resource");
        assertTrue("https:// scheme should be allowed", result);
    }

    @Test
    public void testSuccessfulBitstreamIngest() throws Exception {
        // fully mocked response
        try (MockedStatic<DSpaceHttpClientFactory> mockedFactory =
                     mockHttpClient(200, "Test bitstream content")) {

            Element entry = createValidOREEntry();
            crosswalk.ingest(context, item, entry, false);

            List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
            assertFalse("Should have created bundle", bundles.isEmpty());
            assertFalse("Bundle should have bitstream", bundles.getFirst().getBitstreams().isEmpty());
        }
    }

    @Test
    public void testIngestStreamAsBitstream() throws Exception {
        String testContent = "Test bitstream content";
        InputStream inputStream = new ByteArrayInputStream(testContent.getBytes());

        Element resource = new Element("link", ATOM_NS);
        resource.setAttribute("href", "http://example.com/test.pdf");
        resource.setAttribute("title", "Test Document");
        resource.setAttribute("type", "application/pdf");

        crosswalk.ingestStreamAsBitstream(context, inputStream, bundle, resource, "test-entry");

        assertFalse("Bundle should have bitstream", bundle.getBitstreams().isEmpty());
        assertEquals("Bitstream should have correct name", "Test Document",
                bundle.getBitstreams().getFirst().getName());
    }

    @Test(expected = IOException.class)
    public void testIngestStreamAsBitstreamNullStreamThrowsException() throws Exception {
        Element resource = new Element("link", ATOM_NS);
        resource.setAttribute("href", "http://example.com/test.pdf");

        crosswalk.ingestStreamAsBitstream(context, null, bundle, resource, "test-entry");
    }

    private Element createValidOREEntry() {
        Element entry = new Element("entry", ATOM_NS);
        Element altLink = new Element("link", ATOM_NS);
        altLink.setAttribute("rel", "alternate");
        altLink.setAttribute("href", "http://example.com/entry/123");
        entry.addContent(altLink);
        Element resLink = new Element("link", ATOM_NS);
        resLink.setAttribute("rel", ORE_NS.getURI() + "aggregates");
        resLink.setAttribute("href", "http://example.com/resource.pdf");
        resLink.setAttribute("title", "Test Resource");
        resLink.setAttribute("type", "application/pdf");
        entry.addContent(resLink);
        return entry;
    }

    private Element createOREEntryWithoutResources() {
        Element entry = new Element("entry", ATOM_NS);
        Element altLink = new Element("link", ATOM_NS);
        altLink.setAttribute("rel", "alternate");
        altLink.setAttribute("href", "http://example.com/entry/123");
        entry.addContent(altLink);
        return entry;
    }


    private Element createOREEntryWithInvalidURI() {
        Element entry = new Element("entry", ATOM_NS);
        Element altLink = new Element("link", ATOM_NS);
        altLink.setAttribute("rel", "alternate");
        altLink.setAttribute("href", "http://example.com/entry/123");
        entry.addContent(altLink);
        Element resLink = new Element("link", ATOM_NS);
        resLink.setAttribute("rel", ORE_NS.getURI() + "aggregates");
        resLink.setAttribute("href", "hello, i am an invalid URI");
        entry.addContent(resLink);
        return entry;
    }

    private MockedStatic<DSpaceHttpClientFactory> mockHttpClient(int statusCode, String content) throws IOException {
        MockedStatic<DSpaceHttpClientFactory> mockedFactory = Mockito.mockStatic(DSpaceHttpClientFactory.class);

        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        DSpaceHttpClientFactory mockFactoryInstance = mock(DSpaceHttpClientFactory.class);
        CloseableHttpResponse mockResponse = mockHttpResponse(statusCode, content);

        when(mockFactoryInstance.buildWithRequestConfig(any())).thenReturn(mockClient);
        when(mockClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        mockedFactory.when(DSpaceHttpClientFactory::getInstance).thenReturn(mockFactoryInstance);

        return mockedFactory;
    }

    private CloseableHttpResponse mockHttpResponse(int statusCode, String content) throws IOException {
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        HttpEntity mockEntity = mock(HttpEntity.class);
        StatusLine mockStatus = mock(StatusLine.class);

        when(mockStatus.getStatusCode()).thenReturn(statusCode);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(content.getBytes()));
        when(mockResponse.getStatusLine()).thenReturn(mockStatus);
        when(mockResponse.getEntity()).thenReturn(mockEntity);

        return mockResponse;
    }

}