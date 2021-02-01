/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.service;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

/**
 * Unit tests for {@link DoiCheck}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DoiCheckTest {


    @Test
    public void validDoi() {
        final boolean isDoi = DoiCheck.isDoi("10.1111/jfbc.13557");
        assertThat(isDoi, is(true));
    }

    @Test
    public void validDoiCommaPrefix() {
        final boolean isDoi = DoiCheck.isDoi(",10.1111/jfbc.13557");
        assertThat(isDoi, is(true));
    }

    @Test
    public void validDoiWithSpaces() {
        final boolean isDoi = DoiCheck.isDoi(" 10.1111/jfbc.13557 ");
        assertThat(isDoi, is(true));
    }

    @Test
    public void validDoiCommaPrefixAndSpaces() {
        final boolean isDoi = DoiCheck.isDoi(", 10.1111/jfbc.13557 ");
        assertThat(isDoi, is(true));
    }

    @Test
    public void httpDoi() {
        final boolean isDoi = DoiCheck.isDoi(",http://dx.doi.org/10.1175/JPO3002.1");
        assertThat(isDoi, is(true));
    }

    @Test
    public void httpsDoi() {
        final boolean isDoi = DoiCheck.isDoi(",https://dx.doi.org/10.1175/JPO3002.1");
        assertThat(isDoi, is(true));
    }

    @Test
    public void httpDoiAndSpaces() {
        final boolean isDoi = DoiCheck.isDoi(", http://dx.doi.org/10.1175/JPO3002.1 ");
        assertThat(isDoi, is(true));
    }

    @Test
    public void httpsDoiAndSpaces() {
        final boolean isDoi = DoiCheck.isDoi(", https://dx.doi.org/10.1175/JPO3002.1 ");
        assertThat(isDoi, is(true));
    }

    @Test
    public void invalidDoi() {
        final boolean isDoi = DoiCheck.isDoi("invalid");
        assertThat(isDoi, is(false));
    }

}
