/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.curate;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Write curation report records through the logging framework.
 * Whole lines (strings ending in '\n') are written to the log category "curation".
 * Any partial line is flushed when the reporter is {@code close()}d.
 *
 * @author mhwood
 */
public class LogReporter
        implements Reporter {
    private static final Logger LOG = LoggerFactory.getLogger("curation");
    private final StringBuilder buffer = new StringBuilder();

    @Override
    public Appendable append(CharSequence cs)
            throws IOException {
        for (int pos = 0; pos < cs.length(); pos++) {
            char c = cs.charAt(pos);
            if (c == '\n') {
                LOG.info(buffer.toString());
                buffer.delete(0, buffer.length()); // Clear the buffer
            } else {
                buffer.append(c);
            }
        }
        return this;
    }

    @Override
    public Appendable append(CharSequence cs, int i, int i1)
            throws IOException {
        return append(cs.subSequence(i, i1));
    }

    @Override
    public Appendable append(char c)
            throws IOException {
        return append(String.valueOf(c));
    }

    @Override
    public void close()
            throws Exception {
        if (buffer.length() > 0) {
            LOG.info(buffer.toString());
        }
    }
}
