/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import com.lyncode.xoai.dataprovider.services.api.ResourceResolver;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class DSpaceResourceResolver implements ResourceResolver {
    // Requires usage of Saxon as OAI-PMH uses some XSLT 2 functions
    private static final TransformerFactory transformerFactory = TransformerFactory
            .newInstance("net.sf.saxon.TransformerFactoryImpl", null);

    private final String basePath;

    public DSpaceResourceResolver() {
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        basePath = configurationService.getProperty("oai.config.dir");
    }

    @Override
    public InputStream getResource(String path) throws IOException {
        return new FileInputStream(new File(basePath, path));
    }

    @Override
    public Transformer getTransformer(String path) throws IOException,
        TransformerConfigurationException {
        // construct a Source that reads from an InputStream
        Source mySrc = new StreamSource(getResource(path));
        // specify a system ID (the path to the XSLT-file on the filesystem)
        // so the Source can resolve relative URLs that are encountered in
        // XSLT-files (like <xsl:import href="utils.xsl"/>)
        String systemId = basePath + "/" + path;
        mySrc.setSystemId(systemId);
        return transformerFactory.newTransformer(mySrc);
    }
}
