/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dspace.importer.external.service.DoiCheck;
import org.junit.jupiter.api.Test;

/**
 * Test class for the DoiCheck
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class DoiCheckTest {

    @Test
    public void checkDOIsTest() throws ParseException {
        for (String doi : DOIsToTest()) {
            assertTrue(DoiCheck.isDoi(doi), "The: " + doi + "  is a doi!");
        }
    }

    @Test
    public void checkWrongDOIsTest() throws ParseException {
        for (String key : wrongDOIsToTest()) {
            assertFalse(DoiCheck.isDoi(key), "This : " + key + "  isn't a doi!");
        }
    }

    private List<String> DOIsToTest() {
        return Arrays.asList(
            "10.1430/8105",
            "10.1038/nphys1170",
            "10.1002/0470841559.ch1",
            "10.1594/PANGAEA.726855",
            "10.1594/GFZ.GEOFON.gfz2009kciu",
            "10.3866/PKU.WHXB201112303",
            "10.11467/isss2003.7.1_11",
            "10.3972/water973.0145.db"
        );
    }

    private List<String> wrongDOIsToTest() {
        return Arrays.asList(
            StringUtils.EMPTY,
            "123456789",
            "nphys1170/10.1038",
            "10.", "10",
            "10.1038/"
        );
    }

}