package org.dspace.xoai;

import com.lyncode.xoai.dataprovider.core.XOAIContext;
import com.lyncode.xoai.dataprovider.filter.Scope;
import org.dspace.xoai.services.api.xoai.DSpaceFilterResolver;
import org.junit.Test;

import static org.dspace.xoai.XOAITestdataLoader.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OpenAIREBasicFilterTest {

    @Test
    public void testSolrFilter() throws Exception{
        DSpaceFilterResolver filterResolver = filterResolver();
        XOAIContext xoaiContext = oaiContext(TransformerType.DRIVER);

        String expected = "((((metadata.dc.title:[* TO *])) AND ((metadata.dc.contributor.author:[* TO *]))) AND (((metadata.dc.type:*article) OR (metadata.dc.type:*bachelorThesis) OR (metadata.dc.type:*masterThesis) OR (metadata.dc.type:*doctoralThesis) OR (metadata.dc.type:*book) OR (metadata.dc.type:*bookPart) OR (metadata.dc.type:*review) OR (metadata.dc.type:*conferenceObject) OR (metadata.dc.type:*lecture) OR (metadata.dc.type:*workingPaper) OR (metadata.dc.type:*preprint) OR (metadata.dc.type:*report) OR (metadata.dc.type:*annotation) OR (metadata.dc.type:*contributionToPeriodical) OR (metadata.dc.type:*patent) OR (metadata.dc.type:*dataset) OR (metadata.dc.type:*other)) AND (((item.public:true) OR (item.deleted:true)) AND ((metadata.dc.rights:*open\\ access*) OR (metadata.dc.rights:*openAccess*)))))";

        String actual = filterResolver.buildSolrQuery(Scope.Context, xoaiContext.getCondition());
        assertThat("SOLR driverFilter", actual, is(expected));
    }
}
