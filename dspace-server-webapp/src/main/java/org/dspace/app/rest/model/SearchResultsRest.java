/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.PagedModel;

/**
 * This class' purpose is to create a container for the information used in the SearchResultsResource
 */
public class SearchResultsRest extends DiscoveryResultsRest implements ExtendedPagedRest<SearchResultEntryRest> {
    public static final String NAME = "searchresult";
    public static final String PLURAL_NAME = "searchresults";

    @JsonIgnore
    private PagedModel<SearchResultEntryRest> pagedModel;

    public SearchResultsRest(PagedModel<SearchResultEntryRest> pagedModel) {
        this.pagedModel = pagedModel;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    /**
     * Extend the PagedModel with search-result properties to expose in the REST API
     */
    public SearchResultsPagedModel getPagedModel() {
        SearchResultsPagedModel model = new SearchResultsPagedModel(pagedModel);
        model.setScope(getScope());
        model.setQuery(getQuery());
        model.setAppliedFilters(getAppliedFilters());
        model.setSort(getSort());
        model.setConfiguration(getConfiguration());
        return model;
    }

    public static class AppliedFilter {

        private String filter;
        private String operator;
        private String value;
        private String label;

        public AppliedFilter(final String name, final String operator, final String value, final String label) {
            this.filter = name;
            this.operator = operator;
            this.value = value;
            this.label = label;
        }

        public AppliedFilter() {
        }

        public String getFilter() {
            return filter;
        }

        public void setFilter(final String filter) {
            this.filter = filter;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(final String operator) {
            this.operator = operator;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(final String label) {
            this.label = label;
        }
    }

    public static class Sorting {
        private String by;
        private String order;

        public Sorting(String by, String order) {
            this.by = by;
            this.order = order;
        }

        public Sorting(String by) {
            this.by = by;
            this.order = null;
        }

        public Sorting() {
        }

        public String getBy() {
            return by;
        }

        public void setBy(final String by) {
            this.by = by;
        }

        public String getOrder() {
            return order;
        }

        public void setOrder(final String order) {
            this.order = order;
        }

        public static Sorting fromPage(final Pageable page) {
            if (page != null) {
                Sort sort = page.getSort();
                if (sort != null && sort.iterator().hasNext()) {
                    Sort.Order order = sort.iterator().next();
                    return new Sorting(order.getProperty(), order.getDirection().name());
                }
            }
            return null;
        }
    }
}
