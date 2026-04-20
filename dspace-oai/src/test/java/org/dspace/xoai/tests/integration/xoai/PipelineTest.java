/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.tests.integration.xoai;

import static org.dspace.xoai.tests.support.XmlMatcherBuilder.xml;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.InputStream;
import java.nio.charset.Charset;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import com.lyncode.xoai.util.XSLPipeline;
import org.apache.commons.io.IOUtils;
import org.dspace.xoai.tests.support.XmlMatcherBuilder;
import org.junit.Test;

public class PipelineTest {
    private static TransformerFactory factory = TransformerFactory.newInstance();

    @Test
    public void pipelineTest() throws Exception {
        InputStream input = PipelineTest.class.getClassLoader().getResourceAsStream("item.xml");
        InputStream xslt = PipelineTest.class.getClassLoader().getResourceAsStream("oai_dc.xsl");
        String output = IOUtils.toString(new XSLPipeline(input, true)
                                             .apply(factory.newTemplates(new StreamSource(xslt)))
                                             .getTransformed(), Charset.defaultCharset());

        assertThat(output, oai_dc().withXPath("/oai_dc:dc/dc:title", equalTo("Teste")));

        input.close();
        input = null;
        xslt.close();
        xslt = null;
        output = null;
    }

    private XmlMatcherBuilder oai_dc() {
        return xml()
            .withNamespace("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/")
            .withNamespace("dc", "http://purl.org/dc/elements/1.1/");
    }

}
