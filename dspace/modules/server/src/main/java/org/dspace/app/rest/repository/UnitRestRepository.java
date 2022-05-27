/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnitNameNotProvidedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.UnitRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;
import org.dspace.eperson.service.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Group Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(UnitRest.CATEGORY + "." + UnitRest.NAME)
public class UnitRestRepository extends DSpaceObjectRestRepository<Unit, UnitRest> {
    @Autowired
    UnitService unitService;

    @Autowired
    UnitRestRepository(UnitService dsoService) {
        super(dsoService);
        this.unitService = dsoService;
    }

    @Autowired
    MetadataConverter metadataConverter;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected UnitRest createAndReturn(Context context)
            throws AuthorizeException, RepositoryMethodNotImplementedException {

        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        UnitRest unitRest;

        try {
            unitRest = mapper.readValue(req.getInputStream(), UnitRest.class);
        } catch (IOException excIO) {
            throw new UnprocessableEntityException("error parsing the body ..." + excIO.getMessage());
        }

        if (isBlank(unitRest.getName())) {
            throw new UnitNameNotProvidedException();
        }

        Unit unit;
        try {
            unit = unitService.create(context);
            unit.setName(unitRest.getName());
            unitService.update(context, unit);
            metadataConverter.setMetadata(context, unit, unitRest.getMetadata());
        } catch (SQLException excSQL) {
            throw new RuntimeException(excSQL.getMessage(), excSQL);
        }

        return converter.toRest(unit, utils.obtainProjection());
    }

    @Override
//    @PreAuthorize("hasPermission(#id, 'UNIT', 'READ')")
    @PreAuthorize("hasAuthority('ADMIN')")
    public UnitRest findOne(Context context, UUID id) {
        Unit unit = null;
        try {
            unit = unitService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (unit == null) {
            return null;
        }
        return converter.toRest(unit, utils.obtainProjection());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public Page<UnitRest> findAll(Context context, Pageable pageable) {
        try {
            long total = unitService.countTotal(context);
            List<Unit> units = unitService.findAll(context, pageable.getPageSize(),
                                            Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(units, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

//    @Override
//    public Page<UnitRest> findAll(Context context, Pageable pageable) {
//        try {
//            long total = unitService.countTotal(context);
//            List<Unit> units = unitService.findAll(context, pageable.getPageSize(),
//                                            Math.toIntExact(pageable.getOffset()));
//            return converter.toRestPage(units, pageable, total, utils.obtainProjection());
//        } catch (SQLException e) {
//            throw new RuntimeException(e.getMessage(), e);
//        }
//    }

    @Override
    //@PreAuthorize("hasPermission(#id, 'UNIT', 'WRITE')")
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, UUID id,
                         Patch patch) throws AuthorizeException, SQLException {
        patchDSpaceObject(apiCategory, model, id, patch);
    }


    /**
     * Find the units matching the query parameter. The search is delegated to the
     * {@link UnitService#search(Context, String, int, int)} method
     *
     * @param query    is the *required* query string
     * @param pageable contains the pagination information
     * @return a Page of GroupRest instances matching the user query
     */
    //@PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MANAGE_ACCESS_GROUP')")
    @PreAuthorize("hasAuthority('ADMIN')")
    @SearchRestMethod(name = "byMetadata")
    public Page<UnitRest> findByMetadata(@Parameter(value = "query", required = true) String query,
                                          Pageable pageable) {

        try {
            Context context = obtainContext();
            long total = unitService.searchResultCount(context, query);
            List<Unit> units = unitService.search(context, query, Math.toIntExact(pageable.getOffset()),
                                                           Math.toIntExact(pageable.getPageSize()));
            return converter.toRestPage(units, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<UnitRest> getDomainClass() {
        return UnitRest.class;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, UUID uuid) throws AuthorizeException {
        Unit unit = null;
        try {
            unit = unitService.find(context, uuid);
            if (unit == null) {
                throw new ResourceNotFoundException(
                        UnitRest.CATEGORY + "." + UnitRest.NAME
                                + " with id: " + uuid + " not found"
                );
            }
            try {
                final DSpaceObject parentObject = unitService.getParentObject(context, unit);
                if (parentObject != null) {
                    throw new UnprocessableEntityException(
                            "This group cannot be deleted"
                                    + " as it has a parent " + parentObject.getType()
                                    + " with id " + parentObject.getID());
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            unitService.delete(context, unit);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
