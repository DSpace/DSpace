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
import org.dspace.layout.CrisLayoutBitstream;
import org.dspace.service.DSpaceCRUDService;

public interface CrisLayoutBitstreamService extends DSpaceCRUDService<CrisLayoutBitstream> {

    /**
     * Create a new layout bitstream
     * @param ctx
     * @param stream
     * @return
     * @throws SQLException
     */
    public CrisLayoutBitstream create(Context ctx, CrisLayoutBitstream stream) throws SQLException;

}
