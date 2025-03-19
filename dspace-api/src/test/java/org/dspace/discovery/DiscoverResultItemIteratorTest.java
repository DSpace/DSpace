/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableClaimedTask;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.IndexablePoolTask;
import org.dspace.discovery.indexobject.IndexableWorkflowItem;
import org.dspace.discovery.indexobject.IndexableWorkspaceItem;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class DiscoverResultItemIteratorTest {

    private Context mockContext;
    private DiscoverQuery mockDiscoverQuery;
    private SearchService mockSearchService;
    private DiscoverResult mockDiscoverResult;
    private IndexableItem mockIndexableItem;
    private IndexableWorkflowItem mockIndexableWorkflowItem;
    private IndexableWorkspaceItem mockIndexableWorkspaceItem;
    private IndexablePoolTask mockIndexablePoolTask;
    private IndexableClaimedTask mockIndexableClaimedTask;

    private Item mockItem;
    private XmlWorkflowItem mockWorkflowItem;
    private WorkspaceItem mockWorkspaceItem;
    private PoolTask mockPoolTask;
    private ClaimedTask mockClaimedTask;

    @Before
    public void setUp() throws Exception {
        mockContext = mock(Context.class);
        mockDiscoverQuery = mock(DiscoverQuery.class);
        mockSearchService = mock(SearchService.class);

        mockIndexableItem = mock(IndexableItem.class);
        mockIndexableWorkflowItem = mock(IndexableWorkflowItem.class);
        mockIndexableWorkspaceItem = mock(IndexableWorkspaceItem.class);
        mockIndexablePoolTask = mock(IndexablePoolTask.class);
        mockIndexableClaimedTask = mock(IndexableClaimedTask.class);

        mockItem = mock(Item.class);

        mockWorkflowItem = mock(XmlWorkflowItem.class);
        when(mockWorkflowItem.getItem()).thenReturn(mockItem);

        mockWorkspaceItem = mock(WorkspaceItem.class);
        when(mockWorkspaceItem.getItem()).thenReturn(mockItem);

        mockPoolTask = mock(PoolTask.class);
        when(mockPoolTask.getWorkflowItem()).thenReturn(mockWorkflowItem);
        mockClaimedTask = mock(ClaimedTask.class);
        when(mockClaimedTask.getWorkflowItem()).thenReturn(mockWorkflowItem);


        when(mockIndexableItem.getIndexedObject()).thenReturn(mockItem);
        when(mockIndexableItem.getType()).thenReturn(IndexableItem.TYPE);

        when(mockIndexableWorkflowItem.getIndexedObject()).thenReturn(mockWorkflowItem);
        when(mockIndexableWorkflowItem.getType()).thenReturn(IndexableWorkflowItem.TYPE);

        when(mockIndexableWorkspaceItem.getIndexedObject()).thenReturn(mockWorkspaceItem);
        when(mockIndexableWorkspaceItem.getType()).thenReturn(IndexableWorkspaceItem.TYPE);

        when(mockIndexablePoolTask.getIndexedObject()).thenReturn(mockPoolTask);
        when(mockIndexablePoolTask.getType()).thenReturn(IndexablePoolTask.TYPE);

        when(mockIndexableClaimedTask.getIndexedObject()).thenReturn(mockClaimedTask);
        when(mockIndexableClaimedTask.getType()).thenReturn(IndexableClaimedTask.TYPE);

    }

    @Test
    public void testNextWithIndexableItem() throws SearchServiceException {
        mockDiscoverResult = mock(DiscoverResult.class);
        when(mockDiscoverResult.getIndexableObjects())
            .thenReturn(Collections.singletonList(mockIndexableItem));
        when(mockDiscoverResult.getTotalSearchResults()).thenReturn(2L);

        when(mockSearchService.search(eq(mockContext), any(DiscoverQuery.class))).thenReturn(mockDiscoverResult);

        try (MockedStatic<SearchUtils> mockedStatic = mockStatic(SearchUtils.class)) {
            mockedStatic.when(SearchUtils::getSearchService).thenReturn(mockSearchService);
            DiscoverResultItemIterator iterator = new DiscoverResultItemIterator(mockContext, mockDiscoverQuery);
            assertTrue(iterator.hasNext());
            assertEquals(mockItem, iterator.next());
        }
    }

    @Test
    public void testNextWithIndexableWorkflowItem() throws SearchServiceException {
        mockDiscoverResult = mock(DiscoverResult.class);
        when(mockDiscoverResult.getIndexableObjects())
            .thenReturn(Collections.singletonList(mockIndexableWorkflowItem));
        when(mockDiscoverResult.getTotalSearchResults()).thenReturn(2L);
        when(mockSearchService.search(eq(mockContext), any(DiscoverQuery.class))).thenReturn(mockDiscoverResult);

        try (MockedStatic<SearchUtils> mockedStatic = mockStatic(SearchUtils.class)) {
            mockedStatic.when(SearchUtils::getSearchService).thenReturn(mockSearchService);
            DiscoverResultItemIterator iterator = new DiscoverResultItemIterator(mockContext, mockDiscoverQuery);
            iterator.next();
            assertTrue(iterator.hasNext());
            assertEquals(mockItem, iterator.next());
        }
    }

    @Test
    public void testNextWithIndexableWorkspaceItem() throws SearchServiceException {
        mockDiscoverResult = mock(DiscoverResult.class);
        when(mockDiscoverResult.getIndexableObjects())
            .thenReturn(Collections.singletonList(mockIndexableWorkspaceItem));
        when(mockDiscoverResult.getTotalSearchResults()).thenReturn(2L);
        when(mockSearchService.search(eq(mockContext), any(DiscoverQuery.class))).thenReturn(mockDiscoverResult);


        try (MockedStatic<SearchUtils> mockedStatic = mockStatic(SearchUtils.class)) {
            mockedStatic.when(SearchUtils::getSearchService).thenReturn(mockSearchService);
            DiscoverResultItemIterator iterator = new DiscoverResultItemIterator(mockContext, mockDiscoverQuery);
            iterator.next();
            assertTrue(iterator.hasNext());
            assertEquals(mockItem, iterator.next());
        }

    }

    @Test
    public void testNextWithIndexablePoolTask() throws SearchServiceException {
        mockDiscoverResult = mock(DiscoverResult.class);
        when(mockDiscoverResult.getIndexableObjects())
            .thenReturn(Collections.singletonList(mockIndexablePoolTask));
        when(mockDiscoverResult.getTotalSearchResults()).thenReturn(2L);
        when(mockSearchService.search(eq(mockContext), any(DiscoverQuery.class))).thenReturn(mockDiscoverResult);


        try (MockedStatic<SearchUtils> mockedStatic = mockStatic(SearchUtils.class)) {
            mockedStatic.when(SearchUtils::getSearchService).thenReturn(mockSearchService);
            DiscoverResultItemIterator iterator = new DiscoverResultItemIterator(mockContext, mockDiscoverQuery);
            iterator.next();
            assertTrue(iterator.hasNext());
            assertEquals(mockItem, iterator.next());
        }
    }

    @Test
    public void testNextWithIndexableClaimedTask() throws SearchServiceException {
        mockDiscoverResult = mock(DiscoverResult.class);
        when(mockDiscoverResult.getIndexableObjects())
            .thenReturn(Collections.singletonList(mockIndexableClaimedTask));
        when(mockDiscoverResult.getTotalSearchResults()).thenReturn(2L);
        when(mockSearchService.search(eq(mockContext), any(DiscoverQuery.class))).thenReturn(mockDiscoverResult);


        try (MockedStatic<SearchUtils> mockedStatic = mockStatic(SearchUtils.class)) {
            mockedStatic.when(SearchUtils::getSearchService).thenReturn(mockSearchService);
            DiscoverResultItemIterator iterator = new DiscoverResultItemIterator(mockContext, mockDiscoverQuery);
            iterator.next();
            assertTrue(iterator.hasNext());
            assertEquals(mockItem, iterator.next());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testNextWithInvalidObjectType() throws SearchServiceException {
        mockDiscoverResult = mock(DiscoverResult.class);
        IndexableObject invalidObject = mock(IndexableObject.class);
        when(invalidObject.getIndexedObject()).thenReturn(mockItem);
        when(invalidObject.getType()).thenReturn("InvalidObjectType");
        when(mockDiscoverResult.getIndexableObjects()).thenReturn(Collections.singletonList(invalidObject));
        when(mockSearchService.search(eq(mockContext), any(DiscoverQuery.class))).thenReturn(mockDiscoverResult);

        try (MockedStatic<SearchUtils> mockedStatic = mockStatic(SearchUtils.class)) {
            mockedStatic.when(SearchUtils::getSearchService).thenReturn(mockSearchService);
            DiscoverResultItemIterator iterator = new DiscoverResultItemIterator(mockContext, mockDiscoverQuery);
            iterator.next();
        }
    }
}
