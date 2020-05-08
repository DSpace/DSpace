/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutField;
import org.dspace.service.DSpaceCRUDService;

public interface CrisLayoutFieldService extends DSpaceCRUDService<CrisLayoutField> {

    /**
     * Create a new layout field
     * @param context
     * @param field
     * @return
     * @throws SQLException
     */
    public CrisLayoutField create(Context context, CrisLayoutField field) throws SQLException;

}
