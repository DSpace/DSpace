/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.stylesheets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;

public abstract class AbstractXSLTest {
    // Requires usage of Saxon as OAI-PMH uses some XSLT 2 functions
    private static final TransformerFactory factory = TransformerFactory
            .newInstance("net.sf.saxon.TransformerFactoryImpl", null);

    protected TransformBuilder apply(String xslLocation) throws Exception {
        return new TransformBuilder(xslLocation);
    }

    protected InputStream resource(String location) throws Exception {
        return print(this.getClass().getClassLoader().getResourceAsStream(location));
    }

    private InputStream print(InputStream resourceAsStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copy(resourceAsStream, outputStream);

//        System.out.println(outputStream.toString());

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public static class TransformBuilder {
        private final Transformer transformer;

        public TransformBuilder(String xslLocation) throws Exception {
            this.transformer = factory.newTransformer(
                new StreamSource(new File("../dspace/config/crosswalks/oai/metadataFormats", xslLocation)));
        }

        public String to(InputStream input) throws Exception {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            this.transformer.transform(new StreamSource(input), new StreamResult(outputStream));
            outputStream.close();
            return outputStream.toString();
        }
    }
}
