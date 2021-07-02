/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

/**
 * Interface containing the simple CRUD methods so we don't have to add them
 * over and again to every service which requires these methods.
 *
 * @author kevinvandevelde at atmire.com
 * @param <T> Concrete object type.
 */
public interface DSpaceCRUDService<T> {

    public T create(Context context) throws SQLException, AuthorizeException;

    public T find(Context context, int id) throws SQLException;

    /**
     * Persist a model object.
     *
     * @param context
     * @param t object to be persisted.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     */
    public void update(Context context, T t) throws SQLException, AuthorizeException;


    /**
     * Persist a collection of model objects.
     *
     * @param context
     * @param t object to be persisted.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     */
    public void update(Context context, List<T> t) throws SQLException, AuthorizeException;

    public void delete(Context context, T t) throws SQLException, AuthorizeException;
}
