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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to send an input stream with Range header and ETag support.
 * Based on https://github.com/davinkevin/Podcast-Server/blob/v1.0.0/src/main/java/lan/dk/podcastserver/service/MultiPartFileSenderService.java
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 *
 */
public class MultipartFileSender {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String METHOD_HEAD = "HEAD";
    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";
    private static final String CONTENT_TYPE_MULTITYPE_WITH_BOUNDARY = "multipart/byteranges; boundary=" + MULTIPART_BOUNDARY;
    private static final String CONTENT_DISPOSITION_INLINE = "inline";
    private static final String CONTENT_DISPOSITION_ATTACHMENT = "attachment";
    private static final String IF_NONE_MATCH = "If-None-Match";
    private static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    private static final String ETAG = "ETag";
    private static final String IF_MATCH = "If-Match";
    private static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
    private static final String RANGE = "Range";
    private static final String CONTENT_RANGE = "Content-Range";
    private static final String IF_RANGE = "If-Range";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ACCEPT_RANGES = "Accept-Ranges";
    private static final String BYTES = "bytes";
    private static final String LAST_MODIFIED = "Last-Modified";
    private static final String EXPIRES = "Expires";
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    private static final String IMAGE = "image";
    private static final String ACCEPT = "Accept";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String BYTES_RANGE_FORMAT = "bytes %d-%d/%d";
    private static final String CONTENT_DISPOSITION_FORMAT = "%s;filename=\"%s\"";
    private static final String BYTES_DINVALID_BYTE_RANGE_FORMAT = "bytes */%d";
    private static final String CACHE_CONTROL = "Cache-Control";

    private int bufferSize = 1000000;

    private static final long DEFAULT_EXPIRE_TIME = 60L * 60L * 1000L;

    //no-cache so request is always performed for logging
    private static final String CACHE_CONTROL_SETTING = "private,no-cache";

    private BufferedInputStream inputStream;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private String contentType;
    private String disposition;
    private long lastModified;
    private long length;
    private String fileName;
    private String checksum;

    public MultipartFileSender(final InputStream inputStream) {
        //Convert to BufferedInputStream so we can re-read the stream
        this.inputStream = new BufferedInputStream(inputStream);
    }


    public static MultipartFileSender fromInputStream(InputStream inputStream) {
        return new MultipartFileSender(inputStream);
    }

    public MultipartFileSender with(HttpServletRequest httpRequest) {
        request = httpRequest;
        return this;
    }

    public MultipartFileSender with(HttpServletResponse httpResponse) {
        response = httpResponse;
        return this;
    }

    public MultipartFileSender withLength(long length) {
        this.length = length;
        return this;
    }

    public MultipartFileSender withFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public MultipartFileSender withChecksum(String checksum) {
        this.checksum = checksum;
        return this;
    }

    public MultipartFileSender withMimetype(String mimetype) {
        this.contentType = mimetype;
        return this;
    }

    public MultipartFileSender withLastModified(long lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    public MultipartFileSender withBufferSize(int bufferSize) {
        if(bufferSize > 0) {
            this.bufferSize = bufferSize;
        }
        return this;
    }

    public void serveResource() throws IOException {

        // Validate and process range -------------------------------------------------------------

        // Prepare some variables. The full Range represents the complete file.
        Range full = getFullRange();
        List<Range> ranges = getRanges(full);

        if (ranges == null) {
            //The supplied range values were invalid
            return;
        }

        log.debug("Content-Type : {}", contentType);
        // Initialize response.
        response.reset();
        response.setBufferSize(bufferSize);
        response.setHeader(CONTENT_TYPE, contentType);
        response.setHeader(ACCEPT_RANGES, BYTES);
        response.setHeader(ETAG, checksum);
        response.setDateHeader(LAST_MODIFIED, lastModified);
        response.setDateHeader(EXPIRES, System.currentTimeMillis() + DEFAULT_EXPIRE_TIME);

        //No-cache so that we can log every download
        response.setHeader(CACHE_CONTROL, CACHE_CONTROL_SETTING);


        if (isNullOrEmpty(disposition)) {
            if (contentType == null) {
                contentType = APPLICATION_OCTET_STREAM;
            } else if (!contentType.startsWith(IMAGE)) {
                String accept = request.getHeader(ACCEPT);
                disposition = accept != null && accepts(accept, contentType) ? CONTENT_DISPOSITION_INLINE : CONTENT_DISPOSITION_ATTACHMENT;
            }

            response.setHeader(CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_FORMAT, disposition, fileName));
            log.debug("Content-Disposition : {}", disposition);
        }

        // Content phase
        if (METHOD_HEAD.equals(request.getMethod())) {
            log.debug("HEAD request - skipping content");
            return;
        }
        // Send requested file (part(s)) to client ------------------------------------------------

        // Prepare streams.
        try (OutputStream output = response.getOutputStream()) {


            if (hasNoRanges(full, ranges)) {

                // Return full file.
                log.debug("Return full file");
                response.setContentType(contentType);
                response.setHeader(CONTENT_LENGTH, String.valueOf(length));
                Range.copy(inputStream, output, length, 0, length, bufferSize);

            } else if (ranges.size() == 1) {

                // Return single part of file.
                Range r = ranges.get(0);
                log.debug("Return 1 part of file : from ({}) to ({})", r.start, r.end);
                response.setContentType(contentType);
                response.setHeader(CONTENT_RANGE, String.format(BYTES_RANGE_FORMAT, r.start, r.end, r.total));
                response.setHeader(CONTENT_LENGTH, String.valueOf(r.length));
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                // Copy single part range.
                Range.copy(inputStream, output, length, r.start, r.length, bufferSize);

            } else {

                // Return multiple parts of file.
                response.setContentType(CONTENT_TYPE_MULTITYPE_WITH_BOUNDARY);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                // Cast back to ServletOutputStream to get the easy println methods.
                ServletOutputStream sos = (ServletOutputStream) output;

                // Copy multi part range.
                for (Range r : ranges) {
                    log.debug("Return multi part of file : from ({}) to ({})", r.start, r.end);
                    // Add multipart boundary and header fields for every range.
                    sos.println("--" + MULTIPART_BOUNDARY);
                    sos.println(CONTENT_TYPE + ": " + contentType);
                    sos.println(CONTENT_RANGE + ": " + String.format(BYTES_RANGE_FORMAT, r.start, r.end, r.total));

                    //Mark position of inputstream so we can return to it later
                    inputStream.mark(0);
                    // Copy single part range of multi part range.
                    Range.copy(inputStream, output, length, r.start, r.length, bufferSize);
                    inputStream.reset();

                    sos.println();
                }

                // End with multipart boundary.
                sos.println("--" + MULTIPART_BOUNDARY + "--");
            }
        }


    }

