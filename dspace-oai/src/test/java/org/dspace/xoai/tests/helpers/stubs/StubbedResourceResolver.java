/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.helpers.stubs;

import com.lyncode.xoai.dataprovider.services.api.ResourceResolver;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class StubbedResourceResolver implements ResourceResolver {
    private static TransformerFactory factory = TransformerFactory.newInstance();

    private Map<String, InputStream> inputStreamMap = new HashMap<String, InputStream>();
    private Map<String, Transformer> transformerMap = new HashMap<String, Transformer>();

    @Override
    public InputStream getResource(String path) throws IOException {
        return inputStreamMap.get(path);
    }

    @Override
    public Transformer getTransformer(String path) throws IOException, TransformerConfigurationException {
        return transformerMap.get(path);
    }

    public StubbedResourceResolver hasIdentityTransformerFor(String path) {
        try {
            transformerMap.put(path, factory.newTransformer());
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
}
