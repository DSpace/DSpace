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
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.EtdUnitNameNotProvidedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.EtdUnitRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.content.EtdUnit;
import org.dspace.content.service.EtdUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible for managing the EtdUnit Rest object
 */
@Component(EtdUnitRest.CATEGORY + "." + EtdUnitRest.NAME)
public class EtdUnitRestRepository extends DSpaceObjectRestRepository<EtdUnit, EtdUnitRest> {
    @Autowired
    EtdUnitService etdunitService;

    @Autowired
    EtdUnitRestRepository(EtdUnitService dsoService) {
        super(dsoService);
        this.etdunitService = dsoService;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected EtdUnitRest createAndReturn(Context context)
            throws AuthorizeException, RepositoryMethodNotImplementedException {

        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        EtdUnitRest etdunitRest;

        try {
            etdunitRest = mapper.readValue(req.getInputStream(), EtdUnitRest.class);
        } catch (IOException excIO) {
            throw new UnprocessableEntityException("error parsing the body ..." + excIO.getMessage());
        }

        if (isBlank(etdunitRest.getName())) {
            throw new EtdUnitNameNotProvidedException();
        }

        EtdUnit etdunit;
        try {
            etdunit = etdunitService.create(context);
            etdunit.setName(etdunitRest.getName());
            etdunitService.update(context, etdunit);
            metadataConverter.setMetadata(context, etdunit, etdunitRest.getMetadata());
        } catch (SQLException excSQL) {
            throw new RuntimeException(excSQL.getMessage(), excSQL);
        }

        return converter.toRest(etdunit, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public EtdUnitRest findOne(Context context, UUID id) {
        EtdUnit etdunit = null;
        try {
            etdunit = etdunitService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (etdunit == null) {
            return null;
        }
        return converter.toRest(etdunit, utils.obtainProjection());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public Page<EtdUnitRest> findAll(Context context, Pageable pageable) {
        try {
            long total = etdunitService.countTotal(context);
            List<EtdUnit> etdunits = etdunitService.findAll(context, pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(etdunits, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, UUID id,
            Patch patch) throws AuthorizeException, SQLException {
        patchDSpaceObject(apiCategory, model, id, patch);
    }

    /**
     * Find the etdunits matching the query parameter. The search is delegated to
     * the
     * {@link EtdUnitService#search(Context, String, int, int)} method
     *
     * @param query    is the *required* query string
     * @param pageable contains the pagination information
     * @return a Page of EtdUnitRest instances matching the user query
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @SearchRestMethod(name = "byMetadata")
    public Page<EtdUnitRest> findByMetadata(@Parameter(value = "query", required = true) String query,
            Pageable pageable) {

        try {
            Context context = obtainContext();
            long total = etdunitService.searchResultCount(context, query);
            List<EtdUnit> etdunits = etdunitService.search(context, query, Math.toIntExact(pageable.getOffset()),
                    Math.toIntExact(pageable.getPageSize()));
            return converter.toRestPage(etdunits, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<EtdUnitRest> getDomainClass() {
        return EtdUnitRest.class;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, UUID uuid) throws AuthorizeException {
        EtdUnit etdunit;
        try {
            etdunit = etdunitService.find(context, uuid);
            if (etdunit == null) {
                throw new ResourceNotFoundException(
                        EtdUnitRest.CATEGORY + "." + EtdUnitRest.NAME
                                + " with id: " + uuid + " not found");
            }
            try {
                final DSpaceObject parentObject = etdunitService.getParentObject(context, etdunit);
                if (parentObject != null) {
                    throw new UnprocessableEntityException(
                            "This etdunit cannot be deleted"
                                    + " as it has a parent " + parentObject.getType()
                                    + " with id " + parentObject.getID());
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            etdunitService.delete(context, etdunit);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
