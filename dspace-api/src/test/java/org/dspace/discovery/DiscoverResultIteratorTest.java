/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class DiscoverResultIteratorTest {

    private SearchService mockSearchService;
    private Context mockContext;
    private DiscoverQuery mockDiscoverQuery;
    private DiscoverResult mockDiscoverResultWithTwoItems;
    private DiscoverResult mockEmptyDiscoverResult;
    private IndexableObject mockIndexableObject1;
    private IndexableObject mockIndexableObject2;
    private Item mockItem1;
    private Item mockItem2;

    @Before
    public void setUp() throws SearchServiceException {
        // Mock dependencies
        mockSearchService = mock(SearchService.class);
        mockContext = mock(Context.class);
        mockDiscoverQuery = mock(DiscoverQuery.class);
        mockIndexableObject1 = mock(IndexableObject.class);
        mockIndexableObject2 = mock(IndexableObject.class);

        mockItem1 = mock(Item.class);
        mockItem2 = mock(Item.class);

        // Stub the getIndexedObject method
        when(mockIndexableObject1.getIndexedObject()).thenReturn(mockItem1);
        when(mockIndexableObject2.getIndexedObject()).thenReturn(mockItem2);

        // Mock DiscoverResult with two elements
        mockDiscoverResultWithTwoItems = mock(DiscoverResult.class);
        when(mockDiscoverResultWithTwoItems.getIndexableObjects())
            .thenReturn(Arrays.asList(mockIndexableObject1, mockIndexableObject2));

        when(mockDiscoverResultWithTwoItems.getTotalSearchResults()).thenReturn(2L);

        // Mock empty DiscoverResult
        mockEmptyDiscoverResult = mock(DiscoverResult.class);
        when(mockEmptyDiscoverResult.getIndexableObjects())
            .thenReturn(Collections.emptyList());
        when(mockEmptyDiscoverResult.getTotalSearchResults()).thenReturn(0L);

        // Mock search service behavior
        when(mockSearchService.search(eq(mockContext), any(DiscoverQuery.class)))
            .thenReturn(mockDiscoverResultWithTwoItems)
            .thenReturn(mockEmptyDiscoverResult);
    }

    @Test
    public void testHasNextWithTwoResults() {
        try (MockedStatic<SearchUtils> mockedStatic = mockStatic(SearchUtils.class)) {
            mockedStatic.when(SearchUtils::getSearchService).thenReturn(mockSearchService);
            DiscoverResultIterator<Item, UUID> iterator =
                new DiscoverResultIterator<>(mockContext, null, mockDiscoverQuery);

            assertTrue(iterator.hasNext());
            iterator.next();
            assertTrue(iterator.hasNext());
            iterator.next();
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testHasNextWhenNoResults() throws SearchServiceException {
        try (MockedStatic<SearchUtils> mockedStatic = mockStatic(SearchUtils.class)) {
            mockedStatic.when(SearchUtils::getSearchService).thenReturn(mockSearchService);
            when(mockSearchService.search(eq(mockContext), any(DiscoverQuery.class)))
                .thenReturn(mockEmptyDiscoverResult);

            DiscoverResultIterator<Item, UUID> iterator =
                new DiscoverResultIterator<>(mockContext, null, mockDiscoverQuery);
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testReturnsTotalSearchResults() {
        try (MockedStatic<SearchUtils> mockedStatic = mockStatic(SearchUtils.class)) {
            mockedStatic.when(SearchUtils::getSearchService).thenReturn(mockSearchService);
            DiscoverResultIterator<Item, UUID> iterator =
                new DiscoverResultIterator<>(mockContext, null, mockDiscoverQuery);
            assertEquals(2L, iterator.getTotalSearchResults());
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void testNextThrowsExceptionWhenNoMoreElements() {
        try (MockedStatic<SearchUtils> mockedStatic = mockStatic(SearchUtils.class)) {
            mockedStatic.when(SearchUtils::getSearchService).thenReturn(mockSearchService);
            DiscoverResultIterator<Item, UUID> iterator =
                new DiscoverResultIterator<>(mockContext, null, mockDiscoverQuery);
            iterator.next();
            iterator.next();
            iterator.next();
        }
    }

    @Test
    public void testMaxResultsLimit() {
        try (MockedStatic<SearchUtils> mockedStatic = mockStatic(SearchUtils.class)) {
            mockedStatic.when(SearchUtils::getSearchService).thenReturn(mockSearchService);
            DiscoverResultIterator<Item, UUID> iterator = new DiscoverResultIterator<>(mockContext, null,
                                                                                       mockDiscoverQuery, true, 1);
            assertTrue(iterator.hasNext());
            iterator.next();
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testWithUncacheEntitiesTrue() throws Exception {
        try (MockedStatic<SearchUtils> mockedStatic = mockStatic(SearchUtils.class)) {
            mockedStatic.when(SearchUtils::getSearchService).thenReturn(mockSearchService);
            DiscoverResultIterator<Item, UUID> iterator =
                new DiscoverResultIterator<>(mockContext, null, mockDiscoverQuery, true, -1);
            assertTrue(iterator.hasNext());
            iterator.next();
            assertTrue(iterator.hasNext());
            iterator.next();
            assertFalse(iterator.hasNext());

            verify(mockContext).uncacheEntity(mockItem1);
            verify(mockContext).uncacheEntity(mockItem2);
        }
    }

}

