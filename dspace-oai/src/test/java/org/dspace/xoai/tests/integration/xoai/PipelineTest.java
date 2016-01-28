/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.tests.integration.xoai;

import com.lyncode.xoai.util.XSLPipeline;
import org.junit.Test;
import org.parboiled.common.FileUtils;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

import static com.lyncode.test.matchers.xml.XPathMatchers.xPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PipelineTest {
    private static TransformerFactory factory = TransformerFactory.newInstance();

    @Test
    public void pipelineTest () throws Exception {
        InputStream input = PipelineTest.class.getClassLoader().getResourceAsStream("item.xml");
        InputStream xslt = PipelineTest.class.getClassLoader().getResourceAsStream("oai_dc.xsl");
        String output = FileUtils.readAllText(new XSLPipeline(input, true)
                .apply(factory.newTransformer(new StreamSource(xslt)))
                .getTransformed());

        assertThat(output, xPath("/oai_dc:dc/dc:title", equalTo("Teste")));
        
        input.close();
        input = null;
        xslt.close();
        xslt = null;
        output = null;
    }
}
