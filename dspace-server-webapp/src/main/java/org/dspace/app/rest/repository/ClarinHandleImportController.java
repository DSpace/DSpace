/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.dspace.app.rest.utils.ContextUtil.obtainContext;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.HandleRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.handle.Handle;
import org.dspace.handle.service.HandleClarinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Specialized controller created for Clarin-Dspace import handle without dspace object.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
@RestController
@RequestMapping("/api/clarin/import")
public class ClarinHandleImportController {
    @Autowired
    private HandleClarinService handleClarinService;
    @Autowired
    private ConverterService converter;
    @Autowired
    private Utils utils;

    /**
     * Endpoint for import handle without dspace object.
     * The mapping for requested endpoint, for example
     * <pre>
     * {@code
     * https://<dspace.server.url>/api/clarin/import/handle
     * }
     * </pre>
     * @param request request
     * @return handle converted to the rest
     * @throws AuthorizeException if authorization error
     * @throws SQLException       if database error
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(method = RequestMethod.POST, value = "/handle")
    public HandleRest importHandle(HttpServletRequest request) throws AuthorizeException, SQLException {
        Context context = obtainContext(request);
        if (Objects.isNull(context)) {
            throw new RuntimeException("Context is null!");
        }
        ObjectMapper mapper = new ObjectMapper();
        HandleRest handleRest = null;
        try {
            ServletInputStream input = request.getInputStream();
            handleRest = mapper.readValue(input, HandleRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body", e1);
        }

        Handle handle;
        try {
            handle = handleClarinService.createHandle(context, handleRest.getHandle());
            //set handle attributes
            handle.setResourceTypeId(handleRest.getResourceTypeID());
            // Save created handle
            handleClarinService.save(context, handle);
        } catch (SQLException e) {
            throw new RuntimeException("Error while trying to create new Handle and update it", e);
        }
        handleRest = converter.toRest(handle, utils.obtainProjection());
        context.commit();
        return handleRest;
    }
}
