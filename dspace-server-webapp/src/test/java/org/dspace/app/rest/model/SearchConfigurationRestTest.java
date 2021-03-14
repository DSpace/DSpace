/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * This class' purpose is to test the SearchConfigurationRest
 */
public class SearchConfigurationRestTest {

    SearchConfigurationRest searchConfigurationRest;

    @Before
    public void setUp() throws Exception {
        searchConfigurationRest = new SearchConfigurationRest();
    }


    @Test
    public void testFiltersNotNullAfterConstructor() throws Exception {
        assertNotNull(searchConfigurationRest.getFilters());
    }

    @Test
    public void testSortOptionsNotNullAfterConstructor() throws Exception {
        assertNotNull(searchConfigurationRest.getSortOptions());
    }

    @Test
    public void testAddFilterToEmptyListAndListContainsThatFilter() throws Exception {
        SearchConfigurationRest.Filter filter = new SearchConfigurationRest.Filter();
        filter.setFilter("filter");
        searchConfigurationRest.addFilter(filter);
        assertEquals(filter, searchConfigurationRest.getFilters().get(0));
    }

    @Test
    public void testAddSortOptionToEmptyListAndListContainsThatSortOption() throws Exception {
        SearchConfigurationRest.SortOption sortOption = new SearchConfigurationRest.SortOption();
        sortOption.setActualName("sort option");
        searchConfigurationRest.addSortOption(sortOption);
        assertEquals(sortOption, searchConfigurationRest.getSortOptions().get(0));
    }

    @Test
    public void testAddMultipleFiltersToListAndListIsConstructedProperly() throws Exception {
        SearchConfigurationRest.Filter filter = new SearchConfigurationRest.Filter();
        filter.setFilter("filter");
        searchConfigurationRest.addFilter(filter);
        SearchConfigurationRest.Filter filter2 = new SearchConfigurationRest.Filter();
        filter.setFilter("filter2");
        searchConfigurationRest.addFilter(filter2);

        assertEquals(2, searchConfigurationRest.getFilters().size());

        assertTrue(searchConfigurationRest.getFilters().get(0) == filter || searchConfigurationRest.getFilters()
                                                                                                   .get(0) == filter2);
        assertTrue(searchConfigurationRest.getFilters().get(1) == filter || searchConfigurationRest.getFilters()
                                                                                                   .get(1) == filter2);

    }

    @Test
    public void testAddMultipleSortOptionsToListAndListIsConstructedProperly() throws Exception {
        SearchConfigurationRest.SortOption sortOption = new SearchConfigurationRest.SortOption();
        sortOption.setActualName("sort option");
        searchConfigurationRest.addSortOption(sortOption);
        SearchConfigurationRest.SortOption sortOption2 = new SearchConfigurationRest.SortOption();
        sortOption2.setActualName("sort option2");
        searchConfigurationRest.addSortOption(sortOption2);

        assertEquals(2, searchConfigurationRest.getSortOptions().size());

        assertTrue(
            searchConfigurationRest.getSortOptions().get(0) == sortOption || searchConfigurationRest.getSortOptions()
                                                                                                    .get(
                                                                                                        0) ==
                sortOption2);
        assertTrue(
            searchConfigurationRest.getSortOptions().get(1) == sortOption || searchConfigurationRest.getSortOptions()
                                                                                                    .get(
                                                                                                        1) ==
                sortOption2);

    }


    @Test
    public void testOperatorConstructorWithProperValueReturnsCorrectValue() throws Exception {
        SearchConfigurationRest.Filter.Operator operator = new SearchConfigurationRest.Filter.Operator("operator");
        assertEquals("operator", operator.getOperator());
    }


    @Test
    public void testFilterGetOperatorsAfterConstructorReturnsEmptyList() throws Exception {
        SearchConfigurationRest.Filter filter = new SearchConfigurationRest.Filter();
        assertTrue(filter.getOperators().isEmpty());
    }

    @Test
    public void testFilterAddOperatorAddsProperlyAndIsIncludedInGetOperators() throws Exception {
        SearchConfigurationRest.Filter filter = new SearchConfigurationRest.Filter();
        SearchConfigurationRest.Filter.Operator operator = new SearchConfigurationRest.Filter.Operator("operator");
        filter.addOperator(operator);
        assertEquals(operator, filter.getOperators().get(0));
    }

    @Test
    public void testFilterAddDefaultOperatorsToListPopulatesList() throws Exception {
        SearchConfigurationRest.Filter filter = new SearchConfigurationRest.Filter();
        filter.addDefaultOperatorsToList();
        assertTrue(!filter.getOperators().isEmpty());
    }
}