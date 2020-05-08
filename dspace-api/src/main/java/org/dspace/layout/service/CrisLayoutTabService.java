/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service;

import java.sql.SQLException;

import org.dspace.content.EntityType;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.service.DSpaceCRUDService;

/**
 * 
 * @author Danilo Di Nuzzo (danilo dot dinuzzo at 4science dot it)
 *
 */
public interface CrisLayoutTabService extends DSpaceCRUDService<CrisLayoutTab> {

    /**
     * Create a new tab object
     * 
     * @param context the dspace application context
     * @param tab
     * @return a CrisLayoutTab instance of the object created
     * @throws SQLException
     */
    public CrisLayoutTab create(Context context, CrisLayoutTab tab) throws SQLException;

    /**
     * Create a new tab with EntityType and priority (both properties are required)
     * @param context the dspace application context
     * @param eType EntityType of new Tab
     * @param priority Priority of new Tab
     * @return a CrisLayoutTab instance
     * @throws SQLException
     */
    public CrisLayoutTab create(Context context, EntityType eType, Integer priority) throws SQLException;

}
