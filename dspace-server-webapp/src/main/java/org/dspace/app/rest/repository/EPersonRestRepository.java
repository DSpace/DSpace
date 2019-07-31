/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.EPersonConverter;
import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.hateoas.EPersonResource;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.EPersonPatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;


/**
 * This is the repository responsible to manage EPerson Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(EPersonRest.CATEGORY + "." + EPersonRest.NAME)
public class EPersonRestRepository extends DSpaceObjectRestRepository<EPerson, EPersonRest> {

    @Autowired
    AuthorizeService authorizeService;

    private final EPersonService es;

    @Autowired
    MetadataConverter metadataConverter;

    @Autowired
    EPersonPatch epersonPatch;

    public EPersonRestRepository(EPersonService dsoService,
                                 EPersonConverter dsoConverter,
                                 EPersonPatch dsoPatch) {
        super(dsoService, dsoConverter, dsoPatch);
        this.es = dsoService;
    }

    @Override
    protected EPersonRest createAndReturn(Context context)
            throws AuthorizeException {
        // this need to be revisited we should receive an EPersonRest as input
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        EPersonRest epersonRest = null;
        try {
            epersonRest = mapper.readValue(req.getInputStream(), EPersonRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("error parsing the body... maybe this is not the right error code");
        }

        EPerson eperson = null;
        try {
            eperson = es.create(context);

            // this should be probably moved to the converter (a merge method?)
            eperson.setCanLogIn(epersonRest.isCanLogIn());
            eperson.setRequireCertificate(epersonRest.isRequireCertificate());
            eperson.setEmail(epersonRest.getEmail());
            eperson.setNetid(epersonRest.getNetid());
            if (epersonRest.getPassword() != null) {
                es.setPassword(eperson, epersonRest.getPassword());
            }
            es.update(context, eperson);
            metadataConverter.setMetadata(context, eperson, epersonRest.getMetadata());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return dsoConverter.convert(eperson);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'EPERSON', 'READ')")
    public EPersonRest findOne(Context context, UUID id) {
        EPerson eperson = null;
        try {
            eperson = es.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (eperson == null) {
            return null;
        }
        return dsoConverter.fromModel(eperson);
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<EPersonRest> findAll(Context context, Pageable pageable) {
        List<EPerson> epersons = null;
        int total = 0;
        try {
            if (!authorizeService.isAdmin(context)) {
                throw new RESTAuthorizationException(
                        "The EPerson collection endpoint is reserved to system administrators");
            }
            total = es.countTotal(context);
            epersons = es.findAll(context, EPerson.EMAIL, pageable.getPageSize(), pageable.getOffset());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<EPersonRest> page = new PageImpl<EPerson>(epersons, pageable, total).map(dsoConverter);
        return page;
    }

    /**
     * Find the epersons matching the query q parameter. The search is delegated to the
     * {@link EPersonService#search(Context, String, int, int)} method
     *
     * @param q
     *            is the *required* query string
     * @param pageable
     *            contains the pagination information
     * @return a Page of EPersonRest instances matching the user query
     */
    @SearchRestMethod(name = "byName")
    public Page<EPersonRest> findByName(@Parameter(value = "q", required = true) String q,
            Pageable pageable) {
        List<EPerson> epersons = null;
        int total = 0;
        try {
            Context context = obtainContext();
            epersons = es.search(context, q, pageable.getOffset(), pageable.getOffset() + pageable.getPageSize());
            total = es.searchResultCount(context, q);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<EPersonRest> page = new PageImpl<EPerson>(epersons, pageable, total).map(dsoConverter);
        return page;
    }

    /**
     * Find the eperson with the provided email address if any. The search is delegated to the
     * {@link EPersonService#findByEmail(Context, String)} method
     *
     * @param email
     *            is the *required* email address
     * @param pageable
     *            contains the pagination information
     * @return a Page of EPersonRest instances matching the user query
     */
    @SearchRestMethod(name = "byEmail")
    public EPersonRest findByEmail(@Parameter(value = "email", required = true) String email) {
        EPerson eperson = null;
        try {
            Context context = obtainContext();
            eperson = es.findByEmail(context, email);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (eperson == null) {
            return null;
        }
        return dsoConverter.fromModel(eperson);
    }

    @Override
    @PreAuthorize("hasPermission(#uuid, 'EPERSON', #patch)")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, UUID uuid,
                         Patch patch) throws AuthorizeException, SQLException {
        patchDSpaceObject(apiCategory, model, uuid, patch);
    }

    @Override
    protected void updateDSpaceObject(EPerson ePerson, EPersonRest ePersonRest)
            throws AuthorizeException, SQLException {
        super.updateDSpaceObject(ePerson, ePersonRest);

        Context context = obtainContext();
        if (ePersonRest.getPassword() != null) {
            es.setPassword(ePerson, ePersonRest.getPassword());
        }
        if (ePersonRest.isRequireCertificate() != ePerson.getRequireCertificate()) {
            ePerson.setRequireCertificate(ePersonRest.isRequireCertificate());
        }
        if (ePersonRest.isCanLogIn() != ePerson.canLogIn()) {
            ePerson.setCanLogIn(ePersonRest.isCanLogIn());
        }
        if (!Objects.equals(ePersonRest.getEmail(), ePerson.getEmail())) {
            ePerson.setEmail(ePersonRest.getEmail());
        }
        if (!Objects.equals(ePersonRest.getNetid(), ePerson.getNetid())) {
            ePerson.setNetid(ePersonRest.getNetid());
        }

        es.update(context, ePerson);
    }

    @Override
    protected void delete(Context context, UUID id) throws AuthorizeException {
        EPerson eperson = null;
        try {
            eperson = es.find(context, id);
            List<String> constraints = es.getDeleteConstraints(context, eperson);
            if (constraints != null && constraints.size() > 0) {
                throw new UnprocessableEntityException(
                        "The eperson cannot be deleted due to the following constraints: "
                                + StringUtils.join(constraints, ", "));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        try {
            es.delete(context, eperson);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<EPersonRest> getDomainClass() {
        return EPersonRest.class;
    }

    @Override
    public EPersonResource wrapResource(EPersonRest eperson, String... rels) {
        return new EPersonResource(eperson, utils, rels);
    }

}
