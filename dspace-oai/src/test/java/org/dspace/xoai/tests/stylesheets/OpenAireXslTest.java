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
import static org.hamcrest.Matchers.containsString;

import org.dspace.xoai.tests.support.XmlMatcherBuilder;
import org.junit.Test;

public class OpenAireXslTest extends AbstractXSLTest {

    @Test
    public void openAireCanTransformOrcidAuthors() throws Exception {
        String result = apply("oai_openaire.xsl").to(resource("xoai-test2.xml"));

        // match title
        assertThat(result, openAire().withXPath(
                "/oaire:resource/datacite:titles/datacite:title",
                containsString("Test Item")));

        // match authors
        assertThat(result, openAire().withXPath(
                "/oaire:resource/datacite:creators/datacite:creator[1]/datacite:creatorName",
                containsString("PlainText, One")));
        assertThat(result, openAire().withXPath(
                "/oaire:resource/datacite:creators/datacite:creator[2]/datacite:creatorName",
                containsString("Smith, John")));
        assertThat(result, openAire().withXPath(
                "/oaire:resource/datacite:creators/datacite:creator[3]/datacite:creatorName",
                containsString("Doe, Jane")));
        assertThat(result, openAire().withXPath(
                "/oaire:resource/datacite:creators/datacite:creator[4]/datacite:creatorName",
                containsString("PlainText, Two")));

        // match ORCID IDs
        // first author shouldn't have an ORCID ID
        assertThat(result, openAire().withXPath(
                "/oaire:resource/datacite:creators/datacite:creator[1]/datacite:nameIdentifier" +
                        "[@schemeURI='http://orcid.org']", containsString("")));
        assertThat(result, openAire().withXPath(
                "/oaire:resource/datacite:creators/datacite:creator[2]/datacite:nameIdentifier" +
                        "[@schemeURI='http://orcid.org']", containsString("0000-0000-0000-1234")));
        assertThat(result, openAire().withXPath(
                "/oaire:resource/datacite:creators/datacite:creator[3]/datacite:nameIdentifier" +
                        "[@schemeURI='http://orcid.org']", containsString("0000-0000-0000-5678")));
        // final author shouldn't have an ORCID ID
        assertThat(result, openAire().withXPath(
                "/oaire:resource/datacite:creators/datacite:creator[1]/datacite:nameIdentifier" +
                        "[@schemeURI='http://orcid.org']", containsString("")));

    }

    private XmlMatcherBuilder openAire() {
        return xml().withNamespace("oaire", "http://namespace.openaire.eu/schema/oaire/")
                    .withNamespace("datacite", "http://datacite.org/schema/kernel-4")
                    .withNamespace("dc", "http://purl.org/dc/elements/1.1/")
                    .withNamespace("doc", "http://www.lyncode.com/xoai")
                    .withNamespace("rdf", "http://www.w3.org/TR/rdf-concepts/")
                    .withNamespace("vc", "http://www.w3.org/2007/XMLSchema-versioning")
                    .withNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    }
}
