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

import static org.dspace.xoai.XOAITestdataLoader.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class OpenAIRE30DdcBookTest {

    private static Map<String, List<String>> result;

    private final String dc;
    private final List<String> value;

    public OpenAIRE30DdcBookTest(String dc, List<String> value) {
        this.dc = dc;
        this.value = value;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        result = loadMetadata("/ddcBook.xml", TransformerType.OPENAIRE);
    }

    @Parameterized.Parameters(name = "{index}: transformTest({0}) = {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"dc:description", Arrays.asList("Catalogue published in connection with the exhibition The Porcelain Room\n" +
                        "                        (Porselensrommet), held at Galleri Format, Oslo, 27 October - 27 November 2016, and at Nordnorsk\n" +
                        "                        Kunstnersenter, Svolvær, 31 March - 28 May 2017. Texts: Knut Astrup Bull, Irene Nordli, Tyra T.\n" +
                        "                        Tronstad. Funded by Oslo National Academy of the Arts and Billedkunstnernes vederlagsfond.\n" +
                        "                    ", "Oslo National Academy of the Arts and Billedkunstnernes vederlagsfond.")},
                {"dc:type", Collections.singletonList("info:eu-repo/semantics/book")},
                {"dc:language", Collections.singletonList("eng")},
                {"dc:subject", Arrays.asList("keramikk", "ceramics", "info:eu-repo/classification/ddc/738")},
                {"dc:title", Collections.singletonList("The Porcelain Room. Porselensrommet")},
                {"dc:date", Collections.singletonList("2016")},
                {"dc:creator", Collections.singletonList("Nordli, Irene")},
                {"dc:rights", Collections.singletonList("info:eu-repo/semantics/openAccess")},
                {"dc:identifier", Arrays.asList("http://hdl.handle.net/11250/2428016")},
                {"dc:publisher", Collections.singletonList("Irene Nordli")},
                {"dc:source", Collections.singletonList("48")},
        });
    }

    @Test
    public void transformTest() throws Exception {
        assertMetadataFieldsSize(result.size(), data().size());
        assertThat(dc, result.get(dc), is(value));
    }
}
