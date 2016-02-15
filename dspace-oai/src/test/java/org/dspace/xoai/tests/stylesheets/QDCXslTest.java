/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.stylesheets;

import org.dspace.xoai.tests.support.XmlMatcherBuilder;
import org.junit.Test;

import static org.dspace.xoai.tests.support.XmlMatcherBuilder.xml;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class QDCXslTest extends AbstractXSLTest {

    @Test
    public void qdcCanTransformInput() throws Exception {
        String result = apply("qdc.xsl").to(resource("xoai-test1.xml"));

        assertThat(result, is(qdc().withXPath("//dc:title", equalTo("Test Webpage"))));
    }

    private XmlMatcherBuilder qdc () {
        return xml()
                .withNamespace("dqc", "http://dspace.org/qualifieddc/")
                .withNamespace("dcterms", "http://purl.org/dc/terms/")
                .withNamespace("dc", "http://purl.org/dc/elements/1.1/")
                ;
    }
}
