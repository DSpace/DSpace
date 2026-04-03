/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * Class that holds information about an open InputStream
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public final class BitstreamInputStreamOpenInfo {

    private static final int STACK_TRACE_LINES = 30;

    private final UUID id;
    private final String kind;
    private final Instant openedAt;
    private final Throwable openedAtThrowable;

    public BitstreamInputStreamOpenInfo(UUID id,
                                        String kind,
                                        Instant openedAt,
                                        Throwable openedAtThrowable) {
        this.id = id;
        this.kind = kind;
        this.openedAt = openedAt;
        this.openedAtThrowable = openedAtThrowable;
    }

    /**
     * Get the ID of this info object
     *
     * @return the ID of this info object
     */
    public UUID id() {
        return id;
    }

    /**
     * Get the kind of open InputStream, usually the method it was retrieved in
     *
     * @return the kind of open InputStream
     */
    public String kind() {
        return kind;
    }

    /**
     * Get the Instant the InputStream was opened
     *
     * @return the Instant the InputStream was opened
     */
    public Instant openedAt() {
        return openedAt;
    }

    /**
     * Get the throwable of were the InputStream was opened
     *
     * @return the throwable of were the InputStream was opened
     */
    public Throwable openedAtThrowable() {
        return openedAtThrowable;
    }

    /**
     * Converts openedAtThrowable into a String of the stacktrace and returns the first x amount of lines,
     * where "x" is configured in STACK_TRACE_LINES
     *
     * @return a print of the stacktrace
     */
    public String openedAtStackTrace() {
        if (openedAtThrowable == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        openedAtThrowable.printStackTrace(pw);
        String[] stackTraceLines = sw.toString().split("\n");
        return String.join("\n", Arrays.copyOfRange(
                stackTraceLines, 0, Math.min(STACK_TRACE_LINES, stackTraceLines.length)));
    }

    @Override
    public String toString() {
        return "BitstreamInputStreamOpenInfo{" +
                "id=" + id +
                ", kind='" + kind + '\'' +
                ", openedAt=" + openedAt +
                ", openedAtThrowable=\n" + openedAtStackTrace() +
                '}';
    }
}
