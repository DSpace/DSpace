
package org.datadryad.test;

import com.lyncode.xoai.dataprovider.OAIRequestParameters;
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

public class ListMetadataFormatsTest extends DryadXoaiRegressionTest
{
    public ListMetadataFormatsTest() {
        super();
    }
    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testListSets() throws IOException, JDOMException {
        this.initProvider("","");

        Map<String, List<String>> map = new HashMap<String, List<String>>();
        map.put("verb", Arrays.asList("ListMetadataFormats"));
        OAIRequestParameters requestParameters = new OAIRequestParameters(map);

        OutputStream out = new ByteArrayOutputStream();
        String id = "foobarbazquux";
        XOAICacheManager.handle(id, dataProvider, requestParameters, out);
        out.flush();
        out.close();

        SAXBuilder builder = new SAXBuilder();
        Document resultDoc = builder.build(new StringReader(out.toString()));

        File expectedFile = new File(this.getClass().getResource("/responses/ListMetadataFormats.xml").getFile());
        Document expectedDoc = builder.build(new FileReader(expectedFile));

        assert(compareDocument(resultDoc, expectedDoc) == true);
    }
}
