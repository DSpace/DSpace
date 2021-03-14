/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.handler;

import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

/**
 * This is an interface to be implemented by classes that want to handle a UriList call
 * @param <T>   the class of the object that's returned by the handle method
 */
public interface UriListHandler<T> {

    /**
     * This method will take the UriList and method as input and verify whether the implementing UriListHandler
     * can handle this input or not
     * @param uriList   The list of UriList Strings to be checked if they're supported
     * @param method    The request method to be checked if it's supported
     * @param clazz     The class to be returned by the handle method
     * @return          A boolean indicating whether the implementing UriListHandler can handle this input
     */
    boolean supports(List<String> uriList, String method, Class clazz);

    /**
     * This method will take all the required input and validate them to see if there are any issues before
     * calling the handle method
     * @param context   The relevant DSpace context
     * @param request   The current request
     * @param uriList   The list of UriList Strings
     * @return          A boolean indicating whether all this input is valid for the implementing UriListHandler
     */
    boolean validate(Context context, HttpServletRequest request, List<String> uriList)
        throws AuthorizeException;

    /**
     * This method will perform the actual handle logic
     * @param context   The relevant DSpace context
     * @param request   The current request
     * @param uriList   The list of UriList Strings
     * @return          The object of class T that was handled
     */
    T handle(Context context, HttpServletRequest request, List<String> uriList) throws SQLException, AuthorizeException;

}
