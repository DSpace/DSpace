package org.dspace.xoai;


import com.lyncode.xoai.dataprovider.core.XOAIContext;
import com.lyncode.xoai.dataprovider.filter.Scope;
import org.dspace.xoai.services.api.xoai.DSpaceFilterResolver;
import org.junit.Test;

import static org.dspace.xoai.XOAITestdataLoader.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OpenAIRE30FilterTest {

    @Test
    public void testSolrFilter() throws Exception{
        DSpaceFilterResolver filterResolver = filterResolver();
        XOAIContext xoaiContext = oaiContext(TransformerType.OPENAIRE);

        String expected = "((((metadata.dc.title:[* TO *])) AND ((metadata.dc.contributor.author:[* TO *]))) AND ((*:* AND NOT((metadata.dc.rights.accessrights:*closed\\ access*) OR (metadata.dc.rights.accessrights:*closedAccess*) OR (metadata.dc.rights.accessrights:*embargoed\\ access*) OR (metadata.dc.rights.accessrights:*embargoedAccess*) OR (metadata.dc.rights.accessrights:*restricted\\ access*) OR (metadata.dc.rights.accessrights:*restrictedAccess*))) AND (((item.public:true) OR (item.deleted:true)) OR ((metadata.dc.relation.project:EC\\/FP*) OR (metadata.dc.relation.project:EC\\/H2020*) OR (metadata.dc.relation.project:info\\:eu\\-repo\\/grantAgreement\\/EC\\/FP*) OR (metadata.dc.relation.project:info\\:eu\\-repo\\/grantAgreement\\/EC\\/H2020*)))))";
        String actual = filterResolver.buildSolrQuery(Scope.Context, xoaiContext.getCondition());

        assertThat("SOLR driverFilter", actual, is(expected));
    }
}
