/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.unit.services.impl.solr;

import com.lyncode.xoai.dataprovider.data.Filter;
import com.lyncode.xoai.dataprovider.filter.Scope;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import com.lyncode.xoai.dataprovider.filter.conditions.AndCondition;
import com.lyncode.xoai.dataprovider.filter.conditions.Condition;
import com.lyncode.xoai.dataprovider.filter.conditions.CustomCondition;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.ParameterList;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.ParameterMap;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.StringValue;
import org.dspace.xoai.filter.DSpaceMetadataExistsFilter;
import org.dspace.xoai.filter.DSpaceSetSpecFilter;
import org.dspace.xoai.filter.DateFromFilter;
import org.dspace.xoai.filter.DateUntilFilter;
import org.dspace.xoai.services.impl.solr.DSpaceSolrQueryResolver;
import org.dspace.xoai.tests.unit.services.impl.AbstractQueryResolverTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DSpaceSolrQueryResolverTest extends AbstractQueryResolverTest {
    private static final Date DATE = new Date();
    private static final String SET = "col_testSet";
    private static final String FIELD_1 = "dc.title";
    private static final String FIELD_2 = "dc.type";

    private DSpaceSolrQueryResolver underTest = new DSpaceSolrQueryResolver();;

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

        String result = underTest.buildQuery(scopedFilters);

        assertThat(result, is("((item.lastmodified:[" + escapedFromDate(DATE) + " TO *]))"));
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

        String result = underTest.buildQuery(scopedFilters);

        assertThat(result, is("(((item.lastmodified:["+escapedFromDate(DATE)+" TO *]) AND (item.lastmodified:[* TO "+escapedUntilDate(DATE)+"])))"));
    }

    @Test
    public void customConditionForMetadataExistsFilterWithOneSingleValue() throws Exception {
        List<ScopedFilter> scopedFilters = new ArrayList<ScopedFilter>();
        ParameterMap filterConfiguration = new ParameterMap().withValues(new StringValue()
                .withValue(FIELD_1)
                .withName("fields"));

        scopedFilters.add(new ScopedFilter(new CustomCondition(getFilterResolver(),
                DSpaceMetadataExistsFilter.class,
                filterConfiguration),
                Scope.Query));

        String result = underTest.buildQuery(scopedFilters);

        assertThat(result, is("(((metadata."+FIELD_1+":[* TO *])))"));
    }

    @Test
    public void customConditionForMetadataExistsFilterWithMultipleValues() throws Exception {
        List<ScopedFilter> scopedFilters = new ArrayList<ScopedFilter>();
        ParameterMap filterConfiguration = new ParameterMap().withValues(new ParameterList()
                .withValues(
                        new StringValue().withValue(FIELD_1),
                        new StringValue().withValue(FIELD_2)
                        )
                .withName("fields"));

        scopedFilters.add(new ScopedFilter(new CustomCondition(getFilterResolver(),
                DSpaceMetadataExistsFilter.class,
                filterConfiguration),
                Scope.Query));

        String result = underTest.buildQuery(scopedFilters);

        assertThat(result, is("(((metadata."+FIELD_1+":[* TO *] OR metadata."+FIELD_2+":[* TO *])))"));
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

        String result = underTest.buildQuery(scopedFilters);

        assertThat(result, is("((item.deleted:true OR (item.lastmodified:[" + escapedFromDate(DATE) + " TO *])))"));
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

        String result = underTest.buildQuery(scopedFilters);

        assertThat(result, is("((item.lastmodified:[" + escapedFromDate(DATE) + " TO *])) AND ((item.collections:"+SET+"))"));
    }

}
