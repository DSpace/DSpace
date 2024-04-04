/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;
import static org.dspace.core.Constants.BITSTREAM;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.hateoas.BundleResource;
import org.dspace.app.rest.repository.BundlePrimaryBitstreamLinkRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This RestController is responsible for managing primaryBitstreams on bundles.
 * The endpoint can be found at /api/core/bundles/{bundle-uuid}/primaryBitstream
 */
@RestController
@RequestMapping("/api/" + BundleRest.CATEGORY + "/" + BundleRest.PLURAL_NAME
    + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/" + BundleRest.PRIMARY_BITSTREAM)
public class PrimaryBitstreamController {

    @Autowired
    private BundlePrimaryBitstreamLinkRepository repository;
    @Autowired
    private ConverterService converter;
    @Autowired
    private Utils utils;

    /**
     * This method creates a primaryBitstream on the given Bundle.
     * <br><code>
     * curl -i -X POST "http://{dspace.server.url}/api/core/bundles/{bundle-uuid}/primaryBitstream"
     * -H "Content-type:text/uri-list"
     * -d "https://{dspace.server.url}/api/core/bitstreams/{bitstream-uuid}"
     * </code>
     *
     * @param uuid      The UUID of the Bundle on which the primaryBitstream will be set
     * @param request   The HttpServletRequest
     * @return          The Bundle on which the primaryBitstream was set
     */
    @PreAuthorize("hasPermission(#uuid, 'BUNDLE', 'WRITE')")
    @RequestMapping(method = RequestMethod.POST, consumes = {"text/uri-list"})
    public ResponseEntity<RepresentationModel<?>> createPrimaryBitstream(@PathVariable UUID uuid,
                                                                         HttpServletRequest request) {
        Context context = ContextUtil.obtainContext(request);
        BundleRest bundleRest = repository.createPrimaryBitstream(context, uuid,
                                                                  getBitstreamFromRequest(context, request),
                                                                  utils.obtainProjection());
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(),
                                                (RepresentationModel<?>) converter.toResource(bundleRest));
    }

    /**
     * This method updates the primaryBitstream on the given Bundle.
     * <br><code>
     * curl -i -X PUT "http://{dspace.server.url}/api/core/bundles/{bundle-uuid}/primaryBitstream"
     * -H "Content-type:text/uri-list"
     * -d "https://{dspace.server.url}/api/core/bitstreams/{bitstream-uuid}"
     * </code>
     *
     * @param uuid      The UUID of the Bundle of which the primaryBitstream will be updated
     * @param request   The HttpServletRequest
     * @return          The Bundle of which the primaryBitstream was updated
     */
    @PreAuthorize("hasPermission(#uuid, 'BUNDLE', 'WRITE')")
    @RequestMapping(method = RequestMethod.PUT, consumes = {"text/uri-list"})
    public BundleResource updatePrimaryBitstream(@PathVariable UUID uuid,
                                                 HttpServletRequest request) {
        Context context = ContextUtil.obtainContext(request);
        BundleRest bundleRest = repository.updatePrimaryBitstream(context, uuid,
                                                                  getBitstreamFromRequest(context, request),
                                                                  utils.obtainProjection());
        return converter.toResource(bundleRest);
    }

    /**
     * This method deletes the primaryBitstream on the given Bundle.
     * <br><code>
     * curl -i -X DELETE "http://{dspace.server.url}/api/core/bundles/{bundle-uuid}/primaryBitstream"
     * </code>
     *
     * @param uuid      The UUID of the Bundle of which the primaryBitstream will be deleted
     * @param request   The HttpServletRequest
     * @return          The Bundle of which the primaryBitstream was deleted
     */
    @PreAuthorize("hasPermission(#uuid, 'BUNDLE', 'WRITE')")
    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<RepresentationModel<?>> deletePrimaryBitstream(@PathVariable UUID uuid,
                                                                         HttpServletRequest request) {
        Context context = ContextUtil.obtainContext(request);
        repository.deletePrimaryBitstream(context, uuid);
        return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
    }

    /**
     * This method parses a URI from the request body and resolves it to a Bitstream.
     *
     * @param context   The current DSpace context
     * @param request   The HttpServletRequest
     * @return          The resolved Bitstream
     */
    private Bitstream getBitstreamFromRequest(Context context, HttpServletRequest request) {
        List<DSpaceObject> dsoList = utils.constructDSpaceObjectList(context, utils.getStringListFromRequest(request));
        if (dsoList.size() != 1 || dsoList.get(0).getType() != BITSTREAM) {
            throw new UnprocessableEntityException("URI does not resolve to an existing bitstream.");
        }
        return (Bitstream) dsoList.get(0);
    }
}
