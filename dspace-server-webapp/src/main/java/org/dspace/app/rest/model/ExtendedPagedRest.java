/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 * Interface to retrieve the {@link ExtendedPagedModel} from a Rest class returned by a search method
 * @param <T>
 */
public interface ExtendedPagedRest<T extends RestAddressableModel> {
    ExtendedPagedModel<T> getPagedModel();
}
