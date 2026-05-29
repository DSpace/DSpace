/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.dspace.AbstractUnitTest;
import org.junit.Before;
import org.junit.Test;

public class MatomoResponseReaderTest extends AbstractUnitTest {

    MatomoResponseReader matomoResponseReader;

    @Before
    public void setUp() throws Exception {
        matomoResponseReader = new MatomoResponseReader();
    }

    @Test
    public void testReadNullResponse() {
        assertThat(matomoResponseReader.fromJSON(null), nullValue());
    }

    @Test
    public void testReadEmptyResponse() {
        MatomoResponse actual = matomoResponseReader.fromJSON("");
        assertThat(actual, nullValue());
    }

    @Test
    public void testReadEmptyJsonResponse() {
        MatomoResponse actual = matomoResponseReader.fromJSON("{}");
        assertThat(actual, notNullValue());
        assertThat(actual.status(), nullValue());
        assertThat(actual.tracked(), is(0));
        assertThat(actual.invalid(), is(0));
        assertThat(actual.invalidIndices(), nullValue());
    }

    @Test
    public void testReadSuccessResponse() {
        MatomoResponse actual =
            matomoResponseReader.fromJSON(
                "{\"status\":\"success\",\"tracked\":1,\"invalid\":0,\"invalid_indices\":[]}"
            );
        assertThat(actual, notNullValue());
        assertThat(actual.status(), is("success"));
        assertThat(actual.tracked(), is(1));
        assertThat(actual.invalid(), is(0));
        assertThat(actual.invalidIndices(), notNullValue());
        assertThat(actual.invalidIndices().length, is(0));
    }

    @Test
    public void testReadFailedResponse() {
        MatomoResponse actual =
            matomoResponseReader.fromJSON(
                "{\"status\":\"success\",\"tracked\":0,\"invalid\":1,\"invalid_indices\":[0]}"
            );
        assertThat(actual, notNullValue());
        assertThat(actual.status(), is("success"));
        assertThat(actual.tracked(), is(0));
        assertThat(actual.invalid(), is(1));
        assertThat(actual.invalidIndices(), notNullValue());
        assertThat(actual.invalidIndices().length, is(1));
        assertThat(actual.invalidIndices()[0], is(0));
    }
}