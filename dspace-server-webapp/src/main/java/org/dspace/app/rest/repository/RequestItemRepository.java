/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.repository;

import javax.inject.Inject;

import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.app.rest.converter.RequestItemConverter;
import org.dspace.app.rest.model.RequestItemRest;
import org.dspace.app.rest.model.hateoas.RequestItemResource;
import org.dspace.core.Context;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;

/**
 * Controller to expose item requests.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
@Controller(RequestItemRest.CATEGORY + '.' + RequestItemRest.NAME)
public class RequestItemRepository
        extends DSpaceRestRepository<RequestItemRest, String> {
    @Inject
    protected RequestItemService requestItemService;

    @Inject
    protected RequestItemConverter requestItemConverter;

    @Override
    public RequestItemRest findOne(Context context, String id) {
        RequestItem requestItem = requestItemService.findByToken(context, id);
        return requestItemConverter.fromModel(requestItem);
    }

    @Override
    public Page<RequestItemRest> findAll(Context context, Pageable pageable) {
        // TODO ? There is no enumerator in RequestItemService
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Class<RequestItemRest> getDomainClass() {
        return RequestItemRest.class;
    }
}
