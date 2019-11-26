/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

/**
 * This is the interface for Link Repositories.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface LinkRestRepository {
    default boolean isEmbeddableRelation(Object data, String name) {
        return true;
    }
}