    public boolean isValid() throws IOException {
        if (response == null || request == null) {
            return false;
        }

        if (inputStream == null) {
            log.error("Input stream has no content");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
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

    public boolean isNoRangeRequest() throws IOException {
        Range full = getFullRange();
        List<Range> ranges = getRanges(full);

        if(hasNoRanges(full, ranges)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean hasNoRanges(final Range full, final List<Range> ranges) {
        return ranges != null && (ranges.isEmpty() || ranges.get(0) == full);
    }

    private Range getFullRange() {
        return new Range(0, length - 1, length);
    }


    private List<Range> getRanges(final Range fullRange) throws IOException {
        List<Range> ranges = new ArrayList<>();

        // Validate and process Range and If-Range headers.
        String range = request.getHeader(RANGE);
        if (nonNull(range)) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                log.error("Range header should match format \"bytes=n-n,n-n,n-n...\". If not, then return 416.");
                response.setHeader(CONTENT_RANGE, String.format(BYTES_DINVALID_BYTE_RANGE_FORMAT, length)); // Required in 416.
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return null;
            }

            String ifRange = request.getHeader(IF_RANGE);
            if (nonNull(ifRange) && !ifRange.equals(fileName)) {
                try {
                    //Assume that the If-Range contains a date
                    long ifRangeTime = request.getDateHeader(IF_RANGE); // Throws IAE if invalid.

                    if (ifRangeTime == -1 || ifRangeTime + 1000 <= lastModified) {
                        //Our file has been updated, send the full range
                        ranges.add(fullRange);
                    }

                } catch (IllegalArgumentException ignore) {
                    //Assume that the If-Range contains an ETag
                    if (!matches(ifRange, checksum)) {
                        //Our file has been updated, send the full range
                        ranges.add(fullRange);
                    }
                }
            }

            // If any valid If-Range header, then process each part of byte range.
            if (ranges.isEmpty()) {
                log.debug("If any valid If-Range header, then process each part of byte range.");
                for (String part : range.substring(6).split(",")) {
                    // Assuming a file with length of 100, the following examples returns bytes at:
                    // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                    long start = Range.sublong(part, 0, part.indexOf("-"));
                    long end = Range.sublong(part, part.indexOf("-") + 1, part.length());

                    if (start == -1) {
                        start = length - end;
                        end = length - 1;
                    } else if (end == -1 || end > length - 1) {
                        end = length - 1;
                    }

                    // Check if Range is syntactically valid. If not, then return 416.
                    if (start > end) {
                        log.warn("Check if Range is syntactically valid. If not, then return 416.");
                        response.setHeader(CONTENT_RANGE, String.format(BYTES_DINVALID_BYTE_RANGE_FORMAT, length)); // Required in 416.
                        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return null;
                    }

                    // Add range.
                    ranges.add(new Range(start, end, length));
                }
            }
        }
        return ranges;
    }

    private static boolean isNullOrEmpty(String disposition) {
        return StringUtils.isBlank(disposition);
    }


    private static class Range {
        long start;
        long end;
        long length;
        long total;

        /**
         * Construct a byte range.
         *
         * @param start Start of the byte range.
         * @param end   End of the byte range.
         * @param total Total length of the byte source.
         */
        public Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = this.end - this.start + 1;
            this.total = total;
        }

        private static List<Range> relativize(List<Range> ranges) {

            List<Range> builder = new ArrayList<>(ranges.size());

            Range prevRange = null;
            for (Range r : ranges) {
                Range newRange = isNull(prevRange) ? r : new Range(r.start - prevRange.end - 1, r.end - prevRange.end - 1, r.total);
                builder.add(newRange);
                prevRange = r;
            }

            return builder;
        }

        public static long sublong(String value, int beginIndex, int endIndex) {
            String substring = value.substring(beginIndex, endIndex);
            return (substring.length() > 0) ? Long.parseLong(substring) : -1;
        }

        private static void copy(InputStream input, OutputStream output, long inputSize, long start, long length, int bufferSize) throws IOException {
            byte[] buffer = new byte[bufferSize];
            int read;

            if (inputSize == length) {
                // Write full range.
                while ((read = input.read(buffer)) > 0) {
                    output.write(buffer, 0, read);
                    output.flush();
                }
            } else {
                input.skip(start);
                long toRead = length;

                while ((read = input.read(buffer)) > 0) {
                    if ((toRead -= read) > 0) {
                        output.write(buffer, 0, read);
                        output.flush();
                    } else {
                        output.write(buffer, 0, (int) toRead + read);
                        output.flush();
                        break;
                    }
                }
            }
        }
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