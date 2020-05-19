/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import javax.mail.MessagingException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.authorization.AuthorizationFeatureService;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.RegistrationRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.SiteService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.AccountService;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This will be the Controller class that handles calls to the /api/eperson/registrations endpoints
 */
@RestController
@RequestMapping("/api/" + RegistrationRest.CATEGORY + "/" + RegistrationRest.NAME_PLURAL)
public class RegistrationRestController {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private SiteService siteService;

    @Autowired
    private ConverterService converterService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private EPersonService ePersonService;

    /**
     * This method will be used to either register a new user or to send forgotten password info in a mail.
     * It can be called by doing a POST request to the /api/eperson/registrations endpoint.
     * It'll create a RegistrationRest object from the inputstream in the request and it'll check whether the email
     * defined in that object is in the DB or not.
     * If it is in the db then we'll send the forgotten password info, if it wasn't in the database then we'll send
     * registration info.
     *
     * @param request   The current request
     * @param response  The current response
     * @return          An empty response containing a 201 status code
     * @throws SQLException If something goes wrong
     * @throws IOException  If something goes wrong
     * @throws MessagingException   If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<RepresentationModel<?>> register(HttpServletRequest request, HttpServletResponse response)
        throws SQLException, IOException, MessagingException, AuthorizeException {

        Context context = ContextUtil.obtainContext(request);
        ObjectMapper mapper = new ObjectMapper();
        RegistrationRest registrationRest;
        try {
            ServletInputStream input = request.getInputStream();
            registrationRest = mapper.readValue(input, RegistrationRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body.", e1);
        }
        if (StringUtils.isBlank(registrationRest.getEmail())) {
            throw new UnprocessableEntityException("The email cannot be omitted from the Registration endpoint");
        }
        EPerson eperson = ePersonService.findByEmail(context, registrationRest.getEmail());
        if (eperson != null) {
            if (!AuthorizeUtil.authorizeUpdatePassword(context, eperson.getEmail())) {
                throw new DSpaceBadRequestException("Password cannot be updated for the given EPerson with email: " +
                                                        eperson.getEmail());
            }
            accountService.sendForgotPasswordInfo(context, registrationRest.getEmail());
        } else {
            if (!AuthorizeUtil.authorizeNewAccountRegistration(context, request)) {
                throw new AccessDeniedException(
                    "Registration is disabled, you are not authorized to create a new Authorization");
            }
            accountService.sendRegistrationInfo(context, registrationRest.getEmail());
        }
        context.complete();
        return ControllerUtils.toEmptyResponse(HttpStatus.CREATED);
    }

}
