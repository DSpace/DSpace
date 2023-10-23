/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.CorrectionTypeRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.correctiontype.CorrectionType;
import org.dspace.correctiontype.service.CorrectionTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * The CorrectionType REST Repository
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@Component(CorrectionTypeRest.CATEGORY + "." + CorrectionTypeRest.NAME)
public class CorrectionTypeRestRepository extends DSpaceRestRepository<CorrectionTypeRest, String> {

    @Autowired
    private CorrectionTypeService correctionTypeService;

    @Autowired
    private ItemService itemService;

    @PreAuthorize("permitAll()")
    @Override
    public CorrectionTypeRest findOne(Context context, String id) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @PreAuthorize("permitAll()")
    @Override
    public Page<CorrectionTypeRest> findAll(Context context, Pageable pageable) {
        List<CorrectionType> correctionTypes = correctionTypeService.findAll();
        return converter.toRestPage(correctionTypes, pageable, utils.obtainProjection());
    }

    @PreAuthorize("permitAll()")
    @SearchRestMethod(name = "findByItem")
    public Page<CorrectionTypeRest> findByItem(@Parameter(value = "uuid", required = true) UUID uuid,
                                               Pageable pageable) {
        Context context = obtainContext();
        try {
            Item item = itemService.find(context, uuid);
            if (null == item) {
                throw new UnprocessableEntityException("item not found");
            }

            List<CorrectionType> correctionTypes;
            try {
                correctionTypes = correctionTypeService.findByItem(context, item);
            } catch (AuthorizeException e) {
                throw new RESTAuthorizationException(e);
            }

            return converter.toRestPage(correctionTypes, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @PreAuthorize("permitAll()")
    @SearchRestMethod(name = "findByTopic")
    public CorrectionTypeRest findByTopic(@Parameter(value = "topic", required = true) String topic) {
        CorrectionType correctionType = correctionTypeService.findByTopic(topic);
        if (null == correctionType) {
            return null;
        }
        return converter.toRest(correctionType, utils.obtainProjection());
    }

    @Override
    public Class<CorrectionTypeRest> getDomainClass() {
        return CorrectionTypeRest.class;
    }

}