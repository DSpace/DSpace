/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import java.text.MessageFormat;
import java.util.List;

import org.apache.logging.log4j.Logger;

/**
 * This class extends {@link DiscoveryConfiguration} and add method for set parameters
 * to filter query list
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class DiscoveryRelatedItemConfiguration extends DiscoveryConfiguration {

    private static final Logger log =
            org.apache.logging.log4j.LogManager.getLogger(DiscoveryRelatedItemConfiguration.class);

    public void setFilterQueriesParameters(Object...parameters) {
        List<String> queries = this.getDefaultFilterQueries();
        if (queries != null && !queries.isEmpty()) {
            for ( int i = 0; i < queries.size(); i++ ) {
                queries.set(i, MessageFormat.format(queries.get(i), parameters));
            }
        } else {
            log.warn("you are trying to set queries parameters on an empty queries list");
        }
    }

}
