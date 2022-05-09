/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static javax.mail.internet.MimeUtility.encodeText;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.FastHttpDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

/**
 * This class takes data from the Bitstream/File that has to be send. It'll then digest this input and save it in
 * its local variables.
 * When calling {{@link #initialiseHeaders()}}, the input and information will be used to set the proper headers
 * with this info and return an Object of {@link HttpHeaders} to be used in the response that'll be generated
 */
public class HttpHeadersInitializer {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String METHOD_HEAD = "HEAD";
    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";
    private static final String CONTENT_TYPE_MULTITYPE_WITH_BOUNDARY = "multipart/byteranges; boundary=" +
        MULTIPART_BOUNDARY;
    public static final String CONTENT_DISPOSITION_INLINE = "inline";
    public static final String CONTENT_DISPOSITION_ATTACHMENT = "attachment";
    private static final String IF_NONE_MATCH = "If-None-Match";
    private static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    private static final String ETAG = "ETag";
    private static final String IF_MATCH = "If-Match";
    private static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ACCEPT_RANGES = "Accept-Ranges";
    private static final String BYTES = "bytes";
    private static final String LAST_MODIFIED = "Last-Modified";
    private static final String EXPIRES = "Expires";
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    private static final String IMAGE = "image";
    private static final String ACCEPT = "Accept";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String CONTENT_DISPOSITION_FORMAT = "%s;filename=\"%s\"";
    private static final String CACHE_CONTROL = "Cache-Control";

    private int bufferSize = 1000000;

    private static final long DEFAULT_EXPIRE_TIME = 60L * 60L * 1000L;

    //no-cache so request is always performed for logging
    private static final String CACHE_CONTROL_SETTING = "private,no-cache";

    private HttpServletRequest request;
    private HttpServletResponse response;
    private String contentType;
    private String disposition;
    private long lastModified;
    private long length;
    private String fileName;
    private String checksum;

    public HttpHeadersInitializer() {
        //Convert to BufferedInputStream so we can re-read the stream
    }

    public HttpHeadersInitializer with(HttpServletRequest httpRequest) {
        request = httpRequest;
        return this;
    }

    public HttpHeadersInitializer with(HttpServletResponse httpResponse) {
        response = httpResponse;
        return this;
    }

    public HttpHeadersInitializer withLength(long length) {
        this.length = length;
        return this;
    }

    public HttpHeadersInitializer withFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public HttpHeadersInitializer withChecksum(String checksum) {
        this.checksum = checksum;
        return this;
    }

    public HttpHeadersInitializer withMimetype(String mimetype) {
        this.contentType = mimetype;
        return this;
    }

    public HttpHeadersInitializer withLastModified(long lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    public HttpHeadersInitializer withBufferSize(int bufferSize) {
        if (bufferSize > 0) {
            this.bufferSize = bufferSize;
        }
        return this;
    }
    public HttpHeadersInitializer withDisposition(String contentDisposition) {
        this.disposition = contentDisposition;
        return this;
    }

    /**
     * This method will be called to create a {@link HttpHeaders} object which will contain the headers needed
     * to form a proper response when returning the Bitstream/File
     * @return  A {@link HttpHeaders} object containing the information for the Bitstream/File to be sent
     * @throws IOException If something goes wrong
     */
    public HttpHeaders initialiseHeaders() throws IOException {

        HttpHeaders httpHeaders = new HttpHeaders();
        // Validate and process range -------------------------------------------------------------

        log.debug("Content-Type : {}", contentType);
        //TODO response.reset() => Can be re-instated/investigated once we upgrade to Spring 5.2.9, see issue #3056
        // Initialize response.
        response.setBufferSize(bufferSize);
        if (contentType != null) {
            httpHeaders.put(CONTENT_TYPE, Collections.singletonList(contentType));
        }
        httpHeaders.put(ACCEPT_RANGES, Collections.singletonList(BYTES));
        if (checksum != null) {
            httpHeaders.put(ETAG, Collections.singletonList(checksum));
        }
        httpHeaders.put(LAST_MODIFIED, Collections.singletonList(FastHttpDateFormat.formatDate(lastModified)));
        httpHeaders.put(EXPIRES, Collections.singletonList(FastHttpDateFormat.formatDate(
            System.currentTimeMillis() + DEFAULT_EXPIRE_TIME)));

        //No-cache so that we can log every download
        httpHeaders.put(CACHE_CONTROL, Collections.singletonList(CACHE_CONTROL_SETTING));

        if (isNullOrEmpty(disposition)) {
            if (contentType == null) {
                contentType = APPLICATION_OCTET_STREAM;
            } else if (!contentType.startsWith(IMAGE)) {
                String accept = request.getHeader(ACCEPT);
                disposition = accept != null && accepts(accept,
                                                        contentType) ? CONTENT_DISPOSITION_INLINE :
                    CONTENT_DISPOSITION_ATTACHMENT;
            }

        }

        httpHeaders.put(CONTENT_DISPOSITION, Collections.singletonList(String.format(CONTENT_DISPOSITION_FORMAT,
                                                                                     disposition,
                                                                                     encodeText(fileName))));
        log.debug("Content-Disposition : {}", disposition);

        // Content phase
        if (METHOD_HEAD.equals(request.getMethod())) {
            log.debug("HEAD request - skipping content");
            return null;
        }

        return httpHeaders;

    }

    /**
     * This method will validate whether or not the given Response/Request/Information/Variables are valid.
     * If they're invalid, the Response shouldn't be given.
     * This will do null checks on the response, request, inputstream and filename.
     * Other than this, it'll check Request headers to see if their information is correct.
     * @return
     * @throws IOException
     */
    public boolean isValid() throws IOException {
        if (response == null || request == null) {
            return false;
        }

        if (StringUtils.isEmpty(fileName)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }

        // Validate request headers for caching ---------------------------------------------------
        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        String ifNoneMatch = request.getHeader(IF_NONE_MATCH);
        if (nonNull(ifNoneMatch) && matches(ifNoneMatch, checksum)) {
            log.debug("If-None-Match header should contain \"*\" or ETag. If so, then return 304.");
            response.setHeader(ETAG, checksum); // Required in 304.
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return false;
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
        // This header is ignored if any If-None-Match header is specified.
        long ifModifiedSince = request.getDateHeader(IF_MODIFIED_SINCE);
        if (isNull(ifNoneMatch) && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified) {
            log.debug("If-Modified-Since header should be greater than LastModified. If so, then return 304.");
            response.setHeader(ETAG, checksum); // Required in 304.
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return false;
        }

        // Validate request headers for resume ----------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = request.getHeader(IF_MATCH);
        if (nonNull(ifMatch) && !matches(ifMatch, checksum)) {
            log.error("If-Match header should contain \"*\" or ETag. If not, then return 412.");
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return false;
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
        long ifUnmodifiedSince = request.getDateHeader(IF_UNMODIFIED_SINCE);
        if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified) {
            log.error("If-Unmodified-Since header should be greater than LastModified. If not, then return 412.");
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return false;
        }

        return true;
    }


    private static boolean isNullOrEmpty(String disposition) {
        return StringUtils.isBlank(disposition);
    }


    private static boolean accepts(String acceptHeader, String toAccept) {
        String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
        Arrays.sort(acceptValues);

        return Arrays.binarySearch(acceptValues, toAccept) > -1
            || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
            || Arrays.binarySearch(acceptValues, "*/*") > -1;
    }

    private static boolean matches(String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1 || Arrays.binarySearch(matchValues, "*") > -1;
    }

}
