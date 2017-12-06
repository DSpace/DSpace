/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.integration.xoai;

import org.junit.Test;

import static org.dspace.xoai.tests.helpers.SyntacticSugar.and;
import static org.dspace.xoai.tests.helpers.SyntacticSugar.given;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ListSetsTest extends AbstractDSpaceTest {
    @Test
    public void listSetsWithLessSetsThenMaxSetsPerPage () throws Exception {
        given(theConfiguration()
                .withMaxListSetsSize(100)
                .withContextConfigurations(aContext("request")));
        and(given(theSetRepository()
                .doesSupportSets()
                .withSet("name", "spec")));

        againstTheDataProvider().perform(get("/request?verb=ListSets"))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(responseDate().exists())
                .andExpect(verb(is("ListSets")))
                .andExpect(oaiXPath("//set").nodeCount(1))
                .andExpect(oaiXPath("//set/setSpec").string("spec"))
                .andExpect(oaiXPath("//set/setName").string("name"))
                .andExpect(resumptionToken().doesNotExist());
    }

    @Test
    public void listSetsWithMoreSetsThenMaxSetsPerPage () throws Exception {
        given(theConfiguration()
                .withMaxListSetsSize(10)
                .withContextConfigurations(aContext("request")));

        and(given(theSetRepository()
                .doesSupportSets()
                .withRandomlyGeneratedSets(20)));

        againstTheDataProvider().perform(get("/request?verb=ListSets"))
                .andExpect(status().isOk())
                .andExpect(responseDate().exists())
                .andExpect(verb(is("ListSets")))
                .andExpect(oaiXPath("//set").nodeCount(10))
                .andExpect(resumptionToken().string("////10"))
                .andExpect(oaiXPath("//resumptionToken/@completeListSize").number(Double.valueOf(20)));

        and(againstTheDataProvider().perform(get("/request?verb=ListSets&resumptionToken=////10"))
                .andExpect(status().isOk())
                .andExpect(responseDate().exists())
                .andExpect(verb(is("ListSets")))
                .andExpect(oaiXPath("//set").nodeCount(10))
                .andExpect(resumptionToken().string("")));
    }
}
