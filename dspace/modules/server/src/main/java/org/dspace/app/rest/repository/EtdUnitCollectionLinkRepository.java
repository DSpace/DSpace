package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.EtdUnitRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.EtdUnit;
import org.dspace.content.service.EtdUnitService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for the direct "collections" subresource of an individual
 * etdunit.
 */
@Component(EtdUnitRest.CATEGORY + "." + EtdUnitRest.NAME + "." + EtdUnitRest.COLLECTIONS)
public class EtdUnitCollectionLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    EtdUnitService etdunitService;

    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<CollectionRest> getCollections(@Nullable HttpServletRequest request,
            UUID etdunitId,
            @Nullable Pageable optionalPageable,
            Projection projection) {
        try {
            Context context = obtainContext();
            EtdUnit etdunit = etdunitService.find(context, etdunitId);
            if (etdunit == null) {
                throw new ResourceNotFoundException("No such etdunit: " + etdunitId);
            }
            return converter.toRestPage(etdunit.getCollections(), optionalPageable, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
