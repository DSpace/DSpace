package org.dspace.xoai.tests.stylesheets;

import org.apache.commons.io.IOUtils;
import org.dspace.xoai.tests.support.XmlMatcherBuilder;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

public abstract class AbstractXSLTest {
    private static final TransformerFactory factory = TransformerFactory.newInstance();

    protected TransformBuilder apply (String xslLocation) throws Exception {
        return new TransformBuilder(xslLocation);
    }

    protected InputStream resource (String location) throws Exception {
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
            this.transformer = factory.newTransformer(new StreamSource(new File("../dspace/config/crosswalks/oai/metadataFormats", xslLocation)));
        }

        public String to(InputStream input) throws Exception {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            this.transformer.transform(new StreamSource(input), new StreamResult(outputStream));
            outputStream.close();
            return outputStream.toString();
        }
    }
}
