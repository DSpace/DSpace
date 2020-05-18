/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.converter.CrisLayoutMetadataComponentConverter;
import org.dspace.app.rest.model.CrisLayoutBoxRest;
import org.dspace.app.rest.model.CrisLayoutMetadataComponentRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.layout.factory.CrisLayoutServiceFactory;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Link repository for the configuration subresource of a specific box
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component(CrisLayoutBoxRest.CATEGORY + "." + CrisLayoutBoxRest.NAME + "." + CrisLayoutBoxRest.CONFIGURATON)
public class CrisLayoutBoxConfigurationLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private CrisLayoutServiceFactory serviceFactory;

    @Autowired
    private CrisLayoutMetadataComponentConverter mcConverter;

    public CrisLayoutMetadataComponentRest getConfiguration(
            @Nullable HttpServletRequest request,
            Integer boxId,
            @Nullable Pageable pageable,
            Projection projection) {
        Context context = obtainContext();
        CrisLayoutBoxService service = serviceFactory.getBoxService();
        CrisLayoutMetadataComponentRest values = null;
        try {
            values = mcConverter.convert( service.find(context, boxId) );
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return values;
    }
}
