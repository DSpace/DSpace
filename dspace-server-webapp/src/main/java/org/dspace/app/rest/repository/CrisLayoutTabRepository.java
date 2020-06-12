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
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.CrisLayoutTabConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.CrisLayoutTabRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.ResourcePatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.service.CrisLayoutTabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
/**
 * This is the repository responsible to manage CrisLayoutTab Rest object
 * 
 * @author Danilo Di Nuzzo (danilo dot dinuzzo at 4science dot it)
 *
 */
@Component(CrisLayoutTabRest.CATEGORY + "." + CrisLayoutTabRest.NAME)
public class CrisLayoutTabRepository extends DSpaceRestRepository<CrisLayoutTabRest, Integer>
    implements ReloadableEntityObjectRepository<CrisLayoutTab, Integer> {

    private final CrisLayoutTabService service;

    @Autowired
    private CrisLayoutTabConverter tabConverter;

    @Autowired
    private ResourcePatch<CrisLayoutTab> resourcePatch;

    public CrisLayoutTabRepository(CrisLayoutTabService service) {
        this.service = service;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#findOne(org.dspace.core.Context, java.io.Serializable)
     */
    @Override
    @PreAuthorize("permitAll")
    public CrisLayoutTabRest findOne(Context context, Integer id) {
        CrisLayoutTab tab = null;
        try {
            tab = service.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if ( tab == null ) {
            return null;
        }
        return converter.toRest(tab, utils.obtainProjection());
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#findAll
     * (org.dspace.core.Context, org.springframework.data.domain.Pageable)
     */
    @Override
    public Page<CrisLayoutTabRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @SearchRestMethod(name = "findByItem")
    public Page<CrisLayoutTabRest> findByItem(
            @Parameter(value = "uuid", required = true) String itemUuid, Pageable pageable) {
        Context context = obtainContext();
        List<CrisLayoutTab> tabList = null;
        Long totalRow = null;
        try {
            tabList = service.findByItem(
                context,
                itemUuid);
            totalRow = Long.valueOf(tabList.size());
            int lastIndex = (pageable.getPageNumber() + 1) * pageable.getPageSize();
            tabList = tabList.subList(
                    pageable.getPageNumber() * pageable.getPageSize(),
                    (tabList.size() < lastIndex) ? tabList.size() : lastIndex );
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRestPage(tabList, pageable, totalRow, utils.obtainProjection());
    }

    @SearchRestMethod(name = "findByEntityType")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<CrisLayoutTabRest> findByEntityType(
            @Parameter(value = "type", required = true) String type, Pageable pageable) {
        Context context = obtainContext();
        List<CrisLayoutTab> tabList = null;
        Long totalRow = null;
        try {
            totalRow = service.countByEntityType(context, type);
            tabList = service.findByEntityType(
                context,
                type,
                pageable.getPageSize(),
                (pageable.getPageNumber() * pageable.getPageSize()) );
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (tabList == null) {
            return null;
        }
        return converter.toRestPage(tabList, pageable, totalRow, utils.obtainProjection());
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#getDomainClass()
     */
    @Override
    public Class<CrisLayoutTabRest> getDomainClass() {
        return CrisLayoutTabRest.class;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.ReloadableEntityObjectRepository#findDomainObjectByPk
     * (org.dspace.core.Context, java.io.Serializable)
     */
    @Override
    public CrisLayoutTab findDomainObjectByPk(Context context, Integer id) throws SQLException {
        return service.find(context, id);
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.ReloadableEntityObjectRepository#getPKClass()
     */
    @Override
    public Class<Integer> getPKClass() {
        return Integer.class;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected CrisLayoutTabRest createAndReturn(Context context) {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        CrisLayoutTabRest tabRest = null;
        try {
            tabRest = mapper.readValue(req.getInputStream(), CrisLayoutTabRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("error parsing the body... maybe this is not the right error code");
        }

        CrisLayoutTab tab = tabConverter.toModel(context, tabRest);
        try {
            service.create(context, tab);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRest(tab, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public void patch(
            Context context, HttpServletRequest request, String apiCategory, String model, Integer id,
            Patch patch)
            throws UnprocessableEntityException, DSpaceBadRequestException {
        CrisLayoutTab tab = null;
        try {
            tab = service.find(context, id);
            if (tab == null) {
                throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
            }
            resourcePatch.patch(obtainContext(), tab, patch.getOperations());
            service.update(context, tab);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, Integer id) throws AuthorizeException {
        try {
            CrisLayoutTab tab = service.find(context, id);
            if (tab == null) {
                throw new ResourceNotFoundException("CrisLayoutTab with id: " + id + " not found");
            }
            service.delete(context, tab);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
