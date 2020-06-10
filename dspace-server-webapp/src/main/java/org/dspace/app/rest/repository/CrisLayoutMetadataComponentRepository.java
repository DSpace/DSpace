/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;

import org.dspace.app.rest.converter.CrisLayoutMetadataComponentConverter;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.CrisLayoutMetadataComponentRest;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage CrisLayoutMetadataComponent Rest object
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component(CrisLayoutMetadataComponentRest.CATEGORY + "." + CrisLayoutMetadataComponentRest.NAME)
public class CrisLayoutMetadataComponentRepository
    extends DSpaceRestRepository<CrisLayoutMetadataComponentRest, String>
    implements ReloadableEntityObjectRepository<CrisLayoutBox, Integer> {

    @Autowired
    private CrisLayoutBoxService service;

    @Autowired
    private CrisLayoutMetadataComponentConverter mcConverter;

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.ReloadableEntityObjectRepository#
     * findDomainObjectByPk(org.dspace.core.Context, java.io.Serializable)
     */
    @Override
    public CrisLayoutBox findDomainObjectByPk(Context context, Integer id) throws SQLException {
        return null;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.ReloadableEntityObjectRepository#getPKClass()
     */
    @Override
    public Class<Integer> getPKClass() {
        return Integer.class;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#findOne(org.dspace.core.Context, java.io.Serializable)
     */
    @Override
    @PreAuthorize("permitAll")
    public CrisLayoutMetadataComponentRest findOne(Context context, String shortname) {
        CrisLayoutMetadataComponentRest values = null;
        try {
            values = mcConverter.convert( service.findByShortname(context, shortname) );
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return values;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#
     * findAll(org.dspace.core.Context, org.springframework.data.domain.Pageable)
     */
    @Override
    public Page<CrisLayoutMetadataComponentRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not Implemented!", "");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#getDomainClass()
     */
    @Override
    public Class<CrisLayoutMetadataComponentRest> getDomainClass() {
        return CrisLayoutMetadataComponentRest.class;
    }

}
