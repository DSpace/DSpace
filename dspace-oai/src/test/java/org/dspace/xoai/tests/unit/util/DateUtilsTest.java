/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.unit.util;

import static org.junit.Assert.assertEquals;

import java.time.Instant;

import org.dspace.xoai.util.DateUtils;
import org.junit.Test;

public class DateUtilsTest {

    @Test
    public void testFormatOaiDate() {
        Instant date = Instant.parse("2025-10-09T18:53:58.376565922Z");
        String formatted = DateUtils.format(date);
        assertEquals("2025-10-09T18:53:58Z", formatted);
    }
}
