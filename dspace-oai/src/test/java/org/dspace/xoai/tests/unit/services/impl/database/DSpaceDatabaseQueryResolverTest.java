/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.unit.services.impl.database;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dspace.core.Constants;
import org.dspace.xoai.filter.DSpaceMetadataExistsFilter;
import org.dspace.xoai.filter.DSpaceSetSpecFilter;
import org.dspace.xoai.filter.DateFromFilter;
import org.dspace.xoai.filter.DateUntilFilter;
import org.dspace.xoai.services.api.database.DatabaseQuery;
import org.dspace.xoai.services.impl.database.DSpaceDatabaseQueryResolver;
import org.dspace.xoai.tests.unit.services.impl.AbstractQueryResolverTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lyncode.builder.DateBuilder;
import com.lyncode.xoai.dataprovider.data.Filter;
import com.lyncode.xoai.dataprovider.filter.Scope;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import com.lyncode.xoai.dataprovider.filter.conditions.AndCondition;
import com.lyncode.xoai.dataprovider.filter.conditions.Condition;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.ParameterList;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.ParameterMap;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.StringValue;

public class DSpaceDatabaseQueryResolverTest extends AbstractQueryResolverTest {
    private static final Date DATE = new Date();
    private static final String SET = "col_testSet";
    private static final String FIELD_1 = "dc.title";
    private static final String FIELD_2 = "dc.type";
    private static final int START = 0;
    private static final int LENGTH = 100;


    private DSpaceDatabaseQueryResolver underTest = new DSpaceDatabaseQueryResolver();;

    @Before
    public void autowire () {
        autowire(underTest);
    }
    
    @After
    public void cleanup() {
        underTest = null;
    }

    @Test
    public void fromFilterQuery() throws Exception {
        List<ScopedFilter> scopedFilters = new ArrayList<ScopedFilter>();
        scopedFilters.add(new ScopedFilter(new Condition() {
            @Override
            public Filter getFilter() {
                return new DateFromFilter(DATE);
            }
        }, Scope.Query));

        DatabaseQuery result = underTest.buildQuery(scopedFilters, START, LENGTH);

        assertThat(result.getQuery(), is("SELECT i.* FROM item i  WHERE i.in_archive=true AND (i.last_modified >= ?) ORDER BY i.item_id OFFSET ? LIMIT ?"));
        assertThat(((java.sql.Date)result.getParameters().get(0)).getTime(), is(fromDate(DATE).getTime()));
        assertThat((Integer) result.getParameters().get(1), is(START));
        assertThat((Integer) result.getParameters().get(2), is(LENGTH));
    }

    @Test
    public void fromAndUntilFilterQuery() throws Exception {
        List<ScopedFilter> scopedFilters = new ArrayList<ScopedFilter>();
        Condition fromCondition = new Condition() {
            @Override
            public Filter getFilter() {
                return new DateFromFilter(DATE);
            }
        };
        Condition untilCondition = new Condition() {
            @Override
            public Filter getFilter() {
                return new DateUntilFilter(DATE);
            }
        };
        scopedFilters.add(new ScopedFilter(new AndCondition(getFilterResolver(),
                fromCondition, untilCondition), Scope.Query));

        DatabaseQuery result = underTest.buildQuery(scopedFilters, START, LENGTH);

        assertThat(result.getQuery(), is("SELECT i.* FROM item i  WHERE i.in_archive=true AND ((i.last_modified >= ?) AND (i.last_modified <= ?)) ORDER BY i.item_id OFFSET ? LIMIT ?"));
        assertThat(((java.sql.Date)result.getParameters().get(0)).getTime(), is(fromDate(DATE).getTime()));
        assertThat(((java.sql.Date)result.getParameters().get(1)).getTime(), is(untilDate(DATE).getTime()));
        assertThat((Integer) result.getParameters().get(2), is(START));
        assertThat((Integer) result.getParameters().get(3), is(LENGTH));
    }

    @Test
    public void customConditionForMetadataExistsFilterWithOneSingleValue() throws Exception {
        theFieldResolver().hasField(FIELD_1, 1);
        List<ScopedFilter> scopedFilters = new ArrayList<ScopedFilter>();
        ParameterMap filterConfiguration = new ParameterMap().withValues(new StringValue()
                .withValue(FIELD_1)
                .withName("fields"));

        final DSpaceMetadataExistsFilter metadataExistsFilter = new DSpaceMetadataExistsFilter();
        metadataExistsFilter.setConfiguration(filterConfiguration);
        metadataExistsFilter.setFieldResolver(theFieldResolver());
        scopedFilters.add(new ScopedFilter(new Condition()
        {
            @Override
            public Filter getFilter()
            {
                return metadataExistsFilter;
            }
        }, Scope.Query));

        DatabaseQuery result = underTest.buildQuery(scopedFilters, START, LENGTH);

        assertThat(result.getQuery(), is("SELECT i.* FROM item i  WHERE i.in_archive=true AND ((EXISTS (SELECT tmp.* FROM metadatavalue tmp WHERE tmp.resource_id=i.item_id AND tmp.resource_type_id=" + Constants.ITEM + " AND tmp.metadata_field_id=?))) ORDER BY i.item_id OFFSET ? LIMIT ?"));
        assertThat(((Integer) result.getParameters().get(0)), is(1));
        assertThat((Integer) result.getParameters().get(1), is(START));
        assertThat((Integer) result.getParameters().get(2), is(LENGTH));
    }

