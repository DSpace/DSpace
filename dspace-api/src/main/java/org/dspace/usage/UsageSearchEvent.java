/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usage;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * Extends the standard usage event to contain search information
 * search information includes the query/queries used and the scope
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class UsageSearchEvent extends UsageEvent {

    private String query;
    private String dsoType;
    private DSpaceObject scope;
    private String configuration;
    private List<AppliedFilter> appliedFilters;
    private Sort sort;
    private Page page;

    public UsageSearchEvent(Action action, HttpServletRequest request,
                                Context context,
                                DSpaceObject object) {
        super(action, request, context, object);
    }

    public void setDsoType(String dsoType) {
        this.dsoType = dsoType;
    }

    public String getDsoType() {
        return dsoType;
    }

    public void setScope(DSpaceObject scope) {
        this.scope = scope;
    }

    public DSpaceObject getScope() {
        return scope;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getConfiguration() {
        return configuration;
    }

    public List<AppliedFilter> getAppliedFilters() {
        return appliedFilters;
    }

    public void setAppliedFilters(List<AppliedFilter> appliedFilters) {
        this.appliedFilters = appliedFilters;
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public static class Page {

        private int size;
        private int totalElements;
        private int totalPages;
        private int number;

        public Page(int size, int totalElements, int totalPages, int number) {
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.number = number;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(int totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }
    }

    public static class Sort {

        private String by;
        private String order;

        public Sort(String by, String order) {
            this.by = by;
            this.order = order;
        }

        public String getBy() {
            return by;
        }

        public void setBy(String by) {
            this.by = by;
        }

        public String getOrder() {
            return order;
        }

        public void setOrder(String order) {
            this.order = order;
        }
    }

    public static class AppliedFilter {

        private String filter;
        private String operator;
        private String value;
        private String label;

        public AppliedFilter(String filter, String operator, String value, String label) {
            this.filter = filter;
            this.operator = operator;
            this.value = value;
            this.label = label;
        }

        public String getFilter() {
            return filter;
        }

        public void setFilter(String filter) {
            this.filter = filter;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
