/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.ContextUtil.obtainContext;

import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.app.rest.repository.ClarinLicenseRestRepository;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for import licenses into database.
 * Endpoint: /api/licenses/import/{value}
 * This controller can:
 *     - import labels in json format into database (POST /api/licenses/import/labels)
 *     - import extended mapping in json format - create mapped dictionary (POST /api/licenses/import/extendedMapping)
 *     - import licenses in json format into database (POST /api/licenses/import/licenses)
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
@RestController
@RequestMapping("/api/clarin/import")
public class ClarinLicenseImportController {
    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(ClarinLicenseImportController.class);

    @Autowired
    private EPersonService ePersonService;
    @Autowired
    private ClarinLicenseRestRepository clarinLicenseRestRepository;

    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(method = RequestMethod.POST, path = "/license")
    public ClarinLicenseRest importLicense(HttpServletRequest request)
            throws AuthorizeException, SQLException {
        Context context = obtainContext(request);
        if (Objects.isNull(context)) {
            throw new RuntimeException("Context is null!");
        }
        //get param
        UUID epersonUUID = UUID.fromString(request.getParameter("eperson"));
        EPerson ePerson = ePersonService.find(context, epersonUUID);

        //set current user to eperson and create license
        EPerson currUser = context.getCurrentUser();
        context.setCurrentUser(ePerson);
        //turn off authorization
        context.turnOffAuthorisationSystem();
        ClarinLicenseRest clarinLicenseRest = clarinLicenseRestRepository.createAndReturn();
        context.restoreAuthSystemState();
        //set back current use
        context.setCurrentUser(currUser);

        context.complete();
        return clarinLicenseRest;
    }


}