package org.dspace.xoai;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static org.dspace.xoai.XOAITestdataLoader.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class OpenAIRE30ChapterTest {

    private static Map<String, List<String>> result;

    private final String dc;
    private final List<String> value;

    public OpenAIRE30ChapterTest(String dc, List<String> value) {
        this.dc = dc;
        this.value = value;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        result = loadMetadata("/fp7Chapter.xml", TransformerType.OPENAIRE);
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
                {"dc:type", Arrays.asList("info:eu-repo/semantics/bookPart", "info:eu-repo/semantics/conferenceObject")},
                {"dc:language", Collections.singletonList("eng")},
                {"dc:title", Collections.singletonList("Prefetching query results and its impact on search engines")},
                {"dc:date", Collections.singletonList("2012")},
                {"dc:creator", Arrays.asList("Jonassen, Simon", "Cambazoglu, B. Barla", "Silvestri, Fabrizio")},
                {"dc:identifier", Collections.singletonList("http://hdl.handle.net/11250/2393863")},
                {"dc:publisher", Collections.singletonList("Association for Computing Machinery (ACM)")},
                {"dc:source", Collections.singletonList("631-640")},
                {"dc:relation", Arrays.asList("info:eu-repo/semantics/altIdentifier/doi/10.1145/2348283.2348368",
                        "info:eu-repo/semantics/altIdentifier/isbn/978-1-4503-1472-5", "info:eu-repo/grantAgreement/EC/FP7/123444")},
                {"dc:rights", Collections.singletonList("info:eu-repo/semantics/openAccess")}
        });
    }

    @Test
    public void transformTest() throws Exception {
        assertMetadataFieldsSize(result.size(), data().size());
        assertThat(dc, result.get(dc), is(value));
    }
}
