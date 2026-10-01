/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dspace.importer.external.service.DoiCheck;
import org.junit.Test;

/**
 * Test class for the DoiCheck
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class DoiCheckTest {

    @Test
    public void checkDOIsTest() throws ParseException {
        for (String doi : DOIsToTest()) {
            assertTrue("The: " + doi + "  is a doi!", DoiCheck.isDoi(doi));
        }
    }

    @Test
    public void checkWrongDOIsTest() throws ParseException {
        for (String key : wrongDOIsToTest()) {
            assertFalse("This : " + key + "  isn't a doi!", DoiCheck.isDoi(key));
        }
    }

    @Test
    public void purgeDoiValueNormalizesTicketExamples() {
        for (String[] example : ticketDoiExamples()) {
            String input = example[0];
            String expected = example[1];
            String normalized = DoiCheck.purgeDoiValue(input);
            assertEquals("Failed for input: " + input, expected, normalized);
            assertTrue("Normalized value should be recognized as a DOI: " + normalized,
                       DoiCheck.isDoi(normalized));
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

    /**
     * DOI forms agreed in the ticket: input → normalized DOI.
     */
    private List<String[]> ticketDoiExamples() {
        return Arrays.asList(
            new String[] { "10.1038/s41561-018-0221-6", "10.1038/s41561-018-0221-6" },
            new String[] { "https://doi.org/10.1038/s41561-018-0221-6", "10.1038/s41561-018-0221-6" },
            new String[] { "https://dx.doi.org/10.1038/s41561-018-0221-6", "10.1038/s41561-018-0221-6" },
            new String[] { "HTTPS://DOI.ORG/10.5281/zenodo.5511912", "10.5281/zenodo.5511912" },
            new String[] { "10.1038/s41561-018-0221-6?download=true", "10.1038/s41561-018-0221-6" },
            new String[] { "https://doi.org/10.1038/s41561-018-0221-6?pdf=1", "10.1038/s41561-018-0221-6" },
            new String[] { "10.1038/s41561-018-0221-6.", "10.1038/s41561-018-0221-6" },
            new String[] { "https://link.springer.com/article/10.1016%2Fj.comnet.2021.108245",
                "10.1016/j.comnet.2021.108245" },
            new String[] { "https://api.elsevier.com/content/abstract/doi/10.1016/j.comnet.2021.108245",
                "10.1016/j.comnet.2021.108245" }
        );
    }

}