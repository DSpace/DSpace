/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface containing the simple CRUD methods so we don't have to add them over and again to every service which
 * requires these methods
 *
 * @author kevinvandevelde at atmire.com
 */
public interface DSpaceCRUDService<T> {

    public T create(Context context) throws SQLException, AuthorizeException;

    public T find(Context context, int id) throws SQLException;

    public void update(Context context, T t) throws SQLException, AuthorizeException;

    public void update(Context context, List<T> t) throws SQLException, AuthorizeException;

    public void delete(Context context, T t) throws SQLException, AuthorizeException;
}
