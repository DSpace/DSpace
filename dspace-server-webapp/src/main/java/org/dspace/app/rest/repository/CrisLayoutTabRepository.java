/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;

import org.dspace.app.rest.model.CrisLayoutTabRest;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.service.CrisLayoutTabService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * @author Danilo Di Nuzzo (danilo dot dinuzzo at 4science dot it)
 *
 */
@Component(CrisLayoutTabRest.CATEGORY + "." + CrisLayoutTabRest.NAME)
public class CrisLayoutTabRepository extends DSpaceRestRepository<CrisLayoutTabRest, Integer>
    implements ReloadableEntityObjectRepository<CrisLayoutTab, Integer> {

    private final CrisLayoutTabService service;

    public CrisLayoutTabRepository(CrisLayoutTabService service) {
        this.service = service;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#findOne(org.dspace.core.Context, java.io.Serializable)
     */
    @Override
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
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#getDomainClass()
     */
    @Override
    public Class<CrisLayoutTabRest> getDomainClass() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.ReloadableEntityObjectRepository#findDomainObjectByPk
     * (org.dspace.core.Context, java.io.Serializable)
     */
    @Override
    public CrisLayoutTab findDomainObjectByPk(Context context, Integer id) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.ReloadableEntityObjectRepository#getPKClass()
     */
    @Override
    public Class<Integer> getPKClass() {
        // TODO Auto-generated method stub
        return null;
    }

}
