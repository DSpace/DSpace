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
 * Whole lines (strings ending in '\n') are written to a log category named by
 * the task ID, for example "curation.virusscan".
 * Any partial line is flushed when the reporter is {@code close()}d.
 *
 * @author mhwood
 */
public class LogReporter
        implements Reporter {
    private static Logger LOG;
    private final String taskID;
    private final StringBuilder buffer = new StringBuilder();

    private LogReporter() {
        taskID = null;
    }

    public LogReporter(String object, String task) {
        taskID = task;
    }

    private Logger getLogger() {
        if (null == LOG) {
            LOG = LoggerFactory.getLogger("curation." + taskID);
        }
        return LOG;
    }

    @Override
    public Appendable append(CharSequence cs)
            throws IOException {
        for (int pos = 0; pos < cs.length(); pos++) {
            char c = cs.charAt(pos);
            if (c == '\n') {
                getLogger().info(buffer.toString());
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
        getLogger().info(buffer.toString());
    }
}
