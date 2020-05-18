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
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.dspace.service.DSpaceCRUDService;

/**
 * Interface of service to manage CrisLayoutFieldBitstream component of layout
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public interface CrisLayoutFieldBitstreamService extends DSpaceCRUDService<CrisLayoutFieldBitstream> {

    /**
     * This method stores in the database a CrisLayoutFieldBitstream {@link CrisLayoutFieldBitstream} instance.
     * @param ctx The relevant DSpace Context
     * @param stream an instance of CrisLayoutFieldBitstream
     * @return the stored instance of CrisLayoutFieldBitstream
     * @throws SQLException An exception that provides information on a database errors.
     */
    public CrisLayoutFieldBitstream create(Context ctx, CrisLayoutFieldBitstream stream) throws SQLException;

}
