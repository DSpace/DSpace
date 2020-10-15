/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class OaiDCChapterTest {


    private static Map<String, List<String>> result;

    private final String dc;
    private final List<String> value;

    public OaiDCChapterTest(String dc, List<String> value) {
        this.dc = dc;
        this.value = value;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        result = XOAITestdataLoader.loadMetadata("/fp7Chapter.xml", XOAITestdataLoader.TransformerType.REQUEST);
    }

    @Parameterized.Parameters(name = "{index}: transformTest({0}) = {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"dc:description", Arrays.asList("We investigate the impact of query result prefetching on the efficiency and\n" +
                        "                        effectiveness of web search engines. We propose offline and online strategies for selecting and\n" +
                        "                        ordering queries whose results are to be prefetched. The offline strategies rely on query log\n" +
                        "                        analysis and the queries are selected from the queries issued on the previous day. The online\n" +
                        "                        strategies select the queries from the result cache, relying on a machine learning model that\n" +
                        "                        estimates the arrival times of queries. We carefully evaluate the proposed prefetching\n" +
                        "                        techniques via simulation on a query log obtained from Yahoo! web search. We demonstrate that\n" +
                        "                        our strategies are able to improve various performance metrics, including the hit rate, query\n" +
                        "                        response time, result freshness, and query degradation rate, relative to a state-of-the-art\n" +
                        "                        baseline.\n" +
                        "                    ",
                        "© Jonassen, Cambazoglu & Silvestri | ACM 2012. This is the author's version\n" +
                        "                        of the work. It is posted here for your personal use. Not for redistribution. The definitive\n" +
                        "                        Version of Record was published in SIGIR '12 Proceedings of the 35th international ACM SIGIR\n" +
                        "                        conference on Research and development in information retrieval,\n" +
                        "                        http://dx.doi.org/10.1145/2348283.2348368\n" +
                        "                    ")},
                {"dc:type", Arrays.asList("Chapter", "Conference object", "Peer reviewed")},
                {"dc:language", Collections.singletonList("eng")},
                {"dc:title", Collections.singletonList("Prefetching query results and its impact on search engines")},
                {"dc:date", Arrays.asList("2013-01-12T15:22:51Z", "2016-06-23T10:56:52Z", "2017-04-19T10:37:37Z", "2013-01-12T15:22:51Z",
                        "2016-06-23T10:56:52Z", "2017-04-19T10:37:37Z", "2012", "2013-01-12T15:22:51Z")},
                {"dc:creator", Arrays.asList("Jonassen, Simon", "Cambazoglu, B. Barla", "Silvestri, Fabrizio")},
                {"dc:identifier", Arrays.asList("Hersh, William R. [Eds.] The 35th International ACM SIGIR conference on research\n" +
                        "                        and development in Information Retrieval, SIGIR '12, Portland, OR, USA, August 12-16, 2012 p.\n" +
                        "                        631-640\n" +
                        "                    ", "urn:isbn:978-1-4503-1472-5", "http://hdl.handle.net/11250/2393863", "cristin:986632", "http://dx.doi.org/10.1145/2348283.2348368")},
                {"dc:publisher", Collections.singletonList("Association for Computing Machinery (ACM)")},
                {"dc:source", Collections.singletonList("631-640")},
                {"dc:relation", Collections.singletonList("EC/FP7/123444")}
        });
    }

    @Test
    public void transformTest() throws Exception {
        XOAITestdataLoader.assertMetadataFieldsSize(result.size(), data().size());
        assertThat(dc, result.get(dc), is(value));
    }
}
