package org.dspace.importer.external.crossref;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link CrossRefDoiCheck}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class CrossRefDoiCheckTest {

    private CrossRefDoiCheck crossRefDoiCheck;

    private WebTarget webTarget = Mockito.mock(WebTarget.class);
    private Builder builder = Mockito.mock(Builder.class);
    private WebTarget request = mock(WebTarget.class);

    @Before
    public void setUp() throws Exception {
        when(webTarget.path(anyString())).thenReturn(request);
        when(request.request()).thenReturn(builder);
        crossRefDoiCheck = new CrossRefDoiCheck(webTarget);
    }

    @Test
    public void validDoi() {
        final boolean isDoi = crossRefDoiCheck.isDoi("10.1111/jfbc.13557");
        assertThat(isDoi, is(true));
    }

    @Test
    public void validDoiCommaPrefix() {
        final boolean isDoi = crossRefDoiCheck.isDoi(",10.1111/jfbc.13557");
        assertThat(isDoi, is(true));
    }

    @Test
    public void httpDoi() {
        final boolean isDoi = crossRefDoiCheck.isDoi(",http://dx.doi.org/10.1175/JPO3002.1");
        assertThat(isDoi, is(true));
    }

    @Test
    public void httpsDoi() {
        final boolean isDoi = crossRefDoiCheck.isDoi(",https://dx.doi.org/10.1175/JPO3002.1");
        assertThat(isDoi, is(true));
    }

    @Test
    public void invalidDoi() {
        final boolean isDoi = crossRefDoiCheck.isDoi("invalid");
        assertThat(isDoi, is(false));
    }

    @Test
    public void existingDoi() {

        final Response response = response(200);
        when(builder.head()).thenReturn(response);
        final boolean validDoi = crossRefDoiCheck.validExistingDoi("10.1111/jfbc.13557");
        assertThat(validDoi, is(true));
    }

    @Test
    public void notExistingDoi() {

        final Response response = response(404);
        when(builder.head()).thenReturn(response);
        final boolean validDoi = crossRefDoiCheck.validExistingDoi("10.1111/jfbc.13557");
        assertThat(validDoi, is(false));
    }

    @Test
    public void existingCommaPrefix() {
        final Response response = response(200);
        when(builder.head()).thenReturn(response);
        final boolean validDoi = crossRefDoiCheck.validExistingDoi(",10.1111/jfbc.13557");
        assertThat(validDoi, is(true));
    }


    @Test
    public void existingHttpDoi() {
        final Response response = response(200);
        when(builder.head()).thenReturn(response);
        final boolean validDoi = crossRefDoiCheck.validExistingDoi(",http://dx.doi.org/10.1175/JPO3002.1");
        assertThat(validDoi, is(true));
    }

    @Test
    public void existingHttpsDoi() {
        final Response response = response(200);
        when(builder.head()).thenReturn(response);
        final boolean validDoi = crossRefDoiCheck.validExistingDoi(",https://dx.doi.org/10.1175/JPO3002.1");
        assertThat(validDoi, is(true));
    }

    private Response response(final int code) {
        final Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(code);
        return response;
    }
}
