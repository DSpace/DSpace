/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class VerboseDescription {
    private StringBuilder sb;

    public VerboseDescription() {
        this.sb = new StringBuilder();
    }

    public VerboseDescription append(String s) {
        this.sb.append(this.getDatePrefix() + s + "\n");
        return this;
    }

    public String toString() {
        return this.sb.toString();
    }

    private String getDatePrefix() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "[" + formatter.format(LocalDateTime.now(ZoneOffset.UTC)) + "] ";
    }
}
