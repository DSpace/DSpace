
package org.datadryad.test;

import com.lyncode.xoai.dataprovider.OAIRequestParameters;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.xoai.util.XOAICacheManager;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListIdentifiersTest extends DryadXoaiRegressionTest
{
    public ListIdentifiersTest() {
        super();
    }
    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testListIdentifiers() throws IOException, JDOMException, SolrServerException {
        String path = this.getClass().getResource("/solr/ListIdentifiers.xml").getFile();
        this.setQueryResponse(path, "utf-8");
        this.initProvider("","");

        Map<String, List<String>> map = new HashMap<String, List<String>>();
        map.put("verb", Arrays.asList("ListIdentifiers"));
        map.put("metadataPrefix", Arrays.asList("oai_dc"));
        OAIRequestParameters requestParameters = new OAIRequestParameters(map);

        OutputStream out = new ByteArrayOutputStream();
        String id = "foobarbazquux";
        XOAICacheManager.handle(id, dataProvider, requestParameters, out);
        out.flush();
        out.close();

        SAXBuilder builder = new SAXBuilder();
        Document resultDoc = builder.build(new StringReader(out.toString()));

        File expectedFile = new File(this.getClass().getResource("/responses/ListIdentifiers.xml").getFile());
        Document expectedDoc = builder.build(new FileReader(expectedFile));

        assert(compareDocument(resultDoc, expectedDoc) == true);
    }
}
