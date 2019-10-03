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

import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.hateoas.BitstreamResource;
import org.dspace.app.rest.repository.CollectionRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/" + CollectionRest.CATEGORY + "/" + CollectionRest.PLURAL_NAME)
public class CollectionRestController {

    @Autowired
    protected Utils utils;

    @Autowired
    private BitstreamConverter bitstreamConverter;

    @Autowired
    private CollectionRestRepository collectionRestRepository;

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = RequestMethod.POST,
            value = "{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}/logo",
            headers = "content-type=multipart/form-data")
    public ResponseEntity<ResourceSupport> createLogo(HttpServletRequest request, @PathVariable UUID uuid,
                                                      @RequestParam("file") MultipartFile uploadfile)
            throws SQLException, IOException, AuthorizeException {

        Context context = ContextUtil.obtainContext(request);
        Bitstream bitstream = collectionRestRepository.createLogo(context, uuid, uploadfile);

        return ControllerUtils.toResponseEntity(HttpStatus.CREATED,  null,
                new BitstreamResource(bitstreamConverter.fromModel(bitstream), utils));
    }

    /*@RequestMapping(method = RequestMethod.GET,
            value = "{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}/logo")
    public BitstreamResource getLogo(HttpServletRequest request, @PathVariable UUID uuid) {
        Context context = ContextUtil.obtainContext(request);

        Bitstream bitstream = collectionRestRepository.getLogo(context, uuid);
        return new BitstreamResource(bitstreamConverter.fromModel(bitstream), utils);
    }*/
}
