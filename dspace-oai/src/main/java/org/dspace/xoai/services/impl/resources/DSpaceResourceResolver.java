/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.resources;

import com.lyncode.xoai.dataprovider.services.api.ResourceResolver;
import org.dspace.core.ConfigurationManager;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DSpaceResourceResolver implements ResourceResolver {
    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private final String basePath = ConfigurationManager.getProperty("oai", "config.dir");

    @Override
    public InputStream getResource(String path) throws IOException {
        return new FileInputStream(new File(basePath, path));
    }

    @Override
    public Transformer getTransformer(String path) throws IOException, TransformerConfigurationException {
        return transformerFactory.newTransformer(new StreamSource(getResource(path)));
    }
}
