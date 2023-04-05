/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.Serializable;
import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

/**
 * This interface must be implemented by all the rest repository that need to
 * provide access to the DSpace API model objects corresponding to the REST
 * resources that it manages
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 * @param <T> the ReloadableEntity type
 * @param <PK> the primary key type
 */
public interface ReloadableEntityObjectRepository<T extends ReloadableEntity<PK>,
    PK extends Serializable> {

    /**
     * 
     * @param context the DSpace context
     * @param id      the primary key shared between the rest and dspace api object
     * @return the dspace api model object related to the specified id
     * @throws SQLException if a database error occurs
     */
    T findDomainObjectByPk(Context context, PK id) throws SQLException;

    /**
     * 
     * @return the class of the primary key
     */
    Class<PK> getPKClass();
}
