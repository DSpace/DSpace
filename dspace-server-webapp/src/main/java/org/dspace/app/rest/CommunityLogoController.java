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
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.hateoas.BitstreamResource;
import org.dspace.app.rest.repository.CommunityRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * This RestController takes care of the creation and deletion of Communities' nested objects
 * This class will typically receive the UUID of a Community and it'll perform logic on its nested objects
 */
@RestController
@RequestMapping("/api/" + CommunityRest.CATEGORY + "/" + CommunityRest.PLURAL_NAME
    + CommunityLogoController.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/logo")
public class CommunityLogoController {

    /**
     * Regular expression in the request mapping to accept UUID as identifier
     */
    protected static final String REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID =
        "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}";

    @Autowired
    protected Utils utils;

    @Autowired
    private CommunityRestRepository communityRestRepository;

    @Autowired
    private CommunityService communityService;

    /**
     * This method will add a logo to the community.
     *
     * curl -X POST http://<dspace.server.url>/api/core/communities/1c11f3f1-ba1f-4f36-908a-3f1ea9a557eb/logo' \
     *  -XPOST -H 'Content-Type: multipart/form-data' \
     *  -H 'Authorization: Bearer eyJhbGciOiJI...' \
     *  -F "file=@Downloads/test.png"
     *
     * Example:
     * <pre>
     * {@code
     * curl -X POST http://<dspace.server.url>/api/core/communities/1c11f3f1-ba1f-4f36-908a-3f1ea9a557eb/logo' \
     *  -XPOST -H 'Content-Type: multipart/form-data' \
     *  -H 'Authorization: Bearer eyJhbGciOiJI...' \
     *  -F "file=@Downloads/test.png"
     * }
     * </pre>
     * @param request       The StandardMultipartHttpServletRequest that will contain the logo in its body
     * @param uuid          The UUID of the community
     * @return              The created bitstream
     * @throws SQLException If something goes wrong
     * @throws IOException  If something goes wrong
     * @throws AuthorizeException   If the user doesn't have the correct rights
     */
    @PreAuthorize("hasPermission(#uuid, 'COMMUNITY', 'WRITE')")
    @RequestMapping(method = RequestMethod.POST,
            headers = "content-type=multipart/form-data")
    public ResponseEntity<ResourceSupport> createLogo(HttpServletRequest request, @PathVariable UUID uuid,
                                      @RequestParam(value = "file", required = false) MultipartFile uploadfile)
            throws SQLException, IOException, AuthorizeException {

        if (uploadfile == null) {
            throw new UnprocessableEntityException("No file was given");
        }

        Context context = ContextUtil.obtainContext(request);

        Community community = communityService.find(context, uuid);
        if (community == null) {
            throw new ResourceNotFoundException(
                    "The given uuid did not resolve to a community on the server: " + uuid);
        }
        BitstreamRest bitstream = communityRestRepository.setLogo(context, community, uploadfile);

        BitstreamResource bitstreamResource = new BitstreamResource(bitstream, utils);
        context.complete();
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), bitstreamResource);
    }

}
