/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import org.junit.Test;

/**
 *
 * @author mwood
 */
public class AuthorityValueTest {
    /**
     * Test of stringToDate method, of class AuthorityValue.
     */
    @Test
    public void testStringToDate() {
        Date expected;
        Date actual;

        // Test an invalid date.
        actual = AuthorityValue.stringToDate("not a date");
        assertNull("Unparsable date should return null", actual);

        // Test a date-time without zone or offset.
        expected = Date.from(LocalDateTime.of(1957, 01, 27, 01, 23, 45)
                .atZone(ZoneId.systemDefault())
                .toInstant());
        actual = AuthorityValue.stringToDate("1957-01-27T01:23:45");
        assertEquals("Local date-time should convert", expected, actual);

        // Test a date-time with milliseconds and offset from UTC.
        expected = Date.from(LocalDateTime.of(1957, 01, 27, 01, 23, 45, 678_000_000)
                .atZone(ZoneOffset.of("-05"))
                .toInstant());
        actual = AuthorityValue.stringToDate("1957-01-27T01:23:45.678-05");
        assertEquals("Zoned date-time with milliseconds should convert",
                expected, actual);
    }
}
