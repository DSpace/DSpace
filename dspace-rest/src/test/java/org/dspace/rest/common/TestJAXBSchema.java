/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class TestJAXBSchema {

    private static class TestSchemaOutputResolver extends SchemaOutputResolver {

        private final Writer output;

        public TestSchemaOutputResolver(Writer output) {
            this.output = output;
        }

        public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
            StreamResult result = new StreamResult(output);
            result.setSystemId("xsd0.xsd");
            return result;
        }

    }

    @Test
    public void testFullSchema() throws Exception {
        StringWriter writer = new StringWriter();
        TestSchemaOutputResolver resolver = new TestSchemaOutputResolver(writer);
        JAXBContext context = JAXBContext.newInstance(
                Bitstream.class,
                CheckSum.class,
                Collection.class,
                Community.class,
                DSpaceObject.class,
                Item.class,
                MetadataEntry.class,
                ResourcePolicy.class,
                Status.class
                );
        context.generateSchema(resolver);

        String res = "org/dspace/rest/common/expected_xsd0.xsd";
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(res);
        String expected = IOUtils.toString(is, "UTF-8");

        // System.err.println(writer.toString());

        assertEquals("JAXB schema", expected, writer.toString());
    }

}
