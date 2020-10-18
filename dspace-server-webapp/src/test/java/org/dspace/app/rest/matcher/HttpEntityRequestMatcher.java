/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.mockito.ArgumentMatcher;

/**
 * Custom {@link ArgumentMatcher} to compare verify the
 * {@link HttpEntityEnclosingRequestBase} attributes.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class HttpEntityRequestMatcher implements ArgumentMatcher<HttpEntityEnclosingRequestBase> {

    private String content;
    private String httpMethod;

    public HttpEntityRequestMatcher(String content, String httpMethod) {
        this.content = content;
        this.httpMethod = httpMethod;
    }

    @Override
    public boolean matches(HttpEntityEnclosingRequestBase request) {
        String requestContent = getRequestContentAsString(request);
        return requestContent.equals(content) && request.getMethod().equals(httpMethod);
    }

    private String getRequestContentAsString(HttpEntityEnclosingRequestBase request) {
        try {
            return IOUtils.toString(request.getEntity().getContent(), Charset.defaultCharset());
        } catch (UnsupportedOperationException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}