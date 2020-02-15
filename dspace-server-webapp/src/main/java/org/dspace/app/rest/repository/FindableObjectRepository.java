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
import org.dspace.discovery.FindableObject;

/**
 * This interface must be implemented by all the rest repository that deal with representation of {@link FindableObject}
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 * @param <F>
 *            the FindableObject type
 * @param <PK>
 *            the primary key type
 */
public interface FindableObjectRepository<F extends FindableObject<PK>, PK extends Serializable> {
    F findDomainObjectByPk(Context context, PK id) throws SQLException;

    Class<PK> getPKClass();
}
