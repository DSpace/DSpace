/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.hateoas.EPersonResource;
import org.dspace.app.rest.repository.EPersonRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@RestController
@RequestMapping("/api/" + EPersonRest.CATEGORY + "/" + EPersonRest.PLURAL_NAME)
public class EPersonRegistrationRestController {

    @Autowired
    private EPersonRestRepository ePersonRestRepository;

    @Autowired
    private ConverterService converter;

    /**
     * This method will merge the data coming from a {@link org.dspace.eperson.RegistrationData} into the current
     * logged-in user.
     * <br/>
     * The request must have an empty body, and a token parameter should be provided:
     * <pre>
     *  <code>
     *   curl -X POST http://${dspace.url}/api/eperson/epersons/${id-eperson}?token=${token}&override=${metadata-fields}
     *        -H "Content-Type: application/json"
     *        -H "Authorization: Bearer ${bearer-token}"
     *  </code>
     * </pre>
     * @param request httpServletRequest incoming
     * @param uuid uuid of the eperson
     * @param token registration token
     * @param override fields to override inside from the registration data to the eperson
     * @return
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{uuid}")
    public ResponseEntity<RepresentationModel<?>> post(
        HttpServletRequest request,
        @PathVariable String uuid,
        @RequestParam @NotNull String token,
        @RequestParam(required = false) List<String> override
    ) throws Exception {
        Context context = ContextUtil.obtainContext(request);
        try {
            context.turnOffAuthorisationSystem();
            EPersonRest epersonRest =
                ePersonRestRepository.mergeFromRegistrationData(context, UUID.fromString(uuid), token, override);
            EPersonResource resource = converter.toResource(epersonRest);
            return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), resource);
        } catch (Exception e) {
            throw e;
        } finally {
            context.restoreAuthSystemState();
        }
    }

}
