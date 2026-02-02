/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.correctiontype.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.correctiontype.CorrectionType;

/**
 * Service interface class for the CorrectionType object.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public interface CorrectionTypeService {

    /**
     * Retrieves a CorrectionType object from the system based on a unique identifier.
     * 
     * @param id The unique identifier of the CorrectionType object to be retrieved.
     * @return   The CorrectionType object corresponding to the provided identifier,
     *           or null if no object is found.
     */
    public CorrectionType findOne(String id);

    /**
     * Retrieves a list of all CorrectionType objects available in the system.
     * 
     * @return Returns a List containing all CorrectionType objects in the system.
     */
    public List<CorrectionType> findAll();

    /**
     * Retrieves a list of CorrectionType objects related to the provided Item.
     * 
     * @param context               Current DSpace session.
     * @param item                  Target item
     * @throws AuthorizeException   If authorize error
     * @throws SQLException         If a database error occurs during the operation.
     */
    public List<CorrectionType> findByItem(Context context, Item item) throws AuthorizeException, SQLException;

    /**
     * Retrieves a CorrectionType object associated with a specific topic.
     * 
     * @param topic  The topic for which the CorrectionType object is to be retrieved.
     */
    public CorrectionType findByTopic(String topic);

}
