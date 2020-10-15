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
public class OpenAIRE30OpenAccessTest {
    private static Map<String, List<String>> result;

    private final String dc;
    private final List<String> value;

    public OpenAIRE30OpenAccessTest(String dc, List<String> value) {
        this.dc = dc;
        this.value = value;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        result = loadMetadata("/cristinOpenAccess.xml", TransformerType.OPENAIRE);
    }

    @Parameterized.Parameters(name = "{index}: transformTest({0}) = {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"dc:type", Arrays.asList("info:eu-repo/semantics/article", "info:eu-repo/semantics/publishedVersion")},
                {"dc:language", Collections.singletonList("EN")},
                {"dc:title", Collections.singletonList("ApoptoProteomics, an Integrated Database for Analysis of Proteomics Data Obtained\n" +
                        "                    from Apoptotic Cells\n" +
                        "                ")},
                {"dc:date", Collections.singletonList("2012")},
                {"dc:creator", Arrays.asList("Arntzen, Magnus", "Thiede, Bernd")},
                {"dc:identifier", Collections.singletonList("http://hdl.handle.net/11250.1/12615")},
                {"dc:publisher", Collections.singletonList("American Society for Biochemistry and Molecular Biology")},
                {"dc:source", Arrays.asList("Molecular & Cellular Proteomics", "11", "2", "15")},
                {"dc:rights", Collections.singletonList("info:eu-repo/semantics/openAccess")},
                {"dc:relation", Arrays.asList("info:eu-repo/semantics/altIdentifier/issn/1535-9476",
                        "info:eu-repo/semantics/altIdentifier/doi/10.1074/mcp.M111.010447")}
        });
    }

    @Test
    public void transformTest() throws Exception {
        assertMetadataFieldsSize(result.size(), data().size());
        assertThat(dc, result.get(dc), is(value));
    }
}