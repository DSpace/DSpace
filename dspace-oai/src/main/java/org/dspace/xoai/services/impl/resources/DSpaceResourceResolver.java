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
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import com.lyncode.xoai.dataprovider.services.api.ResourceResolver;
import net.sf.saxon.jaxp.SaxonTransformerFactory;
import net.sf.saxon.s9api.ExtensionFunction;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xoai.services.impl.resources.functions.BibtexifyFn;
import org.dspace.xoai.services.impl.resources.functions.FormatFn;
import org.dspace.xoai.services.impl.resources.functions.GetAuthorFn;
import org.dspace.xoai.services.impl.resources.functions.GetContactFn;
import org.dspace.xoai.services.impl.resources.functions.GetFundingFn;
import org.dspace.xoai.services.impl.resources.functions.GetLangForCodeFn;
import org.dspace.xoai.services.impl.resources.functions.GetPropertyFn;
import org.dspace.xoai.services.impl.resources.functions.GetSizeFn;
import org.dspace.xoai.services.impl.resources.functions.GetUploadedMetadataFn;
import org.dspace.xoai.services.impl.resources.functions.LogMissingFn;
import org.dspace.xoai.services.impl.resources.functions.LogMissingMsgFn;
import org.dspace.xoai.services.impl.resources.functions.StringReplaceFn;
import org.dspace.xoai.services.impl.resources.functions.UriToLicenseFn;
import org.dspace.xoai.services.impl.resources.functions.UriToMetaShareFn;
import org.dspace.xoai.services.impl.resources.functions.UriToRestrictionsFn;

public class DSpaceResourceResolver implements ResourceResolver {
    // Requires usage of Saxon as OAI-PMH uses some XSLT 2 functions
    private static final TransformerFactory transformerFactory = TransformerFactory
            .newInstance("net.sf.saxon.TransformerFactoryImpl", null);
    static {
        /*
         * Any additional extension functions that might be used in XST transformations
         * should be added to this list. Look at those already added for inspiration.
         */
        List<ExtensionFunction> extensionFunctionList = List.of(
                new GetPropertyFn(), new StringReplaceFn(), new UriToMetaShareFn(),
                new UriToLicenseFn(), new LogMissingMsgFn(), new UriToRestrictionsFn(),
                new GetContactFn(), new GetAuthorFn(), new GetFundingFn(), new GetLangForCodeFn(),
                new GetPropertyFn(), new GetSizeFn(), new GetUploadedMetadataFn(), new LogMissingFn(),
                new BibtexifyFn(), new FormatFn()
        );

        SaxonTransformerFactory saxonTransformerFactory = (SaxonTransformerFactory) transformerFactory;
        for (ExtensionFunction en :
                extensionFunctionList) {
            saxonTransformerFactory.getProcessor().registerExtensionFunction(en);
        }
    }
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
    public Templates getTemplates(String path) throws IOException, TransformerConfigurationException {
        // construct a Source that reads from an InputStream
        Source mySrc = new StreamSource(getResource(path));
        // specify a system ID (the path to the XSLT-file on the filesystem)
        // so the Source can resolve relative URLs that are encountered in
        // XSLT-files (like <xsl:import href="utils.xsl"/>)
        String systemId = basePath + "/" + path;
        mySrc.setSystemId(systemId);
        return transformerFactory.newTemplates(mySrc);
    }
}
