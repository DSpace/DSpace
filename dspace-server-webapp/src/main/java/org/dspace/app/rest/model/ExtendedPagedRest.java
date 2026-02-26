package org.dspace.app.rest.model;

/**
 * Interface to retrieve the {@link ExtendedPagedModel} from a Rest class returned by a search method
 * @param <T>
 */
public interface ExtendedPagedRest<T extends RestAddressableModel> {
    ExtendedPagedModel<T> getPagedModel();
}
