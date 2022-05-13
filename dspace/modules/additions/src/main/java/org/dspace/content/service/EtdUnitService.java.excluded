/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.EtdUnit;
import org.dspace.core.Context;

/**
 * Service interface class for the EtdUnit object.
 * The implementation of this class is responsible for all business logic calls for the EtdUnit object and is autowired by spring
 *
 * @author mohideen at umd.edu
 */
public interface EtdUnitService extends DSpaceObjectService<EtdUnit>, DSpaceObjectLegacySupportService<EtdUnit>
{
    /**
     * Create a new etdunit, with a new ID.
     *
     * @param parent parent etdunit
     * @param context
     *            DSpace context object
     *
     * @return the newly created etdunit
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public EtdUnit create(Context context) throws SQLException, AuthorizeException;

    /**
     * find the etdunit by its ID
     *
     * @param context
     * @param id
     *
     * @return the etdunit with the given id
     * @throws SQLException if database error
     */
    public EtdUnit find(Context context, UUID id) throws SQLException;

    /**
     * Find the etdunit by its name - assumes name is unique
     *
     * @param context
     * @param name
     *
     * @return EtdUnit
     */
    public EtdUnit findByName(Context context, String name) throws SQLException;

    /**
     * Finds all etdunits in the site
     *
     * @param context
     *            DSpace context
     *
     * @return list of all etdunits in the site
     */
    public List<EtdUnit> findAll(Context context) throws SQLException;

    /**
     * Finds all etdunits that are mapped to the collection
     *
     * @param context
     *            DSpace context
     * @param collction
     *            collection
     *
     * @return list of all etdunits mapped to the given collection
     */
    public List<EtdUnit> findAllByCollection(Context context, Collection collection) throws SQLException;

     /**
     * Find the etdunits that match the search query across etdunit_id or name
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     *
     * @return array of EtdUnit objects
     */
    public List<EtdUnit> search(Context context, String query) throws SQLException;

    /**
     * Find the etdunits that match the search query across etdunit_id or name
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * @param offset
     *            Inclusive offset
     * @param limit
     *            Maximum number of matches returned
     *
     * @return list of EtdUnit objects
     */
    public List<EtdUnit> search(Context context, String query, int offset, int limit) throws SQLException;

    /**
     * Returns the total number of etdunits returned by a specific query,
     * without the overhead of creating the EtdUnit objects to store the
     * results.
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     *
     * @return the number of etdunits mathching the query
     */
    public int searchResultCount(Context context, String query) throws SQLException;

   
    /**
     * Return an list of collections of this etdunit and its subcommunities
     *
     * @param context context
     * @param etdunit etdunit
     * @return an array of collections
     * @throws SQLException if database error
     */

    public List<Collection> getAllCollections(Context context, EtdUnit etdunit)
            throws SQLException;


    /**
     * Add an exisiting collection to the etdunit
     *
     * @param context context
     * @param etdunit etdunit
     * @param collection
     *            collection to add
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public void addCollection(Context context, EtdUnit etdunit, Collection collection)
            throws SQLException, AuthorizeException;

    /**
     * Remove a collection. If it only belongs to one parent etdunit,
     * then it is permanently deleted. If it has more than one parent etdunit,
     * it is simply unmapped from the current etdunit.
     *
     * @param context context
     * @param c collection to remove
     * @param etdunit etdunit
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException if IO error
     */
    public void removeCollection(Context context, EtdUnit etdunit, Collection c)
            throws SQLException, AuthorizeException;

    /**
     * Returns true or false based on whether a given collection is a member
     * to the etdunit.
     * 
     * @param etdunit etdunit
     * @param collection collection
     * 
     * @return true if collection is a member of the etdunit, false otherwise
     */
    public boolean isMember(EtdUnit etdunit, Collection collection);

    /**
     * return TRUE if context's user can edit etdunit, false otherwise
     *
     * @param context context
     * @return boolean true = current user can edit etdunit
     * @throws SQLException if database error
     */
    public boolean canEditBoolean(Context context) throws SQLException;

    public void canEdit(Context context) throws AuthorizeException, SQLException;

    int countTotal(Context context) throws SQLException;
    
}
