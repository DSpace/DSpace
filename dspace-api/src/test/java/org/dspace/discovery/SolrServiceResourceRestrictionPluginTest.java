/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.RequestService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.model.Request;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link SolrServiceResourceRestrictionPlugin}
 * Focuses on the role gate and memoization logic for admin scope queries.
 *
 * @author Bram Luyten (bram at atmire dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrServiceResourceRestrictionPluginTest {

    @InjectMocks
    private SolrServiceResourceRestrictionPlugin plugin;

    @Mock
    private AuthorizeService authorizeService;

    @Mock
    private GroupService groupService;

    @Mock
    private Context context;

    @Mock
    private DiscoverQuery discoverQuery;

    @Mock
    private SolrQuery solrQuery;

    @Mock
    private EPerson currentUser;

    @Mock
    private Group anonymousGroup;

    @Mock
    private SearchService searchService;

    @Mock
    private RequestService requestService;

    @Mock
    private Request request;

    @Before
    public void setUp() throws Exception {
        // Setup common mocks
        when(groupService.findByName(any(Context.class), eq(Group.ANONYMOUS))).thenReturn(anonymousGroup);
        when(anonymousGroup.getID()).thenReturn(java.util.UUID.randomUUID());
        when(context.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getID()).thenReturn(java.util.UUID.randomUUID());

        Set<Group> emptyGroups = new HashSet<>();
        when(groupService.allMemberGroupsSet(any(Context.class), any(EPerson.class))).thenReturn(emptyGroups);
    }

    /**
     * Test that when user is NOT a C/C admin, createLocationQueryForAdministrableItems is NOT called
     */
    @Test
    public void testNonAdminUser_DoesNotCallAdminScopeQuery() throws Exception {
        // Arrange: user is not a site admin, not a C/C admin
        when(authorizeService.isAdmin(context)).thenReturn(false);
        when(authorizeService.isCommunityAdmin(context)).thenReturn(false);
        when(authorizeService.isCollectionAdmin(context)).thenReturn(false);

        try (MockedStatic<DSpaceServicesFactory> mockedFactory = Mockito.mockStatic(DSpaceServicesFactory.class)) {
            DSpaceServicesFactory factory = mock(DSpaceServicesFactory.class);
            mockedFactory.when(DSpaceServicesFactory::getInstance).thenReturn(factory);
            when(factory.getRequestService()).thenReturn(requestService);
            when(requestService.getCurrentRequest()).thenReturn(request);

            ServiceManager serviceManager = mock(ServiceManager.class);
            when(factory.getServiceManager()).thenReturn(serviceManager);
            when(serviceManager.getServiceByName(eq(SearchService.class.getName()), 
                                                 eq(SearchService.class))).thenReturn(searchService);

            // Act
            plugin.additionalSearchParameters(context, discoverQuery, solrQuery);

            // Assert: createLocationQueryForAdministrableItems should NOT be called
            verify(searchService, never()).createLocationQueryForAdministrableItems(any(Context.class));
        }
    }

    /**
     * Test that when user IS a community admin, createLocationQueryForAdministrableItems IS called
     */
    @Test
    public void testCommunityAdmin_CallsAdminScopeQuery() throws Exception {
        // Arrange: user is a community admin but not site admin
        when(authorizeService.isAdmin(context)).thenReturn(false);
        when(authorizeService.isCommunityAdmin(context)).thenReturn(true);
        when(authorizeService.isCollectionAdmin(context)).thenReturn(false);

        try (MockedStatic<DSpaceServicesFactory> mockedFactory = Mockito.mockStatic(DSpaceServicesFactory.class)) {
            DSpaceServicesFactory factory = mock(DSpaceServicesFactory.class);
            mockedFactory.when(DSpaceServicesFactory::getInstance).thenReturn(factory);
            when(factory.getRequestService()).thenReturn(requestService);
            when(requestService.getCurrentRequest()).thenReturn(request);
            when(request.getAttribute("dspace.discovery.adminScopeLocations")).thenReturn(null);

            ServiceManager serviceManager = mock(ServiceManager.class);
            when(factory.getServiceManager()).thenReturn(serviceManager);
            when(serviceManager.getServiceByName(eq(SearchService.class.getName()), 
                                                 eq(SearchService.class))).thenReturn(searchService);
            when(searchService.createLocationQueryForAdministrableItems(context))
                .thenReturn("location:(m123 OR l456)");

            // Act
            plugin.additionalSearchParameters(context, discoverQuery, solrQuery);

            // Assert: createLocationQueryForAdministrableItems should be called once
            verify(searchService, times(1)).createLocationQueryForAdministrableItems(context);
            verify(request, times(1)).setAttribute(eq("dspace.discovery.adminScopeLocations"), 
                                                   eq("location:(m123 OR l456)"));
        }
    }

    /**
     * Test that when user IS a collection admin, createLocationQueryForAdministrableItems IS called
     */
    @Test
    public void testCollectionAdmin_CallsAdminScopeQuery() throws Exception {
        // Arrange: user is a collection admin but not site admin
        when(authorizeService.isAdmin(context)).thenReturn(false);
        when(authorizeService.isCommunityAdmin(context)).thenReturn(false);
        when(authorizeService.isCollectionAdmin(context)).thenReturn(true);

        try (MockedStatic<DSpaceServicesFactory> mockedFactory = Mockito.mockStatic(DSpaceServicesFactory.class)) {
            DSpaceServicesFactory factory = mock(DSpaceServicesFactory.class);
            mockedFactory.when(DSpaceServicesFactory::getInstance).thenReturn(factory);
            when(factory.getRequestService()).thenReturn(requestService);
            when(requestService.getCurrentRequest()).thenReturn(request);
            when(request.getAttribute("dspace.discovery.adminScopeLocations")).thenReturn(null);

            ServiceManager serviceManager = mock(ServiceManager.class);
            when(factory.getServiceManager()).thenReturn(serviceManager);
            when(serviceManager.getServiceByName(eq(SearchService.class.getName()), 
                                                 eq(SearchService.class))).thenReturn(searchService);
            when(searchService.createLocationQueryForAdministrableItems(context))
                .thenReturn("location:(l789)");

            // Act
            plugin.additionalSearchParameters(context, discoverQuery, solrQuery);

            // Assert: createLocationQueryForAdministrableItems should be called once
            verify(searchService, times(1)).createLocationQueryForAdministrableItems(context);
            verify(request, times(1)).setAttribute(eq("dspace.discovery.adminScopeLocations"), 
                                                   eq("location:(l789)"));
        }
    }

    /**
     * Test memoization: when called twice in same request, DB query should only happen once
     */
    @Test
    public void testMemoization_CallsAdminScopeQueryOnlyOnce() throws Exception {
        // Arrange: user is a community admin
        when(authorizeService.isAdmin(context)).thenReturn(false);
        when(authorizeService.isCommunityAdmin(context)).thenReturn(true);
        when(authorizeService.isCollectionAdmin(context)).thenReturn(false);

        try (MockedStatic<DSpaceServicesFactory> mockedFactory = Mockito.mockStatic(DSpaceServicesFactory.class)) {
            DSpaceServicesFactory factory = mock(DSpaceServicesFactory.class);
            mockedFactory.when(DSpaceServicesFactory::getInstance).thenReturn(factory);
            when(factory.getRequestService()).thenReturn(requestService);
            when(requestService.getCurrentRequest()).thenReturn(request);

            ServiceManager serviceManager = mock(ServiceManager.class);
            when(factory.getServiceManager()).thenReturn(serviceManager);
            when(serviceManager.getServiceByName(eq(SearchService.class.getName()), 
                                                 eq(SearchService.class))).thenReturn(searchService);

            String cachedLocations = "location:(m123 OR l456)";
            
            // First call: cache miss
            when(request.getAttribute("dspace.discovery.adminScopeLocations")).thenReturn(null);
            when(searchService.createLocationQueryForAdministrableItems(context))
                .thenReturn(cachedLocations);

            // Act: First call
            plugin.additionalSearchParameters(context, discoverQuery, solrQuery);

            // Now simulate the cache hit for the second call
            when(request.getAttribute("dspace.discovery.adminScopeLocations")).thenReturn(cachedLocations);

            // Act: Second call
            plugin.additionalSearchParameters(context, discoverQuery, solrQuery);

            // Assert: createLocationQueryForAdministrableItems should be called only ONCE
            verify(searchService, times(1)).createLocationQueryForAdministrableItems(context);
            // setAttribute should also be called only once (during first call)
            verify(request, times(1)).setAttribute(eq("dspace.discovery.adminScopeLocations"), 
                                                   eq(cachedLocations));
        }
    }

    /**
     * Test that site admin does not trigger the admin scope query (they already have full access)
     */
    @Test
    public void testSiteAdmin_DoesNotCallAdminScopeQuery() throws Exception {
        // Arrange: user is a site admin
        when(authorizeService.isAdmin(context)).thenReturn(true);

        try (MockedStatic<DSpaceServicesFactory> mockedFactory = Mockito.mockStatic(DSpaceServicesFactory.class)) {
            DSpaceServicesFactory factory = mock(DSpaceServicesFactory.class);
            mockedFactory.when(DSpaceServicesFactory::getInstance).thenReturn(factory);
            when(factory.getRequestService()).thenReturn(requestService);

            ServiceManager serviceManager = mock(ServiceManager.class);
            when(factory.getServiceManager()).thenReturn(serviceManager);
            when(serviceManager.getServiceByName(eq(SearchService.class.getName()), 
                                                 eq(SearchService.class))).thenReturn(searchService);

            // Act
            plugin.additionalSearchParameters(context, discoverQuery, solrQuery);

            // Assert: For site admins, the entire resource query is skipped, so no admin scope query
            verify(searchService, never()).createLocationQueryForAdministrableItems(any(Context.class));
        }
    }

    /**
     * Test handling when there's no current request (e.g., CLI context)
     */
    @Test
    public void testNoCurrentRequest_StillCallsAdminScopeQuery() throws Exception {
        // Arrange: user is a community admin, but there's no HTTP request
        when(authorizeService.isAdmin(context)).thenReturn(false);
        when(authorizeService.isCommunityAdmin(context)).thenReturn(true);
        when(authorizeService.isCollectionAdmin(context)).thenReturn(false);

        try (MockedStatic<DSpaceServicesFactory> mockedFactory = Mockito.mockStatic(DSpaceServicesFactory.class)) {
            DSpaceServicesFactory factory = mock(DSpaceServicesFactory.class);
            mockedFactory.when(DSpaceServicesFactory::getInstance).thenReturn(factory);
            when(factory.getRequestService()).thenReturn(requestService);
            when(requestService.getCurrentRequest()).thenReturn(null); // No current request

            ServiceManager serviceManager = mock(ServiceManager.class);
            when(factory.getServiceManager()).thenReturn(serviceManager);
            when(serviceManager.getServiceByName(eq(SearchService.class.getName()), 
                                                 eq(SearchService.class))).thenReturn(searchService);
            when(searchService.createLocationQueryForAdministrableItems(context))
                .thenReturn("location:(m999)");

            // Act
            plugin.additionalSearchParameters(context, discoverQuery, solrQuery);

            // Assert: Should still call the method, but won't cache (no request to cache in)
            verify(searchService, times(1)).createLocationQueryForAdministrableItems(context);
        }
    }
}

