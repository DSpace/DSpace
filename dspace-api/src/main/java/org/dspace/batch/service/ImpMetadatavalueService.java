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

import org.dspace.batch.ImpMetadatavalue;
import org.dspace.batch.ImpRecord;
import org.dspace.core.Context;

/***
 * Interface used to access ImpMetadatavalue entities.
 * 
 * @See {@link org.dspace.batch.ImpMetadatavalue}
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 *
 */
public interface ImpMetadatavalueService {
    /**
     * Create a new ImpMetadatavalue object
     * 
     * @param context          The relevant DSpace Context.
     * @param impMetadatavalue The initial data of ImpMetadatavalue
     * @return the created ImpMetadatavalue object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public ImpMetadatavalue create(Context context, ImpMetadatavalue impMetadatavalue) throws SQLException;

    /***
     * Insert a metadata value.
     * 
     * @param impMetadatavalue
     * @param schema           The schema
     * @param element          The element
     * @param qualifier        The qualifier
     * @param language         The language
     * @param value            The metadata value
     */
    public void setMetadata(ImpMetadatavalue impMetadatavalue, String schema, String element, String qualifier,
            String language, String value);

    /***
     * Search by import Id
     * 
     * @param context   The relevant DSpace Context
     * @param impRecord The ImpRecord object
     * @return the list of founded ImpMetadatavalue objects
     * @throws SQLException
     */
    public List<ImpMetadatavalue> searchByImpRecordId(Context context, ImpRecord impRecord) throws SQLException;

    /**
     * Save a ImpMetadatavalue object
     *
     * @param context          The relevant DSpace Context.
     * @param impMetadatavalue The ImpMetadatavalue object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public void update(Context context, ImpMetadatavalue impMetadatavalue) throws SQLException;

    /**
     * Save a ImpMetadatavalue object
     *
     * @param context          The relevant DSpace Context.
     * @param impMetadatavalue The ImpMetadatavalue object
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    public void delete(Context context, ImpMetadatavalue impMetadatavalue) throws SQLException;
}
