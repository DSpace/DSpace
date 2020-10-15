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
public class OaiDCJournalArticleTest {

    private static Map<String, List<String>> result;

    private final String dc;
    private final List<String> value;


    public OaiDCJournalArticleTest(String dc, List<String> value) {
        this.dc = dc;
        this.value = value;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        result = loadMetadata("/h2020JournalArticle.xml", TransformerType.REQUEST);
    }

    @Parameterized.Parameters(name = "{index}: transformTest({0}) = {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"dc:description", Arrays.asList("Dette er en beskrivelse.", "Dette er et sammendrag.", "Sonset av vaffelelskernes landsforening")},
                {"dc:language", Collections.singletonList("nob")},
                {"dc:subject", Arrays.asList("baking", "vaffeljern", "VDP::Samfunnsvitenskap: 200::Demografi: 300")},
                {"dc:coverage", Collections.singletonList("Norge, Trondheim")},
                {"dc:relation", Arrays.asList("Vaffelserie;9", "Dette er første del av dokumentet.", "Dette er andre del av dokumentet.", "EC/H2020/641918")},
                {"dc:creator", Arrays.asList("Knutsdotter, Kari", "Nordmann, Hans")},
                {"dc:rights", Arrays.asList("Navngivelse 4.0 Internasjonal", "http://creativecommons.org/licenses/by/4.0/deed.no", "Forfattelen har alle rettigheter til å smake alle vaffler som lages basert på egene oppskrifter.")},
                {"dc:type", Arrays.asList("Journal article", "Research report")},
                {"dc:title", Arrays.asList("Min beste vaffeloppskrift", "Alternativ tittel")},
                {"dc:contributor", Arrays.asList("Hansen, Bjørg", "Olsen, Ole")},
                {"dc:date", Arrays.asList("2016-11-15T08:33:24Z", "2016-11-15T08:33:24Z", "2016-11-01")},
                {"dc:identifier", Arrays.asList("Acta Orthopaedica. 2014, 85 (5), 463-469)", "urn:issn:1476-4687", "http://hdl.handle.net/11250.1/70", "https://doi.org/10.1016/j.erss.2015.11.003")},
                {"dc:publisher", Collections.singletonList("Tapir akademiske forlag")},
                {"dc:source", Arrays.asList("Vaffeltidsskriftet", "99", "9", "9-99")}
        });
    }

    @Test
    public void transformTest() throws Exception {
        assertMetadataFieldsSize(result.size(), data().size());
        assertThat(dc, result.get(dc), is(value));
    }

}
