package org.dspace.app.rest.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.Tika;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class BitstreamResourceHttpRequestHandler extends ResourceHttpRequestHandler {

    private static final Log logger = LogFactory.getLog(ResourceHttpRequestHandler.class);

    private Resource resource;


    /**
     * Processes a resource request.
     * <p>Checks for the existence of the requested resource in the configured list of locations.
     * If the resource does not exist, a {@code 404} response will be returned to the client.
     * If the resource exists, the request will be checked for the presence of the
     * {@code Last-Modified} header, and its value will be compared against the last-modified
     * timestamp of the given resource, returning a {@code 304} status code if the
     * {@code Last-Modified} value  is greater. If the resource is newer than the
     * {@code Last-Modified} value, or the header is not present, the content resource
     * of the resource will be written to the response with caching headers
     * set to expire one year in the future.
     */
    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // For very general mappings (e.g. "/") we need to check 404 first
        Resource resource = getResource(request);
        if (resource == null) {
            logger.trace("No matching resource found - returning 404");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            response.setHeader("Allow", getAllowHeader());
            return;
        }

        // Supported methods and required session
        checkRequest(request);

        // Header phase
        if (new ServletWebRequest(request, response).checkNotModified(resource.lastModified())) {
            logger.trace("Resource not modified - returning 304");
            return;
        }

        // Apply cache settings, if any
        prepareResponse(response);

        // Check the media type for the resource
        MediaType mediaType = getMediaType(request, resource);
        if (mediaType != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Determined media type '" + mediaType + "' for " + resource);
            }
        }
        else {
            if (logger.isTraceEnabled()) {
                logger.trace("No media type found for " + resource + " - not sending a content-type header");
            }
        }

        // Content phase
        if (METHOD_HEAD.equals(request.getMethod())) {
            setHeaders(response, resource, mediaType);
            logger.trace("HEAD request - skipping content");
            return;
        }

        ServletServerHttpResponse outputMessage = new ServletServerHttpResponse(response);
        if (request.getHeader(HttpHeaders.RANGE) == null) {
            setHeaders(response, resource, mediaType);
            getResourceHttpMessageConverter().write(resource, mediaType, outputMessage);
        }
        else {
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);
            try {
                HttpHeaders headers = inputMessage.getHeaders();

                /** CUSTOM: Limit range **/
                List<HttpRange> httpRanges = headers.getRange();
                httpRanges = limitRangeHeader(headers, httpRanges);

                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                if (httpRanges.size() == 1) {
                    ResourceRegion resourceRegion = httpRanges.get(0).toResourceRegion(resource);
                    getResourceRegionHttpMessageConverter().write(resourceRegion, mediaType, outputMessage);
                }
                else {
                    getResourceRegionHttpMessageConverter().write(
                            HttpRange.toResourceRegions(httpRanges, resource), mediaType, outputMessage);
                }
            }
            catch (IllegalArgumentException ex) {
                response.setHeader("Content-Range", "bytes */" + resource.contentLength());
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            }
        }
    }

    private List<HttpRange> limitRangeHeader(HttpHeaders headers, List<HttpRange> httpRanges) throws IOException {
        List<String> ranges = headers.get(HttpHeaders.RANGE);
        if (ranges.get(0).trim().endsWith("-")) {
            long start = httpRanges.get(0).getRangeStart(0);
            long end = Math.min(start + 20480, resource.contentLength() - 1);
            headers.set(HttpHeaders.RANGE, "bytes=" + start + "-" + end);
            return headers.getRange();
        }

        return httpRanges;

    }


    public BitstreamResourceHttpRequestHandler(Resource resource) {
        super();
        this.resource = resource;
        try {
            afterPropertiesSet();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Resource getResource(HttpServletRequest request) throws IOException {
        return this.resource;
    }

    @Override
    protected MediaType getMediaType(HttpServletRequest request, Resource resource) {
        Tika tika = new Tika();
        try {
            String mimetype = tika.detect(resource.getInputStream());
            return MediaType.parseMediaType(mimetype);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



}
