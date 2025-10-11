package org.dspace.xoai.tests.unit.util;

import org.dspace.xoai.util.DateUtils;
import org.junit.Test;

import java.time.Instant;
import static org.junit.Assert.assertEquals;

public class DateUtilsTest {

    @Test
    public void testFormatOaiDate(){
        Instant date = Instant.parse("2025-10-09T18:53:58.376565922Z");
        String formatted = DateUtils.format(date);
        assertEquals("2025-10-09T18:53:58Z", formatted);
    }
}