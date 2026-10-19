/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.stylesheets;

import static org.dspace.xoai.tests.support.XmlMatcherBuilder.xml;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import org.dspace.xoai.tests.support.XmlMatcherBuilder;
import org.junit.Test;

public class RioxxXslTest extends AbstractXSLTest {
    @Test
    public void rioxxCanTransformInput() throws Exception {
        String result = apply("rioxx.xsl").to(resource("xoai-rioxx-test.xml"));

        assertThat(result, is(rioxx().withXPath("//dc:title", equalTo("The Intercorrelation Between " +
                "Executive Function, Physics Problem Solving, Mathematical, and Matrix Reasoning Skills: " +
                "Reflections from a Small-Scale Experiment"))));
    }

    private XmlMatcherBuilder rioxx() {
        return xml()
                .withNamespace("rioxx", "http://www.rioxx.net/schema/v3.0/rioxx/")
                .withNamespace("rioxxterms", "http://docs.rioxx.net/schema/v3.0/rioxxterms/")
                .withNamespace("dcterms", "http://purl.org/dc/terms/")
                .withNamespace("dc", "http://purl.org/dc/elements/1.1/");
    }
}
