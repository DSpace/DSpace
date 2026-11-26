/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.springframework.hateoas.PagedModel;

/**
 * An extended PagedModel to allow for declaration of extra properties to expose in the REST API
 * Return an implementation of this class in an {@link ExtendedPagedRest}
 * @param <T>
 */
public abstract class ExtendedPagedModel<T extends RestAddressableModel> extends PagedModel<T> {
    /**
     * Provide an assembled PagedModel to extend from
     */
    public ExtendedPagedModel(PagedModel<T> pagedModel) {
        super(pagedModel.getContent(), pagedModel.getMetadata(), pagedModel.getLinks(), pagedModel.getResolvableType());
    }
}
