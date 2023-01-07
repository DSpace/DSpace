/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import static org.junit.Assert.assertEquals;

import java.util.Objects;

import org.dspace.AbstractUnitTest;
import org.dspace.authenticate.clarin.ShibHeaders;
import org.junit.Test;

/**
 * Unit test for the class ShibHeaders. In this class is testing the added constructor the create the ShibHeaders
 * object from the String.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ShibHeadersTest extends AbstractUnitTest {

    /**
     * Test constructor to create ShibHeaders object from the String.
     */
    @Test
    public void testParsingStringHeaders() {
        String shibHeadersString = "shib-netid=123456\nshib-identity-provider=Test Idp\n" +
        "x-csrf-token=f06905b1-3458-4c3c-bd91-78e97fe7b2e1";

        ShibHeaders shibHeaders = new ShibHeaders(shibHeadersString);
        assertEquals(Objects.nonNull(shibHeaders), true);
    }
}
