/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.batch.ImpBitstream;
import org.dspace.batch.ImpBitstreamMetadatavalue;
import org.dspace.core.Context;

/***
 * Interface used to access ImpBitstreamMetadatavalue entities.
 * 
 * @See {@link org.dspace.batch.ImpBitstreamMetadatavalue}
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 *
 */
public interface ImpBitstreamMetadatavalueService {
    /**
     * Create a new ImpBitstreamMetadatavalue object
     * 
     * @param context                   The relevant DSpace Context.
     * @param impBitstreamMetadatavalue The initial data of
     *                                  ImpBitstreamMetadatavalue
     * @return the created ImpBitstreamMetadatavalue object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public ImpBitstreamMetadatavalue create(Context context, ImpBitstreamMetadatavalue impBitstreamMetadatavalue)
            throws SQLException;

    /***
     * Insert a metadata value.
     * 
     * @param impBitstreamMetadatavalue
     * @param schema                    The schema
     * @param element                   The element
     * @param qualifier                 The qualifier
     * @param language                  The language
     * @param value                     The metadata value
     */
    public void setMetadata(ImpBitstreamMetadatavalue impBitstreamMetadatavalue, String schema, String element,
            String qualifier, String language, String value);

    /***
     * Search all ImpBitstreamMetadatavalue objects by import Bitstream Id
     * 
     * @param context      The relevant DSpace Context
     * @param impBitstream The ImpBitstream object
     * @return the list of found ImpBitstreamMetadatavalue objects
     * @throws SQLException
     */
    public List<ImpBitstreamMetadatavalue> searchByImpBitstream(Context context, ImpBitstream impBitstream)
            throws SQLException;

    /**
     * Save a ImpBitstreamMetadatavalue object
     *
     * @param context                   The relevant DSpace Context.
     * @param impBitstreamMetadatavalue The ImpBitstreamMetadatavalue object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public void update(Context context, ImpBitstreamMetadatavalue impBitstreamMetadatavalue) throws SQLException;

    /**
     * Delete a ImpBitstreamMetadatavalue object
     *
     * @param context                   The relevant DSpace Context.
     * @param impBitstreamMetadatavalue The ImpBitstreamMetadatavalue object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public void delete(Context context, ImpBitstreamMetadatavalue impBitstreamMetadatavalue) throws SQLException;
}
