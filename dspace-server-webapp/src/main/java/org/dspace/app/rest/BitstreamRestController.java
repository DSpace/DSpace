/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.ContextUtil.obtainContext;
import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.hateoas.BitstreamResource;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.HttpHeadersInitializer;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.disseminate.service.CitationDocumentService;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.EventService;
import org.dspace.usage.UsageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is a specialized controller to provide access to the bitstream binary
 * content
 *
 * The mapping for requested endpoint try to resolve a valid UUID, for example
 * <pre>
 * {@code
 * https://<dspace.server.url>/api/core/bitstreams/26453b4d-e513-44e8-8d5b-395f62972eff/content
 * }
 * </pre>
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 */
@RestController
@RequestMapping("/api/" + BitstreamRest.CATEGORY + "/" + BitstreamRest.PLURAL_NAME
    + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID)
public class BitstreamRestController {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(BitstreamRestController.class);

    //Most file systems are configured to use block sizes of 4096 or 8192 and our buffer should be a multiple of that.
    private static final int BUFFER_SIZE = 4096 * 10;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    BitstreamFormatService bitstreamFormatService;

    @Autowired
    private EventService eventService;

    @Autowired
    private CitationDocumentService citationDocumentService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    ConverterService converter;

    @Autowired
    Utils utils;

    @PreAuthorize("hasPermission(#uuid, 'BITSTREAM', 'READ')")
    @RequestMapping( method = {RequestMethod.GET, RequestMethod.HEAD}, value = "content")
    public ResponseEntity retrieve(@PathVariable UUID uuid, HttpServletResponse response,
                         HttpServletRequest request) throws IOException, SQLException, AuthorizeException {


        Context context = ContextUtil.obtainContext(request);

        Bitstream bit = bitstreamService.find(context, uuid);
        EPerson currentUser = context.getCurrentUser();

        if (bit == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        Long lastModified = bitstreamService.getLastModified(bit);
        BitstreamFormat format = bit.getFormat(context);
        String mimetype = format.getMIMEType();
        String name = getBitstreamName(bit, format);

        if (StringUtils.isBlank(request.getHeader("Range"))) {
            //We only log a download request when serving a request without Range header. This is because
            //a browser always sends a regular request first to check for Range support.
            eventService.fireEvent(
                new UsageEvent(
                    UsageEvent.Action.VIEW,
                    request,
                    context,
                    bit));
        }

        try {
            long filesize = bit.getSizeBytes();
            Boolean citationEnabledForBitstream = citationDocumentService.isCitationEnabledForBitstream(bit, context);

            HttpHeadersInitializer httpHeadersInitializer = new HttpHeadersInitializer()
                .withBufferSize(BUFFER_SIZE)
                .withFileName(name)
                .withChecksum(bit.getChecksum())
                .withMimetype(mimetype)
                .with(request)
                .with(response);

            if (lastModified != null) {
                httpHeadersInitializer.withLastModified(lastModified);
            }

            //Determine if we need to send the file as a download or if the browser can open it inline
            long dispositionThreshold = configurationService.getLongProperty("webui.content_disposition_threshold");
            if (dispositionThreshold >= 0 && filesize > dispositionThreshold) {
                httpHeadersInitializer.withDisposition(HttpHeadersInitializer.CONTENT_DISPOSITION_ATTACHMENT);
            }

            org.dspace.app.rest.utils.BitstreamResource bitstreamResource =
                new org.dspace.app.rest.utils.BitstreamResource(name, uuid,
                    currentUser != null ? currentUser.getID() : null,
                    context.getSpecialGroupUuids(), citationEnabledForBitstream);

            //We have all the data we need, close the connection to the database so that it doesn't stay open during
            //download/streaming
            context.complete();

            //Send the data
            if (httpHeadersInitializer.isValid()) {
                HttpHeaders httpHeaders = httpHeadersInitializer.initialiseHeaders();
                return ResponseEntity.ok().headers(httpHeaders).body(bitstreamResource);
            }

        } catch (ClientAbortException ex) {
            log.debug("Client aborted the request before the download was completed. " +
                          "Client is probably switching to a Range request.", ex);
        } catch (Exception e) {
            throw e;
        }
        return null;
    }

    private String getBitstreamName(Bitstream bit, BitstreamFormat format) {
        String name = bit.getName();
        if (name == null) {
            // give a default name to the file based on the UUID and the primary extension of the format
            name = bit.getID().toString();
            if (format != null && format.getExtensions() != null && format.getExtensions().size() > 0) {
                name += "." + format.getExtensions().get(0);
            }
        }
        return name;
    }

    private boolean isNotAnErrorResponse(HttpServletResponse response) {
        Response.Status.Family responseCode = Response.Status.Family.familyOf(response.getStatus());
        return responseCode.equals(Response.Status.Family.SUCCESSFUL)
            || responseCode.equals(Response.Status.Family.REDIRECTION);
    }

    /**
     * This method will update the bitstream format of the bitstream that corresponds to the provided bitstream uuid.
     *
     * @param uuid The UUID of the bitstream for which to update the bitstream format
     * @param request  The request object
     * @return The wrapped resource containing the bitstream which in turn contains the bitstream format
     * @throws SQLException       If something goes wrong in the database
     */
    @RequestMapping(method = PUT, consumes = {"text/uri-list"}, value = "format")
    @PreAuthorize("hasPermission(#uuid, 'BITSTREAM','WRITE')")
    @PostAuthorize("returnObject != null")
    public BitstreamResource updateBitstreamFormat(@PathVariable UUID uuid,
                                                   HttpServletRequest request) throws SQLException {

        Context context = obtainContext(request);

        List<BitstreamFormat> bitstreamFormats = utils.constructBitstreamFormatList(request, context);

        if (bitstreamFormats.size() > 1) {
            throw new DSpaceBadRequestException("Only one bitstream format is allowed");
        }

        BitstreamFormat bitstreamFormat = bitstreamFormats.stream().findFirst()
                .orElseThrow(() -> new DSpaceBadRequestException("No valid bitstream format was provided"));

        Bitstream bitstream = bitstreamService.find(context, uuid);

        if (bitstream == null) {
            throw new ResourceNotFoundException("Bitstream with id: " + uuid + " not found");
        }

        bitstream.setFormat(context, bitstreamFormat);

        context.commit();

        BitstreamRest bitstreamRest = converter.toRest(context.reloadEntity(bitstream), utils.obtainProjection());
        return converter.toResource(bitstreamRest);
    }
}
