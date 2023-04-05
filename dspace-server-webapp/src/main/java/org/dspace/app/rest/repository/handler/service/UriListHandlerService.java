/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.handler.service;

import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.repository.handler.UriListHandler;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * This class is a wrapper Service class for the {@link UriListHandler} objects. It will find the right one and try to
 * execute it for the given arguments
 */
@Component
public class UriListHandlerService {

    private final static Logger log = LogManager.getLogger();

    @Autowired
    private List<UriListHandler> uriListHandlers;

    /**
     * This method will take the UriList, the request, relevant DSpace context and the class of the object to be handled
     * It'll then loop over all the UriListHandlers defined within the codebase and first check if it supports the given
     * method and the urilist. If the handler supports this, it'll then validate the input and only if it's valid, it'll
     * execute the handle method and perform the logic
     *
     * @param context   The relevant DSpace context
     * @param request   The current active Request
     * @param uriList   The list of Strings representing the UriList to be handled
     * @param clazz     The class to be hadled
     * @param <T>       The class to be returned, same as the class parameter above
     * @return          The object that was handled through this method
     */
    public <T> T handle(Context context, HttpServletRequest request, List<String> uriList, Class<T> clazz)
        throws SQLException, AuthorizeException {

        // Loop all the uriListHandlers
        for (UriListHandler uriListHandler : uriListHandlers) {
            // Does the class support the given uri list and the request method
            if (uriListHandler.supports(uriList, request.getMethod(), clazz)) {
                // Can the class handle the given uri list and can the given class, params and authorization be handled
                if (uriListHandler.validate(context, request, uriList)) {
                    // If all these things succeed, call handle
                    return (T) uriListHandler.handle(context, request, uriList);
                } else {
                    throw new DSpaceBadRequestException("The input given to the UriListHandler was invalid");
                }
            }
        }

        throw new DSpaceBadRequestException("No UriListHandler was found that supports the inputs given");
    }

}