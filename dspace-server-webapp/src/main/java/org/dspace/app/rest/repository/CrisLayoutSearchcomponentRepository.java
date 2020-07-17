/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;

import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.CrisLayoutSearchComponentRest;
import org.dspace.content.EntityType;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component(CrisLayoutSearchComponentRest.CATEGORY + "." + CrisLayoutSearchComponentRest.NAME)
public class CrisLayoutSearchcomponentRepository extends DSpaceRestRepository<CrisLayoutSearchComponentRest, String> {

    @Autowired
    private CrisLayoutBoxService service;

    @Override
    public Page<CrisLayoutSearchComponentRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not Implemented!", "");
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#findOne(org.dspace.core.Context, java.io.Serializable)
     */
    @Override
    @PreAuthorize("permitAll")
    public CrisLayoutSearchComponentRest findOne(Context context, String id) {
        String boxConfigurationId = null;
        CrisLayoutSearchComponentRest rVal = null;

        try {
            CrisLayoutBox box = service.findByShortname(context, id);
            if (box != null && box.getType() != null) {
                rVal = new CrisLayoutSearchComponentRest();
                rVal.setId(box.getShortname());

                boxConfigurationId = box.getType();
                EntityType entity = box.getEntitytype();
                if (entity != null) {
                    boxConfigurationId += "." + entity.getLabel();
                }
                boxConfigurationId += "." + box.getShortname();
                rVal.setConfiguration(boxConfigurationId);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return rVal;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#getDomainClass()
     */
    @Override
    public Class<CrisLayoutSearchComponentRest> getDomainClass() {
        return CrisLayoutSearchComponentRest.class;
    }
}
