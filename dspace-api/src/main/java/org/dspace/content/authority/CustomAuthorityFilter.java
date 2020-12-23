/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Provides some custom filter queries to be used during Item Authority
 * lookup.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public abstract class CustomAuthorityFilter {

    private List<String> supportedEntities;

    /**
     *
     * @return a list of custom solr filter queries
     */
    public List<String> getFilterQueries(String relationshipType) {
        if (appliesTo(relationshipType)) {
            return createFilterQueries();
        }
        return Collections.emptyList();
    }

    protected abstract List<String> createFilterQueries();

    /**
     * Defines if instance can provide additional filter queries
     * for given relationship type
     */
    private boolean appliesTo(String relationshipType) {
        return Optional.ofNullable(supportedEntities)
            .map(e -> e.contains(relationshipType))
            .orElse(true);
    }

    public void setSupportedEntities(List<String> supportedEntities) {
        this.supportedEntities = supportedEntities;
    }
}
