/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.model;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.dspace.AbstractUnitTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MatomoCookieConverterTest extends AbstractUnitTest {

    @Test
    public void testSingleCookieConverter() {
        String convert = MatomoCookieConverter.convert(
            new MatomoRequestDetails()
                .addCookie("cookie1", "value1")
        );
        assertEquals("cookie1=value1", convert);
    }

    @Test
    public void testMultipleCookieConverter() {
        String convert = MatomoCookieConverter.convert(
            new MatomoRequestDetails()
                .addCookie("cookie1", "value1")
                .addCookie("cookie2", "value2")
        );
        assertEquals("cookie1=value1;cookie2=value2", convert);
    }

    @Test
    public void testMultipleDetailsConverter() {
        String convert = MatomoCookieConverter.convert(
            List.of(
                new MatomoRequestDetails()
                    .addCookie("cookie1", "value1")
                    .addCookie("cookie2", "value2"),
                new MatomoRequestDetails()
                    .addCookie("cookie1", "value11")
                    .addCookie("cookie2", "value22")
            )
        );
        assertEquals("cookie1=value11;cookie2=value22", convert);
    }
}
