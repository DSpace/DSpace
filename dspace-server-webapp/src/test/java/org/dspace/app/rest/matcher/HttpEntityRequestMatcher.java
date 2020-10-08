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
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class HttpEntityRequestMatcher implements ArgumentMatcher<HttpEntityEnclosingRequestBase> {

    private String xmlFile;
    private String httpMethod;

    public HttpEntityRequestMatcher(String xmlFiel, String httpMethod) {
        this.xmlFile = xmlFiel;
        this.httpMethod = httpMethod;
    }

    @Override
    public boolean matches(HttpEntityEnclosingRequestBase argument) {
        String requestContent = null;
        try {
            requestContent = IOUtils.toString(argument.getEntity().getContent(), Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (requestContent.equals(xmlFile) && argument.getMethod().equals(httpMethod)) {
           return true;
        }
        return false;
    }
}