    @Test
    public void customConditionForMetadataExistsFilterWithMultipleValues() throws Exception {
        theFieldResolver().hasField(FIELD_1, 1).hasField(FIELD_2, 2);
        List<ScopedFilter> scopedFilters = new ArrayList<ScopedFilter>();
        ParameterMap filterConfiguration = new ParameterMap().withValues(new ParameterList()
                .withValues(
                        new StringValue().withValue(FIELD_1),
                        new StringValue().withValue(FIELD_2)
                )
                .withName("fields"));

        final DSpaceMetadataExistsFilter metadataExistsFilter = new DSpaceMetadataExistsFilter();
        metadataExistsFilter.setConfiguration(filterConfiguration);
        metadataExistsFilter.setFieldResolver(theFieldResolver());
        scopedFilters.add(new ScopedFilter(new Condition()
        {
            @Override
            public Filter getFilter()
            {
                return metadataExistsFilter;
            }
        }, Scope.Query));

        DatabaseQuery result = underTest.buildQuery(scopedFilters, START, LENGTH);

        assertThat(result.getQuery(), is("SELECT i.* FROM item i  WHERE i.in_archive=true AND ((EXISTS (SELECT tmp.* FROM metadatavalue tmp WHERE tmp.resource_id=i.item_id AND tmp.resource_type_id=" + Constants.ITEM + " AND tmp.metadata_field_id=?) OR EXISTS (SELECT tmp.* FROM metadatavalue tmp WHERE tmp.resource_id=i.item_id AND tmp.resource_type_id=" + Constants.ITEM + " AND tmp.metadata_field_id=?))) ORDER BY i.item_id OFFSET ? LIMIT ?"));
        assertThat(((Integer) result.getParameters().get(0)), is(1));
        assertThat(((Integer) result.getParameters().get(1)), is(2));
        assertThat((Integer) result.getParameters().get(2), is(START));
        assertThat((Integer) result.getParameters().get(3), is(LENGTH));
    }

    @Test
    public void fromFilterInMetadataFormatScope() throws Exception {
        List<ScopedFilter> scopedFilters = new ArrayList<ScopedFilter>();
        scopedFilters.add(new ScopedFilter(new Condition() {
            @Override
            public Filter getFilter() {
                return new DateFromFilter(DATE);
            }
        }, Scope.MetadataFormat));

        DatabaseQuery result = underTest.buildQuery(scopedFilters, START, LENGTH);

        assertThat(result.getQuery(), is("SELECT i.* FROM item i  WHERE i.in_archive=true AND (item.deleted:true OR (i.last_modified >= ?)) ORDER BY i.item_id OFFSET ? LIMIT ?"));
        assertThat(((java.sql.Date)result.getParameters().get(0)).getTime(), is(fromDate(DATE).getTime()));
        assertThat((Integer) result.getParameters().get(1), is(START));
        assertThat((Integer) result.getParameters().get(2), is(LENGTH));
    }

    @Test
    public void fromAndSetFilterQuery() throws Exception {
        List<ScopedFilter> scopedFilters = new ArrayList<ScopedFilter>();
        scopedFilters.add(new ScopedFilter(new Condition() {
            @Override
            public Filter getFilter() {
                return new DateFromFilter(DATE);
            }
        }, Scope.Query));
        scopedFilters.add(new ScopedFilter(new Condition() {
            @Override
            public Filter getFilter() {
                return new DSpaceSetSpecFilter(collectionsService, handleResolver, SET);
            }
        }, Scope.Query));

        DatabaseQuery result = underTest.buildQuery(scopedFilters, START, LENGTH);

        assertThat(result.getQuery(), is("SELECT i.* FROM item i  WHERE i.in_archive=true AND (i.last_modified >= ?) AND true ORDER BY i.item_id OFFSET ? LIMIT ?"));
        assertThat(((java.sql.Date)result.getParameters().get(0)).getTime(), is(fromDate(DATE).getTime()));
        assertThat((Integer) result.getParameters().get(1), is(START));
        assertThat((Integer) result.getParameters().get(2), is(LENGTH));
    }

    private Date fromDate(Date date) {
        return new DateBuilder(date).setMinMilliseconds().build();
    }

    private Date untilDate(Date date) {
        return new DateBuilder(date).setMaxMilliseconds().build();
    }
}
