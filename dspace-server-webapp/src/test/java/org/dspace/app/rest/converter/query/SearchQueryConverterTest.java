/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter.query;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.dspace.app.rest.parameter.SearchFilter;
import org.junit.Before;
import org.junit.Test;

public class SearchQueryConverterTest {

    SearchQueryConverter searchQueryConverter;


    @Before
    public void setUp() throws Exception {
        searchQueryConverter = new SearchQueryConverter();
    }

    @Test
    public void convertAuthorContainsSearchFilterTest() {
        SearchFilter searchFilter = new SearchFilter("author", "query", "test*");
        List<SearchFilter> list = new LinkedList<>();
        list.add(searchFilter);
        List<SearchFilter> transformedList = searchQueryConverter.convert(list);

        assertEquals(transformedList.get(0).getOperator(), "contains");
        assertEquals(list.get(0).getOperator(), "query");
        assertEquals(transformedList.get(0).getName(), "author");
        assertEquals(list.get(0).getName(), "author");
        assertEquals(transformedList.get(0).getValue(), "test");
        assertEquals(list.get(0).getValue(), "test*");
    }

    @Test
    public void convertAuthorNotContainsSearchFilterTest() {
        SearchFilter searchFilter = new SearchFilter("author", "query", "-test*");
        List<SearchFilter> list = new LinkedList<>();
        list.add(searchFilter);
        List<SearchFilter> transformedList = searchQueryConverter.convert(list);

        assertEquals(transformedList.get(0).getOperator(), "notcontains");
        assertEquals(list.get(0).getOperator(), "query");
        assertEquals(transformedList.get(0).getName(), "author");
        assertEquals(list.get(0).getName(), "author");
        assertEquals(transformedList.get(0).getValue(), "test");
        assertEquals(list.get(0).getValue(), "-test*");
    }

    @Test
    public void convertAuthorEqualsSearchFilterTest() {
        SearchFilter searchFilter = new SearchFilter("author", "query", "test");
        List<SearchFilter> list = new LinkedList<>();
        list.add(searchFilter);
        List<SearchFilter> transformedList = searchQueryConverter.convert(list);

        assertEquals(transformedList.get(0).getOperator(), "equals");
        assertEquals(list.get(0).getOperator(), "query");
        assertEquals(transformedList.get(0).getName(), "author");
        assertEquals(list.get(0).getName(), "author");
        assertEquals(transformedList.get(0).getValue(), "test");
        assertEquals(list.get(0).getValue(), "test");
    }

    @Test
    public void convertAuthorNotEqualsSearchFilterTest() {
        SearchFilter searchFilter = new SearchFilter("author", "query", "-test");
        List<SearchFilter> list = new LinkedList<>();
        list.add(searchFilter);
        List<SearchFilter> transformedList = searchQueryConverter.convert(list);

        assertEquals(transformedList.get(0).getOperator(), "notequals");
        assertEquals(list.get(0).getOperator(), "query");
        assertEquals(transformedList.get(0).getName(), "author");
        assertEquals(list.get(0).getName(), "author");
        assertEquals(transformedList.get(0).getValue(), "test");
        assertEquals(list.get(0).getValue(), "-test");
    }

    @Test
    public void convertAuthorAuthoritySearchFilterTest() {
        SearchFilter searchFilter = new SearchFilter("author", "query", "id:test");
        List<SearchFilter> list = new LinkedList<>();
        list.add(searchFilter);
        List<SearchFilter> transformedList = searchQueryConverter.convert(list);

        assertEquals(transformedList.get(0).getOperator(), "authority");
        assertEquals(list.get(0).getOperator(), "query");
        assertEquals(transformedList.get(0).getName(), "author");
        assertEquals(list.get(0).getName(), "author");
        assertEquals(transformedList.get(0).getValue(), "test");
        assertEquals(list.get(0).getValue(), "id:test");
    }

    @Test
    public void convertAuthorNotAuthoritySearchFilterTest() {
        SearchFilter searchFilter = new SearchFilter("author", "query", "-id:test");
        List<SearchFilter> list = new LinkedList<>();
        list.add(searchFilter);
        List<SearchFilter> transformedList = searchQueryConverter.convert(list);

        assertEquals(transformedList.get(0).getOperator(), "notauthority");
        assertEquals(list.get(0).getOperator(), "query");
        assertEquals(transformedList.get(0).getName(), "author");
        assertEquals(list.get(0).getName(), "author");
        assertEquals(transformedList.get(0).getValue(), "test");
        assertEquals(list.get(0).getValue(), "-id:test");
    }
}
