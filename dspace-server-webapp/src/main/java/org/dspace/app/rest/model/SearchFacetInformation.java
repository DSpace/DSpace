/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.parameter.resolver.SearchFilterResolverUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Class storing discovery search information about a facet entry and its values
 * The purpose is to share the information with link repositories to construct links, it is not meant to be included in
 * returned REST information
 */
public class SearchFacetInformation {
    private String facetName;
    private String scope;
    private String configuration;
    private String prefix;
    private String query;
    private List<SearchFilter> searchFilters;
    private List<String> dsoTypes;
    private Integer pageNumber;
    private Integer pageSize;
    private List<String> sort;

    public static SearchFacetInformation fromRequest(HttpServletRequest request, String facetName) {
        SearchFacetInformation information = fromRequest(request);
        information.setFacetName(facetName);
        return information;
    }

    public static SearchFacetInformation fromRequest(HttpServletRequest request) {
        SearchFacetInformation information = new SearchFacetInformation();
        information.setScope(request.getParameter("scope"));
        information.setConfiguration(request.getParameter("configuration"));
        information.setPrefix(request.getParameter("prefix"));
        information.setQuery(request.getParameter("query"));
        information.setSearchFilters(SearchFilterResolverUtil.resolveSearchFilters(request));
        String[] dsoTypeValues = request.getParameterValues("dsoType");
        List<String> dsoTypes = List.of();
        if (dsoTypeValues != null) {
            dsoTypes = List.of(dsoTypeValues);
        }
        information.setDsoTypes(dsoTypes);
        String page = request.getParameter("page");
        information.setPageNumber(page != null ? Integer.parseInt(page) : null);
        String size = request.getParameter("size");
        information.setPageSize(size != null ? Integer.parseInt(size) : null);
        String[] sortValues = request.getParameterValues("sort");
        List<String> sort = List.of();
        if (sortValues != null) {
            sort = List.of(sortValues);
        }
        information.setSort(sort);
        return information;
    }

    public void appendQueryParameters(UriComponentsBuilder builder, boolean withPageParams) {
        if (getPrefix() != null) {
            builder.queryParam("prefix", getPrefix());
        }
        if (getQuery() != null) {
            builder.queryParam("query", getQuery());
        }
        if (getConfiguration() != null) {
            builder.queryParam("configuration", getConfiguration());
        }
        if (getScope() != null) {
            builder.queryParam("scope", getScope());
        }
        if (CollectionUtils.isNotEmpty(getDsoTypes())) {
            builder.queryParam("dsoType", getDsoTypes());
        }
        if (CollectionUtils.isNotEmpty(getSearchFilters())) {
            for (SearchFilter filter : getSearchFilters()) {
                builder.queryParam("f." + filter.getName(), filter.getValue() + "," +
                    filter.getOperator());
            }
        }
        if (withPageParams) {
            if (CollectionUtils.isNotEmpty(getSort())) {
                builder.queryParam("sort", getSort());
            }
            if (getPageNumber() != null) {
                builder.queryParam("page", getPageNumber().toString());
            }
            if (getPageSize() != null) {
                builder.queryParam("size", getPageSize().toString());
            }
        }
    }

    public String getFacetName() {
        return facetName;
    }

    public void setFacetName(String facetName) {
        this.facetName = facetName;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<SearchFilter> getSearchFilters() {
        return searchFilters;
    }

    public void setSearchFilters(List<SearchFilter> searchFilters) {
        this.searchFilters = searchFilters;
    }

    public List<String> getDsoTypes() {
        return dsoTypes;
    }

    public void setDsoTypes(List<String> dsoTypes) {
        this.dsoTypes = dsoTypes;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<String> getSort() {
        return sort;
    }

    public Sort getPageableSort() {
        Sort sort = Sort.unsorted();
        for (String s : CollectionUtils.emptyIfNull(getSort())) {
            String[] parts = s.split(",");
            if (parts.length == 2) {
                sort = sort.and(Sort.by(Sort.Direction.fromString(parts[1]), parts[0]));
            } else if (parts.length == 1) {
                sort = sort.and(Sort.by(parts[0]));
            }
        }
        return sort;
    }

    public void setSort(List<String> sort) {
        this.sort = sort;
    }

    public Pageable getPageable() {
        return PageRequest.of(
            getPageNumber() != null ? getPageNumber() : 0,
            getPageSize() != null ? getPageSize() : 20,
            getPageableSort());
    }
}
