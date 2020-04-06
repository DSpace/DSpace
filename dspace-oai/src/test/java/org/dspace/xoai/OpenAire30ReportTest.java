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
public class OpenAire30ReportTest {

    private static Map<String, List<String>> result;

    private final String dc;
    private final List<String> value;


    public OpenAire30ReportTest(String dc, List<String> value) {
        this.dc = dc;
        this.value = value;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        result = loadMetadata("/report.xml", TransformerType.OPENAIRE);
    }

    @Parameterized.Parameters(name = "{index}: transformTest({0}) = {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"dc:description", Collections.singletonList("Final project report.")},
                {"dc:type", Collections.singletonList("info:eu-repo/semantics/report")},
                {"dc:language", Collections.singletonList("eng")},
                {"dc:title", Arrays.asList("Final report for ESA Validation Data Centre, EVDC. 5 May 2015.", "Sluttrapport for ESA Validation Centre, EVDC.")},
                {"dc:date", Collections.singletonList("2015")},
                {"dc:relation", Arrays.asList("info:eu-repo/semantics/altIdentifier/isbn/978‐82‐425‐2761‐5", "NILU OR;9/2015", "European Space Agency: 103045")},
                {"dc:creator", Collections.singletonList("Fjæraa, Ann Mari")},
                {"dc:rights", Collections.singletonList("info:eu-repo/semantics/openAccess")},
                {"dc:identifier", Collections.singletonList("http://hdl.handle.net/11250/2382891")}
        });
    }

    @Test
    public void transformTest() throws Exception {
        assertMetadataFieldsSize(result.size(), data().size());
        assertThat(dc, result.get(dc), is(value));
    }

}