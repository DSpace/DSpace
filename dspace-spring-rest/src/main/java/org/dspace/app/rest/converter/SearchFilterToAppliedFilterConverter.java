/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to convert the SearchFilter object into a SearchResultsRest.AppliedFilter object
 */
@Component
public class SearchFilterToAppliedFilterConverter {

    @Autowired
    private AuthorityValueService authorityValueService;

    public SearchResultsRest.AppliedFilter convertSearchFilter(Context context, SearchFilter searchFilter) {

        AuthorityValue authorityValue = null;
        if (searchFilter.hasAuthorityOperator()) {
            // FIXME this is obviously wrong as it assumes that the authorityValueService is able to retrieve the label
            // for each Authority. Indeed, the AuthorityValueService regardless to his name is specific of the
            // "SOLRAuthority" implementation and should not have a prominent role.
            // Moreover, it is not possible to discover which authority is responsible for the value selected in the
            // facet as the authority is bind at the metadata level and so a facet could contains values from multiple
            // authorities
            // https://jira.duraspace.org/browse/DS-4209
            authorityValue = authorityValueService.findByUID(context, searchFilter.getValue());
        }

        SearchResultsRest.AppliedFilter appliedFilter;
        if (authorityValue == null) {
            appliedFilter = new SearchResultsRest.AppliedFilter(searchFilter.getName(), searchFilter.getOperator(),
                                                                searchFilter.getValue(), searchFilter.getValue());
        } else {
            appliedFilter = new SearchResultsRest.AppliedFilter(searchFilter.getName(), searchFilter.getOperator(),
                                                                searchFilter.getValue(), authorityValue.getValue());
        }

        return appliedFilter;
    }
}
